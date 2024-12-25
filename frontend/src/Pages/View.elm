module Pages.View exposing (viewPage)

import Common.Model exposing (Model)
import Common.Msg as Msg exposing (Msg)
import Common.Types as CT
import Elements.Lists exposing (elemLists)
import Html exposing (Html, text)
import Html.Attributes exposing (..)
import Http
import Material.Checkbox as Checkbox
import Material.Drawer.Modal as ModalDrawer
import Material.IconButton as IconButton
import Material.LayoutGrid as LayoutGrid
import Material.List as MaterialList
import Material.List.Item as ListItem
import Material.TopAppBar as TopAppBar
import String.Nonempty as NE


viewPage : Model -> Html Msg
viewPage model =
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
                        , Html.span [ TopAppBar.title ]
                            [ text (Maybe.withDefault "<none>" (Maybe.map (\l -> NE.toString l.name) model.current)) ]
                        ]
                    ]
                ]
            ]
        , Html.div [ TopAppBar.fixedAdjust ]
            [ LayoutGrid.layoutGrid []
                [ LayoutGrid.inner []
                    [ LayoutGrid.cell []
                        [ Maybe.withDefault (Html.div [] []) (Maybe.map viewTodoList model.current)
                        , viewError model.error
                        ]
                    ]
                ]
            ]
        ]


viewTodoList : CT.ListUpdated -> Html Msg
viewTodoList l =
    let
        items =
            case List.map viewRecord l.items of
                x :: xs ->
                    MaterialList.list
                        (MaterialList.config
                            |> MaterialList.setDense True
                            |> MaterialList.setRipples False
                        )
                        x
                        xs

                [] ->
                    Html.div [] []
    in
    Html.div [] [ items ]


viewRecord : CT.Record -> ListItem.ListItem Msg
viewRecord r =
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
        (ListItem.config |> onClick)
        [ Checkbox.config |> Checkbox.setState (Just state) |> Checkbox.checkbox
        , NE.toString r.value |> text
        ]


viewError : Maybe ( String, Http.Error ) -> Html Msg
viewError me =
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
                [ Html.h3 [] [ "Loading of " ++ what ++ "failed" |> text ]
                , reason
                ]
