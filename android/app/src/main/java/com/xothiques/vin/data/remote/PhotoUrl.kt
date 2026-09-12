package com.xothiques.vin.data.remote

/**
 * The backend returns photo URLs as host-relative paths (e.g.
 * "/api/vin/photos/<uuid>.jpg") since it has no idea what address the
 * household will reach it at. We prefix with the same "http://localhost"
 * placeholder used as Retrofit's base URL -- AuthInterceptor rewrites the
 * scheme/host/port of every request (including the ones Coil's shared
 * OkHttpClient makes, see VinApplication) to the actual configured server,
 * and also attaches the JWT the /photos route requires.
 */
fun resolvePhotoUrl(path: String?): String? {
    if (path.isNullOrBlank()) return null
    return if (path.startsWith("http://") || path.startsWith("https://")) {
        path
    } else {
        "http://localhost${if (path.startsWith("/")) path else "/$path"}"
    }
}
