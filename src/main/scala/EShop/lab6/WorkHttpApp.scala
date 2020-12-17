package EShop.lab6

import EShop.lab5.ProductCatalog.{GetItems, Items}
import EShop.lab5.{ProductCatalog, SearchService}
import EShop.lab6.CounterOfProductCatalog.CounterSpecificInfo
import EShop.lab6.HttpWorker.Response
import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.event.LoggingReceive
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout
import spray.json.{DefaultJsonProtocol, JsObject, JsString, JsValue, JsonFormat}

import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object HttpWorker {
  case class Work(work: String)
  case class Response(result: String)
}

class HttpWorker extends Actor with ActorLogging {
  import HttpWorker._

  def receive: Receive = LoggingReceive {
    case Work(a) =>
      log.info(s"I got to work on $a")
      sender ! Response("Done")
  }

}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val workerWork     = jsonFormat1(HttpWorker.Work)
  implicit val workerResponse = jsonFormat1(HttpWorker.Response)

  //custom formatter just for example
  implicit val uriFormat = new JsonFormat[java.net.URI] {
    override def write(obj: java.net.URI): spray.json.JsValue = JsString(obj.toString)
    override def read(json: JsValue): URI = json match {
      case JsString(url) => new URI(url)
      case _             => throw new RuntimeException("Parsing exception")
    }
  }
  implicit val counterFormat = new JsonFormat[CounterSpecificInfo] {
    override def write(obj: CounterSpecificInfo): JsValue = {
      JsObject(
        Map(
          "instanceName" -> JsString(obj.instanceName),
          "counter"      -> JsString(obj.counter.toString)
        )
      )
    }

    override def read(json: JsValue): CounterSpecificInfo = {
      CounterSpecificInfo(
        json.asJsObject.getFields("instanceName").head.toString,
        IntJsonFormat.read(json.asJsObject.getFields("counter").head)
      )
    }
  }

}

object WorkHttpApp extends App {
  new WorkHttpServer().startServer("localhost", 9000)
}

class WorkHttpServer extends HttpApp with JsonSupport {

  val system  = ActorSystem("ReactiveRouters")
  val workers = system.actorOf(RoundRobinPool(5).props(ProductCatalog.props(new SearchService())), "workersRouter")

  implicit val timeout: Timeout = 5.seconds

  override protected def routes: Route = {
    path("work") {
      post {
        entity(as[HttpWorker.Work]) { _ =>
          complete {
            (workers ? GetItems("gerber", List("cream"))).mapTo[Items].map(items => Response(items.toString))
          }
        }
      }
    }
  }

}
