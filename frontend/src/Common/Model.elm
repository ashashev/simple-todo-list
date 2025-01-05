module Common.Model exposing (Model, init)

import Browser.Navigation as Nav
import Common.EditMsg exposing (EditMsg)
import Common.Types exposing (ListInfo, ListUpdated)
import Http exposing (Error)
import Random exposing (Seed)
import Url exposing (Url)


type alias Model =
    { key : Nav.Key
    , seed : Seed
    , url : Url
    , lists : List ListInfo
    , current : Maybe ListUpdated
    , error : Maybe ( String, Error )
    , drawerOpened : Bool
    , undo : List (Maybe ListUpdated)
    , redo : List (Maybe ListUpdated)
    , renameDialog : Maybe ( String, String, String -> EditMsg )
    }


init : Nav.Key -> Int -> Url -> Model
init key seed url =
    Model key (Random.initialSeed seed) url [] Nothing Nothing False [] [] Nothing
