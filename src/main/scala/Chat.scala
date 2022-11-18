import akka.actor.typed.{ActorRef, Behavior}
import scala.collection.mutable
import akka.actor.typed.scaladsl.Behaviors
object Chat {
  sealed trait ChatCommand
  final case class ProcessMessage(sender: String, content: String)
      extends ChatCommand
  final case class AddNewUser(ref: ActorRef[String]) extends ChatCommand

  def apply: Behavior[ChatCommand] =
    Behaviors.setup { _ =>
      var participants = List.empty[ActorRef[String]]
      val messageQueue = mutable.Queue.empty[String]
      Behaviors.receiveMessage[ChatCommand] {
        case ProcessMessage(sender, content) =>
          val message = s"$sender: $content"
          println("new msg")
          messageQueue.enqueue(message)
          participants.foreach(ref => ref ! message)
          Behaviors.same
        case AddNewUser(ref) =>
         println("mew user")
          participants = ref :: participants
          messageQueue.foreach(m => ref ! m)
          Behaviors.same
      }

    }
}
