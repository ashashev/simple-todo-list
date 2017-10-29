package simpletodolist.server

import akka.actor.{Actor, ActorRef, Props}
import simpletodolist.library._

object Client {
  def props(storage: ActorRef) = Props(new Client(storage))

  case class Connected(outgoing: ActorRef)

}

class Client(storage: ActorRef) extends Actor {

  import Client._

  override def receive: Receive = {
    case Connected(outgoing) =>
      context.become(connected(outgoing))
  }

  override def postStop(): Unit = {
    storage ! Storage.Disjoin
  }

  private def connected(outgoing: ActorRef): Receive = {
    storage ! Storage.Join

    def calcReceiver(sender: ActorRef) = {
      if (sender == storage) outgoing
      else storage
    }

    {
      case cmd@Replace(_) =>
        calcReceiver(sender()) ! cmd
      case cmd@Get =>
        calcReceiver(sender()) ! cmd
      case cmd@Update(_) =>
        calcReceiver(sender()) ! cmd
    }
  }
}
