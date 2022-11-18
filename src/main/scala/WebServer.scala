import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{Message, TextMessage, BinaryMessage}
import akka.http.scaladsl.server.Directives._
import scala.io.StdIn
import akka.stream.scaladsl.{Source, Flow, Sink, Keep}
import akka.stream.typed.scaladsl.ActorSource
import akka.stream.OverflowStrategy
import scala.concurrent.{ExecutionContextExecutor, Future}
import akka.Done
import akka.actor.ActorRef
import Chat._

object Webserver extends App {
  implicit val system = ActorSystem(Chat.apply, "my-system")
  // needed for the future flatMap/onComplete in the end
  val chatActor = system
  implicit val executionContext = system.executionContext
  // val actorChat= system.actorOf(Props[Chat]())
  case class User(name: String)
  val route1 =
    path("hello") {
      get {
        complete(
          HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            "<h1>Say hello to akka-http</h1>"
          )
        )
      }
    }

  val route2 =
    path("t") {
      get {
        complete(
          HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            "hello"
          )
        )
      }
    }

  val route3 =
    path("web") {
      getFromResource("web/index.html") ~ getFromResourceDirectory("web")
    }

  val websocketRoute =
    pathPrefix("websocket") {
      path(Remaining) { name =>
        {
          handleWebSocketMessages(ws(name))
        }
      }
    }
  val routes = concat(route1, route3, route2, websocketRoute)
  val bindingFuture = Http().newServerAt("127.0.0.1", 9081).bind(routes)

  println(
    s"Server now online. Please navigate to http://localhost:9080/hello\nPress RETURN to stop..."
  )
//   StdIn.readLine() // let it run until user presses return
//   bindingFuture
//     .flatMap(_.unbind()) // trigger unbinding from the port
//     .onComplete(_ => system.terminate()) // and shutdown when done

  def greeter: Flow[Message, Message, Any] =
    Flow[Message].mapConcat {
      case tm: TextMessage =>
        TextMessage(
          Source.single("Hello ") ++ tm.textStream ++ Source.single("!")
        ) :: Nil
      case bm: BinaryMessage =>
        // ignore binary messages but drain content to avoid the stream being clogged
        bm.dataStream.runWith(Sink.ignore)
        Nil
    }

  def ws(name: String): Flow[Message, Message, Any] = {
    val source: Source[TextMessage, Unit] =
      ActorSource
        .actorRef[String](
          PartialFunction.empty,
          PartialFunction.empty,
          5,
          OverflowStrategy.fail
        )
        .map[TextMessage](TextMessage(_))
        .mapMaterializedValue(sourceRef => chatActor ! AddNewUser(sourceRef))
    val sink: Sink[Message, Future[Done]] = Sink
      .foreach[Message] {
        case tm: TextMessage =>
          chatActor ! ProcessMessage(name, tm.getStrictText)
        case _ =>
          println("User send unsupported message")
      }

    Flow.fromSinkAndSource(sink, source)
  }

}
