# How to use the LINE plugin

Send messages and execution summaries to LINE channels from Kestra flows.

## Authentication

Set `channelAccessToken` to your LINE channel access token (issued in the LINE Developers console under your Messaging API channel). Store it in a [secret](https://kestra.io/docs/concepts/secret).

## Tasks

`LineExecution` sends a structured execution summary including status, duration, and an execution link to a LINE channel, and is designed for use with a [Flow trigger](https://kestra.io/docs/workflow-components/triggers) in a dedicated monitoring namespace that watches other namespaces for failures.
