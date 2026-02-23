// AUTO-GENERATED — do not edit by hand.
// Regenerate with: scala-cli run generator/
import sttp.client3.*
import sttp.client3.circe.*

class SampleApiClient(baseUrl: String) extends AutoCloseable:
  private val backend = HttpClientSyncBackend()

  /** Returns a list of users. */
  def getUsers(): Either[String, List[String]] =
    basicRequest
      .get(uri"$baseUrl/users")
      .response(asJson[List[String]])
      .send(backend)
      .body
      .left
      .map(_.getMessage)

  override def close(): Unit = backend.close()

object SampleApiClient:
  val defaultBaseUrl = "http://api.example.com/v1"

  def apply(baseUrl: String = defaultBaseUrl): SampleApiClient =
    new SampleApiClient(baseUrl)
