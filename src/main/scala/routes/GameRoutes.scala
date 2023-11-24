package routes

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import actors._
import utils.JsonSupport
import scala.util.{Failure, Success}

class GameRoutes(system: ActorSystem, gameActor: ActorRef)(implicit val ec: ExecutionContext) extends JsonSupport {
  implicit val timeout: Timeout = Timeout(55.seconds)

  val routes: Route =
    path("health") {
      get {
        complete(StatusCodes.OK, "Hello, Akka HTTP!")
      }
    } ~
      pathPrefix("move") {
        path("police") {
          get {
            val result = (gameActor ? MovePolice).mapTo[GameResponse]
            complete(result)
          }
        } ~
          path("thief") {
            get {
              val result = (gameActor ? MoveThief).mapTo[GameResponse]
              complete(result)
            }
          }
      } ~
      path("state") {
        get {
          val result = (gameActor ? GetState).mapTo[GameResponse]
          complete(result)
        }
      } ~
      path("restart") {
        get {
          val result = (gameActor ? RestartGame).mapTo[MoveResult]
          complete(result)
        }
      } ~ path("autoPlay") {
      get {
        handleAutoPlay()
      }
    }

  private def handleAutoPlay(): Route = {
    onComplete(autoPlayGame()) {
      case Success(result) =>
        complete(StatusCodes.OK, result)
      case Failure(ex) =>
        complete(StatusCodes.InternalServerError, s"Unexpected error: ${ex.getMessage}")
    }
  }

  private def autoPlayGame(): Future[MoveResult] =
    (gameActor ? GetState).flatMap {
      case StateResult(_) =>
        // If the game is not over, continue playing
        moveThiefAndPolice().map(result => MoveResult(result.message))
      case moveResult@MoveResult(_) =>
        // If the game is already over, return the result
        Future.successful(moveResult)
    } recover {
      case ex => MoveResult(s"Unexpected error: ${ex.getMessage}")
    }

  private def moveThiefAndPolice(): Future[MoveResult] = {
    for {
      thiefResult <- (gameActor ? MoveThief).mapTo[GameResponse]
      policeResult <- (gameActor ? MovePolice).mapTo[GameResponse]
      result <- (thiefResult, policeResult) match {
        case (MoveResult(thiefMove), StateResult(policeState)) =>
          Future.successful(MoveResult(s"$thiefMove"))
        case (StateResult(thiefState), MoveResult(policeMove)) =>
          Future.successful(MoveResult(s"$policeMove"))
        case (StateResult(_), StateResult(_)) =>
          moveThiefAndPolice()
        case (MoveResult(thiefMove), MoveResult(policeMove)) =>
          Future.successful(MoveResult(s"$thiefMove"))
        case _ =>
          Future.successful(MoveResult("Unexpected response"))
      }
    } yield result
  }
}
