package com.sms.service;

import com.sms.model.Assignment;
import com.sms.model.Submission;
import com.sms.repository.AssignmentRepository;
import com.sms.repository.SubmissionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final CourseService courseService;
    private final SubmissionRepository submissionRepository;
    private final FileStorageService storage;

    public AssignmentService(AssignmentRepository assignmentRepository, CourseService courseService,
                             SubmissionRepository submissionRepository, FileStorageService storage) {
        this.assignmentRepository = assignmentRepository;
        this.courseService = courseService;
        this.submissionRepository = submissionRepository;
        this.storage = storage;
    }

    public List<Assignment> findAll() {
        return assignmentRepository.findAll();
    }

    public Assignment findById(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
    }

    public Assignment create(Assignment assignment) {
        assignment.setId(null);
        return save(assignment);
    }

    public Assignment update(Long id, Assignment assignment) {
        findById(id);
        assignment.setId(id);
        return save(assignment);
    }

    /** Submissions are removed by the database; their PDF files are removed here. */
    public void delete(Long id) {
        Assignment assignment = findById(id);
        List<Submission> submissions = submissionRepository.findByAssignmentId(id);
        assignmentRepository.delete(assignment);
        submissions.forEach(s -> storage.delete(SubmissionService.FOLDER, s.getFileName()));
    }

    private Assignment save(Assignment assignment) {
        if (assignment.getCourse() == null || assignment.getCourse().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course is required");
        }
        assignment.setCourse(courseService.findById(assignment.getCourse().getId()));
        return assignmentRepository.save(assignment);
    }
}
