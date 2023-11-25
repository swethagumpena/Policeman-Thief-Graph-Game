package actors

import NetGraphAlgebraDefs.{Action, NodeObject}
import akka.actor.Actor
import com.google.common.graph.MutableValueGraph
import org.slf4j.LoggerFactory
import utils.SimilarityScore.jaccardSimilarity
import utils.WriteResultToFile.gameSummary

import scala.collection.mutable
import scala.jdk.CollectionConverters._

// Define your messages
case class MovePolice()

case class MoveThief()

case class GetState()

case class RestartGame()

sealed trait GameResponse

case class MoveResult(message: String) extends GameResponse

case class StateResult(message: String) extends GameResponse

class GameActor(originalValueGraph: MutableValueGraph[NodeObject, Action], perturbedValueGraph: MutableValueGraph[NodeObject, Action], outputFilePath: String) extends Actor {
  // Initialize your game state
  var policeState: Int = getRandomNode().id
  var thiefState: Int = getFarthestNodeFrom(policeState, perturbedValueGraph).id

  private val logger = LoggerFactory.getLogger(getClass)

  // Define game state variables
  private var gameIsOver: Boolean = false
  private var winner: Option[String] = None

  // Store game events
  private var gameEvents: List[String] = List()

  // Game logic implementation
  override def receive: Receive = {
    case MovePolice => handleMove(police = true)
    case MoveThief => handleMove(police = false)
    case GetState => handleGetState()
    case RestartGame => handleRestartGame()
  }

  def handleMove(police: Boolean): Unit = {
    if (gameIsOver) {
      sendGameOverMessage()
      return
    }
    val (currentState, currentGraph) =
      if (police) (policeState, originalValueGraph)
      else (thiefState, originalValueGraph)

    val perturbedSuccessor = getSuccessor(currentState, originalValueGraph)
    perturbedSuccessor match {
      case Some(state) =>
        if (currentGraph.nodes().contains(state)) {
          if (police) policeState = state.id else thiefState = state.id
          checkGameStatus()
        } else {
          gameIsOver = true
          winner = Some(if (police) "Thief" else "Police")
          sendInvalidMoveGameOverMessage(state.id)
        }
      case None =>
        gameIsOver = true
        winner = Some(if (police) "Thief" else "Police")
        sendDeadEndReachedGameOverMessage(currentState)
    }
  }

  def handleGetState(): Unit = {
    if (gameIsOver) {
      sendGameOverMessage()
    } else {
      checkGameStatus()
    }
  }

  def handleRestartGame(): Unit = {
    gameIsOver = false
    winner = None
    policeState = getRandomNode().id
    thiefState = getFarthestNodeFrom(policeState, perturbedValueGraph).id
    val resultMessage = "Game restarted."
    logger.info(resultMessage)
    gameEvents = gameEvents :+ resultMessage
    sender() ! MoveResult(resultMessage)
  }

  def checkGameStatus(): Unit = {
    if (policeState == thiefState) {
      gameIsOver = true
      winner = Some("Police")
      val resultMessage = s"Police has caught the Thief at $policeState. Game over. Police has won. Restart the game."
      logger.info(resultMessage)
      gameEvents = gameEvents :+ resultMessage
      sender() ! MoveResult(resultMessage)
    } else {
      val nodeWithValuableData = perturbedValueGraph.nodes().asScala.find(_.id == thiefState)
      val valuableNodeId = nodeWithValuableData.map(_.id).getOrElse(0)
      if (nodeWithValuableData.exists(_.valuableData)) {
        gameIsOver = true
        winner = Some("Thief")
        val resultMessage = s"Thief has reached valuable data at ${valuableNodeId}. Game over. Thief has won. Restart the game."
        logger.info(resultMessage)
        gameEvents = gameEvents :+ resultMessage
        sender() ! MoveResult(resultMessage)
      } else {
        val resultMessage = s"Police is at $policeState, Thief is at $thiefState."
        logger.info(resultMessage)
        gameEvents = gameEvents :+ resultMessage
        sender() ! StateResult(resultMessage)
      }
    }
  }

  def sendGameOverMessage(): Unit = {
    val resultMessage = s"Game over. ${winner.map(w => s"$w has won.").getOrElse("It's a draw.")} Restart the game."
    logger.info(resultMessage)
    gameEvents = gameEvents :+ resultMessage
    sender() ! MoveResult(resultMessage)
    gameSummary(outputFilePath, gameEvents)
  }

  def sendDeadEndReachedGameOverMessage(currentNode: Int): Unit = {
    val resultMessage = s"Cannot move further at $currentNode. Game over. ${winner.map(w => s"$w has won.").getOrElse("It's a draw.")} Restart the game."
    logger.info(resultMessage)
    gameEvents = gameEvents :+ resultMessage
    sender() ! MoveResult(resultMessage)
    gameSummary(outputFilePath, gameEvents)
  }

  def sendInvalidMoveGameOverMessage(plannedMove: Int): Unit = {
    val resultMessage = s"Invalid move at $plannedMove. Game over. ${winner.map(w => s"$w has won.").getOrElse("It's a draw.")} Restart the game."
    logger.info(resultMessage)
    gameEvents = gameEvents :+ resultMessage
    sender() ! MoveResult(resultMessage)
    gameSummary(outputFilePath, gameEvents)
  }

  private def getRandomNode(): NodeObject = {
    val nodes = perturbedValueGraph.nodes().asScala.toSeq
    val randomIndex = util.Random.nextInt(perturbedValueGraph.nodes().size())
    val randomNode = nodes(randomIndex)
    randomNode
  }

  private def getSuccessor(id: Int, originalGraph: MutableValueGraph[NodeObject, Action]): Option[NodeObject] = {
    // Find the node with the specified id in the perturbed graph
    val nodeWithIdOption = perturbedValueGraph.nodes().asScala.find(_.id == id)

    // Compute Jaccard similarity for each successor
    val successorWithSimilarity: Option[(NodeObject, Double)] = nodeWithIdOption.flatMap { node =>
      val successors = perturbedValueGraph.successors(node).asScala.toSeq

      if (successors.nonEmpty) {
        val (bestSuccessor, highestSimilarity) = successors.foldLeft((Option.empty[NodeObject], 0.0)) {
          case ((currentBest, currentSimilarity), successorNode) =>
            val originalNode = originalGraph.nodes().asScala.find(_.id == successorNode.id)
            val similarity = originalNode.map(original =>
              jaccardSimilarity(original, successorNode)
            ).getOrElse(0.0)

            if (similarity > currentSimilarity) {
              (Some(successorNode), similarity)
            } else if (similarity == currentSimilarity && util.Random.nextBoolean()) {
              // Randomly choose a successor if similarity is the same
              (Some(successorNode), similarity)
            } else {
              (currentBest, currentSimilarity)
            }
        }
        bestSuccessor.map(successor => (successor, highestSimilarity))
      } else {
        None
      }
    }
    successorWithSimilarity.map(_._1)
  }

  def getFarthestNodeFrom(startNode: Int, graph: MutableValueGraph[NodeObject, Action]): NodeObject = {
    val visited: mutable.Set[NodeObject] = mutable.Set[NodeObject]()
    val queue: mutable.Queue[(NodeObject, Int)] = mutable.Queue[(NodeObject, Int)]()
    val startNodeObject = graph.nodes().asScala.find(_.id == startNode).getOrElse(throw new NoSuchElementException("Node not found"))
    queue.enqueue((startNodeObject, 0))
    visited.add(startNodeObject)
    var farthestNode: NodeObject = startNodeObject
    var maxDistance: Int = 0

    while (queue.nonEmpty) {
      val (currentNode, distance) = queue.dequeue()
      if (distance > maxDistance) {
        farthestNode = currentNode
        maxDistance = distance
      }

      // Traverse both incoming and outgoing edges
      val neighbors: Iterable[NodeObject] = (graph.predecessors(currentNode).asScala ++ graph.successors(currentNode).asScala).filterNot(visited.contains)
      for (neighbor <- neighbors) {
        visited.add(neighbor)
        queue.enqueue((neighbor, distance + 1))
      }
    }
    farthestNode
  }
}