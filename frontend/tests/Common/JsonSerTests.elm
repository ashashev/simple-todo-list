module Common.JsonSerTests exposing (..)

import Common.Types as CT
import Expect
import Json.Decode as JD
import Result
import String.Nonempty exposing (NonemptyString(..))
import Test exposing (..)


recordSerTests : Test
recordSerTests =
    let
        good =
            """
        {
          "id": "item-1",
          "value": "It is list's item",
          "checked": false
        }
        """

        record =
            CT.Record
                (CT.RecordId <| NonemptyString 'i' "tem-1")
                (NonemptyString 'I' "t is list's item")
                False
    in
    describe "JSON serialization of Record structure"
        [ test "Read Record structure from JSON" <|
            \_ ->
                let
                    got =
                        JD.decodeString CT.decoderRecord good
                in
                Expect.equal (Result.Ok record) got
        , test "Write Record structure to JSON" <|
            \_ ->
                let
                    got =
                        CT.encoderRecord record
                in
                Expect.equal (JD.decodeString JD.value good) (Ok got)
        ]


itemUpdatedSerTests : Test
itemUpdatedSerTests =
    let
        good =
            """
        {
          "lid": "list-1",
          "rid": "item-1",
          "checked": true
        }
        """

        event =
            CT.ItemUpdated
                (CT.ListId <| NonemptyString 'l' "ist-1")
                (CT.RecordId <| NonemptyString 'i' "tem-1")
                True
    in
    describe "JSON serialization of ItemUpdated structure"
        [ test "Read structure from JSON" <|
            \_ ->
                let
                    got =
                        JD.decodeString CT.decoderItemUpdated good
                in
                Expect.equal (Result.Ok event) got
        , test "Write structure to JSON" <|
            \_ ->
                let
                    got =
                        CT.encoderItemUpdated event
                in
                Expect.equal (JD.decodeString JD.value good) (Ok got)
        ]


listUpdatedSerTests : Test
listUpdatedSerTests =
    let
        good =
            """
        {
          "lid": "list-1",
          "name": "my list",
          "items": [
            {
              "id": "item-1",
              "value": "It is list's item",
              "checked": false
            },
            {
              "id": "item-2",
              "value": "It is another list's item",
              "checked": true
            }
          ]
        }
        """

        r1 =
            CT.Record
                (CT.RecordId <| NonemptyString 'i' "tem-1")
                (NonemptyString 'I' "t is list's item")
                False

        r2 =
            CT.Record
                (CT.RecordId <| NonemptyString 'i' "tem-2")
                (NonemptyString 'I' "t is another list's item")
                True

        event =
            CT.ListUpdated
                (CT.ListId <| NonemptyString 'l' "ist-1")
                (NonemptyString 'm' "y list")
                [ r1, r2 ]
    in
    describe "JSON serialization of ListUpated structure"
        [ test "Read structure from JSON" <|
            \_ ->
                let
                    got =
                        JD.decodeString CT.decoderListUpdated good
                in
                Expect.equal (Result.Ok event) got
        , test "Write structure to JSON" <|
            \_ ->
                let
                    got =
                        CT.encoderListUpdated event
                in
                Expect.equal (JD.decodeString JD.value good) (Ok got)
        ]
