import scala.util.Using

@main def run(): Unit =
  Using.resource(SampleApiClient()) { client =>
    client.getUsers() match
      case Right(users) =>
        println(s"Users (${users.size}):")
        users.foreach(u => println(s"  - $u"))
      case Left(err) =>
        System.err.println(s"Error fetching users: $err")
  }
