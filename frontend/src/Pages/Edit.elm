module Pages.Edit exposing (editPage, editUpdate)

import Browser.Navigation as Nav
import Common.EditMsg as EditMsg exposing (EditMsg(..))
import Common.Model exposing (Model)
import Common.Msg as Msg exposing (Msg(..))
import Common.Types as CT
import Elements.Lists exposing (elemLists)
import Html exposing (Html, text)
import Html.Attributes exposing (..)
import Http
import Material.Checkbox as Checkbox
import Material.Drawer.Modal as ModalDrawer
import Material.IconButton as IconButton
import Material.List as MaterialList
import Material.List.Item as ListItem
import Material.TextField as TextField
import Material.TopAppBar as TopAppBar
import Random
import String.Nonempty as NE
import UUID exposing (UUID)
import Url exposing (Protocol(..))


editPage : Model -> Html Msg
editPage model =
    Html.div []
        [ Html.div
            [ style "display" "flex"
            , style "flex-flow" "row nowrap"
            ]
            [ ModalDrawer.drawer
                (ModalDrawer.config
                    |> ModalDrawer.setOpen model.drawerOpened
                    |> ModalDrawer.setOnClose Msg.CloseDrawer
                )
                [ ModalDrawer.header [] [ Html.h3 [ ModalDrawer.title ] [ text "Available lists:" ] ]
                , ModalDrawer.content [] [ elemLists (Maybe.map (\l -> l.lid) model.current) model.lists ]
                ]
            , ModalDrawer.scrim [] []
            ]
        , Html.div []
            [ TopAppBar.regular (TopAppBar.config |> TopAppBar.setFixed True)
                [ TopAppBar.row []
                    [ TopAppBar.section [ TopAppBar.alignStart ]
                        [ IconButton.iconButton
                            (IconButton.config
                                |> IconButton.setOnClick Msg.OpenDrawer
                                |> IconButton.setAttributes
                                    [ TopAppBar.navigationIcon ]
                            )
                            (IconButton.icon "menu")
                        , editTopBarTitle model.current
                        ]
                    ]
                , TopAppBar.row [] (editTopBarActions model)
                ]
            ]
        , Html.div [ TopAppBar.fixedAdjust ] []
        , Html.div [ TopAppBar.fixedAdjust ]
            [ Maybe.withDefault (Html.div [] []) (Maybe.map editTodoList model.current)
            , editError model.error
            ]
        ]


editUpdate : EditMsg -> Model -> ( Model, Cmd Msg )
editUpdate msg model =
    case msg of
        EditMsg.ChangeItem rid checked ->
            ( { model
                | current = Maybe.map (changeRecord rid checked) model.current
                , undo = model.current :: model.undo
              }
            , Cmd.none
            )

        EditMsg.DeleteItem rid ->
            ( { model
                | current = Maybe.map (removeRecord rid) model.current
                , undo = model.current :: model.undo
              }
            , Cmd.none
            )

        EditMsg.RenameItem rid name ->
            case NE.fromString name of
                Just name_ ->
                    ( { model
                        | current = Maybe.map (renameRecord rid name_) model.current
                        , undo = model.current :: model.undo
                        , renameDialog = Nothing
                      }
                    , Cmd.none
                    )

                Nothing ->
                    ( { model
                        | renameDialog = Nothing
                      }
                    , Cmd.none
                    )

        EditMsg.RenameList nn ->
            case NE.fromString nn of
                Nothing ->
                    ( model, Cmd.none )

                Just name ->
                    ( { model
                        | current = Maybe.map (\l -> { l | name = name }) model.current
                      }
                    , Cmd.none
                    )

        EditMsg.NewItem ->
            let
                ( id, ns ) =
                    Random.step UUID.generator model.seed

                mrid =
                    id |> UUID.toString |> NE.fromString |> Maybe.map CT.RecordId

                add : CT.ListUpdated -> CT.RecordId -> CT.ListUpdated
                add l rid =
                    let
                        v =
                            NE.NonemptyString 'N' "ew item"

                        i =
                            CT.Record rid v False
                    in
                    { l | items = List.append l.items [ i ] }
            in
            ( { model
                | seed = ns
                , current = Maybe.map2 add model.current mrid
                , undo = model.current :: model.undo
              }
            , Cmd.none
            )

        EditMsg.Undo ->
            case model.undo of
                x :: xs ->
                    ( { model
                        | undo = xs
                        , current = x
                        , redo = model.current :: model.redo
                      }
                    , Cmd.none
                    )

                _ ->
                    ( model, Cmd.none )

        EditMsg.Redo ->
            case model.redo of
                x :: xs ->
                    ( { model
                        | redo = xs
                        , current = x
                        , undo = model.current :: model.undo
                      }
                    , Cmd.none
                    )

                _ ->
                    ( model, Cmd.none )

        EditMsg.Complete ->
            case model.current of
                Nothing ->
                    ( model, Cmd.none )

                Just l ->
                    let
                        resultMapper : Result Http.Error String -> Msg
                        resultMapper result =
                            result |> Result.map (\_ -> ()) |> EditMsg.SaveResult |> Msg.Edit
                    in
                    ( model
                    , Http.post
                        { url = "/list/" ++ CT.toString l.lid
                        , body = Http.jsonBody (CT.encoderListUpdated l)
                        , expect = Http.expectString resultMapper
                        }
                    )

        EditMsg.SaveResult (Err err) ->
            ( { model | error = Just ( "update", err ) }, Cmd.none )

        EditMsg.SaveResult (Ok ()) ->
            case model.current of
                Nothing ->
                    ( model, Cmd.none )

                Just l ->
                    ( { model
                        | error = Nothing
                      }
                    , Nav.pushUrl model.key ("/view/" ++ CT.toString l.lid)
                    )


changeRecord : CT.RecordId -> Bool -> CT.ListUpdated -> CT.ListUpdated
changeRecord rid checked l =
    let
        xs =
            List.map
                (\r ->
                    if r.id /= rid then
                        r

                    else
                        { r | checked = checked }
                )
                l.items
    in
    { l | items = xs }


removeRecord : CT.RecordId -> CT.ListUpdated -> CT.ListUpdated
removeRecord rid l =
    let
        xs =
            List.filter ((\r -> r.id) >> (==) rid >> not) l.items
    in
    { l | items = xs }


renameRecord : CT.RecordId -> NE.NonemptyString -> CT.ListUpdated -> CT.ListUpdated
renameRecord rid value l =
    let
        xs =
            List.map
                (\r ->
                    if r.id /= rid then
                        r

                    else
                        { r | value = value }
                )
                l.items
    in
    { l | items = xs }


renameList : CT.ListId -> NE.NonemptyString -> List CT.ListInfo -> List CT.ListInfo
renameList lid name =
    List.map
        (\l ->
            if l.lid /= lid then
                l

            else
                { l | name = name }
        )


editTopBarTitle : Maybe CT.ListUpdated -> Html Msg
editTopBarTitle maybeList =
    case maybeList of
        Nothing ->
            Html.span [ TopAppBar.title ] [ text "<none>" ]

        Just l ->
            Html.span [ TopAppBar.title ]
                [ TextField.filled
                    (TextField.config
                        |> TextField.setValue (Just <| NE.toString l.name)
                        |> TextField.setOnChange (EditMsg.RenameList >> Msg.Edit)
                    )
                ]


editTopBarActions : Model -> List (Html Msg)
editTopBarActions model =
    let
        button icon disabled msg =
            if disabled then
                IconButton.iconButton
                    (IconButton.config
                        |> IconButton.setAttributes
                            [ TopAppBar.actionItem
                            , Html.Attributes.style "color" "gray"
                            ]
                        |> IconButton.setDisabled True
                    )
                    (IconButton.icon icon)

            else
                IconButton.iconButton
                    (IconButton.config
                        |> IconButton.setAttributes [ TopAppBar.actionItem ]
                        |> IconButton.setOnClick (Msg.Edit msg)
                    )
                    (IconButton.icon icon)
    in
    case model.current of
        Nothing ->
            []

        Just l ->
            [ TopAppBar.section [ TopAppBar.alignStart ]
                [ IconButton.iconButton
                    (IconButton.config
                        |> IconButton.setAttributes [ TopAppBar.actionItem ]
                        |> IconButton.setOnClick (Msg.Edit EditMsg.Complete)
                    )
                    (IconButton.icon "done")
                , IconButton.iconButton
                    (IconButton.config
                        |> IconButton.setAttributes [ TopAppBar.actionItem ]
                        |> IconButton.setHref ("/view/" ++ CT.toString l.lid |> Just)
                    )
                    (IconButton.icon "close")
                ]
            , TopAppBar.section [ TopAppBar.alignEnd ]
                [ button "undo" (List.isEmpty model.undo) EditMsg.Undo
                , button "redo" (List.isEmpty model.redo) EditMsg.Redo
                , IconButton.iconButton
                    (IconButton.config
                        |> IconButton.setAttributes [ TopAppBar.actionItem ]
                        |> IconButton.setOnClick (Msg.Edit EditMsg.NewItem)
                    )
                    (IconButton.icon "add")
                ]
            ]


editTodoList : CT.ListUpdated -> Html Msg
editTodoList l =
    let
        items =
            case List.map editRecord l.items of
                x :: xs ->
                    MaterialList.list
                        (MaterialList.config
                            |> MaterialList.setRipples False
                        )
                        x
                        xs

                [] ->
                    Html.div [] []
    in
    Html.div [] [ Html.map Msg.Edit items ]


editRecord : CT.Record -> ListItem.ListItem EditMsg
editRecord r =
    let
        state =
            if r.checked then
                Checkbox.checked

            else
                Checkbox.unchecked

        onClick : ListItem.Config Msg -> ListItem.Config Msg
        onClick =
            ListItem.setOnClick (Msg.ItemChanged r.id (not r.checked))
    in
    ListItem.listItem
        --(ListItem.config |> onClick)
        ListItem.config
        [ Checkbox.config |> Checkbox.setState (Just state) |> Checkbox.checkbox
        , TextField.filled
            (TextField.config
                |> TextField.setValue (Just <| NE.toString r.value)
                |> TextField.setOnChange (EditMsg.RenameItem r.id)
            )
        , IconButton.iconButton
            (IconButton.config |> IconButton.setOnClick (EditMsg.DeleteItem r.id))
            (IconButton.icon "delete")
        ]


editError : Maybe ( String, Http.Error ) -> Html Msg
editError me =
    case me of
        Nothing ->
            Html.p [] []

        Just ( what, cause ) ->
            let
                reason =
                    case cause of
                        Http.BadUrl err ->
                            Html.p [] [ text err ]

                        Http.Timeout ->
                            Html.p [] [ text "Timeout" ]

                        Http.NetworkError ->
                            Html.p [] [ text "Network error" ]

                        Http.BadStatus err ->
                            Html.p [] [ "Status code: " ++ String.fromInt err |> text ]

                        Http.BadBody err ->
                            Html.p [] [ text err ]
            in
            Html.p []
                [ Html.h3 [] [ "Loading of " ++ what ++ " failed" |> text ]
                , reason
                ]
