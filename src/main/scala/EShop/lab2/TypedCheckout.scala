package EShop.lab2

import EShop.lab2.TypedCartActor.ConfirmCheckoutClosed
import EShop.lab3.TypedOrderManager.ConfirmPaymentStarted
import EShop.lab3.{TypedOrderManager, TypedPayment}
import akka.actor.Cancellable
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import cats.implicits.catsSyntaxOptionId

import scala.language.postfixOps
import scala.concurrent.duration._

object TypedCheckout {
  sealed trait Command
  case object StartCheckout                                                                       extends Command
  case class SelectDeliveryMethod(method: String)                                                 extends Command
  case object CancelCheckout                                                                      extends Command
  case object ExpireCheckout                                                                      extends Command
  case class SelectPayment(payment: String, orderManagerRef: ActorRef[TypedOrderManager.Command]) extends Command
  case object ExpirePayment                                                                       extends Command
  case object ConfirmPaymentReceived                                                              extends Command

  sealed trait Event
  case object CheckOutClosed                                         extends Event
  case class PaymentStarted(payment: ActorRef[TypedPayment.Command]) extends Event
  case object CheckoutStarted                                        extends Event
  case object CheckoutCancelled                                      extends Event
  case class DeliveryMethodSelected(method: String)                  extends Event

  sealed abstract class State(val timerOpt: Option[Cancellable])
  case object WaitingForStart                           extends State(None)
  case class SelectingDelivery(timer: Cancellable)      extends State(Some(timer))
  case class SelectingPaymentMethod(timer: Cancellable) extends State(Some(timer))
  case object Closed                                    extends State(None)
  case object Cancelled                                 extends State(None)
  case class ProcessingPayment(timer: Cancellable)      extends State(Some(timer))
}

class TypedCheckout(
  cartActor: ActorRef[TypedCartActor.Command]
) {
  import TypedCheckout._

  val checkoutTimerDuration: FiniteDuration = 1 seconds
  val paymentTimerDuration: FiniteDuration  = 1 seconds

  def start: Behavior[TypedCheckout.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case StartCheckout =>
          selectingDelivery(context.scheduleOnce(checkoutTimerDuration, context.self, ExpireCheckout))
    }
  )

  def selectingDelivery(timer: Cancellable): Behavior[TypedCheckout.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case SelectDeliveryMethod(_) =>
          selectingPaymentMethod(timer)
        case CancelCheckout =>
          timer.cancel()
          cancelled
        case ExpireCheckout =>
          cancelled
    }
  )

  def selectingPaymentMethod(timer: Cancellable): Behavior[TypedCheckout.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case SelectPayment(payment, orderManagerRef) =>
          val paymentRef = context.spawn(new TypedPayment(payment, orderManagerRef, context.self).start, "TypedPayment")
          orderManagerRef ! ConfirmPaymentStarted(paymentRef)
          timer.cancel()
          processingPayment(context.scheduleOnce(paymentTimerDuration, context.self, ExpirePayment))
        case CancelCheckout =>
          timer.cancel()
          cancelled
        case ExpireCheckout =>
          cancelled
    }
  )

  def processingPayment(timer: Cancellable): Behavior[TypedCheckout.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case ConfirmPaymentReceived =>
          cartActor ! ConfirmCheckoutClosed
          timer.cancel()
          closed
        case CancelCheckout =>
          timer.cancel()
          cancelled
        case ExpirePayment =>
          cancelled
    }
  )

  def cancelled: Behavior[TypedCheckout.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case _ =>
          Behaviors.stopped
    }
  )

  def closed: Behavior[TypedCheckout.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case _ =>
          Behaviors.stopped
    }
  )

}
