package EShop.lab3

import EShop.lab2.TypedCartActor.StartCheckout
import EShop.lab2.TypedCheckout.{SelectDeliveryMethod, SelectPayment}
import EShop.lab2.{TypedCartActor, TypedCheckout}
import EShop.lab3.TypedPayment.DoPayment
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object TypedOrderManager {

  sealed trait Command
  case class AddItem(id: String, sender: ActorRef[Ack])                                               extends Command
  case class RemoveItem(id: String, sender: ActorRef[Ack])                                            extends Command
  case class SelectDeliveryAndPaymentMethod(delivery: String, payment: String, sender: ActorRef[Ack]) extends Command
  case class Buy(sender: ActorRef[Ack])                                                               extends Command
  case class Pay(sender: ActorRef[Ack])                                                               extends Command
  case class ConfirmCheckoutStarted(checkoutRef: ActorRef[TypedCheckout.Command])                     extends Command
  case class ConfirmPaymentStarted(paymentRef: ActorRef[TypedPayment.Command])                        extends Command
  case object ConfirmPaymentReceived                                                                  extends Command

  sealed trait Ack
  case object Done extends Ack //trivial ACK
}

class TypedOrderManager {

  import TypedOrderManager._

  def start: Behavior[TypedOrderManager.Command] = uninitialized

  def uninitialized: Behavior[TypedOrderManager.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case AddItem(id, sender) =>
          val cartActor = context.spawn(new TypedCartActor().start, "TypedCart")
          cartActor ! TypedCartActor.AddItem(id)
          sender ! Done
          open(cartActor)
    }
  )

  def open(cartActor: ActorRef[TypedCartActor.Command]): Behavior[TypedOrderManager.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case AddItem(id, sender) =>
          cartActor ! TypedCartActor.AddItem(id)
          sender ! Done
          Behaviors.same
        case RemoveItem(id, sender) =>
          cartActor ! TypedCartActor.RemoveItem(id)
          sender ! Done
          Behaviors.same
        case Buy(sender) =>
          cartActor ! StartCheckout(context.self)
          inCheckoutWaitingForConfirmationCheckout(cartActor, sender)
    }
  )

  def inCheckoutWaitingForConfirmationCheckout(
    cartActorRef: ActorRef[TypedCartActor.Command],
    senderRef: ActorRef[Ack]
  ): Behavior[TypedOrderManager.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case ConfirmCheckoutStarted(checkoutRef) =>
          senderRef ! Done
          inCheckout(checkoutRef)
    }
  )

  def inCheckoutWaitingForConfirmationPayment(
    checkoutActorRef: ActorRef[TypedCheckout.Command],
    senderRef: ActorRef[Ack]
  ): Behavior[TypedOrderManager.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case ConfirmPaymentStarted(paymentRef) =>
          senderRef ! Done
          inPayment(paymentRef, senderRef)
    }
  )

  def inCheckout(checkoutActorRef: ActorRef[TypedCheckout.Command]): Behavior[TypedOrderManager.Command] =
    Behaviors.receive(
      (context, msg) =>
        msg match {
          case SelectDeliveryAndPaymentMethod(delivery, payment, sender) =>
            checkoutActorRef ! SelectDeliveryMethod(delivery)
            checkoutActorRef ! SelectPayment(payment, context.self)
            inCheckoutWaitingForConfirmationPayment(checkoutActorRef, sender)
      }
    )

  def inPayment(senderRef: ActorRef[Ack]): Behavior[TypedOrderManager.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case ConfirmPaymentReceived =>
          senderRef ! Done
          finished
    }
  )

  def inPayment(
    paymentActorRef: ActorRef[TypedPayment.Command],
    senderRef: ActorRef[Ack]
  ): Behavior[TypedOrderManager.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case Pay(sender) =>
          paymentActorRef ! DoPayment
          inPayment(sender)
    }
  )

  def finished: Behavior[TypedOrderManager.Command] = Behaviors.stopped
}
