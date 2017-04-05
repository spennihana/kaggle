module Utils exposing (..)

import Task
import Mouse exposing (Position)
import Window

{-| Useful for creating callbacks that will never fail,
    by allowing programmer to "invoke" some message.

    Example:
        update: Foo -> Msg -> (Foo, Cmd Msg)
        update foo msg =
            case msg of
                ...
                  MyMsg -> model ! [doTask MyOtherMsg]
                ...

    Mostly this is a convenience method for eliminating the `onFail`
    condition of Task.perform (where it would look like doTask NoOp Success,
    instead we have the simpler doTask Success).

    See: https://groups.google.com/forum/#!topic/elm-discuss/5Q9ktTuavgY
-}
doTask: msg -> Cmd msg
doTask m = Task.perform (\_ -> m) (Task.succeed always)

px: Int -> String
px i = (toString i) ++ "px"

fst: List a -> a -> a
fst l def = Maybe.withDefault def (List.head l)

snd: List a -> a -> a
snd l def = fst (Maybe.withDefault [] (List.tail l)) def

-- Callback from browser with window size
winsize : (Window.Size -> msg) -> Cmd msg
winsize msg = Task.perform msg Window.size

toInt: String -> Int
toInt s = Result.withDefault -1 (String.toInt s)

type alias DragInfo =
  { dragging: Bool,
    y0: Int,  -- initial height of window less the height/width
    h0: Int,  -- initial height of the log widget
    resizer: Int -> Int -> Int -> Int,
    toP: Position -> Int
  }

upDownResize: Int -> Int -> Int -> Int   -- delta_y == -delta_h => (y0 - y') == -(h0 - h') ==> h' = y0+h0-y'
upDownResize h0 y0 y_ = y0 + h0 - y_

leftRightResize: Int -> Int -> Int -> Int  -- delta_x
leftRightResize w0 x0 x_ = x_ + w0 - x0

dragInfoInit: (Int -> Int -> Int -> Int) -> (Position -> Int) -> DragInfo
dragInfoInit resizer toP = { dragging=False,
                         y0= -1,
                         h0=300,
                         resizer=resizer,
                         toP=toP
                        }

type alias ClickOptions = { preventDefault : Bool, stopPropagation : Bool }
clickOptionsTT : ClickOptions
clickOptionsTT = {preventDefault=True, stopPropagation=True}
clickOptionsTF : ClickOptions
clickOptionsTF = {preventDefault=True, stopPropagation=False}
clickOptionsFT : ClickOptions
clickOptionsFT = {preventDefault=False, stopPropagation=True}
clickOptionsFF : ClickOptions
clickOptionsFF = {preventDefault=False, stopPropagation=False}