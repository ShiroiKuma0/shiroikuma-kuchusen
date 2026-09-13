package ac.mdiq.podcini.storage.model

import ac.mdiq.podcini.shared.CaptionSpec
import io.github.xilinjia.krdb.types.EmbeddedRealmObject

class TranscriptMeta: EmbeddedRealmObject {
    var url: String? = null
    var type: String? = null
    var language: String? = null
    var rel: String? = null

    constructor() {}

    constructor(url: String?, type: String?, language: String?, rel: String?) {
        this.url = url
        this.type = type
        this.language = language
        this.rel = rel
    }
}

fun CaptionSpec.toTranscriptMeta(): TranscriptMeta {
    return TranscriptMeta(this.url, this.mimeType, this.language, this.suffix)
}