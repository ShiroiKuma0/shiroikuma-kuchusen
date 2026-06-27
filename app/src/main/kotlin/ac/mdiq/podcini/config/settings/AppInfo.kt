package ac.mdiq.podcini.config.settings

const val githubAddress = "https://github.com/XilinJia/Podcini.A/"

const val developerEmail = "xilin.vw@gmail.com"

fun getCopyrightNoticeText(): String {
    // Fork: upstream shows an "unofficial version / GPL violation" banner whenever the package
    // name hash isn't one of its own. Our fork (shiroikuma.kuchusen) is a lawful GPL fork, so we
    // suppress that banner entirely by always returning an empty string. The call site in
    // PrefsScreen already guards on isNotBlank(), so nothing renders.
    return ""
}
