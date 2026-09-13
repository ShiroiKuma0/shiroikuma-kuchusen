package ac.mdiq.podcini.storage.model

import io.github.xilinjia.krdb.types.EmbeddedRealmObject

class CaptionCue: EmbeddedRealmObject {
    var startMs: Long = 0
    var endMs: Long = 0
    var speaker: String = ""
    var text: String = ""

    constructor() {}

    constructor(startMs: Long, endMs: Long, text: String, speaker: String = "") {
        this.startMs = startMs
        this.endMs = endMs
        this.speaker = speaker
        this.text = text
    }
}