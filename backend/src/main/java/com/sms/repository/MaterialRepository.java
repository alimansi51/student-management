package com.sms.repository;

import com.sms.model.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findByCourseIdIn(Collection<Long> courseIds);
}
