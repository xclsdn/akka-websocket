import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.scaladsl
import akka.actor.typed.ActorRef
import ChatRoom._

object ChatServer {
  abstract trait ChatServerCmd
  final case class GetRoomList(user: String) extends ChatServerCmd
  final case class Online(userName: String, userRef: ActorRef[String])
      extends ChatServerCmd
  final case class JoinChatRoom(roomName: String, userName: String)
      extends ChatServerCmd
  final case class LeaveChatRoom(roomName: String, userName: String)
      extends ChatServerCmd
  final case class CreateChatRoom(roomName: String) extends ChatServerCmd
  final case class SendMsgToRoom(
      roomName: String,
      msg: String,
      userName: String
  ) extends ChatServerCmd

  def apply(): Behavior[ChatServerCmd] =
    Behaviors.setup[ChatServerCmd] { context =>
      val activeUsers = collection.mutable.Map[String, ActorRef[String]]()
      val chatRooms = collection.mutable.Map[String, ActorRef[ChatRoomCmd]]()
      ChatServer(activeUsers, chatRooms)
    }
  def ChatServer(
      activeUsers: collection.mutable.Map[String, ActorRef[String]],
      chatRooms: collection.mutable.Map[String, ActorRef[ChatRoomCmd]]
  ): Behavior[ChatServerCmd] =
    Behaviors.receive {
      case (context, GetRoomList(user)) =>
        val userRef = activeUsers(user)
        userRef ! chatRooms.toString()
        Behaviors.same
      case (context, CreateChatRoom(roomName)) =>
        val room: ActorRef[ChatRoomCmd] =
          context.spawn(ChatRoom(roomName), name = roomName)
        chatRooms(roomName) = room
        ChatServer(activeUsers, chatRooms)
      case (context, Online(userName, userRef)) =>
        println(s"${userName} add to chatServer")
        activeUsers(userName) = userRef
        userRef ! "Welcome connect to the chat room."
        ChatServer(activeUsers, chatRooms)
      case (context, JoinChatRoom(roomName, userName)) =>
        val roomRef = chatRooms(roomName)
        val userRef = activeUsers(userName)
        println(
          s"put the ${userName}: ${userRef} into ChatRoom ${roomName} : ${roomRef}"
        )
        roomRef ! JoinRoom(userRef)
        ChatServer(activeUsers, chatRooms)
      case (context, LeaveChatRoom(roomName, userName)) =>
        val roomRef = chatRooms(roomName)
        val userRef = activeUsers(userName)
        println(
          s"${userName}: ${userRef} leave ChatRoom ${roomName} : ${roomRef}"
        )
        roomRef ! LeaveRoom(userRef)
        ChatServer(activeUsers, chatRooms)
      case (context, SendMsgToRoom(roomName, msg, userName)) =>
        val roomRef = chatRooms(roomName)
        roomRef ! NewMessage(msg, userName)
        ChatServer(activeUsers, chatRooms)
    }
}
