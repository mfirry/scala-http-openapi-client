//> using scala 3.3.7
//> using dep "org.yaml:snakeyaml:2.2"

import org.yaml.snakeyaml.Yaml
import scala.jdk.CollectionConverters.*
import java.io.{FileReader, FileWriter}

// ── Types ────────────────────────────────────────────────────────────────────

type YMap  = java.util.Map[String, Object]
type YList = java.util.List[Object]

def ymap(m: YMap, key: String): Option[YMap] =
  Option(m.get(key)).collect { case v: java.util.Map[?, ?] => v.asInstanceOf[YMap] }

def ystr(m: YMap, key: String): Option[String] =
  Option(m.get(key)).collect { case v: String => v }

def ylist(m: YMap, key: String): Option[YList] =
  Option(m.get(key)).collect { case v: java.util.List[?] => v.asInstanceOf[YList] }

// ── OpenAPI type → Scala type ─────────────────────────────────────────────

def scalaType(schema: YMap): String =
  ystr(schema, "type").getOrElse("Any") match
    case "array"   => ymap(schema, "items").map(scalaType).map(t => s"List[$t]").getOrElse("List[Any]")
    case "integer" => "Int"
    case "number"  => "Double"
    case "boolean" => "Boolean"
    case _         => "String"

// ── Naming ───────────────────────────────────────────────────────────────────

def toClassName(title: String): String =
  title.split("\\s+").map(_.capitalize).mkString + "Client"

def toMethodName(httpMethod: String, path: String): String =
  val segments = path.split("/").filter(s => s.nonEmpty && !s.startsWith("{")).toList
  (httpMethod.toLowerCase :: segments) match
    case Nil          => "call"
    case head :: tail => head + tail.map(_.capitalize).mkString

// ── Data model ────────────────────────────────────────────────────────────────

case class Param(name: String, tpe: String, in: String)
case class Endpoint(
  name:       String,
  httpMethod: String,
  path:       String,
  summary:    String,
  params:     List[Param],
  returnType: String
)

// ── Parsing ───────────────────────────────────────────────────────────────────

def parseEndpoints(paths: YMap): List[Endpoint] =
  val httpMethods = Set("get", "post", "put", "patch", "delete")
  paths.entrySet.asScala.toList.flatMap { pathEntry =>
    val path    = pathEntry.getKey
    val methods = pathEntry.getValue.asInstanceOf[YMap]
    methods.entrySet.asScala.toList.flatMap { methodEntry =>
      val verb = methodEntry.getKey
      Option.when(httpMethods.contains(verb)) {
        val op      = methodEntry.getValue.asInstanceOf[YMap]
        val summary = ystr(op, "summary").getOrElse("")
        val opId    = ystr(op, "operationId")

        val params: List[Param] =
          ylist(op, "parameters")
            .map(_.asScala.toList)
            .getOrElse(Nil)
            .collect { case m: java.util.Map[?, ?] => m.asInstanceOf[YMap] }
            .map { p =>
              Param(
                name = ystr(p, "name").getOrElse("param"),
                tpe  = ymap(p, "schema").map(scalaType).getOrElse("String"),
                in   = ystr(p, "in").getOrElse("query")
              )
            }

        val returnType: String =
          ymap(op, "responses")
            .flatMap(ymap(_, "200"))
            .flatMap(ymap(_, "content"))
            .flatMap(ymap(_, "application/json"))
            .flatMap(ymap(_, "schema"))
            .map(scalaType)
            .getOrElse("Unit")

        Endpoint(
          name       = opId.getOrElse(toMethodName(verb, path)),
          httpMethod = verb,
          path       = path,
          summary    = summary,
          params     = params,
          returnType = returnType
        )
      }
    }
  }

// ── Code rendering ────────────────────────────────────────────────────────────

def renderUri(path: String, pathParams: List[Param]): String =
  val interpolated = pathParams.foldLeft(path) { (acc, p) =>
    acc.replace("{" + p.name + "}", "${" + p.name + "}")
  }
  "uri\"$baseUrl" + interpolated + "\""

def renderMethod(e: Endpoint): String =
  val pathParams  = e.params.filter(_.in == "path")
  val queryParams = e.params.filter(_.in == "query")
  val allParams   = pathParams ++ queryParams
  val paramsStr   = if allParams.isEmpty then ""
                    else allParams.map(p => s"${p.name}: ${p.tpe}").mkString("(", ", ", ")")
  val queryChain  = queryParams
                      .map(p => ".param(\"" + p.name + "\", " + p.name + ".toString)")
                      .mkString
  val responseAs  = if e.returnType == "Unit" then "asString.mapRight(_ => ())"
                    else s"asJson[${e.returnType}]"
  val docLine     = if e.summary.nonEmpty then s"  /** ${e.summary} */\n" else ""

  s"""${docLine}  def ${e.name}$paramsStr: Either[String, ${e.returnType}] =
    |    basicRequest
    |      .${e.httpMethod}(${renderUri(e.path, pathParams)}$queryChain)
    |      .response($responseAs)
    |      .send(backend)
    |      .body
    |      .left
    |      .map(_.getMessage)
    |""".stripMargin

def renderFile(className: String, baseUrl: String, endpoints: List[Endpoint]): String =
  val methods = endpoints.map(renderMethod).mkString("\n")
  s"""// AUTO-GENERATED — do not edit by hand.
    |// Regenerate with: scala-cli run generator/
    |import sttp.client3.*
    |import sttp.client3.circe.*
    |
    |class $className(baseUrl: String) extends AutoCloseable:
    |  private val backend = HttpClientSyncBackend()
    |
    |$methods
    |  override def close(): Unit = backend.close()
    |
    |object $className:
    |  val defaultBaseUrl = "$baseUrl"
    |
    |  def apply(baseUrl: String = defaultBaseUrl): $className =
    |    new $className(baseUrl)
    |""".stripMargin

// ── Entry point ───────────────────────────────────────────────────────────────

@main def generate(): Unit =
  val specPath = "openapi/openapi.yaml"
  val outPath  = "src/SampleApiClient.scala"

  val root    = Yaml().load[YMap](FileReader(specPath))
  val infoMap = ymap(root, "info").getOrElse(java.util.Collections.emptyMap())
  val title   = ystr(infoMap, "title").getOrElse("Api")
  val clsName = toClassName(title)

  val baseUrl =
    ylist(root, "servers")
      .flatMap(_.asScala.headOption)
      .collect { case m: java.util.Map[?, ?] => m.asInstanceOf[YMap] }
      .flatMap(ystr(_, "url"))
      .getOrElse("")

  val paths     = ymap(root, "paths").getOrElse(java.util.Collections.emptyMap())
  val endpoints = parseEndpoints(paths)
  val code      = renderFile(clsName, baseUrl, endpoints)

  val writer = FileWriter(outPath)
  try writer.write(code) finally writer.close()

  println(s"Generated $clsName -> $outPath (${endpoints.size} endpoint(s))")
