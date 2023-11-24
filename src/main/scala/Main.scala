import NetGraphAlgebraDefs.{Action, NodeObject}
import actors.GameActor
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.Materializer
import org.slf4j.LoggerFactory
import com.google.common.graph.{MutableValueGraph, ValueGraphBuilder}
import routes.GameRoutes
import utils.LoadGraph

import scala.concurrent.ExecutionContextExecutor

object Main extends App {

  // Setting up the logger
  private val logger = LoggerFactory.getLogger(getClass)

  def loadAndCreateGraph(graphFilePath: String): Option[MutableValueGraph[NodeObject, Action]] = {
    val (nodes, edges) = LoadGraph.load(graphFilePath)

    if (nodes.isEmpty || edges.isEmpty) {
      logger.warn("Input is not of the right format")
      None
    } else {
      logger.info("Graph successfully loaded")

      // Create a MutableGraph using Guava's GraphBuilder
      val valueGraph: MutableValueGraph[NodeObject, Action] = ValueGraphBuilder.directed().build()

      // Add nodes to the graph
      nodes.foreach(valueGraph.addNode)

      // Add edges to the graph
      edges.foreach { action =>
        val nodeFromOption: Option[NodeObject] = nodes.find(_.id == action.fromNode.id)
        val nodeToOption: Option[NodeObject] = nodes.find(_.id == action.toNode.id)
        for {
          nodeFrom <- nodeFromOption
          nodeTo <- nodeToOption
        } yield valueGraph.putEdgeValue(nodeFrom, nodeTo, action)
      }
      Some(valueGraph)
    }
  }

  val originalValueGraph = loadAndCreateGraph(args(0))
  val perturbedValueGraph = loadAndCreateGraph(args(1))
  val outputFilePath = s"${args(2)}/results.txt"

  // Creating Akka HTTP Actors
  implicit val system: ActorSystem = ActorSystem("akka-http-example")
  implicit val materializer: Materializer = Materializer.matFromSystem
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  // Create GameActor
  val gameActor: ActorRef = (originalValueGraph, perturbedValueGraph) match {
    case (Some(originalGraph), Some(perturbedGraph)) =>
      // Both graphs are present, create the actor
      system.actorOf(Props(new GameActor(originalGraph, perturbedGraph, outputFilePath)), "gameActor")
    case _ =>
      // Case where one or both graphs couldn't be loaded
      throw new IllegalStateException("Failed to create gameActor")
  }

  // Create GameRoutes
  val gameRoutes = new GameRoutes(system, gameActor)
  logger.info(s"Akka Actors created")

  val bindingFuture = Http().newServerAt("0.0.0.0", 8080).bind(gameRoutes.routes)

  logger.info(s"Server online at http://localhost:8080/")

  // To gracefully shutdown the server
  scala.io.StdIn.readLine("Press ENTER to stop the server.")
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
