package xyz.fi5t

import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.utils.io.*
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import okhttp3.tls.decodeCertificatePem
import java.security.cert.X509Certificate

private const val CACERT = "-----BEGIN CERTIFICATE-----\n" +
        "MIIDqDCCApCgAwIBAgIEPhwe6TANBgkqhkiG9w0BAQsFADBiMRswGQYDVQQDDBJ3\n" +
        "d3cubW9ja3NlcnZlci5jb20xEzARBgNVBAoMCk1vY2tTZXJ2ZXIxDzANBgNVBAcM\n" +
        "BkxvbmRvbjEQMA4GA1UECAwHRW5nbGFuZDELMAkGA1UEBhMCVUswIBcNMTYwNjIw\n" +
        "MTYzNDE0WhgPMjExNzA1MjcxNjM0MTRaMGIxGzAZBgNVBAMMEnd3dy5tb2Nrc2Vy\n" +
        "dmVyLmNvbTETMBEGA1UECgwKTW9ja1NlcnZlcjEPMA0GA1UEBwwGTG9uZG9uMRAw\n" +
        "DgYDVQQIDAdFbmdsYW5kMQswCQYDVQQGEwJVSzCCASIwDQYJKoZIhvcNAQEBBQAD\n" +
        "ggEPADCCAQoCggEBAPGORrdkwTY1H1dvQPYaA+RpD+pSbsvHTtUSU6H7NQS2qu1p\n" +
        "sE6TEG2fE+Vb0QIXkeH+jjKzcfzHGCpIU/0qQCu4RVycrIW4CCdXjl+T3L4C0I3R\n" +
        "mIMciTig5qcAvY9P5bQAdWDkU36YGrCjGaX3QlndGxD9M974JdpVK4cqFyc6N4gA\n" +
        "Onys3uS8MMmSHTjTFAgR/WFeJiciQnal+Zy4ZF2x66CdjN+hP8ch2yH/CBwrSBc0\n" +
        "ZeH2flbYGgkh3PwKEqATqhVa+mft4dCrvqBwGhBTnzEGWK/qrl9xB4mTs4GQ/Z5E\n" +
        "8rXzlvpKzVJbfDHfqVzgFw4fQFGV0XMLTKyvOX0CAwEAAaNkMGIwHQYDVR0OBBYE\n" +
        "FH3W3sL4XRDM/VnRayaSamVLISndMA8GA1UdEwEB/wQFMAMBAf8wCwYDVR0PBAQD\n" +
        "AgG2MCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADANBgkqhkiG\n" +
        "9w0BAQsFAAOCAQEAecfgKuMxCBe/NxVqoc4kzacf9rjgz2houvXdZU2UDBY3hCs4\n" +
        "MBbM7U9Oi/3nAoU1zsA8Rg2nBwc76T8kSsfG1TK3iJkfGIOVjcwOoIjy3Z8zLM2V\n" +
        "YjYbOUyAQdO/s2uShAmzzjh9SV2NKtcNNdoE9e6udvwDV8s3NGMTUpY5d7BHYQqV\n" +
        "sqaPGlsKi8dN+gdLcRbtQo29bY8EYR5QJm7QJFDI1njODEnrUjjMvWw2yjFlje59\n" +
        "j/7LBRe2wfNmjXFYm5GqWft10UJ7Ypb3XYoGwcDac+IUvrgmgTHD+E3klV3SUi8i\n" +
        "Gm5MBedhPkXrLWmwuoMJd7tzARRHHT6PBH/ZGw==\n" +
        "-----END CERTIFICATE-----\n"

private const val CLIENT_CERT = "-----BEGIN CERTIFICATE-----\n" +
        "MIIFgjCCA2oCCQCeHxaTaEwG6TANBgkqhkiG9w0BAQsFADCBgjELMAkGA1UEBhMC\n" +
        "UlUxDzANBgNVBAgMBk1vc2NvdzEPMA0GA1UEBwwGTW9zY293MQ8wDQYDVQQKDAZT\n" +
        "ZWNyZXQxDDAKBgNVBAsMA0RldjESMBAGA1UEAwwJbG9jYWxob3N0MR4wHAYJKoZI\n" +
        "hvcNAQkBFg9hZG1pbkBsb2NhbGhvc3QwHhcNMjEwNzMxMTIzMDAzWhcNMjUwMzI5\n" +
        "MTIzMDAzWjCBgjELMAkGA1UEBhMCUlUxDzANBgNVBAgMBk1vc2NvdzEPMA0GA1UE\n" +
        "BwwGTW9zY293MQ8wDQYDVQQKDAZTZWNyZXQxDDAKBgNVBAsMA0RldjESMBAGA1UE\n" +
        "AwwJbG9jYWxob3N0MR4wHAYJKoZIhvcNAQkBFg9hZG1pbkBsb2NhbGhvc3QwggIi\n" +
        "MA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQC4lUH0MAdJkfUIgOqHlxxamlrG\n" +
        "fl9Gl24Ozk2ikuCeAOCDoaZbfE92qKbo8zL1f2HGBuLAqenUVfG9XAc952g5xgLA\n" +
        "RsEKwW6zI4DwdM0+z1F4PYuQ7nVuXfYskj2pahs7WWu0idK7N7p5cbAIa84VXKfa\n" +
        "N/7tmfsOvox6HqJkGmdq9Qz3A4ka4GIE6JYUsN17chNu2JahFyAmOIR0sNvazZJg\n" +
        "xBryy+N1uepvJGB6sHddXw4rcodf69jLlV02p90Qx36WsqdjaPYX/5PlWl9HDTGa\n" +
        "2sSaWL0ak1I+6Y+G8E9/MBX8BausSnST6zJGjsSKRyhn8wbNAjb9fKCH4i0tIYSW\n" +
        "PCEpodCLudugaK4EY1KKGRmJdycJvR1POphxSJodhcuYVENtgNrvXHpx3dffdhw4\n" +
        "IscpnMH2LMKzZA6Agetgc/BXvzfJuvEBxJGcq4d5mjoft2ZUueCdM2gLqwHaeP9U\n" +
        "XyIxX2DDexJ2dsXnt/k3YKGAjQNUuxeZPhSxITAHKCgZkH1eFi+O0p4XC/StppmY\n" +
        "1OM97iJ0q1JLXmJ8O5vJbKTXbRP2a0k/chAvWhKbw11NGS4tBsE77KzPJjI/1Hma\n" +
        "i9qMm0pIoCKg2U2FtjFxhcslLI+uHy2KgZkxOXc7LV2cibmAlLEW8KvJy88X9T+c\n" +
        "yT1jErnHGaYmJrq/XQIDAQABMA0GCSqGSIb3DQEBCwUAA4ICAQCy8OsyLpD2uPM9\n" +
        "+0uNWSCLYqAFxdEtBaABoxLFiPjffOjQoIDb8mQlQ2rY9eBSi/Lwy8f0O+iL419/\n" +
        "9cnY7j5STiUw2Wq2/ynLpXP1JFwcKbGLQr8TxjK3B6PbNku+PK0XDqdm6b5NtVxd\n" +
        "uJrJgKnbSd8o/9+DDEHI/WDMnNhPjpXlEjKLLN0WphAyCojS5Bgjc6TcgNbl8v5c\n" +
        "J6X8M37aU/bBZt7OnrsvmZ5fVeLKcFS6sR0fYnDvg6LnlEklU+6zvx3CW2D3VGLl\n" +
        "UbvSC2ZS5ciEY+yCJQNXjI1Bs4yePJQx6qSYW16nMTiWziwH4v2D5WN5iM5yU2/n\n" +
        "0Avkyw8JjUHiRSIZmRVZ/M0JqGM56OD9W4d6NcXkhEFfrNGA3Z0pLtg8yLxQawjN\n" +
        "bRk5oKMU6wxB3Mkbjaf2EoC9/bOA1VhSsqUkSa5qJJaYn7FGru2Op77rj++1bKUf\n" +
        "oTYnB/u5fm59oPMYDFvuh8UFqIH6I3253fejjBOeWUBSMo90vTZ9NxfSXvkMr6Ae\n" +
        "CnNUTlV9t6bE+KQ8CIwwoSrGbk8QtBFZG5Wo2oO5lcaBOrWIu9XY1E2sDGrThnYO\n" +
        "mmcXsFHCqIBvzxfw0PbBDblaY9erMFzWE8tAuRnJrQwzO4Vus9V7JgQOdSigE/1Q\n" +
        "HGh6/2TZ+8X8OeUlJ7DGIATIb8smzQ==\n" +
        "-----END CERTIFICATE-----\n"

private const val CLIENT_KEY = "-----BEGIN PRIVATE KEY-----\n" +
        "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\n" +
        "7jrTiwcEtANdV45Zd4mAS6ctvNIJvg==\n" +
        "-----END PRIVATE KEY-----\n"

private val PEM_REGEX = Regex("""-----BEGIN ([!-,.-~ ]*)-----([^-]*)-----END \1-----""")

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    val caBundle = CACERT.splitCertificatesPem()
    val heldCertificate = HeldCertificate.decode(CLIENT_CERT + CLIENT_KEY)

    val handshakeCerts = HandshakeCertificates.Builder().apply {
        caBundle.forEach { addTrustedCertificate(it) }
        heldCertificate(heldCertificate)
    }.build()

    val ktorClient = HttpClient(OkHttp) {
        expectSuccess = false
        engine {
            config {
                sslSocketFactory(handshakeCerts.sslSocketFactory(), handshakeCerts.trustManager)
            }
        }
    }

    intercept(ApplicationCallPipeline.Call) {
        val response = ktorClient.request<HttpResponse>("https://localhost:55000${call.request.uri}") {
            method = call.request.httpMethod

            headers {
                append("X-API-KEY", "102CAE97-17F9-4B08-82C0-AB7653F2EC1B")
            }

            if (call.request.httpMethod != HttpMethod.Get) {
                body = call.receiveText()
            }
        }

        call.respond(object : OutgoingContent.WriteChannelContent() {
            override val status: HttpStatusCode = response.status

            override suspend fun writeTo(channel: ByteWriteChannel) {
                response.content.copyAndClose(channel)
            }
        })
    }
}

fun String.splitCertificatesPem(): Sequence<X509Certificate> {
    return PEM_REGEX.findAll(this).map { match ->
        match.groups[0]!!.value.decodeCertificatePem()
    }
}