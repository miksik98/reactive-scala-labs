package EShop.lab3

import EShop.lab2.CartActor.{AddItem, CheckoutStarted, ConfirmCheckoutClosed}
import EShop.lab2.{CartActor, Checkout}
import EShop.lab3.OrderManager.{ConfirmCheckoutStarted, ConfirmPaymentStarted}
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout

class CheckoutTest
  extends TestKit(ActorSystem("CheckoutTest"))
  with AnyFlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll
  with Matchers
  with ScalaFutures {

  import Checkout._

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  var cart: TestProbe                  = _
  var checkout: TestActorRef[Checkout] = _

  override protected def beforeAll(): Unit =
    cart = TestProbe("CheckoutSpec")

  implicit val timeout: Timeout = 1.second

  it should "Send close confirmation to cart" in {
    checkout = TestActorRef(Checkout.props(cart.testActor), cart.testActor)
    checkout ! StartCheckout
    checkout ! SelectDeliveryMethod("inpost")
    checkout ! SelectPayment("paypal")
    expectMsgPF() {
      case ConfirmPaymentStarted(_) => ()
    }
    checkout ! ConfirmPaymentReceived
    cart.expectMsg(ConfirmCheckoutClosed)
  }

}
