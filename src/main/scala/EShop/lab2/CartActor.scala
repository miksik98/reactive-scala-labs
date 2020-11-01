package EShop.lab2

import EShop.lab2.Checkout.CancelCheckout
import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

object CartActor {

  sealed trait Command
  case class AddItem(item: Any)        extends Command
  case class RemoveItem(item: Any)     extends Command
  case object ExpireCart               extends Command
  case object StartCheckout            extends Command
  case object ConfirmCheckoutCancelled extends Command
  case object ConfirmCheckoutClosed    extends Command
  case object GetItems                 extends Command // command made to make testing easier

  sealed trait Event
  case class CheckoutStarted(checkoutRef: ActorRef) extends Event

  def props() = Props(new CartActor())
}

class CartActor() extends Actor {

  import CartActor._

  private val log       = Logging(context.system, this)
  val cartTimerDuration = 5 seconds

  private def scheduleTimer: Cancellable =
    context.system.scheduler.scheduleOnce(cartTimerDuration, self, ExpireCart)

  def receive: Receive = LoggingReceive {
    case AddItem(item) =>
      val cart = Cart.empty
      context become nonEmpty(cart.addItem(item), scheduleTimer)
  }

  def empty: Receive = LoggingReceive {
    case AddItem(item) =>
      val cart = Cart.empty
      context become nonEmpty(cart.addItem(item), scheduleTimer)
  }

  def nonEmpty(cart: Cart, timer: Cancellable): Receive = LoggingReceive {
    case AddItem(item) =>
      context become nonEmpty(cart.addItem(item), timer)
    case RemoveItem(item) =>
      val newCart = cart.removeItem(item)
      if (newCart.size == 0) {
        timer.cancel()
        context become empty
      } else {
        if (newCart.size < cart.size)
          context become nonEmpty(newCart, timer)
      }
    case ExpireCart =>
      timer.cancel()
      context become empty
    case StartCheckout =>
      timer.cancel()
      context become inCheckout(cart)
  }

  def inCheckout(cart: Cart): Receive = LoggingReceive {
    case ConfirmCheckoutClosed | Checkout.CheckOutClosed =>
      context become empty
    case ConfirmCheckoutCancelled =>
      context become nonEmpty(cart, scheduleTimer)
  }

}
