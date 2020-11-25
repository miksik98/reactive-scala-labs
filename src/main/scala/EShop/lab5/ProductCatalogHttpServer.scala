package EShop.lab5

import java.net.URI

import EShop.lab5.ProductCatalog.{GetItems, Item, Items}
import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.pattern.ask
import akka.util.Timeout
import spray.json.{DefaultJsonProtocol, JsArray, JsObject, JsString, JsValue, JsonFormat}

import scala.concurrent.duration._
import scala.util.Success

object ProductCatalogHttpServer extends App {
  def startServer(productCatalog: ActorRef): Unit = {
    val rest = new ProductCatalogHttpServer(productCatalog)
    rest.startServer("localhost", 7777)
  }
}

trait JsonItemSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val itemsFormat = new JsonFormat[Items] {
    override def write(obj: Items): spray.json.JsArray =
      JsArray(
        obj.items
          .map(
            item =>
              JsObject(
                Map(
                  "id"    -> JsString(item.id.toString),
                  "brand" -> JsString(item.brand),
                  "name"  -> JsString(item.name),
                  "price" -> JsString(item.price.toString),
                  "count" -> JsString(item.count.toString)
                )
            )
          )
      )
    override def read(json: JsValue): Items = json match {
      case JsArray(items) =>
        Items(
          items
            .map(
              obj =>
                Item(
                  new URI(obj.asJsObject.getFields("id").head.toString),
                  obj.asJsObject.getFields("brand").head.toString,
                  obj.asJsObject.getFields("name").head.toString,
                  BigDecimalJsonFormat.read(obj.asJsObject.getFields("price").head),
                  IntJsonFormat.read(obj.asJsObject.getFields("count").head)
              )
            )
            .toList
        )
      case _ => throw new RuntimeException("Parsing exception")
    }
  }

}

class ProductCatalogHttpServer(productCatalog: ActorRef) extends HttpApp with JsonItemSupport {

  implicit val timeout: Timeout = 5 seconds

  override protected def routes: Route = {
    path("catalog") {
      pathEndOrSingleSlash {
        get {
          parameters("brand", "keywords".as[String].repeated) { (brand, keywords) =>
            onComplete(productCatalog ? GetItems(brand, keywords.toList)) {
              case Success(items: Items) =>
                complete(itemsFormat.write(items))
              case _ =>
                complete(StatusCodes.InternalServerError)
            }
          }
        }
      }
    }
  }
}
