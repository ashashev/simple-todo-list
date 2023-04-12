module Common.Json exposing (..)

import Json.Decode as JD
import Json.Encode as JE
import String.Nonempty exposing (..)

maybeLiftDecoder : String -> Maybe a -> JD.Decoder a
maybeLiftDecoder err ma =
    case ma of
        Nothing ->
            JD.fail err

        Just a ->
            JD.succeed a


decoderNonemptyString : JD.Decoder NonemptyString
decoderNonemptyString =
    JD.andThen
        (fromString >> maybeLiftDecoder "empty string is prohibited here")
        JD.string


encoderNonemptyString : NonemptyString -> JE.Value
encoderNonemptyString ne = JE.string <| toString ne
