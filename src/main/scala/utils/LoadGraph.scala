package utils

import NetGraphAlgebraDefs.{Action, NetGraphComponent, NodeObject}
import org.slf4j.LoggerFactory

import java.io.{FileInputStream, InputStream, ObjectInputStream}
import java.net.URL
import scala.util.Try

object LoadGraph {
  private val logger = LoggerFactory.getLogger(getClass)

  // Function to load a graph from a file path or URL
  def load(filePath: String): (List[NodeObject], List[Action]) = {
    val inputStream: Option[InputStream] = if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
      Try(new URL(filePath).openStream()).toOption
    } else {
      Try(new FileInputStream(filePath)).toOption
    }
    inputStream match {
      case Some(stream) =>
        // If a valid stream is obtained, proceed with reading the object
        val objectInputStream = new ObjectInputStream(stream)
        val ng = objectInputStream.readObject().asInstanceOf[List[NetGraphComponent]]

        // Separate nodes and edges from the loaded components
        val nodes = ng.collect { case node: NodeObject => node }
        val edges = ng.collect { case edge: Action => edge }

        // Close the stream after use
        objectInputStream.close()

        // Return the nodes and edges
        (nodes, edges)
      case None =>
        logger.error("Invalid file path or URL")
        throw new IllegalArgumentException("Invalid file path or URL")
    }
  }
}
