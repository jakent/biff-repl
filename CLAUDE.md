# CLAUDE.md

## Project Overview
Personal interactive website built with Biff (Clojure) + HTMX.
The main feature is a web-based REPL where visitors can run sandboxed Clojure commands.

## Tech Stack
- **Biff v1.9.1** — Clojure web framework
- **XTDB** — immutable database
- **HTMX + _hyperscript** — frontend interactivity
- **SCI (Small Clojure Interpreter)** — sandboxed Clojure eval (to be added)
- **Malli** — schema validation
- **CodeMirror 6** — browser code editor (to be added)
- **Tailwind CSS** — styling

## Coding Conventions
- Pure/declarative functions preferred; minimize side effects
- Use Hiccup (via Rum) for all HTML generation
- Prefer threading macros (`->`, `->>`, `as->`)
- All new features need tests in `test/`
- Format with cljfmt; lint with clj-kondo

## Project Structure
```
src/biff_repl/
├── app.clj          # Core app features, forms, WebSocket chat
├── home.clj         # Landing page, auth pages
├── ui.clj           # UI utilities, base template, page wrapper
├── schema.clj       # XTDB + Malli schema definitions
├── middleware.clj   # Ring middleware
├── email.clj        # Email sending (MailerSend)
├── worker.clj       # Background jobs, scheduled tasks
└── settings.clj     # App configuration

dev/
├── repl.clj         # REPL development utilities
└── tasks.clj        # Custom CLI tasks

resources/
├── config.edn       # Application configuration (Aero)
├── fixtures.edn     # Seed/test data
└── public/          # Static assets (CSS, JS, images)

test/                # Test files
```

## Commands
Run all commands from project root:
- `clj -M:dev dev` — start dev server (http://localhost:8080)
- `clj -M:dev test` — run tests
- `clj -M:dev format` — format code with cljfmt
- `clj -M:dev lint` — run clj-kondo linter
- `clj -M:dev help` — list all available tasks

## Development
- nREPL runs on port 7888 in dev mode
- Hot reload via Beholder file watcher
- Use `dev/repl.clj` for REPL-driven development

## Planned Features (Web REPL)
The centerpiece feature is a browser-based REPL:
- **SCI sandbox** for safe Clojure evaluation
- **CodeMirror editor** with Clojure syntax highlighting
- **HTMX-powered** eval via POST `/repl/eval`
- **Custom commands**: `(help)`, `(about-me)`, `(projects)`, `(demo :id)`, etc.
- **Rate limiting**: max 30 evals/minute per session
- **Timeout**: 2 seconds max per eval

When implementing REPL commands:
- Put all commands in `src/biff_repl/repl_commands.clj`
- Register commands in the SCI sandbox context
- Update `(help)` output when adding new commands
- Add tests for command behavior

## Security (Web REPL)
- SCI provides sandboxed interpreter — no filesystem, no network
- Allowlisted functions only
- Rate-limit and timeout all evaluations
- Never expose system internals to visitors

## PR Guidelines
- One feature per PR
- Include tests for new functionality
- Update `(help)` output if adding new REPL commands
- Run `clj -M:dev format` and `clj -M:dev lint` before committing
