package simpletodolist.server

import akka.actor.{Actor, ActorRef, Props}
import simpletodolist.library._

object Client {
  def props(storage: ActorRef) = Props(new Client(storage))

  case class Connected(outgoing: ActorRef)

}

/**
  * It's a mediator between the storage and the network actor.
  *
  * The first message that it should get is the Client.Connected message. After that the client starts to mediate
  * between the storage and the "outgoing" network actor. It just retransmits messages from the storage to the network
  * actor and back.
  *
  * @param storage the storage actor
  */
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
      case Get =>
        calcReceiver(sender()) ! Get
      case cmd@Update(_) =>
        calcReceiver(sender()) ! cmd
    }
  }
}
