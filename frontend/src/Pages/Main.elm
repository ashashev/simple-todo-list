module Pages.Main exposing (..)

import Common.Model exposing (Model)
import Common.Msg exposing (Msg)
import Elements.Lists exposing (elemLists)
import Html exposing (Html, text)
import Material.IconButton as IconButton
import Material.TopAppBar as TopAppBar


viewLists : Model -> Html Msg
viewLists model =
    Html.div []
        [ Html.div []
            [ TopAppBar.regular (TopAppBar.config |> TopAppBar.setFixed True)
                [ TopAppBar.row []
                    [ TopAppBar.section [ TopAppBar.alignStart ]
                        [ IconButton.iconButton
                            (IconButton.config
                                |> IconButton.setAttributes [ TopAppBar.navigationIcon ]
                            )
                            (IconButton.icon "menu")
                        , Html.span [ TopAppBar.title ]
                            [ text "Available lists:" ]
                        ]
                    ]
                ]
            ]
        , Html.div [ TopAppBar.fixedAdjust ]
            [ elemLists (Maybe.map (\l -> l.lid) model.current) model.lists ]
        ]
