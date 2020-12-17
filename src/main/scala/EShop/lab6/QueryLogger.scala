package EShop.lab6

import EShop.lab5.ProductCatalog.ProductCatalogQuery
import EShop.lab6.CounterOfProductCatalog.IncrementCounter
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe, SubscribeAck}
import akka.event.LoggingReceive

object QueryLogger {
  def props(counterOfProductCatalog: ActorRef): Props = Props(new QueryLogger(counterOfProductCatalog))
}

class QueryLogger(counterOfProductCatalog: ActorRef) extends Actor with ActorLogging {
  val mediator: ActorRef = DistributedPubSub(context.system).mediator

  mediator ! Subscribe("query", self)

  //just for test
  Thread.sleep(4000)
  mediator ! Publish("query", ProductCatalogQuery("productcatalog", "gerber cream"))

  override def receive: Receive = LoggingReceive {
    case pcQuery: ProductCatalogQuery =>
      log.info("Got query: {}", pcQuery)
      counterOfProductCatalog ! IncrementCounter(pcQuery.name)
    case SubscribeAck(Subscribe("query", None, `self`)) =>
      log.info("subscribing")
  }
}
