package eu.kanade.tachiyomi.extension.ar.prochan

import android.content.SharedPreferences
import android.widget.Toast
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import keiyoushi.utils.getPreferencesLazy
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class ProChan : ParsedHttpSource(), ConfigurableSource {

    override val name = "ProChan"
    private val defaultBaseUrl = "https://prochan.net"
    override val baseUrl by lazy { getPrefBaseUrl() }
    override val lang = "ar"
    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .rateLimit(10, 1, TimeUnit.SECONDS)
        .build()

    private val preferences: SharedPreferences by getPreferencesLazy()

    // Popular
    override fun popularMangaRequest(page: Int): Request {
        val url = if (page <= 1) "$baseUrl/series" else "$baseUrl/series?page=$page"
        return GET(url, headers)
    }

    override fun popularMangaSelector() = "div.main-list div.series-card"

    override fun popularMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            title = element.select("h3.series-title").text()
            setUrlWithoutDomain(element.select("a").first()!!.attr("href"))
            thumbnail_url = element.select("img.series-cover").first()!!.absUrl("src")
        }
    }

    override fun popularMangaNextPageSelector() = "a.pagination-next"

    // Latest
    override fun latestUpdatesRequest(page: Int): Request {
        val url = if (page <= 1) "$baseUrl/updates" else "$baseUrl/updates?page=$page"
        return GET(url, headers)
    }

    override fun latestUpdatesSelector() = "div.update-list div.update-card"

    override fun latestUpdatesFromElement(element: Element): SManga {
        return SManga.create().apply {
            title = element.select("div.update-card-title").text()
            setUrlWithoutDomain(element.select("a").first()!!.attr("href"))
            thumbnail_url = element.select("img.update-thumb").first()!!.absUrl("src")
        }
    }

    override fun latestUpdatesNextPageSelector() = "a.pagination-next"

    // Search
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/series?search=$query"
        return GET(url, headers)
    }

    override fun searchMangaSelector() = "div.main-list div.series-card"

    override fun searchMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            title = element.select("h3.series-title").text()
            setUrlWithoutDomain(element.select("a").first()!!.attr("href"))
            thumbnail_url = element.select("img.series-cover").first()!!.absUrl("src")
        }
    }

    override fun searchMangaNextPageSelector(): String? = null

    // Details
    override fun mangaDetailsParse(document: Document): SManga {
        return SManga.create().apply {
            title = document.select("h1.series-title").text()
            description = document.select("div.series-description").text()
            genre = document.select("div.series-genres a").joinToString { it.text() }
            thumbnail_url = document.select("img.series-cover").first()!!.absUrl("src")
            status = SManga.UNKNOWN // يمكنك تعديلها حسب الموقع إذا احتجت
            author = document.select("div.series-author").text()
        }
    }

    // Chapters
    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        return document.select("div.chapter-list div.chapter-card").map { chapterFromElement(it) }
    }

    override fun chapterFromElement(element: Element): SChapter {
        return SChapter.create().apply {
            name = element.select("div.chapter-title").text()
            setUrlWithoutDomain(element.select("a").first()!!.attr("href"))
            date_upload = 0 // يمكنك إضافة parsing لتاريخ الفصل إذا أردت
        }
    }

    // Pages
    override fun pageListParse(document: Document): List<Page> {
        return document.select("div.page-list img").mapIndexed { i, element ->
            Page(i, "", element.absUrl("src"))
        }
    }

    override fun imageUrlParse(document: Document): String = throw UnsupportedOperationException("Not used")

    // Preferences
    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        val baseUrlPref = androidx.preference.EditTextPreference(screen.context).apply {
            key = "overrideBaseUrl"
            title = "تعديل الرابط"
            summary = "لتغيير الرابط الأساسي مؤقتًا"
            setDefaultValue(defaultBaseUrl)
            dialogTitle = "تعديل الرابط"
            dialogMessage = "Default: $defaultBaseUrl"
            setOnPreferenceChangeListener { _, _ ->
                Toast.makeText(screen.context, "أعد تشغيل التطبيق لتطبيق التغييرات", Toast.LENGTH_LONG).show()
                true
            }
        }
        screen.addPreference(baseUrlPref)
    }

    private fun getPrefBaseUrl(): String = preferences.getString("overrideBaseUrl", defaultBaseUrl)!!

    init {
        preferences.getString("defaultBaseUrl", null)?.let { prefDefaultBaseUrl ->
            if (prefDefaultBaseUrl != defaultBaseUrl) {
                preferences.edit()
                    .putString("overrideBaseUrl", defaultBaseUrl)
                    .putString("defaultBaseUrl", defaultBaseUrl)
                    .apply()
            }
        }
    }
}
