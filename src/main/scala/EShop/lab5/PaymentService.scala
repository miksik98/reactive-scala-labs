package EShop.lab5

import EShop.lab5.Payment.{PaymentRejected, PaymentRestarted}
import EShop.lab5.PaymentService.{PaymentClientError, PaymentServerError, PaymentSucceeded}
import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}

import scala.concurrent._
import ExecutionContext.Implicits.global

object PaymentService {

  case object PaymentSucceeded // http status 200
  class PaymentClientError extends Exception // http statuses 400, 404
  class PaymentServerError extends Exception // http statuses 500, 408, 418

  def props(method: String, payment: ActorRef) = Props(new PaymentService(method, payment))

}

class PaymentService(method: String, payment: ActorRef) extends Actor with ActorLogging {

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  private val http = Http(context.system)
  private val URI  = getURI

  override def preStart(): Unit = {
    http
      .singleRequest(HttpRequest(method = HttpMethods.GET, uri = URI))
      .onComplete {
        case scala.util.Success(response) =>
          self ! response
        case scala.util.Failure(response) =>
          self ! response
      }
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = payment ! PaymentRestarted

  override def postStop(): Unit = payment ! PaymentRejected

  override def receive: Receive = LoggingReceive {
    case HttpResponse(StatusCodes.OK, _, _, _) =>
      payment ! PaymentSucceeded
    case HttpResponse(error: StatusCodes.ClientError, _, _, _) =>
      throw new PaymentClientError()
    case HttpResponse(error: StatusCodes.ServerError, _, _, _) =>
      throw new PaymentServerError()
    case Failure(exception) =>
      throw exception
  }

  private def getURI: String = method match {
    case "payu"   => "http://127.0.0.1:9000"
    case "paypal" => s"http://httpbin.org/status/408"
    case "visa"   => s"http://httpbin.org/status/200"
    case _        => s"http://httpbin.org/status/404"
  }

}
