package EShop.lab4

import EShop.lab2.CartActor
import EShop.lab2.CartActor.ConfirmCheckoutClosed
import EShop.lab3.OrderManager.ConfirmPaymentStarted
import EShop.lab3.Payment
import akka.actor.{ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}
import akka.persistence.PersistentActor

import scala.util.Random
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object PersistentCheckout {

  def props(cartActor: ActorRef, persistenceId: String) =
    Props(new PersistentCheckout(cartActor, persistenceId))
}

class PersistentCheckout(
  cartActor: ActorRef,
  val persistenceId: String
) extends PersistentActor {

  import EShop.lab2.Checkout._
  private val scheduler = context.system.scheduler
  private val log       = Logging(context.system, this)
  val timerDuration     = 20.seconds

  private def updateState(event: Event, maybeTimer: Option[Cancellable] = None): Unit = {
    event match {
      case CheckoutStarted =>
        context become selectingDelivery(
          maybeTimer.getOrElse(scheduler.scheduleOnce(timerDuration, self, ExpireCheckout))
        )
      case DeliveryMethodSelected(method) =>
        context become selectingPaymentMethod(
          maybeTimer.getOrElse(scheduler.scheduleOnce(timerDuration, self, ExpireCheckout))
        )
      case CheckOutClosed =>
        if (maybeTimer.isDefined) maybeTimer.get.cancel()
        context become closed
      case CheckoutCancelled =>
        if (maybeTimer.isDefined) maybeTimer.get.cancel()
        context become cancelled
      case PaymentStarted(payment) =>
        if (maybeTimer.isDefined) maybeTimer.get.cancel()
        sender ! ConfirmPaymentStarted(payment)
        context become processingPayment(scheduler.scheduleOnce(timerDuration, self, ExpirePayment))
    }
  }

  def receiveCommand: Receive = LoggingReceive {
    case StartCheckout =>
      persist(CheckoutStarted) { event =>
        updateState(event, Some(scheduler.scheduleOnce(timerDuration, self, ExpireCheckout)))
      }
  }

  def selectingDelivery(timer: Cancellable): Receive = LoggingReceive {
    case SelectDeliveryMethod(method) =>
      persist(DeliveryMethodSelected(method)) { event =>
        updateState(event, Some(timer))
      }
    case CancelCheckout =>
      persist(CheckoutCancelled) { event =>
        updateState(event, Some(timer))
      }
    case ExpireCheckout =>
      persist(CheckoutCancelled) { event =>
        updateState(event)
      }
  }

  def selectingPaymentMethod(timer: Cancellable): Receive = LoggingReceive {
    case SelectPayment(payment) => {
      val paymentRef = context.actorOf(Payment.props(payment, sender, self))
      persist(PaymentStarted(paymentRef)) { event =>
        updateState(event, Some(scheduler.scheduleOnce(timerDuration, self, ExpireCheckout)))
      }
    }
    case CancelCheckout =>
      persist(CheckoutCancelled) { event =>
        updateState(event, Some(timer))
      }
    case ExpireCheckout =>
      persist(CheckoutCancelled) { event =>
        updateState(event)
      }
  }

  def processingPayment(timer: Cancellable): Receive = LoggingReceive {
    case ConfirmPaymentReceived =>
      persist(CheckOutClosed) { event =>
        cartActor ! ConfirmCheckoutClosed
        updateState(event, Some(timer))
      }
    case CancelCheckout =>
      persist(CheckoutCancelled) { event =>
        updateState(event, Some(timer))
      }
    case ExpireCheckout =>
      persist(CheckoutCancelled) { event =>
        updateState(event)
      }
  }

  def cancelled: Receive = LoggingReceive {
    case _ => {
      context stop self
    }
  }

  def closed: Receive = LoggingReceive {
    case _ => {
      context stop self
    }
  }

  override def receiveRecover: Receive = {
    case event: Event => updateState(event)
  }
}
