# PipelineCI design doc

**Status:** draft v0.1 · **Author:** ash4827

## 1\. Goal

PipelineCI is a command-line tool that reads a `pipeline.yml` file, runs each step in its own Docker container, and reports whether the pipeline passed or failed. It is a small, local version of what GitHub Actions does, built to learn how container-based CI works.

**Non-goals for v1:** parallel steps, caching, webhooks, a dashboard, GitHub status checks. These are planned for later phases.

**Success criterion:** `pipeline run` on a sample project passes when every step succeeds, fails when any step fails, and leaves no containers behind in either case.

## 2\. Architecture

pipeline.yml

     │

     ▼

┌──────────┐   Pipeline   ┌──────────┐  RunResult  ┌──────────┐

│  Parser  │ ───────────► │  Runner  │ ──────────► │ Reporter │

│ (config) │              │ (Docker) │             │   (ui)   │

└──────────┘              └──────────┘             └──────────┘

 read \+ validate           one container            console output

 the YAML                  per step, in order       \+ exit code

The CLI layer (`cli`) wires the three together. Dependencies point one way: `cli` calls `config`, `runner`, and `ui`; all of them use `model`; `model` depends on nothing. Only `runner` touches Docker, so the parser and reporter can be tested without it.

## 3\. Components

**Parser (`config`)** reads the YAML into a `Pipeline` object and validates it: no empty step list, every step has an `image` and `run`, no duplicate step names. It collects all errors and reports them together instead of stopping at the first.

**Runner (`runner`)** executes steps in order. For each step it pulls the image if needed, creates a container with the project mounted at `/workspace` and the working directory set to it, runs the command with `sh -c`, streams output as it arrives, waits for the exit code, and removes the container, even on failure. On the first failing step, the remaining steps are marked skipped (fail-fast).

**Reporter (`ui`)** prints per-step status and timing as steps finish, then a final summary. It masks secret values in all output.

## 4\. Data model

| Type | Fields |
| :---- | :---- |
| `Pipeline` | `name`, `env`, `steps` |
| `Step` | `name`, `image`, `run`, `env`, `timeout` |
| `StepResult` | `stepName`, `status` (PASSED, FAILED, SKIPPED, TIMED\_OUT), `exitCode`, `duration`, `logs` |
| `RunResult` | `pipelineName`, `stepResults`, `status`, `totalDuration` |

## 5\. Exit codes

| Code | Meaning |
| :---- | :---- |
| 0 | every step passed |
| 1 | a step failed or timed out |
| 2 | config or usage error (bad YAML, Docker not reachable) |

## 6\. Key decisions

- **Java \+ Maven.** Fits the open source ecosystem I want to contribute to, and Maven's layout keeps the project structure predictable.  
- **picocli** for commands and flags, **SnakeYAML** for parsing, **docker-java** for the Docker API, **JUnit 5** for tests.  
- **One container per step**, so that each step gets run in a clean reproducable environment.   
- **Bind-mount the project directory** so files written by one step are visible to the next.  
- **Fail-fast by default.** Continuing after a failure is a later feature (`allow_failure`).  
- **Honor `DOCKER_HOST`** instead of hardcoding a socket path, because the default socket location differs between machines.

## 7\. Risks and open questions

- **Leaked containers** on crash, timeout, or Ctrl-C. Plan: label every container the tool creates, clean up in a shutdown hook, and provide `pipeline clean`.  
- **Log streaming:** Docker multiplexes stdout and stderr into one stream, so the runner has to demultiplex it.  
- **Secrets:** exact syntax is undecided (`$NAME` from the host environment vs `${{ secrets.NAME }}`), and masking must also cover partial or encoded output.  
- **Workspace:** bind mount vs named volume. A bind mount is simpler, but file ownership may differ between host and container.  
- **Docker Desktop on macOS:** bind mounts are slower, and mounted folders need file-sharing permission.  
- **Java version:** compiling for Java 25 while developing on JDK 27 may expose tool incompatibilities. I may need to switch versions in the future.
- **5-minute default:** The 5-minute default hides whether a timeout was set. Will keep
the current default and revisit in phase 5.

## 8\. Milestones

Setup → config parsing → Docker runner → CLI polish → env and secrets → robustness (timeouts, Ctrl-C, cleanup) → v0.1.0 release. Stretch: caching, step dependencies, parallelism, matrix builds, then server mode.