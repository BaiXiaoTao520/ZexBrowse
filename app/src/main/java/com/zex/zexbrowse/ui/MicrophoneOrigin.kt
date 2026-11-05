package com.zex.zexbrowse.ui

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** An unambiguous HTTP(S) origin for both the prompt and remembered decisions. */
internal fun microphoneOrigin(uri: String): String? {
    val url = uri.toHttpUrlOrNull() ?: return null
    val host = if (':' in url.host) "[${url.host}]" else url.host
    return "${url.scheme}://$host:${url.port}"
}
