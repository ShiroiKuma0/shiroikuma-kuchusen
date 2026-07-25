package ac.mdiq.podcini.config.automation

import android.content.Context
import androidx.core.content.edit
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * The gate for 白い熊's 保存復元 automation: a master switch (default OFF) and a shared secret that
 * 自由作業盤 carries in every request.
 *
 * The prefs file is device-local and deliberately outside every export category — the token must
 * never travel inside a backup ZIP, or a restored backup would hand a stranger the key.
 */
object AutomationAuth {
    private const val PREFS = "kuchusen_automation"   // never exported (see KuchusenExport)
    private const val KEY_ENABLED = "automation_enabled"
    private const val KEY_TOKEN = "automation_token"

    private const val TOKEN_BYTES = 24

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) = prefs(context).edit { putBoolean(KEY_ENABLED, enabled) }

    /** The token, generated on first read so the settings row always has a value to show. */
    @Synchronized
    fun token(context: Context): String {
        val stored = prefs(context).getString(KEY_TOKEN, null)
        if (!stored.isNullOrBlank()) return stored
        return newToken(context)
    }

    @Synchronized
    fun regenerate(context: Context): String = newToken(context)

    private fun newToken(context: Context): String {
        val bytes = ByteArray(TOKEN_BYTES).also { SecureRandom().nextBytes(it) }
        val token = bytes.joinToString("") { "%02x".format(it) }
        prefs(context).edit { putString(KEY_TOKEN, token) }
        return token
    }

    /** Constant-time comparison — a length-or-prefix leak is enough to walk a token out. */
    fun matches(context: Context, candidate: String?): Boolean {
        if (candidate.isNullOrEmpty()) return false
        return MessageDigest.isEqual(candidate.toByteArray(), token(context).toByteArray())
    }

    /** `80922d8c…4c49a87c` — enough to recognise the token, not enough to use it. */
    fun abbreviated(token: String): String =
        if (token.length <= 20) token else token.take(8) + "…" + token.takeLast(8)
}
