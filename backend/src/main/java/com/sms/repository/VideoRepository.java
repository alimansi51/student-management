package com.sms.repository;

import com.sms.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface VideoRepository extends JpaRepository<Video, Long> {
    List<Video> findByCourseIdIn(Collection<Long> courseIds);
}
