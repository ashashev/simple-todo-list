module Main exposing (..)

import Browser
import Browser.Navigation as Nav
import Common.Types as CT
import Debug
import Html exposing (..)
import Html.Attributes exposing (..)
import Http
import Json.Decode as JD
import Platform.Cmd as Cmd
import String.Nonempty as NE exposing (NonemptyString)
import Url



-- MAIN


main : Program () Model Msg
main =
    Browser.application
        { init = init
        , view = view
        , update = update
        , subscriptions = subscriptions
        , onUrlChange = UrlChanged
        , onUrlRequest = LinkClicked
        }



-- MODEL


type alias Model =
    { key : Nav.Key
    , url : Url.Url
    , lists : List CT.ListInfo
    , current : Maybe CT.ListUpdated
    , error : Maybe ( String, Http.Error )
    }


init : () -> Url.Url -> Nav.Key -> ( Model, Cmd Msg )
init flags url key =
    let
        _ =
            Debug.log "flags" flags
    in
    ( Model key url [] Nothing Nothing
    , Http.get { url = "/list", expect = Http.expectJson ListsLoaded (JD.list CT.decoderListInfo) }
    )



-- UPDATE


type Msg
    = LinkClicked Browser.UrlRequest
    | UrlChanged Url.Url
    | ListsLoaded (Result Http.Error (List CT.ListInfo))
    | ListLoaded (Result Http.Error CT.ListUpdated)


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        LinkClicked urlRequest ->
            case urlRequest of
                Browser.Internal url ->
                    ( model, Nav.pushUrl model.key (Url.toString url) )

                Browser.External href ->
                    ( model, Nav.load href )

        UrlChanged url ->
            ( { model | url = url }
            , Cmd.none
            )

        ListsLoaded (Ok lists) ->
            let
                cmd =
                    case List.head lists of
                        Nothing ->
                            Cmd.none

                        Just info ->
                            Http.get
                                { url = "/list/" ++ CT.toString info.lid
                                , expect = Http.expectJson ListLoaded CT.decoderListUpdated
                                }
            in
            ( { model | error = Nothing, lists = lists }, cmd )

        ListsLoaded (Err err) ->
            ( { model | error = Just ( "lists", err ) }, Cmd.none )

        ListLoaded (Ok list) ->
            ( { model | error = Nothing, current = Just list }, Cmd.none )

        ListLoaded (Err err) ->
            ( { model | error = Just ( "list", err ) }, Cmd.none )



-- SUBSCRIPTIONS


subscriptions : Model -> Sub Msg
subscriptions _ =
    Sub.none



-- VIEW


view : Model -> Browser.Document Msg
view model =
    { title = "URL Interceptor"
    , body =
        [ text "The current URL is: "
        , b [] [ text (Url.toString model.url) ]
        , ul []
            [ viewLink "/home"
            , viewLink "/profile"
            , viewLink "/reviews/the-century-of-the-self"
            , viewLink "/reviews/public-opinion"
            , viewLink "/reviews/shah-of-shahs"
            ]
        , h2 [] [ text "Lists:" ]
        , ul [] (List.map viewListInfo model.lists)
        , h2 [] [ text "Curent list:" ]
        , Maybe.withDefault (p [] [ text "<none>" ]) (Maybe.map viewTodoList model.current)
        , viewError model.error
        ]
    }


viewLink : String -> Html msg
viewLink path =
    li [] [ a [ href path ] [ text path ] ]


viewListInfo : CT.ListInfo -> Html msg
viewListInfo info =
    li [] [ a [ CT.toString info.lid |> href ] [ NE.toString info.name |> text ] ]


viewTodoList : CT.ListUpdated -> Html msg
viewTodoList l =
    let
        title =
            h3 [] [ NE.toString l.name |> text ]

        items =
            List.map viewRecord l.items |> ul []
    in
    p [] [ title, items ]


viewRecord : CT.Record -> Html msg
viewRecord r =
    li []
        [ text
            (if r.checked then
                "X "

             else
                "O "
            )
        , NE.toString r.value |> text
        ]


viewError : Maybe ( String, Http.Error ) -> Html msg
viewError me =
    case me of
        Nothing ->
            p [] []

        Just ( what, cause ) ->
            let
                reason =
                    case cause of
                        Http.BadUrl err ->
                            p [] [ text err ]

                        Http.Timeout ->
                            p [] [ text "Timeout" ]

                        Http.NetworkError ->
                            p [] [ text "Network error" ]

                        Http.BadStatus err ->
                            p [] [ "Status code: " ++ String.fromInt err |> text ]

                        Http.BadBody err ->
                            p [] [ text err ]
            in
            p []
                [ h3 [] [ "Loading of " ++ what ++ "failed" |> text ]
                , reason
                ]
