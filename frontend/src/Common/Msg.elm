module Common.Msg exposing (..)

import Browser
import Common.Types as CT
import Http
import Url


type Msg
    = LinkClicked Browser.UrlRequest
    | UrlChanged Url.Url
    | ItemChanged CT.RecordId Bool
    | ListsLoaded (Result Http.Error (List CT.ListInfo))
    | ListLoaded (Result Http.Error CT.ListUpdated)
    | ItemUpdated (Result Http.Error CT.ItemUpdated)
    | OpenDrawer
    | CloseDrawer
