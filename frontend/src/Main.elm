module Main exposing (..)

import Browser
import Browser.Navigation as Nav
import Common.Types as CT
import Debug
import Fuzz exposing (result)
import Html exposing (..)
import Html.Attributes exposing (..)
import Http
import Json.Decode as JD
import Json.Encode as JE
import Material.Checkbox as Checkbox
import Material.LayoutGrid as LayoutGrid
import Material.List as MaterialList
import Material.List.Item as ListItem
import Platform.Cmd as Cmd
import String.Nonempty as NE exposing (NonemptyString)
import Test exposing (only)
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
    | ItemChanged CT.RecordId Bool
    | ListsLoaded (Result Http.Error (List CT.ListInfo))
    | ListLoaded (Result Http.Error CT.ListUpdated)
    | ItemUpdated (Result Http.Error CT.ItemUpdated)


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case Debug.log "message" msg of
        LinkClicked urlRequest ->
            case urlRequest of
                Browser.Internal url ->
                    Debug.log ("LinkClicked: " ++ Debug.toString url)
                        ( model, Nav.pushUrl model.key (Url.toString url) )

                Browser.External href ->
                    ( model, Nav.load href )

        UrlChanged url ->
            Debug.log ("UrlChanged: " ++ Debug.toString url)
                ( { model | url = url }
                , Maybe.withDefault Cmd.none (urlToCmd url)
                )

        ItemChanged rid checked ->
            let
                cmd =
                    case model.current of
                        Nothing ->
                            Cmd.none

                        Just l ->
                            let
                                resultMapper : Result Http.Error String -> Msg
                                resultMapper result =
                                    result |> Result.map (\_ -> CT.ItemUpdated l.lid rid checked) |> ItemUpdated
                            in
                            Http.post
                                { url = "/list/" ++ CT.toString l.lid ++ "/item/" ++ CT.ridToString rid
                                , body = Http.jsonBody (JE.object [("checked", JE.bool checked)])
                                , expect = Http.expectString resultMapper
                                }
            in
            ( model, cmd )

        ListsLoaded (Ok lists) ->
            let
                cmd =
                    urlToCmd model.url |> Maybe.withDefault Cmd.none
            in
            Debug.log "ListsLoaded: "
                ( { model | error = Nothing, lists = lists }, cmd )

        ListsLoaded (Err err) ->
            ( { model | error = Just ( "lists", err ) }, Cmd.none )

        ListLoaded (Ok list) ->
            ( { model | error = Nothing, current = Just list }, Cmd.none )

        ListLoaded (Err err) ->
            ( { model | error = Just ( "list", err ) }, Cmd.none )

        ItemUpdated (Ok item) ->
            ( updateItem model item, Cmd.none )

        ItemUpdated (Err err) ->
            ( { model | error = Just ( "item", err ) }, Cmd.none )

updateItem : Model -> CT.ItemUpdated -> Model
updateItem m item =
    let
        x = 2
        upd = List.map (\r -> if r.id == item.rid then {r | checked = item.checked} else r)
        ncur = Maybe.map (\list ->
            if list.lid /= item.lid then list
            else {list | items = upd list.items}
            ) m.current
    in {m | current = ncur}

urlToCmd : Url.Url -> Maybe (Cmd Msg)
urlToCmd url =
    let
        paths =
            Debug.log "paths: "
                (String.split "/" url.path |> List.filter ((/=) ""))
    in
    case paths of
        [ "view", lid ] ->
            Http.get
                { url = "/list/" ++ lid
                , expect = Http.expectJson ListLoaded CT.decoderListUpdated
                }
                |> Just

        [ "edit", lid ] ->
            Http.get
                { url = "/list/" ++ lid
                , expect = Http.expectJson ListLoaded CT.decoderListUpdated
                }
                |> Just

        _ ->
            Nothing



-- SUBSCRIPTIONS


subscriptions : Model -> Sub Msg
subscriptions _ =
    Sub.none



-- VIEW


view : Model -> Browser.Document Msg
view model =
    { title = "URL Interceptor"
    , body =
        [ LayoutGrid.layoutGrid []
            [ LayoutGrid.inner []
                [ LayoutGrid.cell []
                    [ h2 [] [ text "Lists:" ]

                    --, ul [] (List.map viewListInfo model.lists)
                    , viewLists (Maybe.map (\l -> l.lid) model.current) model.lists
                    ]
                , LayoutGrid.cell []
                    [ h2 [] [ text "Current list:" ]
                    , Maybe.withDefault (p [] [ text "<none>" ]) (Maybe.map viewTodoList model.current)
                    , viewError model.error
                    ]
                ]
            ]
        ]

    --        [ text "The current URL is: "
    --        , b [] [ text (Url.toString model.url) ]
    --        , ul []
    --            [ viewLink "/home"
    --            , viewLink "/profile"
    --            , viewLink "/reviews/the-century-of-the-self"
    --            , viewLink "/reviews/public-opinion"
    --            , viewLink "/reviews/shah-of-shahs"
    --            ]
    --        , h2 [] [ text "Lists:" ]
    --        , ul [] (List.map viewListInfo model.lists)
    --        , h2 [] [ text "Current list:" ]
    --        , Maybe.withDefault (p [] [ text "<none>" ]) (Maybe.map viewTodoList model.current)
    --        , viewError model.error
    --        ]
    }


viewLink : String -> Html Msg
viewLink path =
    li [] [ a [ href path ] [ text path ] ]


viewLists : Maybe CT.ListId -> List CT.ListInfo -> Html Msg
viewLists selected ls =
    case ls of
        [] ->
            Html.div [] []

        x :: xs ->
            MaterialList.list (MaterialList.config |> MaterialList.setDense False |> MaterialList.setRipples False)
                (viewListInfo selected x)
                (List.map (viewListInfo selected) xs)


viewListInfo : Maybe CT.ListId -> CT.ListInfo -> ListItem.ListItem Msg
viewListInfo selected info =
    let
        s =
            case selected of
                Just lid ->
                    if lid == info.lid then
                        Just ListItem.selected

                    else
                        Nothing

                _ ->
                    Nothing
    in
    ListItem.listItem (ListItem.config |> ListItem.setSelected s |> ListItem.setHref ("/view/" ++ CT.toString info.lid |> Just))
        [ NE.toString info.name |> text ]



--li [] [ a [ CT.toString info.lid |> href ] [ NE.toString info.name |> text ] ]


viewTodoList : CT.ListUpdated -> Html Msg
viewTodoList l =
    let
        title =
            h3 [] [ NE.toString l.name |> text ]

        items =
            case List.map viewRecord l.items of
                x :: xs ->
                    MaterialList.list MaterialList.config x xs

                [] ->
                    Html.div [] []
    in
    p [] [ title, items ]


viewRecord : CT.Record -> ListItem.ListItem Msg
viewRecord r =
    let
        state =
            if r.checked then
                Checkbox.checked

            else
                Checkbox.unchecked

        onclick : ListItem.Config Msg -> ListItem.Config Msg
        onclick =
            ListItem.setOnClick (ItemChanged r.id (not r.checked))
    in
    ListItem.listItem
        (ListItem.config |> onclick)
        [ Checkbox.config |> Checkbox.setState (Just state) |> Checkbox.checkbox
        , NE.toString r.value |> text
        ]


viewError : Maybe ( String, Http.Error ) -> Html Msg
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
