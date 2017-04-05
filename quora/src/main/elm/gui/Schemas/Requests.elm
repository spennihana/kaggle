module Schemas.Requests exposing (..)

{-| Module handling all http requests and error handling -}

import Json.Decode as JDecode
import Http exposing (emptyBody, encodeUri, Response, Request, Header, Expect, Body, expectJson, Error(..))
import Schemas.Decoders exposing (H2OErr, errDecoder)
import String exposing (..)

import Debug exposing (log)

type Msg a
  = Fail a Error
  | Success a

type OutMsg
  = ErrorMsg H2OErr
  | NoSignal

update : Msg a -> (a, Cmd Msg, OutMsg)
update msg =
  case msg of
    Success a -> (a, Cmd.none, NoSignal)
    Fail a err ->
      let _ = log "HTTP ERROR" err in
      case err of
        BadUrl url -> (a, Cmd.none, ErrorMsg (H2OErr ("Bad url: " ++ url) "" []))
        Timeout -> (a, Cmd.none, ErrorMsg (H2OErr "Timeout" "Timeout" ["See developer console."]))
        NetworkError -> (a,Cmd.none, ErrorMsg (H2OErr "NetworkError" "Cannot Connect" ["SCORE is not running."]))
        BadStatus r -> (a, Cmd.none, ErrorMsg (handleErr r.body))
        BadPayload s r -> (a, Cmd.none, ErrorMsg (H2OErr s "Elm JSON Parse Error" ["See Developer Console.", r.body]))

handleErr : String -> H2OErr
handleErr err =
  let
    result = JDecode.decodeString errDecoder err
    res = case result of
      Ok v -> v
      Err m ->
        let _ = log "raw error from server" err in
        {msg="failed to parse err", exception_type="front end failed to parse server err", stacktrace=[]}
    exception_type = if (String.startsWith "java.lang." res.exception_type) then (String.dropLeft 10 res.exception_type) else res.exception_type
    etype = if exception_type=="IllegalArgumentException" then "Illegal User Input" else exception_type
    msg   = String.dropLeft 16 res.msg  -- 16 is the number of chars in "ERROR MESSAGE: "
  in
    {res | exception_type = etype, msg=msg}

-- the requests to h2o --
doSend: a -> Request a -> Cmd (Msg a)
doSend emptya request =
  Http.send (\res ->
              case res of
                Ok a -> Success a
                Err e -> Fail emptya e
            ) request

doGET : String -> JDecode.Decoder a -> a -> Cmd (Msg a)
doGET url decoder emptya = doSend emptya <| Http.request
                                           {method="GET",
                                            headers=[],
                                            url=url,
                                            body=emptyBody,
                                            expect=expectJson decoder,
                                            timeout=Nothing,
                                            withCredentials=False
                                           }

doPOST : String -> Http.Body -> JDecode.Decoder a -> a -> Cmd (Msg a)
doPOST url body decoder emptya = doSend emptya <| Http.request
                                                 {method="POST",
                                                  headers=[],
                                                  url=url,
                                                  body=body,
                                                  expect=expectJson decoder,
                                                  timeout=Nothing,
                                                  withCredentials=False
                                                 }