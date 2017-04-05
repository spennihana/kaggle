module Main exposing (..)


import Html exposing (..)
import Html.Attributes exposing (class, style, href, id)
import Html.Events exposing (onClick, onWithOptions)
import Window
import Utils exposing (clickOptionsTT)
import Schemas.Decoders exposing (emptySchema, decodeSchema, GetQuestionSchema)
import Schemas.Requests exposing (doGET)

import Debug  exposing (log)

type alias Config = {ip: String, p: String}
main : Program Config Model Msg
main = Html.programWithFlags { init = init, view = view, update = update, subscriptions = subscriptions }

type alias Model =
  { sheight: Int,  -- screen height
    swidth: Int,    -- screen width
    baseurl: String,
    question1: String,
    question2: String,
    label: Int,
    userLabel:Int,
    rowId: Int
  }

type Msg
  = WinSize Window.Size
  | UpdateModel (Schemas.Requests.Msg GetQuestionSchema)
  | NoOp
  | Prev
  | Next
  | Hot
  | Not
  | Save

init : Config -> (Model, Cmd Msg)
init cfg =
  let baseurl="http://"++cfg.ip++":"++cfg.p++"/3/" in
  {sheight= -1, swidth= -1, baseurl=baseurl,
   question1="", question2="", label= -1, userLabel= -1, rowId= -1
  } ! [Utils.winsize WinSize, getQ baseurl]

subscriptions : Model -> Sub Msg
subscriptions model = Sub.batch [Window.resizes WinSize]

update : Msg -> Model -> (Model, Cmd Msg)
update msg model =
  case msg of
    WinSize s -> {model | sheight=s.height, swidth=s.width} ! []
    NoOp -> model ! []
    Prev -> model ! [prevQ model.baseurl model.rowId]
    Next -> model ! [nextQ model.baseurl]
    Hot  -> model ! [set model.baseurl model.label]
    Not  -> model ! [set model.baseurl (1-model.label)]
    Save -> model ! [save model.baseurl]
    UpdateModel gmsg ->
      let (g, cmd, outmsg) = Schemas.Requests.update gmsg in
      {model| question1=g.question1, question2=g.question2, label=g.label, userLabel=g.user_label, rowId=g.row_id} ! []

view : Model -> Html Msg
view model = div[]
                [div[][text <| "Question 1: " ++ (model.question1)],
                 div[][text <| "Question 2: " ++ (model.question2)],
                 div[][text <| "Label: "      ++ (toString model.label)],
                 div[][text <| "User label: " ++ (toString model.userLabel)],
                 div[style[("display", "inline-block")]][
                   div[style[("float", "left")],onClick Prev][prev "fa fa-caret-left" "prev"],
                   div[style[("padding-left", "20px"), ("float", "right")],onClick Next][btn "fa fa-caret-right" "next"]
                 ],
                 horizontalDivider [("width", "255px")],
                 div[style[("display", "inline-block"), ("position", "relative")]][
                    div[onClick Not,style[("float", "left")]][btn2 "fa fa-times" "Label Wrong" "red"],
                    div[onClick Hot,style[("padding-left", "20px"),("float", "right")]][btn2 "fa fa-check" "Label Right" "green"]
                 ],
                 horizontalDivider [("width", "255px")],
                 div[style[("float", "left")],onClick Save][prev "fa fa-download" "save"]
                ]

prev: String -> String -> Html Msg
prev caret txt =
  a[style[("height", "45px"), ("font-size", "0.875rem"), ("cursor", "pointer")]]
   [i[class caret, style[("padding-right", "10px"),("padding-left", "10px")]][], text txt]

btn: String -> String -> Html Msg
btn caret txt =
  a[style[("height", "45px"), ("font-size", "0.875rem"), ("cursor", "pointer")]]
   [text txt,i[class caret, style[("padding-right", "10px"),("padding-left", "10px")]][]]

btn2: String -> String -> String -> Html Msg
btn2 caret txt color =
  a[style[("height", "45px"), ("font-size", "0.875rem"), ("cursor", "pointer"), ("color", color)]]
   [i[class caret, style[("padding-right", "10px"),("padding-left", "10px")]][], text txt]

horizontalDivider attrs = div[style(attrs),class "divider"][]

-- HTTP api

prevQ: String -> Int-> Cmd Msg
prevQ baseurl row_id = get (baseurl ++ "Next?row_id="++ (toString (max (row_id-1) 0)))

getQ: String -> Cmd Msg
getQ baseurl = get (baseurl ++ "Get")

nextQ: String -> Cmd Msg
nextQ baseurl =  get (baseurl ++ "Next?row_id=-1")

set: String -> Int -> Cmd Msg
set baseurl label = get (baseurl ++ "Set?user_label=" ++ (toString label))

save: String -> Cmd Msg
save baseurl = get (baseurl ++ "Save")

get: String -> Cmd Msg
get url = Cmd.map UpdateModel (doGET url decodeSchema emptySchema)
