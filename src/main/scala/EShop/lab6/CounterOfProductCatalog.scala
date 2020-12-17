package EShop.lab6

import EShop.lab6.CounterOfProductCatalog._
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.util.Success

object CounterOfProductCatalog {

  sealed trait Command
  sealed trait Response

  case class GetCounterFor(instanceName: String)                     extends Command
  case class GetCounterMap()                                         extends Command
  case class IncrementCounter(instanceName: String)                  extends Command
  case class CounterSpecificInfo(instanceName: String, counter: Int) extends Response
  case class CounterGeneralInfo(counterMap: Map[String, Int])        extends Response

  def props(instancesNames: Seq[String]): Props = Props(
    new CounterOfProductCatalog(instancesNames)
  )
}

class CounterOfProductCatalog(instancesNames: Seq[String]) extends Actor with ActorLogging {
  var instancesMap: Map[String, Int] = instancesNames.map(name => name -> 0).toMap

  val queryLogger: ActorRef = context.actorOf(QueryLogger.props(self))

  override def receive: Receive = LoggingReceive {
    case GetCounterFor(instanceName) => sender ! CounterSpecificInfo(instanceName, instancesMap(instanceName))
    case GetCounterMap               => sender ! CounterGeneralInfo(instancesMap)
    case IncrementCounter(instanceName) =>
      this.synchronized {
        instancesMap = instancesMap.filter(_._1 == instanceName).mapValues(_ + 1)

        //just for test
        log.info("{}", instancesMap)
      }
  }
}

object CounterOfProductCatalogApp extends App {
  def startServer(counterOfProductCatalog: ActorRef): Unit =
    new CounterOfProductCatalogServer(counterOfProductCatalog).startServer("localhost", 6000)
}

class CounterOfProductCatalogServer(counterOfProductCatalog: ActorRef) extends HttpApp with JsonSupport {
  implicit val timeout: Timeout = 5 seconds

  override protected def routes: Route = {
    path("pc") {
      get {
        parameters("instance") { instance =>
          onComplete(counterOfProductCatalog ? GetCounterFor(instance)) {
            case Success(CounterSpecificInfo(name, counter)) =>
              complete(counterFormat.write(CounterSpecificInfo(name, counter)))
            case _ =>
              complete(StatusCodes.InternalServerError)
          }
        }
      }
    }
  }
}
