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
        val url = when {
            page <= 1 -> "$baseUrl/series"
            else -> "$baseUrl/series?page=$page"
        }
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
        val url = when {
            page <= 1 -> "$baseUrl/updates"
            else -> "$baseUrl/updates?page=$page"
        }
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

    override fun searchMa
