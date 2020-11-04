package EShop.lab3

import EShop.lab2.{Cart, CartActor, Checkout}
import EShop.lab3.OrderManager.ConfirmCheckoutStarted
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.util.Timeout
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._
import akka.pattern.ask
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class CartTest
  extends TestKit(ActorSystem("CartTest"))
  with AnyFlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll
  with Matchers
  with ScalaFutures {

  import CartActor._

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  implicit val timeout: Timeout = 1.second

  val itemID = "CD"

  //use GetItems command which was added to make test easier
  it should "add item properly" in {
    val cartActor         = TestActorRef[CartActor]
    val itemsBeforeAdding = cartActor ? GetItems
    assert(itemsBeforeAdding.futureValue == List.empty)
    cartActor ! AddItem(itemID)
    val itemsAfterAdding = cartActor ? GetItems
    assert(itemsAfterAdding.futureValue == List(itemID))
  }

  it should "be empty after adding and removing the same item" in {
    val cartActor = TestActorRef[CartActor]
    cartActor ! AddItem(itemID)
    cartActor ! RemoveItem(itemID)
    val itemsAfterAddAndRemove = cartActor ? GetItems
    assert(itemsAfterAddAndRemove.futureValue == List.empty)
  }

  it should "start checkout" in {
    val cartActor = system.actorOf(Props[CartActor])
    cartActor ! AddItem(itemID)
    cartActor ! StartCheckout
    expectMsgPF() {
      case ConfirmCheckoutStarted(_) => ()
    }
    cartActor ! GetItems
    expectMsg(List(itemID))
    cartActor ! RemoveItem(itemID)
    cartActor ! GetItems
    expectMsg(List(itemID))
  }
}
