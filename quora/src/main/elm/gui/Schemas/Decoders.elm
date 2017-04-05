module Schemas.Decoders exposing (..)

-- common decoders
import Json.Decode.Extra as JExtra exposing ((|:))
import Json.Decode as Decode exposing (field, null, oneOf, Decoder, andThen, map)
import Array exposing (Array)
import String

(:=): String -> Decoder a -> Decoder a
(:=) = field

type alias GetQuestionSchema = { row_id: Int, question1: String, question2: String, label: Int, user_label: Int }
emptySchema = { row_id= -1, question1="", question2="", label= -1, user_label= -1 }
decodeSchema : Decode.Decoder GetQuestionSchema
decodeSchema =
    Decode.succeed GetQuestionSchema
        |: ("row_id" := Decode.int)
        |: ("question1" := Decode.string)
        |: ("question2" := Decode.string)
        |: ("label" := Decode.int)
        |: ("user_label" := Decode.int)

-- errors
type alias H2OErr =
  { msg : String,
    exception_type : String,
    stacktrace : List String
  }

emptyErr : H2OErr
emptyErr = {msg="", exception_type="", stacktrace=[]}

errDecoder : Decode.Decoder H2OErr
errDecoder =
  Decode.succeed H2OErr
    |: ("msg" := Decode.string)
    |: ("exception_type" := Decode.string)
    |: ("stacktrace" := Decode.list Decode.string)

-- utils
float : Decode.Decoder Float
float = (oneOf [Decode.float, Decode.map strtof Decode.string])

strtof : String -> Float
strtof s =
  case s of
  "NaN" -> 0/0
  "Infinity" -> 1/0
  "-Infinity" -> -1/0
  _ -> Result.withDefault (0/0) (String.toFloat s)