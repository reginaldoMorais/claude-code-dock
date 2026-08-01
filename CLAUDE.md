# CLAUDE.md

## Non-negotiable rules

- **DON'T GUESS, VERIFY** — never assume any fact you can check. Confirm it against the source.
- Always plan before executing a request.
- Follow SOLID principles, maintaining separation of responsibilities and avoiding code duplication.
- For the implementation, I want functional, ready-to-use code—not pseudocode.
- If there are any trade-offs, briefly explain them before implementing.
- Always run unit tests after implementation.
- Always implement new unit tests for new features or modified code.
- Never expose secrets, tokens, or credentials in the codebase. Use environment variables or secure vaults for sensitive information.
- Code (variables, classes, methods, or functions) always in English, and comments in Portuguese.

## Git — Non-negotiable rules

- Never run `git add`, `git commit`, `git push`, or `git checkout` unless the user explicitly asks. When changes are ready to commit, provide the commands for the user to run — do not execute them.
- Branches in English following GitFlow
- Commits in English using [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/)

<!-- code-review-graph MCP tools -->

## MCP Tools: code-review-graph

**IMPORTANT: This project has a knowledge graph. ALWAYS use the
code-review-graph MCP tools BEFORE using Grep/Glob/Read to explore
the codebase.** The graph is faster, cheaper (fewer tokens), and gives
you structural context (callers, dependents, test coverage) that file
scanning cannot.

### When to use graph tools FIRST

- **Exploring code**: `semantic_search_nodes` or `query_graph` instead of Grep
- **Understanding impact**: `get_impact_radius` instead of manually tracing imports
- **Code review**: `detect_changes` + `get_review_context` instead of reading entire files
- **Finding relationships**: `query_graph` with callers_of/callees_of/imports_of/tests_for
- **Architecture questions**: `get_architecture_overview` + `list_communities`

Fall back to Grep/Glob/Read **only** when the graph doesn't cover what you need.

### Key Tools

| Tool                        | Use when                                               |
| --------------------------- | ------------------------------------------------------ |
| `detect_changes`            | Reviewing code changes — gives risk-scored analysis    |
| `get_review_context`        | Need source snippets for review — token-efficient      |
| `get_impact_radius`         | Understanding blast radius of a change                 |
| `get_affected_flows`        | Finding which execution paths are impacted             |
| `query_graph`               | Tracing callers, callees, imports, tests, dependencies |
| `semantic_search_nodes`     | Finding functions/classes by name or keyword           |
| `get_architecture_overview` | Understanding high-level codebase structure            |
| `refactor_tool`             | Planning renames, finding dead code                    |

### Workflow

1. The graph auto-updates on file changes (via hooks).
2. Use `detect_changes` for code review.
3. Use `get_affected_flows` to understand impact.
4. Use `query_graph` pattern="tests_for" to check coverage.
