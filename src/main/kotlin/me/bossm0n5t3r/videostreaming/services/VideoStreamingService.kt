package me.bossm0n5t3r.videostreaming.services

import me.bossm0n5t3r.videostreaming.configurations.LOGGER
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Service
class VideoStreamingService {
    companion object {
        private const val VIDEO = "/video"
        private const val VIDEO_CONTENT = "video/"
        private const val BYTES = "bytes"
        private const val CHUNK_SIZE = 314700L
    }

    fun streamVideo(
        fileName: String,
        fileType: String,
        httpRangeList: String?,
    ): ResponseEntity<ByteArray> {
        return try {
            val fileKey = "$fileName.$fileType"
            var rangeStart = 0L
            var rangeEnd = CHUNK_SIZE
            val fileSize = getFileSize(fileKey)
            if (httpRangeList == null) {
                return ResponseEntity
                    .status(HttpStatus.PARTIAL_CONTENT)
                    .header(HttpHeaders.CONTENT_TYPE, VIDEO_CONTENT + fileType)
                    .header(HttpHeaders.ACCEPT_RANGES, BYTES)
                    .header(HttpHeaders.CONTENT_RANGE, "$BYTES 0-$rangeEnd/$fileSize")
                    .header(HttpHeaders.CONTENT_LENGTH, fileSize.toString())
                    .body(readByteRange(fileKey, rangeStart, rangeEnd)) // Read the object and convert it as bytes
            }
            val ranges = httpRangeList.split("-").dropLastWhile { it.isEmpty() }.toTypedArray()
            rangeStart = ranges[0].substring(6).toLong()
            rangeEnd =
                if (ranges.size > 1) {
                    ranges[1].toLong()
                } else {
                    rangeStart + CHUNK_SIZE
                }

            rangeEnd = minOf(rangeEnd, fileSize - 1)
            val data = readByteRange(fileKey, rangeStart, rangeEnd)
            val contentLength = ((rangeEnd - rangeStart) + 1).toString()
            val httpStatus =
                if (rangeEnd >= fileSize) {
                    HttpStatus.OK
                } else {
                    HttpStatus.PARTIAL_CONTENT
                }
            ResponseEntity
                .status(httpStatus)
                .header(HttpHeaders.CONTENT_TYPE, VIDEO_CONTENT + fileType)
                .header(HttpHeaders.ACCEPT_RANGES, BYTES)
                .header(HttpHeaders.CONTENT_LENGTH, contentLength)
                .header(HttpHeaders.CONTENT_RANGE, "$BYTES $rangeStart-$rangeEnd/$fileSize")
                .body(data)
                .also {
                    LOGGER.info("Succeeded streamVideo: {}, {}, {}, {}, {}", fileName, fileType, httpRangeList, rangeStart, rangeEnd)
                }
        } catch (e: IOException) {
            LOGGER.error("Failed streamVideo: {}, {}, {}, {}", fileName, fileType, httpRangeList, e.message)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    private fun readByteRange(
        filename: String,
        start: Long,
        end: Long,
    ): ByteArray {
        val path = filename.toPath()
        val data = Files.readAllBytes(path)
        return data.sliceArray(start.toInt()..end.toInt())
    }

    private fun String.toPath(): Path = Paths.get(getFilePath(), this)

    private fun getFileSize(fileName: String): Long =
        runCatching { fileName.toPath() }
            .map { sizeFromFile(it) }
            .getOrNull()
            ?: 0L

    private fun getFilePath(): String {
        val url = checkNotNull(javaClass.getResource(VIDEO))
        return File(url.file).absolutePath
    }

    private fun sizeFromFile(path: Path): Long =
        try {
            Files.size(path)
        } catch (ioException: IOException) {
            LOGGER.error("Failed sizeFromFile: {}, ", path, ioException)
            0L
        }
}
