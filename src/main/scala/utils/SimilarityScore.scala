package utils

import NetGraphAlgebraDefs.NodeObject

object SimilarityScore {
  /**
   * Calculates the Jaccard Similarity between two sets.
   *
   * @param originalNodeObject The first node
   * @param perturbedNodeObject The second node
   * @return The Jaccard Similarity as a Double value
   */
  def jaccardSimilarity(originalNodeObject: NodeObject, perturbedNodeObject: NodeObject): Double = {
    // Convert NodeObjects to Sets for easier set operations
    val set1 = originalNodeObject.toSet
    val set2 = perturbedNodeObject.toSet

    // Calculate the size of the intersection between set1 and set2
    val intersectionSize = set1.intersect(set2).size

    // Calculate the size of the union of set1 and set2
    val unionSize = set1.union(set2).size

    // Calculate the Jaccard Similarity
    if (unionSize == 0) 0.0 else intersectionSize.toDouble / unionSize.toDouble
  }
}
