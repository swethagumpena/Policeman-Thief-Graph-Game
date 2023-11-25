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

  /**
   * Handles the move for either the police or the thief.
   * If the game is already over, sends a game over message.
   * Determines the current state and graph based on whether it's a police or thief move.
   * Checks for a perturbed successor and updates the game state accordingly.
   *
   * @param police True if it's a police move, false if it's a thief move.
   */
  def handleMove(police: Boolean): Unit = {
    // Check if the game is already over, and send a game over message if true
    if (gameIsOver) {
      sendGameOverMessage()
      return
    }

    // Determine the current state and graph based on the type of move
    val (currentState, currentGraph) =
      if (police) (policeState, originalValueGraph)
      else (thiefState, originalValueGraph)

    // Get the perturbed successor for the current state
    val perturbedSuccessor = getSuccessor(currentState, originalValueGraph)

    // Process the perturbed successor
    perturbedSuccessor match {
      case Some(state) =>
        // If the successor is in the current graph, update the game state and check the game status
        if (currentGraph.nodes().contains(state)) {
          if (police) policeState = state.id else thiefState = state.id
          checkGameStatus()
        } else {
          // If the successor is not in the graph, the game is over with the opponent as the winner
          gameIsOver = true
          winner = Some(if (police) "Thief" else "Police")
          sendInvalidMoveGameOverMessage(state.id)
        }
      case None =>
        // If there is no successor, the game is over with the opponent as the winner
        gameIsOver = true
        winner = Some(if (police) "Thief" else "Police")
        sendDeadEndReachedGameOverMessage(currentState)
    }
  }

  /**
   * Handles the GetState message by either sending a game over message or checking the current game status.
   * If the game is over, sends a game over message; otherwise, checks the game status.
   */
  def handleGetState(): Unit = {
    // If the game is over, send a game over message; otherwise, check the game status
    if (gameIsOver) {
      sendGameOverMessage()
    } else {
      checkGameStatus()
    }
  }

  /**
   * Handles the RestartGame message by resetting game state variables, generating new starting positions,
   * logging the restart information, updating game events, and sending a move result message to the sender.
   */
  def handleRestartGame(): Unit = {
    // Reset game state variables
    gameIsOver = false
    winner = None

    // Generate new starting positions
    policeState = getRandomNode().id
    thiefState = getFarthestNodeFrom(policeState, perturbedValueGraph).id

    // Log the restart information
    val resultMessage = "Game restarted."
    logger.info(resultMessage)

    // Update game events and send a move result message to the sender
    gameEvents = gameEvents :+ resultMessage
    sender() ! MoveResult(resultMessage)
  }

  /**
   * Checks the current game status and performs appropriate actions based on the conditions.
   * If the police has caught the thief, ends the game with the police as the winner.
   * If the thief has reached valuable data, ends the game with the thief as the winner.
   * Otherwise, provides the current state information.
   */
  def checkGameStatus(): Unit = {
    // Check if the police has caught the thief
    if (policeState == thiefState) {
      gameIsOver = true
      winner = Some("Police")

      // Generate and log the result message
      val resultMessage = s"Police has caught the Thief at $policeState. Game over. Police has won. Restart the game."
      logger.info(resultMessage)

      // Update game events and send a move result message to the sender
      gameEvents = gameEvents :+ resultMessage
      sender() ! MoveResult(resultMessage)
    } else {
      // Check if the thief has reached valuable data
      val nodeWithValuableData = perturbedValueGraph.nodes().asScala.find(_.id == thiefState)
      val valuableNodeId = nodeWithValuableData.map(_.id).getOrElse(0)
      if (nodeWithValuableData.exists(_.valuableData)) {
        gameIsOver = true
        winner = Some("Thief")

        // Generate and log the result message
        val resultMessage = s"Thief has reached valuable data at ${valuableNodeId}. Game over. Thief has won. Restart the game."
        logger.info(resultMessage)

        // Update game events and send a move result message to the sender
        gameEvents = gameEvents :+ resultMessage
        sender() ! MoveResult(resultMessage)
      } else {
        // If neither condition is met, provide the current state information
        val resultMessage = s"Police is at $policeState, Thief is at $thiefState."
        logger.info(resultMessage)

        // Update game events and send a state result message to the sender
        gameEvents = gameEvents :+ resultMessage
        sender() ! StateResult(resultMessage)
      }
    }
  }

  /**
   * Sends a game over message with the appropriate result message based on the game outcome.
   * Logs the result message, updates game events, sends a move result message to the sender,
   * and generates a game summary file.
   */
  def sendGameOverMessage(): Unit = {
    val resultMessage = s"Game over. ${winner.map(w => s"$w has won.").getOrElse("It's a draw.")} Restart the game."
    logger.info(resultMessage)

    // Update game events and send a move result message to the sender
    gameEvents = gameEvents :+ resultMessage
    sender() ! MoveResult(resultMessage)

    // Generate a game summary file
    gameSummary(outputFilePath, gameEvents)
  }

  /**
   * Sends a game over message for the scenario where the game cannot continue due to a dead end.
   * Logs the result message, updates game events, sends a move result message to the sender,
   * and generates a game summary file.
   *
   * @param currentNode The node at which the dead end was reached.
   */
  def sendDeadEndReachedGameOverMessage(currentNode: Int): Unit = {
    val resultMessage = s"Cannot move further at $currentNode. Game over. ${winner.map(w => s"$w has won.").getOrElse("It's a draw.")} Restart the game."
    logger.info(resultMessage)

    // Update game events and send a move result message to the sender
    gameEvents = gameEvents :+ resultMessage
    sender() ! MoveResult(resultMessage)

    // Generate a game summary file
    gameSummary(outputFilePath, gameEvents)
  }

  /**
   * Sends a game over message for the scenario where an invalid move was planned.
   * Logs the result message, updates game events, sends a move result message to the sender,
   * and generates a game summary file.
   *
   * @param plannedMove The node at which the invalid move was planned.
   */
  def sendInvalidMoveGameOverMessage(plannedMove: Int): Unit = {
    val resultMessage = s"Invalid move at $plannedMove. Game over. ${winner.map(w => s"$w has won.").getOrElse("It's a draw.")} Restart the game."
    logger.info(resultMessage)

    // Update game events and send a move result message to the sender
    gameEvents = gameEvents :+ resultMessage
    sender() ! MoveResult(resultMessage)

    // Generate a game summary file
    gameSummary(outputFilePath, gameEvents)
  }

  /**
   * Generates a random node from the perturbed value graph.
   *
   * @return A randomly selected node from the perturbed value graph.
   */
  private def getRandomNode(): NodeObject = {
    val nodes = perturbedValueGraph.nodes().asScala.toSeq
    val randomIndex = util.Random.nextInt(perturbedValueGraph.nodes().size())
    val randomNode = nodes(randomIndex)
    randomNode
  }

  /**
   * Retrieves the successor node with the highest Jaccard similarity to the original graph node with the specified ID.
   *
   * @param id            The ID of the node in the original graph.
   * @param originalGraph The original graph from which successors are chosen.
   * @return An option containing the successor node with the highest Jaccard similarity, or None if no successors exist.
   */
  private def getSuccessor(id: Int, originalGraph: MutableValueGraph[NodeObject, Action]): Option[NodeObject] = {
    // Find the node with the specified ID in the perturbed graph
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

  /**
   * Finds and returns the farthest node from a given start node in the specified graph.
   * Uses breadth-first search (BFS) to traverse the graph and determine the farthest node.
   *
   * @param startNode The ID of the starting node.
   * @param graph     The graph in which the farthest node is to be found.
   * @return The farthest node from the start node.
   * @throws NoSuchElementException if the start node is not found in the graph.
   */
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