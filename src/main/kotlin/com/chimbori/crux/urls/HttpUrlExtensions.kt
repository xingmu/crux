package com.chimbori.crux.urls

import com.chimbori.crux.common.countMatches
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

// Checks heuristically whether a given URL is likely to be an article, video, image, or other types. Can optionally
// resolve redirects such as when Facebook or Google show an interstitial page instead of redirecting the user to the
// actual URL.

fun HttpUrl.isAdImage(): Boolean = toString().countMatches("ad") >= 2

fun HttpUrl.isLikelyArticle(): Boolean = !isLikelyImage() && !isLikelyVideo() && !isLikelyAudio() &&
    !isLikelyBinaryDocument() && !isLikelyExecutable() && !isLikelyArchive()

fun HttpUrl.isLikelyVideo(): Boolean = when (encodedPath.substringAfterLast(".")) {
  "avi",
  "flv",
  "mov",
  "mp4",
  "mpeg",
  "mpg",
  "mpg4",
  "wmv",
  -> true
  else -> false
}

fun HttpUrl.isLikelyAudio(): Boolean = when (encodedPath.substringAfterLast(".")) {
  "m3u",
  "mp3",
  "ogg",
  "wav",
  -> true
  else -> false
}

fun HttpUrl.isLikelyBinaryDocument(): Boolean = when (encodedPath.substringAfterLast(".")) {
  "doc",
  "pdf",
  "ppt",
  "rtf",
  "swf",
  "xls",
  -> true
  else -> false
}

fun HttpUrl.isLikelyArchive(): Boolean = when (encodedPath.substringAfterLast(".")) {
  "7z",
  "deb",
  "gz",
  "rar",
  "rpm",
  "tgz",
  "zip",
  -> true
  else -> false
}

fun HttpUrl.isLikelyExecutable(): Boolean = when (encodedPath.substringAfterLast(".")) {
  "bat",
  "bin",
  "dmg",
  "exe",
  -> true
  else -> false
}


fun HttpUrl.isLikelyImage(): Boolean = when (encodedPath.substringAfterLast(".")) {
  "bmp",
  "eps",
  "gif",
  "ico",
  "jpeg",
  "jpg",
  "png",
  -> true
  else -> false
}

@Suppress("unused")
fun HttpUrl.resolveRedirects(): HttpUrl {
  var urlBeforeThisPass = this
  var urlAfterThisPass = this
  while (true) { // Go through redirectors multiple times while the URL is still being changed.
    REDIRECTORS.forEach { redirector ->
      if (redirector.matches(urlBeforeThisPass)) {
        urlAfterThisPass = redirector.resolve(urlBeforeThisPass)
      }
    }
    if (urlBeforeThisPass == urlAfterThisPass) {
      return urlAfterThisPass
    } else {
      urlBeforeThisPass = urlAfterThisPass
    }
  }
}

private val REDIRECTORS = listOf(
  object : RedirectPattern {  // Facebook.
    override fun matches(url: HttpUrl) = url.host.endsWith(".facebook.com") && url.encodedPath == "/l.php"
    override fun resolve(url: HttpUrl) = url.queryParameter("u")?.toHttpUrlOrNull()
      ?: url
  },
  object : RedirectPattern { // Google.
    override fun matches(url: HttpUrl) = url.host.endsWith(".google.com") && url.encodedPath == "/url"
    override fun resolve(url: HttpUrl) = (url.queryParameter("q") ?: url.queryParameter("url"))?.toHttpUrlOrNull()
      ?: url
  }
)

/**
 * Defines a pattern used by a specific service for URL redirection. This should be stateless, and will be called for
 * each URL that needs to be resolved.
 */
internal interface RedirectPattern {
  /** @return true if this RedirectPattern can handle the provided URL, false if not. */
  fun matches(url: HttpUrl): Boolean

  /** @return the actual URL that is pointed to by this redirector URL. */
  fun resolve(url: HttpUrl): HttpUrl
}
