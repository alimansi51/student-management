package com.sms.controller;

import com.sms.model.Video;
import com.sms.service.VideoService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping
    public List<Video> getAll() {
        return videoService.findAll();
    }

    @GetMapping("/{id}")
    public Video getOne(@PathVariable Long id) {
        return videoService.findById(id);
    }

    /** Multipart upload: a "video" part with the JSON details and a "file" part with the video itself. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Video create(@RequestPart("video") Video video,
                        @RequestPart("file") MultipartFile file) {
        return videoService.create(video, file);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Video update(@PathVariable Long id,
                        @RequestPart("video") Video video,
                        @RequestPart(value = "file", required = false) MultipartFile file) {
        return videoService.update(id, video, file);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        videoService.delete(id);
    }

    /** Plays the video. Uses a signed link instead of a login; Spring handles "Range" so the player can skip ahead. */
    @GetMapping("/{id}/stream")
    public ResponseEntity<Resource> stream(@PathVariable Long id, @RequestParam long expires, @RequestParam String sig) {
        return videoService.stream(id, expires, sig);
    }
}
