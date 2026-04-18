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
