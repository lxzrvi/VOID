package dev.voidapp.data

/**
 * A small built-in host blocklist for ads and trackers. It is matched against the
 * host of every sub-resource request (never the page itself), so a blocked ad
 * simply loads as an empty response and the page still works.
 */
object AdBlock {
    private val domains = setOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google.com", "2mdn.net", "adnxs.com", "adsrvr.org",
        "taboola.com", "outbrain.com", "criteo.com", "criteo.net",
        "scorecardresearch.com", "amazon-adsystem.com", "moatads.com",
        "rubiconproject.com", "pubmatic.com", "openx.net", "adform.net",
        "quantserve.com", "popads.net", "propellerads.com", "exoclick.com",
        "adcolony.com", "mopub.com", "smaato.net", "inmobi.com", "yieldmo.com",
        "teads.tv", "adsafeprotected.com", "serving-sys.com", "bidswitch.net",
        "casalemedia.com", "contextweb.com", "lijit.com", "sharethrough.com",
        "33across.com", "zedo.com", "revcontent.com", "mgid.com",
        "ads.twitter.com", "ads.linkedin.com", "adroll.com", "bluekai.com",
        "krxd.net", "demdex.net", "everesttech.net", "mathtag.com",
    )

    fun isBlocked(host: String?): Boolean {
        val h = host?.lowercase() ?: return false
        return domains.any { h == it || h.endsWith(".$it") }
    }
}
