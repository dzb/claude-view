# 🧠 Claude View

A desktop web app for browsing Claude Code conversation history and memory files — clean, fast, local.

![Stack](https://img.shields.io/badge/Java-25%2B-orange) ![Stack](https://img.shields.io/badge/Freeway-1.2-blue) ![Stack](https://img.shields.io/badge/Vue-3.5-green)

<img width="1580" height="1080" alt="image" src="https://github.com/user-attachments/assets/771f1942-5e19-4407-9be9-497b9339589f" />


## What it does

- **Browse conversations** — read Claude Code JSONL transcripts with markdown rendering, syntax highlighting, and noise filtering
- **Manage memories** — view, edit, create, and delete project memory files with YAML frontmatter
- **Prompt history** — scroll through your full `history.jsonl` with instant search
- **Highlights** — auto-curated list of substantial conversations (≥30 lines, ≥200 chars of clean Claude output), stripped of thinking blocks and tool noise
- **Active sessions** — see which Claude Code sessions are currently running
- **Full-text search** — search across both conversations and memories

## Tech stack

| Layer | Tech |
|-------|------|
| Backend | Java 25 + [Freeway](https://github.com/dzb/freeway) 1.2.1 |
| Frontend | Vue 3 + TypeScript + Vite |
| Markdown | marked + highlight.js (GitHub theme) |
| Data | File system reads from `~/.claude/` |

Zero database, zero ORM, zero external runtime dependencies beyond SLF4J.

## Quick start

```bash
# Build and run (single JAR, ~860KB)
mvn clean package -DskipTests
java -jar claude-view/target/claude-view-1.0.jar
```

The server picks a random available port and prints the URL:

```
🧠 Claude View ready:  http://localhost:51234
```

To specify a port:

```bash
java -jar claude-view-1.0.jar --freeway.web.server.port=8080
```

## Development

```bash
# Terminal 1 — backend
mvn compile exec:java -Dexec.mainClass=com.jujin.claudeview.App

# Terminal 2 — frontend (hot reload)
cd claude-view-web && npm run dev
# → http://localhost:5173  (API proxied to backend)
```

## Project structure

```
claude-view/
├── claude-view/              # Java backend
│   └── src/main/java/com/jujin/claudeview/
│       ├── App.java          # Entry point + all routes
│       ├── model/            # 6 data records
│       └── service/          # 4 services (I/O + parsing)
├── claude-view-web/          # Vue 3 frontend
│   └── src/
│       ├── views/            # 5 page views
│       ├── api/client.ts     # Typed API client
│       └── router/index.ts
└── pom.xml                   # Maven parent POM
```

## API

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/projects` | List all Claude Code projects |
| GET | `/api/projects/:name/sessions` | Sessions for a project |
| GET | `/api/conversations/:proj/:id` | Paginated conversation transcript |
| GET | `/api/conversations/:proj/:id/search?q=` | Search within a conversation |
| GET | `/api/memories` | List all memories |
| PUT | `/api/memories/:proj/:file` | Update a memory |
| POST | `/api/memories/:proj` | Create a memory |
| DELETE | `/api/memories/:proj/:file` | Delete a memory |
| GET | `/api/history` | Prompt history |
| GET | `/api/highlights` | Curated substantial conversations |
| GET | `/api/sessions/active` | Currently active sessions |
| GET | `/api/search?q=` | Global text search |

## License

MIT
