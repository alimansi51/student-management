package com.ali.studentmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "courses")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Builder.Default
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Assignment> assignments = new ArrayList<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "course_enrollments",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "student_id"})
    )
    private List<User> enrolledStudents = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Video> videos = new ArrayList<>();

    public void addAssignment(Assignment assignment) {
        assignments.add(assignment);
        assignment.setCourse(this);
    }

    public void addVideo(Video video) {
        videos.add(video);
        video.setCourse(this);
    }

    public void enrollStudent(User student) {
        if (!enrolledStudents.contains(student)) {
            enrolledStudents.add(student);
            student.getEnrolledCourses().add(this);
        }
    }
}