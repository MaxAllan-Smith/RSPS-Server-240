package org.example.app.core.items.wiki

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class WikiItemDataClient(
    private val baseUrl: String,
    private val userAgent: String,
) {

    private val objectMapper =
        jacksonObjectMapper()

    private val httpClient =
        HttpClient
            .newBuilder()
            .connectTimeout(
                Duration.ofSeconds(
                    CONNECT_TIMEOUT_SECONDS
                )
            )
            .build()

    fun mapping():
        List<WikiItemMapping> =
        get(
            path = "mapping",
        )

    fun latest():
        WikiLatestResponse =
        get(
            path = "latest",
        )

    fun averages5m():
        WikiAverageResponse =
        get(
            path = "5m",
        )

    fun averages1h():
        WikiAverageResponse =
        get(
            path = "1h",
        )

    private inline fun <reified T> get(
        path: String,
    ): T {
        val request =
            HttpRequest
                .newBuilder()
                .uri(
                    URI.create(
                        "$baseUrl/$path"
                    )
                )
                .timeout(
                    Duration.ofSeconds(
                        REQUEST_TIMEOUT_SECONDS
                    )
                )
                .header(
                    "User-Agent",
                    userAgent,
                )
                .header(
                    "Accept",
                    "application/json",
                )
                .GET()
                .build()

        val response =
            httpClient.send(
                request,
                HttpResponse
                    .BodyHandlers
                    .ofString(),
            )

        check(
            response.statusCode() in
                200 until 300
        ) {
            "Wiki item API '$path' returned " +
                "HTTP ${response.statusCode()}."
        }

        return objectMapper.readValue(
            response.body()
        )
    }

    private companion object {
        const val CONNECT_TIMEOUT_SECONDS: Long =
            10L

        const val REQUEST_TIMEOUT_SECONDS: Long =
            30L
    }
}

@JsonIgnoreProperties(
    ignoreUnknown = true,
)
data class WikiItemMapping(
    val id: Int,
    val name: String?,
    val examine: String?,
    val members: Boolean?,
    val lowalch: Int?,
    val highalch: Int?,
    val limit: Int?,
    val value: Int?,
    val icon: String?,
)

@JsonIgnoreProperties(
    ignoreUnknown = true,
)
data class WikiLatestResponse(
    val data:
        Map<String, WikiLatestPrice>,
)

@JsonIgnoreProperties(
    ignoreUnknown = true,
)
data class WikiLatestPrice(
    val high: Int?,
    val highTime: Long?,
    val low: Int?,
    val lowTime: Long?,
)

@JsonIgnoreProperties(
    ignoreUnknown = true,
)
data class WikiAverageResponse(
    val timestamp: Long,
    val data:
        Map<String, WikiAveragePrice>,
)

@JsonIgnoreProperties(
    ignoreUnknown = true,
)
data class WikiAveragePrice(
    val avgHighPrice: Int?,
    val highPriceVolume: Int?,
    val avgLowPrice: Int?,
    val lowPriceVolume: Int?,
)