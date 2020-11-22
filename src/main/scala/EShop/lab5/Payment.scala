package EShop.lab5

import EShop.lab3.Payment.{DoPayment, PaymentConfirmed}
import EShop.lab5.Payment.{PaymentRejected, PaymentRestarted}
import EShop.lab5.PaymentService.{PaymentClientError, PaymentServerError, PaymentSucceeded}
import akka.actor.SupervisorStrategy.{Restart, Resume, Stop}
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props}
import akka.event.LoggingReceive
import akka.stream.StreamTcpException

import scala.concurrent.duration._

object Payment {

  case object PaymentRejected
  case object PaymentRestarted

  def props(method: String, orderManager: ActorRef, checkout: ActorRef) =
    Props(new Payment(method, orderManager, checkout))

}

class Payment(
  method: String,
  orderManager: ActorRef,
  checkout: ActorRef
) extends Actor
  with ActorLogging {

  override def receive: Receive = LoggingReceive {
    case DoPayment =>
      context.actorOf(PaymentService.props(method, self))
    case PaymentSucceeded =>
      orderManager ! PaymentConfirmed
    case PaymentRejected =>
      notifyAboutRejection()
    case PaymentRestarted =>
      notifyAboutRestart()
  }

  override val supervisorStrategy: OneForOneStrategy =
    OneForOneStrategy(maxNrOfRetries = 3, withinTimeRange = 1.seconds) {
      case _: PaymentClientError =>
        Restart
      case _: PaymentServerError =>
        Stop
      case _: StreamTcpException =>
        Restart
      case _: Exception =>
        Resume
    }

  private def notifyAboutRejection(): Unit = {
    orderManager ! PaymentRejected
    checkout ! PaymentRejected
  }

  private def notifyAboutRestart(): Unit = {
    orderManager ! PaymentRestarted
    checkout ! PaymentRestarted
  }
}
