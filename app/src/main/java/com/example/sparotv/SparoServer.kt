import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status
import java.io.File
import java.io.FileInputStream

class SparoServer(private val rootDir: File, port: Int = 8080) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        
        // 1. Serve SPARO TV+ Web UI
        if (uri == "/") return newFixedLengthResponse(Status.OK, "text/html", getWebUI())
        
        // 2. Serve JSON mapping of your external drive folders
        if (uri == "/api/library") return newFixedLengthResponse(Status.OK, "application/json", getLibraryJson())

        // 3. Raw Video Streaming (Kodi-style no-compression algorithm)
        if (uri.startsWith("/media/")) {
            val filePath = uri.removePrefix("/media/")
            val file = File(rootDir, filePath)
            return streamVideo(session.headers, file)
        }
        
        return newFixedLengthResponse(Status.NOT_FOUND, "text/plain", "404 Not Found")
    }

    private fun streamVideo(headers: Map<String, String>, file: File): Response {
        if (!file.exists()) return newFixedLengthResponse(Status.NOT_FOUND, "text/plain", "File Missing")

        val fileLen = file.length()
        var startFrom: Long = 0
        var endAt: Long = -1

        // Process HTTP Range Request for lag-free seeking
        var range = headers["range"]
        if (range != null && range.startsWith("bytes=")) {
            range = range.substring(6)
            val minus = range.indexOf('-')
            try {
                if (minus > 0) {
                    startFrom = range.substring(0, minus).toLong()
                    endAt = range.substring(minus + 1).toLong()
                }
            } catch (ignored: NumberFormatException) {}
        }

        val dataLen = if (endAt >= 0) endAt - startFrom + 1 else fileLen - startFrom
        val fis = FileInputStream(file)
        fis.skip(startFrom)

        val response = newFixedLengthResponse(Status.PARTIAL_CONTENT, "video/mp4", fis, dataLen)
        response.addHeader("Accept-Ranges", "bytes")
        response.addHeader("Content-Length", dataLen.toString())
        response.addHeader("Content-Range", "bytes $startFrom-${startFrom + dataLen - 1}/$fileLen")
        return response
    }

    private fun getLibraryJson(): String {
        val moviesDir = File(rootDir, "movie")
        val showsDir = File(rootDir, "show")
        
        // Scan /movie folder
        val movies = moviesDir.listFiles()?.filter { it.isFile }?.map { "\"${it.name}\"" } ?: emptyList()
        
        // Scan /show folder (Iterating through each Show's subfolder)
        val shows = mutableListOf<String>()
        showsDir.listFiles()?.filter { it.isDirectory }?.forEach { showFolder ->
            showFolder.listFiles()?.filter { it.isFile }?.forEach { ep ->
                shows.add("{\"show\": \"${showFolder.name}\", \"episode\": \"${ep.name}\", \"path\": \"show/${showFolder.name}/${ep.name}\"}")
            }
        }
        return "{\"movies\": [${movies.joinToString(",")}], \"shows\": [${shows.joinToString(",")}]}"
    }

    private fun getWebUI(): String {
        // We will define this string in Step 4
        return "HTML_CODE_HERE"
    }
}
