package com.lemsst

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

import java.util.concurrent.ThreadLocalRandom

class RecordedSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://computer-database.gatling.io.")
    .inferHtmlResources(BlackList(""".*\.js""", """.*\.css""", """.*\.gif""", """.*\.jpeg""", """.*\.jpg""", """.*\.ico""", """.*\.woff""", """.*\.woff2""", """.*\.(t|o)tf""", """.*\.png""", """.*detectportal\.firefox\.com.*"""), WhiteList())
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.9")
    .upgradeInsecureRequestsHeader("1")
    .userAgentHeader("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36")

  val users = scenario("Regular users")
    .exec(Search.search, Paginate.paginate)
  val admins = scenario("Administrators")
    .exec(Search.search, Paginate.paginate, Edit.tryMaxEdit)

  setUp(
    users.inject(rampUsers(1) during (10 seconds)),
    admins.inject(rampUsers(1) during (10 seconds))
  ).protocols(httpProtocol)
}


object Search {
  val headers_0 = Map("Proxy-Connection" -> "keep-alive")
  val feeder = csv("data/search.csv").random

  val search = exec(http("request_0")
    .get("/")
    .headers(headers_0))
    .feed(feeder)
    .pause(6)
    .exec(http("request_1")
      .get("/computers?f=${searchCriterion}")
      .headers(headers_0)
      .check(css("a:contains('${searchComputerName}')", "href").saveAs("computerURL")))
    .pause(10)
    .exec(http("Select")
      .get("${computerURL}"))
    .pause(10)
}

object ViewDetails {
  val headers_0 = Map("Proxy-Connection" -> "keep-alive")
  // Open the details
  val viewDetails = exec(http("request_2")
    .get("/computers/89")
    .headers(headers_0))
    .pause(9)
    // Go back to the homepage
    .exec(http("request_3")
    .get("/computers")
    .headers(headers_0))
    .pause(11)
}

object Paginate {
  val headers_0 = Map("Proxy-Connection" -> "keep-alive")
  // Navigate through the pages
  val paginate = repeat(5, "n") {
    exec(http("Page ${n}")
      .get("/computers?p=${n}")
      .headers(headers_0))
      .pause(1)
  }
}

object Edit {
  val headers_0 = Map("Proxy-Connection" -> "keep-alive")

  val headers_10 = Map(
    "Origin" -> "http://computer-database.gatling.io",
    "Proxy-Connection" -> "keep-alive")
  // Add a new computer
  val edit = exec(http("request_9")
    .get("/computers/new")
    .headers(headers_0))
    .pause(21)
    .exec(http("request_10")
      .post("/computers")
      .headers(headers_10)
      .formParam("name", "9c94a4bd-216d-40ba-a402-5d9a52407112")
      .formParam("introduced", "2019-05-31")
      .formParam("discontinued", "")
      .formParam("company", "27")
        .check(status.is(session => 200 + ThreadLocalRandom.current.nextInt(2)))

    )

  val tryMaxEdit = tryMax(2) {
    exec(edit)
  }.exitHereIfFailed

}
