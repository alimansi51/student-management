package com.sms.repository;

import com.sms.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByCourseIdIn(Collection<Long> courseIds);
}
