import sttp.client3.*
import sttp.client3.circe.*

/** Client for the Sample API (http://api.example.com/v1).
  *
  * Implements AutoCloseable so it can be used with [[scala.util.Using]].
  */
class SampleApiClient(baseUrl: String) extends AutoCloseable:
  private val backend = HttpClientSyncBackend()

  /** GET /users – returns the list of user names. */
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
