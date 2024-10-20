package me.bossm0n5t3r.videostreaming.controllers

import me.bossm0n5t3r.videostreaming.services.VideoStreamingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@RestController
@RequestMapping("/api/v1/video/streaming")
class VideoStreamingController(
    private val videoStreamingService: VideoStreamingService,
) {
    @GetMapping("/play/{fileName}/{fileType}")
    fun streamVideo(
        @RequestHeader(value = "Range", required = false) httpRangeList: String?,
        @PathVariable fileName: String,
        @PathVariable fileType: String,
    ): Mono<ResponseEntity<ByteArray>> = videoStreamingService.streamVideo(fileName, fileType, httpRangeList).toMono()
}
