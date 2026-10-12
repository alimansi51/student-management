package com.sms.service;

import com.sms.model.Course;
import com.sms.model.Material;
import com.sms.model.Submission;
import com.sms.model.User;
import com.sms.model.Video;
import com.sms.repository.CourseRepository;
import com.sms.repository.MaterialRepository;
import com.sms.repository.SubmissionRepository;
import com.sms.repository.UserRepository;
import com.sms.repository.VideoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final MaterialRepository materialRepository;
    private final SubmissionRepository submissionRepository;
    private final FileStorageService storage;

    public CourseService(CourseRepository courseRepository, UserRepository userRepository,
                         VideoRepository videoRepository, MaterialRepository materialRepository,
                         SubmissionRepository submissionRepository, FileStorageService storage) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.videoRepository = videoRepository;
        this.materialRepository = materialRepository;
        this.submissionRepository = submissionRepository;
        this.storage = storage;
    }

    public List<Course> findAll() {
        return courseRepository.findAll();
    }

    public Course findById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    }

    public Course create(Course course) {
        course.setId(null);
        course.setStudents(loadStudents(course));
        return courseRepository.save(course);
    }

    public Course update(Long id, Course course) {
        findById(id);
        course.setId(id);
        course.setStudents(loadStudents(course));
        return courseRepository.save(course);
    }

    /**
     * Videos, materials, assignments and submissions of the course are removed by the database;
     * their files are removed here.
     */
    public void delete(Long id) {
        Course course = findById(id);
        List<Video> videos = videoRepository.findByCourseIdIn(List.of(id));
        List<Material> materials = materialRepository.findByCourseIdIn(List.of(id));
        List<Submission> submissions = submissionRepository.findByAssignmentCourseId(id);
        courseRepository.delete(course);
        videos.forEach(v -> storage.delete(VideoService.FOLDER, v.getFileName()));
        materials.forEach(m -> storage.delete(MaterialService.FOLDER, m.getFileName()));
        submissions.forEach(s -> storage.delete(SubmissionService.FOLDER, s.getFileName()));
    }

    /** The client only sends student ids, so load the real users from the database. */
    private List<User> loadStudents(Course course) {
        if (course.getStudents() == null) {
            return new ArrayList<>();
        }
        List<Long> ids = course.getStudents().stream().map(User::getId).toList();
        return new ArrayList<>(userRepository.findAllById(ids));
    }
}
