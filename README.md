# FrozenFlow

A mock server designed for integration testing built on top of [WireMock](https://wiremock.org). It records real HTTP
traffic, transforms it into configurable stubs, and plays them back — enabling reliable, repeatable test environments
without live service dependencies.

## Features

- **Three run modes**: Recording, Playback, and Bypass
- **Automatic stub generation** from recorded HTTP calls with configurable request patterns and response templates
- **JSON and XML support** for request matching and response transformation
- **Conditional mappings** based on request headers and body content
- **Request/response transformers** configurable per service and per path
- **Request pattern types**: relative dates, date-time comparison (before/after), regex, UUID, value matching, existence
  checks, and more
- **Response templating** for headers and body using WireMock's built-in template engine
- **Stateful flows** via [wiremock-state-extension](https://github.com/wiremock/wiremock-state-extension) for multi-step
  interaction scenarios
- **Inter-stub dependencies** — reference response data from other mappings in request patterns
- **Miss tracking** with a custom Admin API for unmatched request diagnostics
- **Simplified snapshot API** — trigger recording snapshots via the Admin API
- **Brotli content encoding** support
- **Docker-ready** with a minimal JRE 21 image

## Requirements

- **Java 21** (or a compatible JRE/JDK)

## Run Modes

| Mode          | Description                                                                                            |
|---------------|--------------------------------------------------------------------------------------------------------|
| **RECORDING** | Proxies requests to real services and records HTTP calls. Snapshots can be triggered via Admin API.    |
| **PLAYBACK**  | Serves pre-recorded stubs with request matching, response templating, transformers, and state support. |
| **BYPASS**    | Pure proxy mode — forwards all requests to real services without recording.                            |

## Environment Variables

| Variable        | Required | Default | Description                              |
|-----------------|----------|---------|------------------------------------------|
| `RUN_MODE`      | Yes      | —       | One of `RECORDING`, `PLAYBACK`, `BYPASS` |
| `WIREMOCK_PORT` | No       | `8090`  | Port the WireMock server listens on      |

## Configuration

Configuration is loaded from `./config/settings.yaml` (YAML format, kebab-case property names).

### Structure

```yaml
general:
    max-miss-reports: 100        # Maximum number of unmatched request reports to keep

snapshot:
    headers: # Headers to include in snapshot request patterns
        - Content-Type
        - Authorization

services:
    my-service:
        proxy-base-url: "https://api.example.com"
        transformers:
            request: [ ]              # Request transformer names (applied in PLAYBACK mode)
            response: [ ]             # Response transformer names (applied in PLAYBACK mode)
        paths:
            -   path: "/v1/resource"
                transformers:
                    request: [ ]
                    response: [ ]
                mappings:
                    -   id: "unique-id"
                        conditions:
                            headers: [ ]
                            body: [ ]
                        request:
                            patterns:
                                query-params: [ ]
                                headers: [ ]
                                body: [ ]
                                state: [ ]
                            dependencies:
                                body: [ ]
                        response:
                            mime-type: "application/json"
                            templates:
                                headers: [ ]
                                body: [ ]
                        state:
                            -   action: put    # put or delete
                                context: "my-context"
                                properties:
                                    key: "value"
                        priority: 1

service-config-files: # Additional service definition files to load
    - "services/my-service.yaml"
```

JSON Schemas for configuration validation are available in [doc/schemas/](doc/schemas/).

## Configuration Structure

```
config/                  # Configuration directory (mounted at runtime)
  settings.yaml          # Main configuration file
  files/                 # Static files to serve
  mappings/
    playback/            # Static stubs for PLAYBACK mode
    proxy/               # Static stubs for RECORDING/BYPASS modes
    all/                 # Static stubs loaded in all modes
```

## Project Structure

```
doc/schemas/             # JSON Schema definitions for configuration
src/main/
  kotlin/
    WireMockServerStarter.kt          # Application entry point
    init/                              # Server initialization and config loading
    snapshot/                          # Snapshot recording service and Admin API
    transform/                         # Stub generation from recorded HTTP calls
      conditions/                      # Conditional matching (headers, body)
      dependencies/                    # Inter-stub dependency resolution
      request/                         # Request pattern transformers (body, headers, query params, state)
      response/                        # Response definition transformers (body, headers)
      state/                           # State management for mappings
      model/                           # Data models
    httpcall/transform/                # Runtime HTTP call transformers (request/response)
    content/                           # JSON and XML content accessors
    http/                              # HTTP call encoding and header utilities
    staticstub/                        # Static mapping file loader
    verification/                      # Miss tracking and unmatched request reporting
    utils/                             # Brotli decoding, extensions, utilities
src/test/                              # Unit and integration tests
```

## Building

```bash
./gradlew build
```

The build produces a JAR with dependencies in `build/libs/`.

## Running Locally

```bash
RUN_MODE=PLAYBACK ./gradlew run
```

## Running with Docker

```bash
# Build the application
./gradlew jar

# Build the Docker image
docker build -t frozen-flow .

# Run
docker run -p 8090:8090 \
  -e RUN_MODE=PLAYBACK \
  -v $(pwd)/config:/home/frozenflow/config \
  frozen-flow
```

## Admin API

The server extends WireMock's Admin API with additional endpoints:

| Method | Endpoint                                | Description                         |
|--------|-----------------------------------------|-------------------------------------|
| `POST` | `/__admin/extended/recordings/snapshot` | Trigger a snapshot (RECORDING mode) |
| `GET`  | `/__admin/extended/misses`              | Get unmatched request reports       |
| `POST` | `/__admin/extended/misses/reset`        | Reset unmatched request reports     |

Standard WireMock Admin API endpoints (`/__admin/mappings`, `/__admin/requests`, etc.) are also available.

## Testing

```bash
./gradlew test
```

## Code Quality

Static analysis is performed with [detekt](https://detekt.dev/):

```bash
./gradlew detekt
```

## Stability Notice

This project has not yet reached version **1.0.0**. Until then, backward compatibility is not guaranteed — any
configuration format, API contracts, and behavior may be subject to change without prior notice.
All changes can be tracked in the changelog.

## Tech Stack

- **Kotlin** 2.1 / **Java** 21
- **WireMock** 3.13
- **wiremock-state-extension** 0.10
- **Jackson** (YAML, JSON, XML) for configuration and content processing
- **Logback** + **Logstash Encoder** for structured logging
- **Ktor** (test HTTP client)
- **JUnit 5** + **AssertJ** + **Kotlin Power-Assert** for testing
- **detekt** for static code analysis
