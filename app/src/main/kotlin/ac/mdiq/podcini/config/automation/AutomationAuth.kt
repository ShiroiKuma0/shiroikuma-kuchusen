package ac.mdiq.podcini.config.automation

import android.content.Context
import androidx.core.content.edit
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * The gate for 白い熊's 保存復元 automation: a master switch that ships **ON**, and a token that is
 * asked for only when 白い熊 says so.
 *
 * ## Why the defaults inverted at contract v2
 *
 * v1 shipped every sister app closed — the switch defaulted to false and a caller also had to
 * present a 48-character secret pasted from this app's settings into the caller's. That is the wrong
 * shape for where this is going: **a pasted secret cannot survive a wipe**, and the case the family
 * now exists to serve is 応用管理 restoring apps *and their data* onto a clean phone, where nothing
 * has been configured and nobody has pasted anything. A gate that only works once the phone is
 * already set up is no gate for setting the phone up.
 *
 * The switch stays — it is the only way to close this app off, and a feature that can be turned on
 * but never off is one 白い熊 cannot retreat from.
 *
 * ## Why every write here is `commit()` and not `apply()`
 *
 * **Because this gate fails OPEN.** v2 flipped [KEY_ENABLED]'s default from false to true, so a
 * write that never reaches disk does not fall back to "off" — it falls back to **ON**. And 応用管理
 * force-stops an app the instant it replies to an import, with `Process.killProcess`: a `SIGKILL`,
 * which leaves an in-flight `apply()` nowhere to land. Turning this app off is the one action 白い熊
 * has for shutting it out, and it is the action most likely to be running near a force-stop, so a
 * lost `setEnabled(false)` silently reopens the door.
 *
 * The other two are the same shape. A lost `automation_require_token = true` leaves the door not
 * asking for the token that was just switched on; a lost lazily-generated token is worse still,
 * because 白い熊 may already have pasted the value into a caller and nothing surfaces the mismatch —
 * that caller simply begins failing `ERROR:bad token`.
 *
 * All three are tiny and infrequent, so synchronous costs nothing anyone waits on.
 *
 * The prefs file is device-local and deliberately outside every export category — the token must
 * never travel inside a backup ZIP, or a restored backup would hand a stranger the key.
 */
object AutomationAuth {
    private const val PREFS = "kuchusen_automation"   // never exported (see KuchusenExport)
    private const val KEY_ENABLED = "automation_enabled"
    private const val KEY_REQUIRE_TOKEN = "automation_require_token"
    private const val KEY_TOKEN = "automation_token"

    private const val TOKEN_BYTES = 24

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Default **true** since v2: the app answers automation out of the box. */
    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit(commit = true) { putBoolean(KEY_ENABLED, enabled) }

    /** Default **false** since v2: the token is an extra a caller may be asked for, not the gate. */
    fun isTokenRequired(context: Context): Boolean = prefs(context).getBoolean(KEY_REQUIRE_TOKEN, false)

    fun setTokenRequired(context: Context, required: Boolean) =
        prefs(context).edit(commit = true) { putBoolean(KEY_REQUIRE_TOKEN, required) }

    /**
     * The whole gate, in one place: null means proceed, anything else is the exact `ERROR:` line to
     * answer with.
     *
     * One function rather than two checks written out at each entry point, because that is how
     * "disabled" and "bad token" drift apart across forty-two apps. The receiver (§1) and the data
     * door (§2a) both come through here.
     *
     * **A token handed to an app that does not require one is IGNORED, never refused.** Tokens live
     * in task arguments and workspace variables that outlive the setting they were pasted for; a
     * caller still sending one — because it was configured last year, or because another app on the
     * batch does want one — must be served. Refusing it would turn "白い熊 turned a switch off" into
     * "half the batch mysteriously fails", which is precisely the friction the switch exists to
     * remove.
     */
    fun refuse(context: Context, candidate: String?): String? = when {
        !isEnabled(context) -> "ERROR:automation disabled"
        isTokenRequired(context) && !matches(context, candidate) -> "ERROR:bad token"
        else -> null
    }

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
        prefs(context).edit(commit = true) { putString(KEY_TOKEN, token) }
        return token
    }

    /**
     * Constant-time comparison — a length-or-prefix leak is enough to walk a token out.
     *
     * Still here, and still constant-time, for the case where the token *is* required: v2 made it
     * optional, not weaker.
     */
    fun matches(context: Context, candidate: String?): Boolean {
        if (candidate.isNullOrEmpty()) return false
        return MessageDigest.isEqual(candidate.toByteArray(), token(context).toByteArray())
    }

    /** `80922d8c…4c49a87c` — enough to recognise the token, not enough to use it. */
    fun abbreviated(token: String): String =
        if (token.length <= 20) token else token.take(8) + "…" + token.takeLast(8)
}
