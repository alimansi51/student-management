package com.sms.service;

import com.sms.model.Course;
import com.sms.model.Video;
import com.sms.repository.VideoRepository;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class VideoService {

    public static final String FOLDER = "videos";

    private final VideoRepository videoRepository;
    private final CourseService courseService;
    private final FileStorageService storage;

    public VideoService(VideoRepository videoRepository, CourseService courseService, FileStorageService storage) {
        this.videoRepository = videoRepository;
        this.courseService = courseService;
        this.storage = storage;
    }

    public List<Video> findAll() {
        return videoRepository.findAll().stream().map(this::withStreamUrl).toList();
    }

    public Video findById(Long id) {
        return withStreamUrl(videoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found")));
    }

    public Video create(Video video, MultipartFile file) {
        video.setId(null);
        video.setCourse(requireCourse(video));
        saveFile(video, file);
        return withStreamUrl(videoRepository.save(video));
    }

    /** Updates the title/course. The video file is only replaced if a new one was chosen. */
    public Video update(Long id, Video changes, MultipartFile file) {
        Video video = findById(id);
        video.setTitle(changes.getTitle());
        video.setCourse(requireCourse(changes));

        String oldFile = null;
        if (file != null && !file.isEmpty()) {
            oldFile = video.getFileName();
            saveFile(video, file);
        }
        Video saved = videoRepository.save(video);
        storage.delete(FOLDER, oldFile);
        return withStreamUrl(saved);
    }

    public void delete(Long id) {
        Video video = findById(id);
        videoRepository.delete(video);
        storage.delete(FOLDER, video.getFileName());
    }

    /** Plays the video for a signed link (see FileStorageService). */
    public ResponseEntity<Resource> stream(Long id, long expires, String sig) {
        storage.checkLink(streamPath(id), expires, sig);
        Video video = findById(id);
        if (video.getFileName() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This video has no file");
        }
        return storage.serve(FOLDER, video.getFileName(), video.getContentType(), video.getOriginalFileName());
    }

    /** A temporary link to play the video, or null if it has no file. */
    public String streamUrl(Video video) {
        return video.getFileName() == null ? null : storage.signedLink(streamPath(video.getId()));
    }

    private static String streamPath(Long id) {
        return "/api/videos/" + id + "/stream";
    }

    private void saveFile(Video video, MultipartFile file) {
        if (file != null && (file.getContentType() == null || !file.getContentType().startsWith("video/"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file must be a video");
        }
        video.setFileName(storage.store(FOLDER, file));
        video.setOriginalFileName(file.getOriginalFilename());
        video.setContentType(file.getContentType());
        video.setFileSize(file.getSize());
    }

    private Course requireCourse(Video video) {
        if (video.getCourse() == null || video.getCourse().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course is required");
        }
        return courseService.findById(video.getCourse().getId());
    }

    private Video withStreamUrl(Video video) {
        video.setStreamUrl(streamUrl(video));
        return video;
    }
}
