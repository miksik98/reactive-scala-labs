package EShop.lab4

import EShop.lab2.CartActor.Event
import EShop.lab2.{Cart, Checkout}
import EShop.lab3.OrderManager.ConfirmCheckoutStarted
import akka.actor.{Cancellable, Props}
import akka.event.{Logging, LoggingReceive}
import akka.persistence.PersistentActor

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._

object PersistentCartActor {

  def props(persistenceId: String) = Props(new PersistentCartActor(persistenceId))
}

class PersistentCartActor(
  val persistenceId: String
) extends PersistentActor {

  import EShop.lab2.CartActor._

  private val log       = Logging(context.system, this)
  val cartTimerDuration = 5.seconds

  private def scheduleTimer: Cancellable =
    context.system.scheduler.scheduleOnce(cartTimerDuration, self, ExpireCart)

  override def receiveCommand: Receive = LoggingReceive {
    empty
  }

  private def updateState(event: Event, timer: Option[Cancellable] = None): Unit = {
    if (timer.isDefined)
      timer.get.cancel()

    event match {
      case CheckoutClosed =>
        context become empty
      case CartExpired =>
        context become empty
      case CheckoutCancelled(cart) =>
        context become nonEmpty(cart, scheduleTimer)
      case ItemAdded(item, cart) =>
        context become nonEmpty(cart, scheduleTimer)
      case CartEmptied =>
        context become empty
      case ItemRemoved(item, cart) =>
        context become nonEmpty(cart, scheduleTimer)
      case CheckoutStarted(checkoutRef, cart) =>
        checkoutRef ! Checkout.StartCheckout
        sender ! ConfirmCheckoutStarted(checkoutRef)
        context become inCheckout(cart)
    }
  }

  def empty: Receive = LoggingReceive {
    case AddItem(item) =>
      persist(ItemAdded(item, Cart.empty.addItem(item))) { state =>
        updateState(state, Some(scheduleTimer))
      }
  }

  def nonEmpty(cart: Cart, timer: Cancellable): Receive = LoggingReceive {
    case AddItem(item) => persist(ItemAdded(item, cart.addItem(item)))(state => updateState(state))
    case RemoveItem(item) =>
      persist(ItemRemoved(item, cart.removeItem(item)))(state => {
        if (state.cart.size == 0) updateState(CartEmptied, Some(timer))
        else updateState(state)
      })
    case StartCheckout =>
      persist(CheckoutStarted(context.actorOf(Checkout.props(self)), cart)) { event =>
        updateState(event, Some(timer))
      }
    case ExpireCart =>
      persist(CartExpired) { event =>
        updateState(event)
      }
  }

  def inCheckout(cart: Cart): Receive = LoggingReceive {
    case ConfirmCheckoutClosed | Checkout.CheckOutClosed =>
      persist(CheckoutClosed) { state =>
        updateState(state)
      }
    case ConfirmCheckoutCancelled =>
      persist(CheckoutCancelled(cart)) { event =>
        updateState(event)
      }
  }

  override def receiveRecover: Receive = {
    case event: Event => updateState(event)
  }
}
