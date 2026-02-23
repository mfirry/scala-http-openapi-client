# scala-http-openapi-client

An HTTP client for the [Sample API](openapi/openapi.yaml), written in Scala 3 using [scala-cli](https://scala-cli.virtuslab.org/) and [sttp](https://sttp.softwaremill.com/).

The client source (`src/SampleApiClient.scala`) is **auto-generated** from the OpenAPI spec by the generator in `generator/`.

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
