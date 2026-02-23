# scala-http-openapi-client

An HTTP client for the [Sample API](openapi/openapi.yaml), written in Scala 3 using [scala-cli](https://scala-cli.virtuslab.org/) and [sttp](https://sttp.softwaremill.com/).

The client source (`src/SampleApiClient.scala`) is **auto-generated** from the OpenAPI spec. Two generators are available:

| Folder | Tool | Output |
|---|---|---|
| `generator/` | Custom scala-cli generator | `src/SampleApiClient.scala` (used by this project) |
| `official-generator/` | [openapi-generator.tech](https://openapi-generator.tech) | `official-generator/generated/` (standalone sbt project) |

---

## Install scala-cli

### macOS

Using [Homebrew](https://brew.sh/):

```sh
brew install Virtuslab/scala-cli/scala-cli
```

Verify the installation:

```sh
scala-cli --version
```

### Windows

Using [Scoop](https://scoop.sh/):

```powershell
scoop install scala-cli
```

Or using [winget](https://learn.microsoft.com/en-us/windows/package-manager/winget/):

```powershell
winget install VirtusLab.ScalaCli
```

Or download the `.msi` installer directly from the [releases page](https://github.com/VirtusLab/scala-cli/releases/latest).

Verify the installation:

```powershell
scala-cli --version
```

---

## Run the generator

The generator reads `openapi/openapi.yaml` and writes `src/SampleApiClient.scala`. Run it whenever the spec changes:

```sh
scala-cli run generator/
```

The generator supports:

- All standard HTTP methods (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`)
- Path parameters (e.g. `/users/{id}`)
- Query parameters
- Response body types (`string`, `integer`, `number`, `boolean`, `array`)
- `operationId` as the method name when present, otherwise derived from the HTTP method and path

---

## Run the client

```sh
scala-cli run .
```

To point the client at a different base URL, edit `src/Main.scala` and pass the desired URL to `SampleApiClient`:

```scala
Using.resource(SampleApiClient(baseUrl = "http://staging-api.example.com")) { client =>
  ...
}
```

To produce a standalone fat JAR:

```sh
scala-cli package . --assembly -o sample-api-client.jar
```

---

## Official OpenAPI Generator

`official-generator/` uses the [official openapi-generator](https://openapi-generator.tech) (`scala-sttp` generator) to produce a full standalone sbt project from the spec.

### Prerequisites

- Node.js and npm — install from [nodejs.org](https://nodejs.org/)
- Java 11+ — required by the openapi-generator CLI

### Steps

**1. Install the generator CLI**

```sh
cd official-generator
npm install
```

**2. Run the generator**

```sh
npm run generate
```

Output lands in `official-generator/generated/`.

**3. Compile the generated project**

```sh
cd generated
sbt compile
```

### What is generated

- **API class** under `com.example.api` backed by sttp (client3) + circe
- Supporting infrastructure under `org.openapitools.client.core` (JSON support, date serializers)

### Changing generation options

Edit the `generate` script in `official-generator/package.json`. The relevant flags are:

| Flag | Current value | Description |
|---|---|---|
| `-g` | `scala-sttp` | Generator name |
| `--additional-properties` | `apiPackage`, `modelPackage`, `jsonLibrary` | Package names and JSON library |
