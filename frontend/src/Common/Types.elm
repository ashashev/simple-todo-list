module Common.Types exposing
    ( ItemUpdated
    , ListId(..)
    , ListInfo
    , ListUpdated
    , Record
    , RecordId(..)
    , decoderItemUpdated
    , decoderListId
    , decoderListInfo
    , decoderListUpdated
    , decoderRecord
    , decoderRecordId
    , encoderItemUpdated
    , encoderListId
    , encoderListInfo
    , encoderListUpdated
    , encoderRecord
    , encoderRecordId
    , toString
    )

import Common.Json as CJ
import Json.Decode as JD
import Json.Decode.Pipeline as JP
import Json.Encode as JE
import String.Nonempty exposing (NonemptyString)
import String.Nonempty as NE


type RecordId
    = RecordId NonemptyString


type alias Record =
    { id : RecordId
    , value : NonemptyString
    , checked : Bool
    }


type ListId
    = ListId NonemptyString


type alias ItemUpdated =
    { lid : ListId
    , rid : RecordId
    , checked : Bool
    }


type alias ListUpdated =
    { lid : ListId
    , name : NonemptyString
    , items : List Record
    }


type alias ListInfo =
    { lid : ListId
    , name : NonemptyString
    }


toString : ListId -> String
toString (ListId s) = NE.toString s

decoderRecordId : JD.Decoder RecordId
decoderRecordId =
    JD.map RecordId CJ.decoderNonemptyString


encoderRecordId : RecordId -> JE.Value
encoderRecordId (RecordId ne) =
    CJ.encoderNonemptyString ne


decoderListId : JD.Decoder ListId
decoderListId =
    JD.map ListId CJ.decoderNonemptyString


encoderListId : ListId -> JE.Value
encoderListId (ListId ne) =
    CJ.encoderNonemptyString ne


decoderRecord : JD.Decoder Record
decoderRecord =
    JD.succeed Record
        |> JP.required "id" decoderRecordId
        |> JP.required "value" CJ.decoderNonemptyString
        |> JP.required "checked" JD.bool


encoderRecord : Record -> JE.Value
encoderRecord r =
    JE.object <|
        [ ( "id", encoderRecordId r.id )
        , ( "value", CJ.encoderNonemptyString r.value )
        , ( "checked", JE.bool r.checked )
        ]


decoderItemUpdated : JD.Decoder ItemUpdated
decoderItemUpdated =
    JD.succeed ItemUpdated
        |> JP.required "lid" decoderListId
        |> JP.required "rid" decoderRecordId
        |> JP.required "checked" JD.bool


encoderItemUpdated : ItemUpdated -> JE.Value
encoderItemUpdated ev =
    JE.object <|
        [ ( "lid", encoderListId ev.lid )
        , ( "rid", encoderRecordId ev.rid )
        , ( "checked", JE.bool ev.checked )
        ]


decoderListUpdated : JD.Decoder ListUpdated
decoderListUpdated =
    JD.succeed ListUpdated
        |> JP.required "lid" decoderListId
        |> JP.required "name" CJ.decoderNonemptyString
        |> JP.required "items" (JD.list decoderRecord)


encoderListUpdated : ListUpdated -> JE.Value
encoderListUpdated ev =
    JE.object <|
        [ ( "lid", encoderListId ev.lid )
        , ( "name", CJ.encoderNonemptyString ev.name )
        , ( "items", JE.list encoderRecord ev.items )
        ]


decoderListInfo : JD.Decoder ListInfo
decoderListInfo =
    JD.succeed ListInfo
        |> JP.required "lid" decoderListId
        |> JP.required "name" CJ.decoderNonemptyString


encoderListInfo : ListInfo -> JE.Value
encoderListInfo ev =
    JE.object <|
        [ ( "lid", encoderListId ev.lid )
        , ( "name", CJ.encoderNonemptyString ev.name )
        ]
