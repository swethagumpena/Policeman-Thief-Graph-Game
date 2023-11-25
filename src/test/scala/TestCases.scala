import NetGraphAlgebraDefs.NodeObject
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.slf4j.{Logger, LoggerFactory}
import utils.SimilarityScore.jaccardSimilarity
import utils.WriteResultToFile.{getBucketName, getKey}
import utils.CreateGuavaGraph.loadAndCreate

class TestCases extends AnyFunSuite with Matchers {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  // SimilarityScore.scala - jaccardSimilarity
  test("Check if Jaccard Similarity returns the right result for unequal values negative") {
    val node1: NodeObject = NodeObject(id = 1, children = 1, props = 2, currentDepth = 3, propValueRange = 4, maxDepth = 5, maxBranchingFactor = 6, maxProperties = 7, storedValue = 8.0, valuableData = true)
    val node2: NodeObject = NodeObject(id = 2, children = 1, props = 2, currentDepth = 3, propValueRange = 4, maxDepth = 5, maxBranchingFactor = 9, maxProperties = 10, storedValue = 11.0, valuableData = true)

    val similarity = jaccardSimilarity(node1, node2)
    // intersection is 5 (1,2,3,4,5). Union is 11 (1,2,3,4,5,6,7,8,9,10,11). 5 / 11 ≈ 0.4545
    similarity shouldEqual 0.4545 +- 0.0001
  }

  // SimilarityScore.scala - jaccardSimilarity
  test("Check if Jaccard Similarity returns the right result for equal values") {
    val node1: NodeObject = NodeObject(id = 1, children = 1, props = 2, currentDepth = 3, propValueRange = 4, maxDepth = 5, maxBranchingFactor = 6, maxProperties = 7, storedValue = 8.0, valuableData = true)
    val node2: NodeObject = NodeObject(id = 2, children = 1, props = 2, currentDepth = 3, propValueRange = 4, maxDepth = 5, maxBranchingFactor = 6, maxProperties = 7, storedValue = 8.0, valuableData = true)

    val similarity = jaccardSimilarity(node1, node2)
    similarity shouldEqual 1.0
  }

  // SimilarityScore.scala - jaccardSimilarity
  test("Check if Jaccard Similarity returns the right result for all different values") {
    val node1: NodeObject = NodeObject(id = 1, children = 1, props = 2, currentDepth = 3, propValueRange = 4, maxDepth = 5, maxBranchingFactor = 6, maxProperties = 7, storedValue = 8.0, valuableData = true)
    val node2: NodeObject = NodeObject(id = 2, children = 9, props = 10, currentDepth = 11, propValueRange = 12, maxDepth = 13, maxBranchingFactor = 14, maxProperties = 15, storedValue = 16.0, valuableData = false)

    val similarity = jaccardSimilarity(node1, node2)
    similarity shouldEqual 0.0
  }

  // CreateGuavaGraph.scala - loadAndCreate
  test("loadAndCreate should return a graph when given a valid file path") {
    val validFilePath = "inputs/NetGraph_14-11-23-23-38-44.ngs.perturbed"
    val result = loadAndCreate(validFilePath)
    result.isDefined shouldBe true
  }

  // WriteResultsToFile.scala - getBucketName
  test("getBucketName should extract bucket name from S3 path") {
    val s3Path = "s3://my-bucket/my-file.txt"
    val bucketName = getBucketName(s3Path)
    assert(bucketName == "my-bucket")
  }

  // WriteResultsToFile.scala - getKey
  test("getKey should extract key from S3 path") {
    val s3Path = "s3://my-bucket/my-file.txt"
    val key = getKey(s3Path)
    assert(key == "my-file.txt")
  }
}