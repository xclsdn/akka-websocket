import akka.actor.typed.{Behavior,ActorRef}
import akka.actor.typed.scaladsl.Behaviors

object ChatRoom{
    abstract trait ChatRoomCmd
    final case class NewMessage(content:String,userName:String) extends ChatRoomCmd
    final case class JoinRoom(user:ActorRef[String]) extends ChatRoomCmd
    final case class LeaveRoom(user:ActorRef[String]) extends ChatRoomCmd

    def apply(roomName:String):Behavior[ChatRoomCmd]=
        ChatRoom(roomName,List.empty,List.empty)

    def ChatRoom(roomName:String,users:List[ActorRef[String]],messages:List[String]):Behavior[ChatRoomCmd]=
        Behaviors.receiveMessage{
            case NewMessage(content,userName)=>
                users.foreach(_ ! s"ChatRoom:${roomName}-Message: ${content}: From ${userName}")
            ChatRoom(roomName,users,content::messages)
            case JoinRoom(userRef) =>
                userRef ! s"ChatRoom: ${roomName} welcome you"
            ChatRoom(roomName,userRef::users,messages)
            case LeaveRoom(userRef) =>
                userRef ! "See you later - ChatRoom: ${roomName}"
            ChatRoom(roomName,users.filter(_ != userRef),messages)  
        }    
}