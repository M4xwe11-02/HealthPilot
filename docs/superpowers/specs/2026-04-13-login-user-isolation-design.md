# Login And User Isolation Design

## Goal
Build a style-consistent login flow and enforce per-user isolation for resume library data, mock interview records, knowledge bases, and RAG chat sessions.

## Approved Direction
- Use first-party username/password auth instead of OAuth.
- Issue opaque Bearer tokens after login.
- Store tokens in the frontend and attach them to every API call.
- Require auth for `/api/**` except `/api/auth/**`.
- Enforce ownership in backend repositories/services, not only in React routes.
- Existing unowned local records are claimed by the first registered user.

## Backend Shape
- Add an `auth` module with `UserEntity`, `AuthSessionEntity`, repositories, password hashing, token issuing, auth service, controller, current-user context, and MVC interceptor.
- Store passwords with PBKDF2 and per-user salts.
- Store only a SHA-256 hash of each session token.
- Implement logout by revoking the matching auth session.
- Return existing `Result<T>` envelopes and reuse `UNAUTHORIZED` / `FORBIDDEN` error codes.

## Data Ownership
- Add owner links to:
  - `ResumeEntity`
  - `KnowledgeBaseEntity`
  - `InterviewSessionEntity`
  - `RagChatSessionEntity`
- Keep answer/message isolation through their owning session.
- Change duplicate checks from global `fileHash` to same-owner `fileHash`.
- Update list/detail/delete/search/stats/session queries to filter by current user.

## Frontend Shape
- Add `AuthProvider`, protected routes, and a login/register page outside the sidebar app shell.
- Keep the authenticated app inside the existing `Layout`.
- Add a compact user/logout area to the sidebar.
- Attach the Bearer token in the shared Axios client and in raw `fetch`/Axios paths used for streaming and downloads.

## LightRAG Boundary
- Validate all selected knowledge-base ids against the current user before local vector or LightRAG queries.
- This gives backend-level access control for knowledge base selection.
- Strict LightRAG workspace isolation remains a later enhancement because the current LightRAG integration queries one shared workspace.
