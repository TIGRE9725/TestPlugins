package com.netvideo

import android.util.Base64
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup
import org.json.JSONArray

class NetVideoProvider : MainAPI() { 
    private val mirrors = listOf(
        "http://186.0.248.95:20202",
        "http://170.246.176.223:12300",
        "http://181.209.112.98:40000",
        "http://45.225.68.1:8532"
    )

    override var mainUrl = mirrors[0]
    override var name = "NetVideo"
    override val hasMainPage = true
    override var lang = "es"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override val mainPage = mainPageOf(
        "/?kids" to "Niños",
        "/?movies&genres=Animaci" to "Animación",
        "/?movies&genres=Acci" to "Acción",
        "/?series" to "Series",
        "/?movies" to "Películas"
    )

    private suspend fun getWithFailover(path: String): String? {
        for (mirror in mirrors) {
            try {
                val fullUrl = "$mirror${if (path.startsWith("/")) path else "/$path"}"
                val res = app.get(fullUrl, cookies = mapOf("setLenguaje" to "spa"), timeout = 10)
                if (res.code == 200) {
                    this.mainUrl = mirror
                    return res.text
                }
            } catch (e: Exception) { continue }
        }
        return null
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val path = if (page <= 1) request.data else "${request.data}&page=$page"
        val html = getWithFailover(path) ?: throw ErrorLoadingException("Error de conexión")
        val document = Jsoup.parse(html)
        
        val items = document.select("a[href*=?item=]").mapNotNull { element ->
            val href = element.attr("href")
            val id = Regex("""item=(\d+)""").find(href)?.groupValues?.get(1) ?: return@mapNotNull null
            val title = element.text().ifEmpty { "Título $id" }
            val poster = fixUrl(element.select("img").attr("src"))

            if (href.contains("serie")) {
                newTvSeriesSearchResponse(title, "$mainUrl/?item=$id&serie") { this.posterUrl = poster }
            } else {
                newMovieSearchResponse(title, "$mainUrl/?item=$id&movie") { this.posterUrl = poster }
            }
        }
        return newHomePageResponse(request.name, items, hasNext = true)
    }

    override suspend fun load(url: String): LoadResponse {
        val path = url.replace(mainUrl, "")
        val html = getWithFailover(path) ?: throw ErrorLoadingException("No se pudo cargar")
        val document = Jsoup.parse(html)
        
        val title = document.select("h2").first()?.text() ?: "Sin título"
        val bgStyle = document.select("[style*=background-image]").attr("style")
        val poster = Regex("""url\(['"]?([^'"]+)['"]?\x29""").find(bgStyle ?: "")?.groupValues?.get(1)
            ?.replace("..", "")?.let { fixUrl(it) }

        if (url.contains("serie")) {
            val episodes = mutableListOf<TvSeriesEpisode>()
            document.select("a[href*=?item=][href*=&season]").forEach { seasonEl ->
                val sHref = seasonEl.attr("href")
                val sId = Regex("""item=(\d+)""").find(sHref)?.groupValues?.get(1)
                val sNum = Regex("""\d+""").find(seasonEl.text())?.value?.toIntOrNull() ?: 1
                
                val watchHtml = getWithFailover("/?watch=$sId&episode")
                Regex("""var\s+(?:serie|videos|movie)\s*=.*(\[.*?\]);""").find(watchHtml ?: "")?.groupValues?.get(1)?.let {
                    val arr = JSONArray(it)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        episodes.add(TvSeriesEpisode(
                            name = obj.optString("name"),
                            episode = obj.optInt("number"),
                            season = sNum,
                            data = obj.getString("stream") 
                        ))
                    }
                }
            }
            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) { this.posterUrl = poster }
        } else {
            val watchUrl = url.replace("item=", "watch=")
            return newMovieLoadResponse(title, url, TvType.Movie, watchUrl) { this.posterUrl = poster }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val b64 = if (data.startsWith("http")) {
            val res = app.get(data).text
            Regex("""var\s+movie\s*=\s*(\[.*?\]);""").find(res)?.groupValues?.get(1)?.let {
                JSONArray(it).getJSONObject(0).getString("stream")
            }
        } else data

        b64?.let {
            val decoded = base64Decode(it).replace("\\/", "/")
            val finalUrl = if (decoded.startsWith("http")) decoded else "$mainUrl${decoded}"
            
            callback(ExtractorLink(
                this.name,
                "Server ONDEMAND",
                finalUrl,
                referer = mainUrl,
                quality = Qualities.P720.value
            ))
        }
        return true
    }
}
