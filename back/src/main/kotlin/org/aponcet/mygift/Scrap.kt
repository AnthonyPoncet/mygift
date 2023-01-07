import it.skrape.core.htmlDocument
import it.skrape.fetcher.BrowserFetcher
import it.skrape.fetcher.extractIt
import it.skrape.fetcher.skrape
import it.skrape.selects.html5.div
import it.skrape.selects.html5.img
import it.skrape.selects.html5.p
import it.skrape.selects.html5.span

data class GiftDetails(var title: String = "", var description: String = "", var price: String = "", var image: String = "")


fun amazon(requestUrl: String) = skrape(BrowserFetcher) {
    request {
        url = requestUrl
    }
    extractIt<GiftDetails> {
        htmlDocument {
            relaxed = true
            it.title = span {
                withId = "productTitle"
                findFirst { text }
            }

            val whole = span {
                withClass = "a-price-whole"
                findFirst { ownText }
            }
            val fraction = span {
                withClass = "a-price-fraction"
                findFirst { ownText }
            }
            val symbol = span {
                withClass = "a-price-symbol"
                findFirst { ownText }
            }
            it.price = "$whole,$fraction$symbol"
            it.image = img {
                withAttribute = Pair("data-a-image-name", "landingImage")
                findFirst { attribute("data-old-hires") }
            }
        }
    }
}

fun main() {
    val title = skrape(BrowserFetcher) {
        request {
            //url = "https://www.amazon.co.uk/Funko-Pokemon-Leafeon-Vinyl-Figure/dp/B09LJQ9KMQ/?_encoding=UTF8&pd_rd_w=Q4bqZ&content-id=amzn1.sym.bba8e40f-79af-4f07-9fba-072d749d3c35&pf_rd_p=bba8e40f-79af-4f07-9fba-072d749d3c35&pf_rd_r=6X5A04M3535PK25RZWGK&pd_rd_wg=TVziV&pd_rd_r=be9908d3-4389-40a1-8df8-8945a8826682&ref_=pd_gw_bmx_gp_3e7221fo"
            url = "https://www.amazon.fr/CalDigit-TS4-Thunderbolt-Station-0-8m-Cable/dp/B09GFT334R/?_encoding=UTF8&pd_rd_w=pV3DU&content-id=amzn1.sym.d01040f0-2953-47b0-8f72-922a5679b397&pf_rd_p=d01040f0-2953-47b0-8f72-922a5679b397&pf_rd_r=N9HDNXJ5HKYR70TTQXAR&pd_rd_wg=PU1FZ&pd_rd_r=77571bb6-9207-486f-b75f-c87c9be0383f&ref_=pd_gw_ci_mcx_mr_hp_atf_m"
        }
        extractIt<GiftDetails> {
            htmlDocument {
                it.title = span {
                    withId = "productTitle"
                    findFirst { text }
                }
                it.description = div {
                    withId = "productDescription"
                    findFirst { p { span { findFirst { text } } } }
                }
                val whole = span {
                    withClass = "a-price-whole"
                    findFirst { ownText }
                }
                val fraction = span {
                    withClass = "a-price-fraction"
                    findFirst { ownText }
                }
                val symbol = span {
                    withClass = "a-price-symbol"
                    findFirst { ownText }
                }
                it.price = "$whole,$fraction$symbol"
                it.image = img {
                    withAttribute = Pair("data-a-image-name", "landingImage")
                    findFirst { attribute("data-old-hires") }
                }
            }
        }
    }

    println(title)
}