import actors.GameActor
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.Materializer
import org.slf4j.LoggerFactory
import routes.GameRoutes
import utils.CreateGuavaGraph.loadAndCreate

import scala.concurrent.ExecutionContextExecutor

object Main extends App {

  // Setting up the logger
  val logger = LoggerFactory.getLogger(getClass)

  val originalValueGraph = loadAndCreate(args(0))
  val perturbedValueGraph = loadAndCreate(args(1))
  val outputFilePath = s"${args(2)}/results.txt"

  // Creating Akka HTTP Actors
  implicit val system: ActorSystem = ActorSystem("akka-http-policeman-thief")
  implicit val materializer: Materializer = Materializer.matFromSystem
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  // Create GameActor
  val gameActor: ActorRef = (originalValueGraph, perturbedValueGraph) match {
    case (Some(originalGraph), Some(perturbedGraph)) =>
      // Both graphs are present, create the actor
      system.actorOf(Props(new GameActor(originalGraph, perturbedGraph, outputFilePath)), "gameActor")
    case _ =>
      // Case where one or both graphs couldn't be loaded
      logger.warn("Failed to create gameActor")
      throw new IllegalStateException("Failed to create gameActor")
  }

  // Create GameRoutes
  val gameRoutes = new GameRoutes(system, gameActor)
  logger.info(s"Akka Actors created")

  val bindingFuture = Http().newServerAt("0.0.0.0", 8080).bind(gameRoutes.routes)

  logger.info(s"Server online at port 8080")

  // To gracefully shutdown the server
  scala.io.StdIn.readLine("Press ENTER to stop the server.")
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
