package EShop.lab6

import EShop.lab5.ProductCatalog.{GetItems, Items}
import EShop.lab5.{PaymentService, ProductCatalog, SearchService}
import akka.actor._
import akka.event.LoggingReceive
import akka.routing._

object Master {
  case class WorkToDistribute(work: String)
}

class Master extends Actor with ActorLogging {
  val nbOfRoutees = 5

  val routees = Vector.fill(nbOfRoutees) {
    val r = context.actorOf(ProductCatalog.props(new SearchService()))
    context watch r // we subscribe for akka.actor.Terminated messages, we want to know when some worker was terminated
    ActorRefRoutee(r)
  }

  def receive: Receive = master(Router(BroadcastRoutingLogic(), routees))

  def master(router: Router): Receive = LoggingReceive {
    case Master.WorkToDistribute(_) =>
      router.route(GetItems("gerber", List("cream")), sender())

    case Terminated(a) => // some worker was terminated
      val r = router.removeRoutee(a)
      if (r.routees.isEmpty)
        context.system.terminate
      else
        context.become(master(r))
  }
}

object Client {
  case object Init
}

class Client extends Actor {
  import Client._

  def receive: Receive = LoggingReceive {
    case Init =>
      val master = context.actorOf(Props(classOf[Master]), "master")
      master ! Master.WorkToDistribute("Find some product")
  }
}

object RoutersDemo extends App {

  val system = ActorSystem("ReactiveRouters")

  val client = system.actorOf(Props(classOf[Client]), "client")

  client ! Client.Init

}

object SimpleRouterDemo extends App {

  val system = ActorSystem("ReactiveRouters")

  val workers =
    system.actorOf(BroadcastPool(5).props(ProductCatalog.props(new SearchService())), "ProductCatalogRouters")

  workers ! GetItems("gerber", List("cream"))
}
