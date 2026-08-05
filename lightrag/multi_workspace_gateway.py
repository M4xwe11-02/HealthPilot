"""Route each application user to an isolated LightRAG server process."""

from __future__ import annotations

import json
import logging
import os
import re
import signal
import socket
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.request
from dataclasses import dataclass
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import BinaryIO


LOG = logging.getLogger("lightrag-workspace-gateway")
WORKSPACE_PATTERN = re.compile(r"user_[1-9][0-9]*\Z")
HOP_BY_HOP_HEADERS = {
    "connection",
    "keep-alive",
    "proxy-authenticate",
    "proxy-authorization",
    "te",
    "trailer",
    "transfer-encoding",
    "upgrade",
}


@dataclass
class WorkspaceProcess:
    process: subprocess.Popen[bytes]
    port: int
    last_used: float
    active_requests: int = 0


class WorkspaceProcessManager:
    def __init__(self) -> None:
        self.startup_timeout = float(os.getenv("LIGHTRAG_WORKSPACE_STARTUP_TIMEOUT", "120"))
        self.idle_timeout = float(os.getenv("LIGHTRAG_WORKSPACE_IDLE_TIMEOUT", "1800"))
        self.max_processes = int(os.getenv("LIGHTRAG_MAX_WORKSPACE_PROCESSES", "16"))
        self._lock = threading.RLock()
        self._processes: dict[str, WorkspaceProcess] = {}
        self._closed = threading.Event()
        self._reaper = threading.Thread(
            target=self._reap_loop,
            name="lightrag-workspace-reaper",
            daemon=True,
        )
        self._reaper.start()

    def acquire(self, workspace: str) -> int:
        with self._lock:
            self._remove_dead_locked()
            managed = self._processes.get(workspace)
            if managed is None:
                self._make_capacity_locked()
                managed = self._start_locked(workspace)
                self._processes[workspace] = managed
            managed.active_requests += 1
            managed.last_used = time.monotonic()
            return managed.port

    def release(self, workspace: str) -> None:
        with self._lock:
            managed = self._processes.get(workspace)
            if managed is None:
                return
            managed.active_requests = max(0, managed.active_requests - 1)
            managed.last_used = time.monotonic()

    def active_workspace_count(self) -> int:
        with self._lock:
            self._remove_dead_locked()
            return len(self._processes)

    def close(self) -> None:
        self._closed.set()
        with self._lock:
            for workspace in list(self._processes):
                self._stop_locked(workspace)

    def _start_locked(self, workspace: str) -> WorkspaceProcess:
        port = self._find_free_port()
        environment = os.environ.copy()
        environment.update({"HOST": "127.0.0.1", "PORT": str(port), "WORKSPACE": workspace})
        command = [sys.executable, "-m", "lightrag.api.lightrag_server"]
        LOG.info("Starting isolated LightRAG workspace=%s port=%s", workspace, port)
        process = subprocess.Popen(command, env=environment, start_new_session=True)

        deadline = time.monotonic() + self.startup_timeout
        while time.monotonic() < deadline:
            exit_code = process.poll()
            if exit_code is not None:
                raise RuntimeError(
                    f"LightRAG workspace {workspace} exited during startup (code {exit_code})"
                )
            try:
                with socket.create_connection(("127.0.0.1", port), timeout=0.5):
                    return WorkspaceProcess(process=process, port=port, last_used=time.monotonic())
            except OSError:
                time.sleep(0.25)

        self._terminate(process)
        raise TimeoutError(f"LightRAG workspace {workspace} did not start in time")

    def _make_capacity_locked(self) -> None:
        if len(self._processes) < self.max_processes:
            return
        idle = [
            (workspace, managed)
            for workspace, managed in self._processes.items()
            if managed.active_requests == 0
        ]
        if not idle:
            raise RuntimeError("All LightRAG workspace processes are busy")
        workspace, _ = min(idle, key=lambda item: item[1].last_used)
        self._stop_locked(workspace)

    def _remove_dead_locked(self) -> None:
        for workspace, managed in list(self._processes.items()):
            if managed.process.poll() is not None:
                LOG.warning("LightRAG workspace process stopped unexpectedly: %s", workspace)
                self._processes.pop(workspace, None)

    def _reap_loop(self) -> None:
        interval = min(60.0, max(5.0, self.idle_timeout / 2))
        while not self._closed.wait(interval):
            now = time.monotonic()
            with self._lock:
                for workspace, managed in list(self._processes.items()):
                    if managed.active_requests == 0 and now - managed.last_used >= self.idle_timeout:
                        LOG.info("Stopping idle LightRAG workspace=%s", workspace)
                        self._stop_locked(workspace)

    def _stop_locked(self, workspace: str) -> None:
        managed = self._processes.pop(workspace, None)
        if managed is not None:
            self._terminate(managed.process)

    @staticmethod
    def _terminate(process: subprocess.Popen[bytes]) -> None:
        if process.poll() is not None:
            return
        process.terminate()
        try:
            process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=5)

    @staticmethod
    def _find_free_port() -> int:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as candidate:
            candidate.bind(("127.0.0.1", 0))
            return int(candidate.getsockname()[1])


class WorkspaceGatewayHandler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"
    manager: WorkspaceProcessManager
    upstream_timeout = float(os.getenv("LIGHTRAG_UPSTREAM_TIMEOUT", "600"))

    def do_GET(self) -> None:  # noqa: N802
        self._proxy()

    def do_POST(self) -> None:  # noqa: N802
        self._proxy()

    def do_DELETE(self) -> None:  # noqa: N802
        self._proxy()

    def do_PUT(self) -> None:  # noqa: N802
        self._proxy()

    def do_PATCH(self) -> None:  # noqa: N802
        self._proxy()

    def do_OPTIONS(self) -> None:  # noqa: N802
        self._proxy()

    def _proxy(self) -> None:
        workspace = self.headers.get("LIGHTRAG-WORKSPACE", "").strip()
        if not workspace:
            if self.command == "GET" and self.path.rstrip("/") == "/health":
                self._send_json(
                    200,
                    {
                        "status": "healthy",
                        "service": "lightrag-workspace-gateway",
                        "active_workspaces": self.manager.active_workspace_count(),
                    },
                )
                return
            self._send_json(400, {"detail": "LIGHTRAG-WORKSPACE header is required"})
            return
        if not WORKSPACE_PATTERN.fullmatch(workspace):
            self._send_json(400, {"detail": "Invalid LightRAG workspace"})
            return

        try:
            port = self.manager.acquire(workspace)
        except Exception as error:
            LOG.exception("Unable to acquire LightRAG workspace=%s", workspace)
            self._send_json(503, {"detail": str(error)})
            return

        try:
            self._forward(workspace, port)
        finally:
            self.manager.release(workspace)

    def _forward(self, workspace: str, port: int) -> None:
        content_length = int(self.headers.get("Content-Length", "0") or 0)
        body = self.rfile.read(content_length) if content_length else None
        headers = {
            key: value
            for key, value in self.headers.items()
            if key.lower() not in HOP_BY_HOP_HEADERS
            and key.lower() not in {"host", "content-length", "lightrag-workspace"}
        }
        request = urllib.request.Request(
            f"http://127.0.0.1:{port}{self.path}",
            data=body,
            headers=headers,
            method=self.command,
        )

        try:
            response = urllib.request.urlopen(request, timeout=self.upstream_timeout)
        except urllib.error.HTTPError as error:
            response = error
        except (OSError, TimeoutError) as error:
            LOG.warning("LightRAG upstream failed workspace=%s: %s", workspace, error)
            self._send_json(502, {"detail": "LightRAG workspace is unavailable"})
            return

        with response:
            self.send_response(response.status)
            for key, value in response.headers.items():
                if key.lower() not in HOP_BY_HOP_HEADERS and key.lower() not in {
                    "content-length",
                    "date",
                    "server",
                }:
                    self.send_header(key, value)
            content_length_header = response.headers.get("Content-Length")
            if content_length_header:
                self.send_header("Content-Length", content_length_header)
            else:
                self.send_header("Connection", "close")
                self.close_connection = True
            self.end_headers()
            self._copy_response(response)

    def _copy_response(self, response: BinaryIO) -> None:
        try:
            while chunk := response.read(8192):
                self.wfile.write(chunk)
                self.wfile.flush()
        except (BrokenPipeError, ConnectionResetError):
            pass

    def _send_json(self, status: int, payload: dict[str, object]) -> None:
        body = json.dumps(payload, ensure_ascii=True).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Connection", "close")
        self.end_headers()
        self.wfile.write(body)
        self.close_connection = True

    def log_message(self, message: str, *args: object) -> None:
        LOG.info("%s - %s", self.address_string(), message % args)


class GatewayServer(ThreadingHTTPServer):
    allow_reuse_address = True
    daemon_threads = True


def main() -> None:
    logging.basicConfig(
        level=os.getenv("LOG_LEVEL", "INFO").upper(),
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
    )
    manager = WorkspaceProcessManager()
    WorkspaceGatewayHandler.manager = manager
    host = os.getenv("LIGHTRAG_GATEWAY_HOST", "0.0.0.0")
    port = int(os.getenv("LIGHTRAG_GATEWAY_PORT", "9621"))
    server = GatewayServer((host, port), WorkspaceGatewayHandler)

    def stop_server(_signum: int, _frame: object) -> None:
        threading.Thread(target=server.shutdown, daemon=True).start()

    signal.signal(signal.SIGTERM, stop_server)
    signal.signal(signal.SIGINT, stop_server)
    LOG.info("LightRAG workspace gateway listening on %s:%s", host, port)
    try:
        server.serve_forever(poll_interval=0.5)
    finally:
        server.server_close()
        manager.close()


if __name__ == "__main__":
    main()
