package utils

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, JsObject, JsValue, RootJsonFormat, deserializationError}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  import actors._

  implicit val moveResultFormat: RootJsonFormat[MoveResult] = jsonFormat1(MoveResult)
  implicit val stateResultFormat: RootJsonFormat[StateResult] = jsonFormat1(StateResult)
  implicit val movePoliceFormat: RootJsonFormat[MovePolice] = jsonFormat0(MovePolice)
  implicit val moveThiefFormat: RootJsonFormat[MoveThief] = jsonFormat0(MoveThief)
  implicit val getStateFormat: RootJsonFormat[GetState] = jsonFormat0(GetState)

  implicit object GameResponseFormat extends RootJsonFormat[GameResponse] {
    def write(obj: GameResponse): JsValue = obj match {
      case mr: MoveResult => moveResultFormat.write(mr)
      case sr: StateResult => stateResultFormat.write(sr)
    }

    def read(json: JsValue): GameResponse = json match {
      case jsObj: JsObject =>
        if (jsObj.fields.contains("message")) moveResultFormat.read(json)
        else stateResultFormat.read(json)
      case _ => deserializationError("Invalid GameResponse JSON")
    }
  }
}
