module Elements.Lists exposing (..)

import Common.Msg exposing (Msg)
import Common.Types as CT
import Html exposing (Html, text)
import Material.List as MaterialList
import Material.List.Item as ListItem
import String.Nonempty as NE


elemLists : Maybe CT.ListId -> List CT.ListInfo -> Html Msg
elemLists selected ls =
    let
        ys =
            List.sortBy (\l -> NE.toString l.name) ls
    in
    case ys of
        [] ->
            Html.div [] []

        x :: xs ->
            MaterialList.list
                (MaterialList.config
                    |> MaterialList.setRipples False
                )
                (elemListInfo selected x)
                (List.map (elemListInfo selected) xs)


elemListInfo : Maybe CT.ListId -> CT.ListInfo -> ListItem.ListItem Msg
elemListInfo selected info =
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
    ListItem.listItem
        (ListItem.config
            |> ListItem.setSelected s
            |> ListItem.setHref ("/view/" ++ CT.toString info.lid |> Just)
        )
        [ NE.toString info.name |> text ]
