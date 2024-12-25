module Common.Model exposing (Model)

import Browser.Navigation as Nav
import Common.Types exposing (ListInfo, ListUpdated)
import Http exposing (Error)
import Url exposing (Url)


type alias Model =
    { key : Nav.Key
    , url : Url
    , lists : List ListInfo
    , current : Maybe ListUpdated
    , error : Maybe ( String, Error )
    , drawerOpened : Bool
    }
