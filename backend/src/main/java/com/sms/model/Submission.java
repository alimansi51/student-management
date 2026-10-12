package com.sms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/** A student's answer to an assignment (a PDF file), optionally graded by the teacher. */
@Entity
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Assignment assignment;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User student;

    /** Typed answer; only used by submissions made before answers became PDF files */
    @Column(length = 2000)
    private String content;

    /** Name of the PDF on disk */
    @JsonIgnore
    private String fileName;

    /** Name of the PDF on the student's computer */
    private String originalFileName;

    private Long fileSize;

    private LocalDateTime submittedAt;

    private Integer grade;

    /** Temporary link to open the PDF, filled in when the submission is sent to the client */
    @Transient
    private String fileUrl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Assignment getAssignment() { return assignment; }
    public void setAssignment(Assignment assignment) { this.assignment = assignment; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
}
