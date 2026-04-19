# Kestra Line Plugin

## What

- Provides plugin components under `io.kestra.plugin.line`.
- Includes classes such as `LineTemplate`, `LineExecution`.

## Why

- What user problem does this solve? Teams need to send notifications to LINE messaging from orchestrated workflows instead of relying on manual console work, ad hoc scripts, or disconnected schedulers.
- Why would a team adopt this plugin in a workflow? It keeps Line steps in the same Kestra flow as upstream preparation, approvals, retries, notifications, and downstream systems.
- What operational/business outcome does it enable? It reduces manual handoffs and fragmented tooling while improving reliability, traceability, and delivery speed for processes that depend on Line.

## How

### Architecture

Single-module plugin. Source packages under `io.kestra.plugin`:

- `line`

Infrastructure dependencies (Docker Compose services):

- `app`

### Key Plugin Classes

- `io.kestra.plugin.line.LineExecution`

### Project Structure

```
plugin-line/
├── src/main/java/io/kestra/plugin/line/
├── src/test/java/io/kestra/plugin/line/
├── build.gradle
└── README.md
```

## References

- https://kestra.io/docs/plugin-developer-guide
- https://kestra.io/docs/plugin-developer-guide/contribution-guidelines
