# Health Guardian Migration Findings

## Requirements
- User wants the remaining interview/resume scenario text converted to medical health management.
- User specifically wants prompts fixed, then verification to see whether the project can run.
- Current worktree already has many health changes, but some files are inconsistent.

## Research Findings
- Core health-report analysis prompts are already medical: `resume-analysis-system.st` and `resume-analysis-user.st`.
- AI consultation question/evaluation prompts are already mostly medical: `interview-question-*` and `interview-evaluation-*`.
- Health knowledge-base RAG prompts are still generic and lack medical safety boundaries.
- README.md still describes an AI interview platform and uses the old `interview-guide` bucket.
- README.txt and .env.example still contain old `interview-guide` examples.
- Docker Compose and application.yml already use many health-guardian names.
- Frontend files have been partially converted, but source still needs a focused user-facing text scan.
- Backend code still contains old comments/logs/API names. Package/API/table renaming is intentionally out of scope for this pass.
- `InterviewQuestionService.java` currently has invalid curly quote text in `buildDefaultFollowUp`, which must be fixed for backend compilation.
- Question type values are still old technical names in Java enum, prompt templates, and frontend TypeScript type.

## Technical Decisions
| Decision | Rationale |
|----------|-----------|
| Use health-guardian as the default product/storage name | Matches current docker-compose.yml and application.yml direction. |
| Keep persisted score field names such as `projectScore` | Avoids database/entity/API migrations while changing labels and prompts. |
| Change prompt question type labels only if code impact is narrow | The enum is serialized in AI output, so changes must include prompts, Java enum, and fallback mappings together. |

## Issues Encountered
| Issue | Resolution |
|-------|------------|
| Prior plan said all prompts were complete, but knowledge-base prompts were still generic | Marked migration as incomplete and added a dedicated prompt phase. |
| PowerShell profile warnings are printed on every command | Ignore unless a command actually fails. |
| Medical prompt migration partially changed labels while retaining old enum values | Update Java enum, prompt constraints, frontend type, and default fallback questions together. |

## Resources
- `app/src/main/resources/prompts/`
- `frontend/src/`
- `app/src/main/java/interview/guide/modules/`
- `README.md`, `README.txt`, `.env.example`, `docker-compose.yml`

## Mobile Responsive Audit (2026-07-12)

### Requirements
- User wants every frontend page audited and fixed for mobile browsers.
- Screenshot confirms the shared upload drop zone collapsed its explanatory text into a one-character-wide vertical column.
- Mobile navigation already changes to a compact top bar and five-item bottom navigation.

### Visual/Browser Findings
- On an approximately 472 CSS-pixel mobile viewport, header and bottom navigation render correctly.
- `FileUploadCard` retained a desktop horizontal row; its icon and wide CTA consumed the row, collapsing the text column vertically and making the card excessively tall.
- The environment has no connected browser runtime, so authenticated visual QA must use user screenshots plus static/build verification.

### Technical Decisions
| Decision | Rationale |
|----------|-----------|
| Stack upload icon, text, and CTA vertically below `sm` | Gives copy a full-width readable measure and keeps the primary action thumb-friendly |
| Set `-webkit-text-size-adjust: 100%` | Prevents iOS/WebView font autosizing from destabilizing carefully sized responsive layouts |

### Static Audit Findings
- `ConsultationConfigPanel` uses a five-column selector and desktop-sized padding/actions; it needs smaller gaps/padding and stacked mobile actions.
- `ConsultationChatPanel` uses `100vh` math and a textarea-plus-vertical-button desktop row; it needs `dvh`/container sizing and mobile-stacked controls.
- Shared confirmation dialogs need bottom-sheet-like mobile alignment, safe-area padding, scroll limits, and full-width actions.
- Report, consultation, and knowledge-base list pages already use horizontal table scrolling, but touch affordance and compact padding need review.
- `PublicDocsPage` preview modal uses desktop padding in header/body/footer and needs mobile reductions plus stacked footer actions.
- `HealthReportDetailPage` has responsive tabs and header actions from the earlier pass; its child analysis and consultation detail panels still use desktop padding in several cards.
- Legacy `HistoryList` is not routed by `App.tsx`, but should still receive safe overflow because it remains a shared component.
- `ConsultationPanel` hides export/delete actions behind hover opacity; touch devices cannot discover hover-only actions, so actions must remain visible on mobile.
- `KnowledgeBaseManagePage` filters wrap but retain content-width selects; mobile should use a full-width search and two equal-width selects.
- Admin document table has an overflow wrapper but no table minimum width, allowing columns to compress unpredictably.
- Login page already hides its decorative desktop panel below `lg`; mobile still needs `dvh`, reduced card padding/radius, and tighter vertical spacing for short screens/keyboard display.
