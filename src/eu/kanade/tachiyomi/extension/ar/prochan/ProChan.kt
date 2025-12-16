package eu.kanade.tachiyomi.extension.ar.prochan

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import eu.kanade.tachiyomi.util.getPreferencesLazy
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import android.app.Application
import android.content.SharedPreferences
import eu.kanade.tachiyomi.preferences.PreferencesHelper
import eu.kanade.tachiyomi.ui.preference.PreferenceScreen
import eu.kanade.tachiyomi.ui.preference.ListPreference
import java.text.SimpleDateFormat
import java.util.Locale

class ProChan(
    override val name: String = "ProChan",
    override val baseUrl: String = "https://prochan.net",
    override val lang: String = "ar",
    override val supportsLatest: Boolean = true
) : ParsedHttpSource(), ConfigurableSource {

    override val client: OkHttpClient = network.client

    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    // -------------------- POPULAR MANGA --------------------
    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/manga?page=$page", headers)
    }

    override fun popularMangaSelector(): String = "div.manga-list div.manga-item"

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        manga.title = element.select("h3.manga-title").text()
        manga.setUrlWithoutDomain(element.select("a").attr("href"))
        manga.thumbnail_url = element.select("img").attr("src")
        return manga
    }

    override fun popularMangaNextPageSelector(): String? = "ul.pagination li.next a"

    // -------------------- LATEST UPDATES --------------------
    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/latest?page=$page", headers)
    }

    override fun latestUpdatesSelector(): String = "div.latest-list div.manga-item"

    override fun latestUpdatesFromElement(element: Element): SManga {
        val manga = SManga.create()
        manga.title = element.select("h3.manga-title").text()
        manga.setUrlWithoutDomain(element.select("a").attr("href"))
        manga.thumbnail_url = element.select("img").attr("src")
        return manga
    }

    override fun latestUpdatesNextPageSelector(): String? = "ul.pagination li.next a"

    // -------------------- SEARCH --------------------
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return GET("$baseUrl/search?query=$query&page=$page", headers)
    }

    override fun searchMangaSelector(): String = "div.search-results div.manga-item"

    override fun searchMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        manga.title = element.select("h3.manga-title").text()
        manga.setUrlWithoutDomain(element.select("a").attr("href"))
        manga.thumbnail_url = element.select("img").attr("src")
        return manga
    }

    override fun searchMangaNextPageSelector(): String? = "ul.pagination li.next a"

    // -------------------- MANGA DETAILS --------------------
    override fun mangaDetailsParse(document: Document): SManga {
        val manga = SManga.create()
        manga.title = document.select("h1.manga-title").text()
        manga.description = document.select("div.description").text()
        manga.genre = document.select("div.genres a").joinToString(", ") { it.text() }
        manga.thumbnail_url = document.select("div.cover img").attr("src")
        manga.status = SManga.ONGOING
        manga.author = document.select("div.author").text()
        return manga
    }

    // -------------------- CHAPTERS --------------------
    override fun chapterListSelector(): String = "div.chapter-list div.chapter-item"

    override fun chapterFromElement(element: Element): SChapter {
        val chapter = SChapter.create()
        chapter.name = element.select("a.chapter-title").text()
        chapter.setUrlWithoutDomain(element.select("a").attr("href"))
        chapter.date_upload = parseDate(element.select("span.chapter-date").text())
        return chapter
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        return super.chapterListParse(response)
    }

    private fun parseDate(date: String): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        return try {
            sdf.parse(date)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    // -------------------- PAGES --------------------
    override fun pageListParse(document: Document): List<Page> {
        return document.select("div.page-list img").mapIndexed { i, img ->
            Page(i, "", img.attr("src"))
        }
    }

    override fun imageUrlParse(document: Document): String = ""

    // -------------------- PREFERENCES --------------------
    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        val languagePref = ListPreference(screen.context).apply {
            key = "language_preference"
            title = "Language"
            entries = arrayOf("Arabic", "English")
            entryValues = arrayOf("ar", "en")
            setDefaultValue("ar")
        }
        screen.addPreference(languagePref)
    }
}
