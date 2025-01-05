module Common.EditMsg exposing (..)

import Common.Types as CT
import Http
import String.Nonempty exposing (NonemptyString)


type EditMsg
    = ChangeItem CT.RecordId Bool
    | DeleteItem CT.RecordId
    | RenameItem CT.RecordId String
    | RenameList String
    | NewItem
    | Undo
    | Redo
    | Complete
    | SaveResult (Result Http.Error ())
