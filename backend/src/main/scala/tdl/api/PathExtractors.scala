package tdl.api

import tdl.model.*

object PathExtractors:

  object ListIdVar:
    def unapply(str: String): Option[ListId] = ListId.maybe(str)
  end ListIdVar

  object RecordIdVar:
    def unapply(str: String): Option[RecordId] = RecordId.maybe(str)
  end RecordIdVar

end PathExtractors
