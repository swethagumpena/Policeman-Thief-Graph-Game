import akka.actor.ActorSystem
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestProbe
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import routes.GameRoutes
import scala.concurrent.duration._

class RouteTest extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {

  implicit val timeout: FiniteDuration = 30.seconds

  val testSystem: ActorSystem = ActorSystem("akka-http-policeman-thief")
  val testProbe: TestProbe = TestProbe()
  val gameRoutes: GameRoutes = new GameRoutes(testSystem, testProbe.ref)

  "GameRoutes" should {

    "return 'Hello, Akka HTTP!' for GET request to /health" in {
      Get("/health") ~> gameRoutes.routes ~> check {
        responseAs[String] shouldEqual "Hello, Akka HTTP!"
      }
    }
  }
}