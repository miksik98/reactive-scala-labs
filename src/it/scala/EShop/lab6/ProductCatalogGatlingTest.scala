package EShop.lab6

import io.gatling.core.Predef.{Simulation, StringBody, rampUsers, scenario, _}
import io.gatling.http.Predef.http

import scala.concurrent.duration._

class ProductCatalogGatlingTest extends Simulation {

  val httpProtocol1 = http
    .baseUrls("http://localhost:9001", "http://localhost:9002", "http://localhost:9003")
    .acceptHeader("text/plain,text/html,application/json,application/xml;")
    .userAgentHeader("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0")

  val scn1 = scenario("ClusterSimulation")
    .exec(
      http("cluster")
        .post("/work")
        .body(StringBody("""{ "work": "work" }"""))
        .asJson
    )
    .pause(5)



  val httpProtocol2 = http
    .baseUrls("http://localhost:9000")
    .acceptHeader("text/plain,text/html,application/json,application/xml;")
    .userAgentHeader("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0")

  val scn2 = scenario("RoutersSimulation")
    .exec(
      http("router")
        .post("/work")
        .body(StringBody("""{ "work": "work" }"""))
        .asJson
    )
    .pause(5)

  setUp(
    scn1.inject(rampUsers(10).during(1.minutes)),
    scn2.inject(rampUsers(6000).during(1.minutes))
  ).protocols(httpProtocol1, httpProtocol2)
}