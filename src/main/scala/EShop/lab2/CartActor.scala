package EShop.lab2

import EShop.lab2.Checkout.CancelCheckout
import EShop.lab3.OrderManager
import EShop.lab3.OrderManager.ConfirmCheckoutStarted
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
  case class CheckoutStarted(checkoutRef: ActorRef, cart: Cart) extends Event
  case class ItemAdded(itemId: Any, cart: Cart)                 extends Event
  case class ItemRemoved(itemId: Any, cart: Cart)               extends Event
  case object CartEmptied                                       extends Event
  case object CartExpired                                       extends Event
  case object CheckoutClosed                                    extends Event
  case class CheckoutCancelled(cart: Cart)                      extends Event

  def props() = Props(new CartActor())
}

class CartActor() extends Actor {

  import CartActor._

  import CartActor._

  private val log       = Logging(context.system, this)
  val cartTimerDuration = 5 seconds

  private def scheduleTimer: Cancellable =
    context.system.scheduler.scheduleOnce(cartTimerDuration, self, ExpireCart)

  def receive: Receive = empty

  def empty: Receive = LoggingReceive {
    case AddItem(item) =>
      val cart = Cart.empty
      context become nonEmpty(cart.addItem(item), scheduleTimer)
    case GetItems =>
      sender ! Cart.empty.items
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
      var checkout = context.actorOf(Checkout.props(self))
      checkout ! Checkout.StartCheckout
      sender ! ConfirmCheckoutStarted(checkout)
      context become inCheckout(cart)
    case GetItems =>
      sender ! cart.items
  }

  def inCheckout(cart: Cart): Receive = LoggingReceive {
    case ConfirmCheckoutClosed | Checkout.CheckOutClosed =>
      context become empty
    case ConfirmCheckoutCancelled =>
      context become nonEmpty(cart, scheduleTimer)
    case GetItems =>
      sender ! cart.items
  }

}
