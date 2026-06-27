# Login User Isolation Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add login and enforce per-user isolation for resumes, interviews, knowledge bases, and RAG chat.

**Architecture:** Use a small first-party auth module with opaque Bearer tokens stored as hashes in the database. Add owner fields to top-level aggregates and update repository/service methods so every user-facing lookup is scoped to the current user. The frontend gets an auth provider, protected routes, and token propagation for Axios, fetch streaming, and blob downloads.

**Tech Stack:** Spring Boot 4, Spring MVC, Spring Data JPA, Java 21, React, Vite, TypeScript, Tailwind CSS.

---

## File Structure
- Create `app/src/main/java/interview/guide/modules/auth/**`: user/session entities, DTOs, repositories, service, password/token helpers, current user context, controller, interceptor.
- Modify `app/src/main/java/interview/guide/common/config/CorsConfig.java`: register auth interceptor while keeping CORS behavior.
- Modify resume, interview, knowledge base, and RAG chat entities/repositories/services/controllers to use current-user ownership.
- Create focused tests under `app/src/test/java/interview/guide/modules/auth/**` and ownership tests near affected modules.
- Create `frontend/src/api/auth.ts`, `frontend/src/auth/AuthContext.tsx`, `frontend/src/auth/ProtectedRoute.tsx`, and `frontend/src/pages/LoginPage.tsx`.
- Modify `frontend/src/App.tsx`, `frontend/src/components/Layout.tsx`, `frontend/src/api/request.ts`, `frontend/src/api/ragChat.ts`, and `frontend/src/api/knowledgebase.ts`.

## Chunk 1: Backend Auth Core
### Task 1: Auth service behavior

**Files:**
- Create: `app/src/test/java/interview/guide/modules/auth/service/AuthServiceTest.java`
- Create: `app/src/main/java/interview/guide/modules/auth/model/UserEntity.java`
- Create: `app/src/main/java/interview/guide/modules/auth/model/AuthSessionEntity.java`
- Create: `app/src/main/java/interview/guide/modules/auth/repository/UserRepository.java`
- Create: `app/src/main/java/interview/guide/modules/auth/repository/AuthSessionRepository.java`
- Create: `app/src/main/java/interview/guide/modules/auth/service/AuthService.java`
- Create: `app/src/main/java/interview/guide/modules/auth/service/PasswordHasher.java`
- Create: `app/src/main/java/interview/guide/modules/auth/service/AuthTokenService.java`

- [ ] **Step 1: Write failing tests**
  - Register returns user data and a Bearer token.
  - Login rejects a wrong password.
  - Token validation rejects revoked/expired sessions.

- [ ] **Step 2: Run test to verify RED**
  - Run: `.\gradlew.bat :app:test --tests "interview.guide.modules.auth.service.AuthServiceTest" --no-daemon --console=plain`
  - Expected: fail because auth classes do not exist.

- [ ] **Step 3: Implement minimal auth core**
  - Add PBKDF2 password hashing.
  - Add random opaque token issuing and SHA-256 storage.
  - Add session revocation for logout.

- [ ] **Step 4: Run test to verify GREEN**
  - Run the same targeted auth test.
  - Expected: pass.

### Task 2: HTTP auth boundary

**Files:**
- Create: `app/src/test/java/interview/guide/modules/auth/AuthControllerIntegrationTest.java`
- Create: `app/src/main/java/interview/guide/modules/auth/AuthController.java`
- Create: `app/src/main/java/interview/guide/modules/auth/AuthInterceptor.java`
- Create: `app/src/main/java/interview/guide/modules/auth/CurrentUserContext.java`
- Modify: `app/src/main/java/interview/guide/common/config/CorsConfig.java`

- [ ] **Step 1: Write failing integration tests**
  - `/api/auth/register` succeeds without an existing token.
  - A protected endpoint rejects requests without `Authorization`.
  - The same protected endpoint accepts a valid Bearer token.

- [ ] **Step 2: Run test to verify RED**
  - Run: `.\gradlew.bat :app:test --tests "interview.guide.modules.auth.AuthControllerIntegrationTest" --no-daemon --console=plain`

- [ ] **Step 3: Implement controller/interceptor**
  - Allow `OPTIONS` and `/api/auth/**`.
  - Set and clear current user around protected API requests.
  - Serialize auth failures as `Result.error(...)`.

- [ ] **Step 4: Run test to verify GREEN**
  - Run the same targeted integration test.

## Chunk 2: Backend Ownership
### Task 3: Resume ownership

**Files:**
- Create/modify tests near `app/src/test/java/interview/guide/modules/resume/**`.
- Modify `ResumeEntity`, `ResumeRepository`, `ResumePersistenceService`, `ResumeHistoryService`, and delete/detail paths.

- [ ] **Step 1: Write failing tests for same-user duplicate and list filtering**
- [ ] **Step 2: Run targeted resume tests and confirm RED**
- [ ] **Step 3: Add owner field and owner-scoped repository methods**
- [ ] **Step 4: Run targeted resume tests and confirm GREEN**

### Task 4: Knowledge base ownership

**Files:**
- Modify tests near `app/src/test/java/interview/guide/modules/knowledgebase/**`.
- Modify `KnowledgeBaseEntity`, `KnowledgeBaseRepository`, `KnowledgeBasePersistenceService`, `KnowledgeBaseListService`, `KnowledgeBaseDeleteService`, query services, and count/stat paths.

- [ ] **Step 1: Write failing tests for same-user duplicate, list/search/stat filtering, and selected-id validation**
- [ ] **Step 2: Run targeted knowledge base tests and confirm RED**
- [ ] **Step 3: Add owner field and owner-scoped repository/service logic**
- [ ] **Step 4: Run targeted knowledge base tests and confirm GREEN**

### Task 5: Interview and RAG chat ownership

**Files:**
- Modify tests near `app/src/test/java/interview/guide/modules/interview/**` and `app/src/test/java/interview/guide/modules/knowledgebase/**`.
- Modify `InterviewSessionEntity`, `InterviewSessionRepository`, interview services/controllers, `RagChatSessionEntity`, `RagChatSessionRepository`, and `RagChatSessionService`.

- [ ] **Step 1: Write failing tests for cross-user session denial**
- [ ] **Step 2: Run targeted tests and confirm RED**
- [ ] **Step 3: Add direct owner fields and owner-scoped lookups**
- [ ] **Step 4: Run targeted tests and confirm GREEN**

## Chunk 3: Frontend Auth UI
### Task 6: Login/register and app protection

**Files:**
- Create `frontend/src/api/auth.ts`
- Create `frontend/src/auth/AuthContext.tsx`
- Create `frontend/src/auth/ProtectedRoute.tsx`
- Create `frontend/src/pages/LoginPage.tsx`
- Modify `frontend/src/App.tsx`
- Modify `frontend/src/components/Layout.tsx`
- Modify `frontend/src/api/request.ts`
- Modify `frontend/src/api/ragChat.ts`
- Modify `frontend/src/api/knowledgebase.ts`

- [ ] **Step 1: Add frontend auth API and context**
- [ ] **Step 2: Add protected routes and login page**
- [ ] **Step 3: Add token headers to Axios, fetch streaming, and downloads**
- [ ] **Step 4: Run frontend build**
  - Run: `pnpm --dir frontend build`
  - Expected: TypeScript and Vite build pass.

## Chunk 4: Verification
- [ ] Run backend targeted tests.
- [ ] Run `.\gradlew.bat :app:test --no-daemon --console=plain` if runtime is reasonable.
- [ ] Run `pnpm --dir frontend build`.
- [ ] Update `task_plan.md`, `findings.md`, and `progress.md` with final status and any limitations.
- [ ] Report LightRAG shared-workspace limitation clearly.
