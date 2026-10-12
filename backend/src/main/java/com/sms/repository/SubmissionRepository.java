package com.sms.repository;

import com.sms.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByStudentId(Long studentId);

    List<Submission> findByAssignmentId(Long assignmentId);

    List<Submission> findByAssignmentCourseId(Long courseId);

    Optional<Submission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
}
