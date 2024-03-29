package taboolib.module.lang

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.ProxyPlayer
import taboolib.module.lang.event.PlayerSelectLocaleEvent
import taboolib.module.lang.event.SystemSelectLocaleEvent
import java.util.*

/**
 * TabooLib
 * taboolib.module.lang.Language
 *
 * @author sky
 * @since 2021/6/18 10:43 下午
 */
object Language {

    val textTransfer = ArrayList<TextTransfer>()

    val languageFile = HashMap<String, LanguageFile>()

    val languageCode = arrayListOf("zh_CN", "en_US")

    val languageCodeTransfer = hashMapOf(
        "zh_hans_cn" to "zh_CN",
        "zh_hant_cn" to "zh_TW",
        "en_ca" to "en_US",
        "en_au" to "en_US",
        "en_gb" to "en_US",
        "en_nz" to "en_US"
    )

    val languageType = hashMapOf(
        "text" to TypeText::class.java,
        "title" to TypeText::class.java,
        "sound" to TypeSound::class.java,
        "actionbar" to TypeActionBar::class.java
    )

    fun getLocale(player: ProxyPlayer): String {
        return PlayerSelectLocaleEvent(player, languageCodeTransfer[player.locale] ?: player.locale).run {
            call()
            locale
        }
    }

    fun getLocale(): String {
        val code = Locale.getDefault().toLanguageTag().replace("-", "_")
        return SystemSelectLocaleEvent(languageCodeTransfer[code] ?: code).run {
            call()
            locale
        }
    }

    @Awake(LifeCycle.LOAD)
    fun reload() {
        languageFile.clear()
        languageFile.putAll(ResourceReader(Language::class.java).files)
    }
}