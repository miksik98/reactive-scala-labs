package EShop.lab2

import akka.actor.{ActorSystem, PoisonPill, Props}
import CartActor._
import Checkout._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object EShopMain extends App {
  val system = ActorSystem("e-shop")

  private def communicationTest(): Unit = {
//    println("______________communication test______________")
//    val actorCustomer = system.actorOf(Props(new Customer))
//
//    actorCustomer ! Init
  }

  private def flowTest(): Unit = {
//    println("___________________flow test___________________")
//    val actorCart = system.actorOf(Props(new CartActor))
//
//    actorCart ! AddItem("book")
//    actorCart ! AddItem("CD")
//    actorCart ! RemoveItem("DVD")
//    actorCart ! RemoveItem("book")
//    actorCart ! CartActor.StartCheckout
//    actorCart ! CartActor.ConfirmCheckoutCancelled
//    actorCart ! RemoveItem("CD")
//    Thread.sleep(5100) //should not expired (empty state)
//    actorCart ! AddItem("Sth")
//    Thread.sleep(5100) //should expired
//    actorCart ! PoisonPill
//
//    var actorCheckout = system.actorOf(Props(new Checkout))
//
//    actorCheckout ! Checkout.StartCheckout
//    actorCheckout ! SelectDeliveryMethod("inpost")
//    actorCheckout ! SelectPayment("credit card")
//    actorCheckout ! ConfirmPaymentReceived
//
//    actorCheckout = system.actorOf(Props(new Checkout))
//
//    actorCheckout ! Checkout.StartCheckout
//    Thread.sleep(1100) //checkout should expired
//
//    actorCheckout = system.actorOf(Props(new Checkout))
//
//    actorCheckout ! Checkout.StartCheckout
//    actorCheckout ! SelectDeliveryMethod("inpost")
//    Thread.sleep(1100) //checkout should expired
//
//    actorCheckout = system.actorOf(Props(new Checkout))
//
//    actorCheckout ! Checkout.StartCheckout
//    actorCheckout ! SelectDeliveryMethod("inpost")
//    actorCheckout ! SelectPayment("credit card")
//    Thread.sleep(1100) //payment should expired
//
//    actorCheckout = system.actorOf(Props(new Checkout))
//
//    actorCheckout ! Checkout.StartCheckout
//    actorCheckout ! CancelCheckout
//
//    actorCheckout = system.actorOf(Props(new Checkout))
//
//    actorCheckout ! Checkout.StartCheckout
//    actorCheckout ! SelectDeliveryMethod("inpost")
//    actorCheckout ! CancelCheckout
//
//    actorCheckout = system.actorOf(Props(new Checkout))
//
//    actorCheckout ! Checkout.StartCheckout
//    actorCheckout ! SelectDeliveryMethod("inpost")
//    actorCheckout ! SelectPayment("credit card")
//    actorCheckout ! CancelCheckout
  }

  communicationTest()
  Thread.sleep(10000)
  flowTest()

  Await.result(system.whenTerminated, Duration.Inf)
}
