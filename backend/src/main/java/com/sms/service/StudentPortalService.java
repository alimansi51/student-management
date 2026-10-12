package com.sms.service;

import com.sms.dto.StudentViews.*;
import com.sms.model.Assignment;
import com.sms.model.Course;
import com.sms.model.Submission;
import com.sms.model.User;
import com.sms.repository.AssignmentRepository;
import com.sms.repository.CourseRepository;
import com.sms.repository.MaterialRepository;
import com.sms.repository.SubmissionRepository;
import com.sms.repository.VideoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Everything a logged-in student can see and do. Always limited to the student's own courses and work. */
@Service
public class StudentPortalService {

    private final UserService userService;
    private final AssignmentService assignmentService;
    private final VideoService videoService;
    private final MaterialService materialService;
    private final SubmissionService submissionService;
    private final CourseRepository courseRepository;
    private final VideoRepository videoRepository;
    private final MaterialRepository materialRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final FileStorageService storage;

    public StudentPortalService(UserService userService, AssignmentService assignmentService,
                                VideoService videoService, MaterialService materialService,
                                SubmissionService submissionService, CourseRepository courseRepository,
                                VideoRepository videoRepository, MaterialRepository materialRepository,
                                AssignmentRepository assignmentRepository, SubmissionRepository submissionRepository,
                                FileStorageService storage) {
        this.userService = userService;
        this.assignmentService = assignmentService;
        this.videoService = videoService;
        this.materialService = materialService;
        this.submissionService = submissionService;
        this.courseRepository = courseRepository;
        this.videoRepository = videoRepository;
        this.materialRepository = materialRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.storage = storage;
    }

    public Profile profile(String username) {
        User s = userService.findByUsername(username);
        return new Profile(s.getId(), s.getFirstName(), s.getLastName(), s.getEmail(), s.getPhone(), s.getUsername());
    }

    public List<CourseView> courses(String username) {
        return enrolledCourses(username).stream()
                .map(c -> new CourseView(c.getId(), c.getTitle(), c.getDescription()))
                .toList();
    }

    public List<VideoView> videos(String username) {
        List<Long> courseIds = courseIds(username);
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return videoRepository.findByCourseIdIn(courseIds).stream()
                .map(v -> new VideoView(v.getId(), v.getTitle(), videoService.streamUrl(v),
                        v.getCourse().getId(), v.getCourse().getTitle()))
                .toList();
    }

    public List<MaterialView> materials(String username) {
        List<Long> courseIds = courseIds(username);
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return materialRepository.findByCourseIdIn(courseIds).stream()
                .sorted(Comparator.comparing(m -> m.getUploadedAt(), Comparator.nullsLast(Comparator.reverseOrder())))
                .map(m -> new MaterialView(m.getId(), m.getTitle(), m.getDescription(), m.getOriginalFileName(),
                        m.getFileSize(), materialService.fileUrl(m), m.getUploadedAt(),
                        m.getCourse().getId(), m.getCourse().getTitle()))
                .toList();
    }

    public List<AssignmentView> assignments(String username) {
        User student = userService.findByUsername(username);
        List<Long> courseIds = courseIds(username);
        if (courseIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Submission> mySubmissions = submissionRepository.findByStudentId(student.getId()).stream()
                .collect(Collectors.toMap(s -> s.getAssignment().getId(), Function.identity(), (a, b) -> a));

        return assignmentRepository.findByCourseIdIn(courseIds).stream()
                .sorted(Comparator.comparing(Assignment::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(a -> toView(a, mySubmissions.get(a.getId())))
                .toList();
    }

    /** Hands in (or replaces) the student's PDF. Not allowed once the teacher has graded it. */
    public AssignmentView submit(String username, Long assignmentId, MultipartFile file) {
        User student = userService.findByUsername(username);
        if (assignmentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignment is required");
        }
        Assignment assignment = assignmentService.findById(assignmentId);
        if (!courseIds(username).contains(assignment.getCourse().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not enrolled in this course");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please choose a PDF file");
        }
        if (!FileStorageService.isPdf(file)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your submission must be a PDF file");
        }

        Submission submission = submissionRepository
                .findByAssignmentIdAndStudentId(assignment.getId(), student.getId())
                .orElseGet(Submission::new);
        if (submission.getGrade() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This assignment is already graded");
        }
        String oldFile = submission.getFileName();
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setContent(null);
        submission.setFileName(storage.store(SubmissionService.FOLDER, file));
        submission.setOriginalFileName(file.getOriginalFilename());
        submission.setFileSize(file.getSize());
        submission.setSubmittedAt(LocalDateTime.now());
        Submission saved = submissionRepository.save(submission);
        storage.delete(SubmissionService.FOLDER, oldFile);
        return toView(assignment, saved);
    }

    public void changePassword(String username, ChangePasswordRequest request) {
        userService.changePassword(userService.findByUsername(username),
                request.currentPassword(), request.newPassword());
    }

    /** Courses, videos, materials, assignments and submitting all go through here. */
    private List<Course> enrolledCourses(String username) {
        return courseRepository.findByStudentsId(activeStudent(username).getId());
    }

    /** A student still on their one-time password may only view their profile and change the password. */
    private User activeStudent(String username) {
        User student = userService.findByUsername(username);
        if (userService.mustChangePassword(student)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Please change your one-time password first");
        }
        return student;
    }

    private List<Long> courseIds(String username) {
        return enrolledCourses(username).stream().map(Course::getId).toList();
    }

    private AssignmentView toView(Assignment a, Submission s) {
        return new AssignmentView(a.getId(), a.getTitle(), a.getDescription(), a.getDueDate(),
                a.getCourse().getId(), a.getCourse().getTitle(),
                s == null ? null : s.getId(),
                s == null ? null : s.getContent(),
                s == null ? null : s.getOriginalFileName(),
                s == null ? null : submissionService.fileUrl(s),
                s == null ? null : s.getSubmittedAt(),
                s == null ? null : s.getGrade());
    }
}
