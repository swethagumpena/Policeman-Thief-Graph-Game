package utils

import NetGraphAlgebraDefs.{Action, NodeObject}
import com.google.common.graph.{MutableValueGraph, ValueGraphBuilder}
import org.slf4j.LoggerFactory

object CreateGuavaGraph {
  private val logger = LoggerFactory.getLogger(getClass)

  // Function to create a Guava graph from a file path or URL
  def loadAndCreate(graphFilePath: String): Option[MutableValueGraph[NodeObject, Action]] = {
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
}
