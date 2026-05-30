package ac.mdiq.podcini.config.settings

import ac.mdiq.podcini.PodciniApp.Companion.getAppContext

const val githubAddress = "https://github.com/XilinJia/Podcini.A/"

const val developerEmail = "xilin.vw@gmail.com"

fun getCopyrightNoticeText(): String {
    var copyrightNoticeText = ""
    val packageHash = getAppContext().packageName.hashCode()
    when {
        packageHash != -961431114 && packageHash != -1902076901 -> {
            copyrightNoticeText = ("This application is based on Podcini."
                    + " The Podcini team does NOT provide support for this unofficial version."
                    + " If you can read this message, the developers of this modification violate the GNU General Public License (GPL).")
        }
        packageHash == -1902076901 -> copyrightNoticeText = "This is a development version of Podcini and not meant for daily use"
    }
    return copyrightNoticeText
}
