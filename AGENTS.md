# Kestra Line Plugin

## What

- Provides plugin components under `io.kestra.plugin.line`.
- Includes classes such as `LineTemplate`, `LineExecution`.

## Why

- This plugin integrates Kestra with Line.
- It provides tasks that send notifications to LINE messaging.

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
