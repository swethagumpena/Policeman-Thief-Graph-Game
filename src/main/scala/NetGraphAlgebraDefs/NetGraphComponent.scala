package NetGraphAlgebraDefs

trait NetGraphComponent extends Serializable

@SerialVersionUID(123L)
case class NodeObject(id: Int, children: Int, props: Int, currentDepth: Int = 1, propValueRange: Int, maxDepth: Int,
                      maxBranchingFactor: Int, maxProperties: Int, storedValue: Double, valuableData: Boolean = false) extends NetGraphComponent {
  val valuableDataValue: Int = if (valuableData) 1 else 0
  def toSet: Set[Int] = Set(children, props, currentDepth, propValueRange, maxDepth, maxBranchingFactor, maxProperties, storedValue.round.toInt, valuableDataValue)
}

@SerialVersionUID(123L)
case class Action(actionType: Int, fromNode: NodeObject, toNode: NodeObject, fromId: Int, toId: Int, resultingValue: Option[Int], cost: Double) extends NetGraphComponent