package EShop.lab2

import EShop.lab2.CartActor.{AddItem, CheckoutStarted, RemoveItem, StartCheckout}
import EShop.lab2.Checkout.{
  CancelCheckout,
  CheckOutClosed,
  ConfirmPaymentReceived,
  PaymentStarted,
  ProcessingPaymentStarted,
  SelectDeliveryMethod,
  SelectPayment,
  SelectingDeliveryStarted,
  Uninitialized
}
import akka.actor.{Actor, ActorRef, Props}
import akka.event.{Logging, LoggingReceive}

object Customer {
  case object Init
}

class Customer extends Actor {

  import Customer._

  val cart: ActorRef = context.actorOf(CartActor.props(self))
  private val log    = Logging(context.system, this)

  def receive: Receive = LoggingReceive {

    case Init =>
      cart ! AddItem("CD")
      cart ! AddItem("book")
      cart ! AddItem("DVD")
      cart ! AddItem("phone")
      cart ! RemoveItem("DVD")
      cart ! RemoveItem("DVD")

      cart ! StartCheckout

    case CheckoutStarted(checkout: ActorRef) =>
      checkout ! SelectDeliveryMethod("inpost")
      checkout ! SelectPayment("credit card")

    case CheckOutClosed =>
      log.debug("Customer: checkout closed!")

    case PaymentStarted(payment: ActorRef) =>
      payment ! ConfirmPaymentReceived

    case ProcessingPaymentStarted(timer) =>
      log.debug("Customer: processing payment started")

    case SelectingDeliveryStarted(timer) =>
      log.debug("Customer: selecting delivery started")

    case Uninitialized =>
      log.debug("Customer: checkout is uninitialized")
  }
}
