module Main exposing (..)

import Browser
import Browser.Navigation as Nav
import Common.Model as Model exposing (Model)
import Common.Msg as Msg exposing (Msg)
import Common.Types as CT
import Debug
import Fuzz exposing (result)
import Html exposing (..)
import Html.Attributes exposing (..)
import Http
import Json.Decode as JD
import Json.Encode as JE
import Pages.Edit exposing (editPage, editUpdate)
import Pages.Main exposing (viewLists)
import Pages.View exposing (viewPage)
import Platform.Cmd as Cmd
import Url



-- MAIN


main : Program Flags Model Msg
main =
    Browser.application
        { init = init
        , view = view
        , update = update
        , subscriptions = subscriptions
        , onUrlChange = Msg.UrlChanged
        , onUrlRequest = Msg.LinkClicked
        }



-- MODEL


type alias Flags =
    { seed : Int }


init : Flags -> Url.Url -> Nav.Key -> ( Model, Cmd Msg )
init flags url key =
    let
        _ =
            Debug.log "flags" flags
    in
    ( Model.init key flags.seed url
    , Http.get { url = "/list", expect = Http.expectJson Msg.ListsLoaded (JD.list CT.decoderListInfo) }
    )



-- UPDATE


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case Debug.log "message" msg of
        Msg.LinkClicked urlRequest ->
            case urlRequest of
                Browser.Internal url ->
                    Debug.log ("LinkClicked: " ++ Debug.toString url)
                        ( model, Nav.pushUrl model.key (Url.toString url) )

                Browser.External href ->
                    ( model, Nav.load href )

        Msg.UrlChanged url ->
            Debug.log ("UrlChanged: " ++ Debug.toString url)
                ( { model | url = url }
                , Maybe.withDefault Cmd.none (urlToCmd url)
                )

        Msg.ItemChanged rid checked ->
            let
                cmd =
                    case model.current of
                        Nothing ->
                            Cmd.none

                        Just l ->
                            let
                                resultMapper : Result Http.Error String -> Msg
                                resultMapper result =
                                    result |> Result.map (\_ -> CT.ItemUpdated l.lid rid checked) |> Msg.ItemUpdated
                            in
                            Http.post
                                { url = "/list/" ++ CT.toString l.lid ++ "/item/" ++ CT.ridToString rid
                                , body = Http.jsonBody (JE.object [ ( "checked", JE.bool checked ) ])
                                , expect = Http.expectString resultMapper
                                }
            in
            ( model, cmd )

        Msg.ListsLoaded (Ok lists) ->
            let
                cmd =
                    urlToCmd model.url |> Maybe.withDefault Cmd.none
            in
            Debug.log "ListsLoaded: "
                ( { model | error = Nothing, lists = lists, drawerOpened = False }, cmd )

        Msg.ListsLoaded (Err err) ->
            ( { model | error = Just ( "lists", err ), drawerOpened = False }, Cmd.none )

        Msg.ListLoaded (Ok list) ->
            ( { model | error = Nothing, current = Just list, drawerOpened = False, undo = [], redo = [] }, Cmd.none )

        Msg.ListLoaded (Err err) ->
            ( { model | error = Just ( "list", err ), drawerOpened = False, undo = [], redo = [] }, Cmd.none )

        Msg.ItemUpdated (Ok item) ->
            ( updateItem model item, Cmd.none )

        Msg.ItemUpdated (Err err) ->
            ( { model | error = Just ( "item", err ) }, Cmd.none )

        Msg.OpenDrawer ->
            ( { model | drawerOpened = True }, Cmd.none )

        Msg.CloseDrawer ->
            ( { model | drawerOpened = False }, Cmd.none )

        Msg.Edit m ->
            editUpdate m model


updateItem : Model -> CT.ItemUpdated -> Model
updateItem m item =
    let
        upd =
            List.map
                (\r ->
                    if r.id == item.rid then
                        { r | checked = item.checked }

                    else
                        r
                )

        ncur =
            Maybe.map
                (\list ->
                    if list.lid /= item.lid then
                        list

                    else
                        { list | items = upd list.items }
                )
                m.current
    in
    { m | current = ncur }


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
                , expect = Http.expectJson Msg.ListLoaded CT.decoderListUpdated
                }
                |> Just

        [ "edit", lid ] ->
            Http.get
                { url = "/list/" ++ lid
                , expect = Http.expectJson Msg.ListLoaded CT.decoderListUpdated
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
    { title = "TODO Lists"
    , body = [ page model ]

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


page : Model -> Html Msg
page model =
    let
        paths =
            Debug.log "paths: "
                (String.split "/" model.url.path |> List.filter ((/=) ""))
    in
    case paths of
        [ "view", _ ] ->
            viewPage model

        [ "edit", _ ] ->
            editPage model

        _ ->
            viewLists model


viewLink : String -> Html Msg
viewLink path =
    li [] [ a [ href path ] [ text path ] ]
