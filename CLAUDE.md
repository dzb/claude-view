# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Full build (single shaded JAR with embedded frontend, ~860KB)
mvn clean package -DskipTests

# Run the built JAR (picks a random free port)
java -jar claude-view/target/claude-view-1.0.jar

# Development — backend (auto-reload not included)
mvn compile exec:java -Dexec.mainClass=com.jujin.claudeview.App

# Development — frontend (hot reload, proxied to backend on :8080)
cd claude-view-web && npm run dev
# → http://localhost:5173

# Frontend type-check + production build
cd claude-view-web && npm run build
# outputs to ../claude-view/src/main/resources/web/
```

**Note:** There are no tests in this project — `-DskipTests` is standard.

## Architecture

```
Browser → Vue 3 SPA → REST API → Java services → ~/.claude/ filesystem
```

### Backend (`claude-view/`)

**Framework:** [Freeway](https://github.com/dzb/freeway) 1.2.1 — a lightweight Java IoC/HTTP framework (not Spring). Key concepts:
- `Module2` — a module that contributes bindings, routes, and hooks to the container
- `Binder` / `b.install()` — dependency wiring (no annotation scanning)
- `RouteGroup` — REST route definitions using lambda handlers
- `FreewayApp.run()` — entry point that boots the Freeway container
- `HttpModule` — built-in HTTP server (random port by default via `--freeway.web.server.port=0`)
- `JsonUtils` — Freeway's JSON parser (used everywhere instead of Jackson/Gson)
- `StaticResourceMount` — serves classpath resources as static files

The entire app is a **single Module2 implementation** in `App.java`, which:
1. Installs `HttpModule` for the web server
2. Manually instantiates the 4 services and binds them
3. Defines all REST routes inline as lambdas in `RouteGroup` contributions
4. Mounts the embedded Vue frontend from `/web` classpath
5. Adds an SPA fallback filter that serves `index.html` for non-API, non-file paths

**Services** (all in `com.jujin.claudeview.service`):
| Interface | Implementation | Purpose |
|-----------|---------------|---------|
| `ClaudeDataService` | `ClaudeDataServiceImpl` | Locate `~/.claude/` and enumerate projects/conversations |
| `ConversationService` | `ConversationServiceImpl` | Parse JSONL conversation files, extract structured lines |
| `MemoryService` | `MemoryServiceImpl` | Read/write YAML frontmatter memory files, maintain MEMORY.md index |
| `SessionService` | `SessionServiceImpl` | Scan `~/.claude/sessions/` for active session metadata |

### Project path encoding

Claude Code stores per-project data under `~/.claude/projects/{sanitized}/` where the sanitized name replaces `/` with `-` (e.g., `/home/bob/my-project` → `-home-bob-my-project`). This encoding is lossy when path segments contain dashes (e.g., `/home/bob/freeway-2`). The `unsanitizeProjectPath()` method in `ClaudeDataServiceImpl` uses a backtracking heuristic to recover the original path. Any code that resolves project names to filesystem paths must go through this method.

### Frontend (`claude-view-web/`)

- **Vue 3 + TypeScript + Vite 6**
- Router: 7 routes (dashboard, conversations list, conversation detail, highlights, memories, project-memories, history) — all lazy-loaded via `() => import(...)`
- `src/api/client.ts` — typed API client with TypeScript interfaces mirroring Java records exactly
- Vite dev server proxies `/api` to `http://localhost:8080`
- Production build outputs to `claude-view/src/main/resources/web/` so the JAR self-contains the frontend

### Data format

- **Conversation files** are JSONL (one JSON object per line) at `~/.claude/projects/{proj}/{uuid}.jsonl`
- Each line contains `type` (user/assistant), `message` (content as string or array with text/thinking/tool_use blocks), `timestamp`, `uuid`, optional `isSidechain`/`agentId`/`attributionAgent` fields
- **Memory files** are Markdown with YAML frontmatter (`---\nname: ...\n---\ncontent`), indexed by `MEMORY.md`
- **History** is a plain JSONL file at `~/.claude/history.jsonl` (one entry per prompt)
- **Active sessions** are JSON files in `~/.claude/sessions/`

### Key patterns

- **No DI annotations** — services are manually instantiated in `App.bind()` and passed their dependencies via constructor. If adding a new service, wire it there.
- **Routes expect sanitized project names** in URL paths (e.g., `-home-bob-my-project`), not raw filesystem paths.
- **The `parseLine()` method** in `ConversationServiceImpl` handles both string content (user messages) and array content blocks (assistant messages with thinking/tool_use/text blocks). Tool results are explicitly skipped.
- **Memory writes auto-update `MEMORY.md`** — the index file is regenerated from the current directory listing after every save/delete.
