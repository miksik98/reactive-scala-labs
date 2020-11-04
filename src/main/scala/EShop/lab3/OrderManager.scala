package EShop.lab3

import EShop.lab2
import EShop.lab2.CartActor.StartCheckout
import EShop.lab2.Checkout.{SelectDeliveryMethod, SelectPayment}
import EShop.lab2.{Cart, CartActor, Checkout}
import EShop.lab3.OrderManager._
import EShop.lab3.Payment.DoPayment
import akka.actor
import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive

object OrderManager {

  sealed trait Command
  case class AddItem(id: String)                                               extends Command
  case class RemoveItem(id: String)                                            extends Command
  case class SelectDeliveryAndPaymentMethod(delivery: String, payment: String) extends Command
  case object Buy                                                              extends Command
  case object Pay                                                              extends Command
  case class ConfirmCheckoutStarted(checkoutRef: ActorRef)                     extends Command
  case class ConfirmPaymentStarted(paymentRef: ActorRef)                       extends Command
  case object ConfirmPaymentReceived                                           extends Command

  sealed trait Ack
  case object Done extends Ack //trivial ACK
}

class OrderManager extends Actor {

  override def receive = uninitialized

  def uninitialized: Receive = LoggingReceive {
    case AddItem(id) =>
      var cartActor = context.actorOf(CartActor.props());
      cartActor ! CartActor.AddItem(id)
      sender ! Done
      context become open(cartActor)
  }

  def open(cartActor: ActorRef): Receive = LoggingReceive {
    case AddItem(id) =>
      cartActor ! CartActor.AddItem(id)
      sender ! Done
    case RemoveItem(id) =>
      cartActor ! CartActor.RemoveItem(id)
      sender ! Done
    case Buy =>
      cartActor ! StartCheckout
      context become inCheckout(cartActor, sender)
  }

  def inCheckout(cartOrCheckoutActorRef: ActorRef, senderRef: ActorRef): Receive = {
    case ConfirmCheckoutStarted(checkoutRef) =>
      senderRef ! Done
      context become inCheckout(checkoutRef)
    case ConfirmPaymentStarted(paymentRef) =>
      senderRef ! Done
      context become inPayment(paymentRef)
  }

  def inCheckout(checkoutActorRef: ActorRef): Receive = LoggingReceive {
    case SelectDeliveryAndPaymentMethod(delivery, payment) =>
      checkoutActorRef ! SelectDeliveryMethod(delivery)
      checkoutActorRef ! SelectPayment(payment)
      context become inCheckout(checkoutActorRef, sender)
  }

  def inPayment(paymentActorRef: ActorRef): Receive = LoggingReceive {
    case Pay =>
      paymentActorRef ! DoPayment
      context become inPayment(paymentActorRef, sender)
  }

  def inPayment(paymentActorRef: ActorRef, senderRef: ActorRef): Receive = {
    case ConfirmPaymentReceived =>
      senderRef ! Done
      context become finished
  }

  def finished: Receive = LoggingReceive {
    case _ => {
      sender ! "order manager finished job"
      context stop self
    }
  }
}
