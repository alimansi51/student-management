package com.sms.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * What the student portal sends and receives. These only carry what a student
 * is allowed to see (for example, no list of other students in a course).
 * The ...Url fields are temporary signed links for opening files.
 */
public final class StudentViews {

    private StudentViews() {
    }

    public record Profile(Long id, String firstName, String lastName, String email, String phone, String username) {
    }

    public record CourseView(Long id, String title, String description) {
    }

    /** streamUrl is null if the video has no file yet. */
    public record VideoView(Long id, String title, String streamUrl, Long courseId, String courseTitle) {
    }

    /** A lecture or other file shared by the teacher. */
    public record MaterialView(Long id, String title, String description, String originalFileName, Long fileSize,
                               String fileUrl, LocalDateTime uploadedAt, Long courseId, String courseTitle) {
    }

    /**
     * An assignment together with this student's submission for it (submission fields are null if not submitted).
     * content is only set for old typed answers; new submissions are PDF files.
     */
    public record AssignmentView(Long id, String title, String description, LocalDate dueDate,
                                 Long courseId, String courseTitle,
                                 Long submissionId, String content, String originalFileName, String fileUrl,
                                 LocalDateTime submittedAt, Integer grade) {
    }

    public record ChangePasswordRequest(String currentPassword, String newPassword) {
    }
}
