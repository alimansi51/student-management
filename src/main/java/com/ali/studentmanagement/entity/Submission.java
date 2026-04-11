package com.ali.studentmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="submissions")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Submission {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private long id ;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name="grade")
    private Double grade ;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

}
