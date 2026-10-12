package com.sms.controller;

import com.sms.model.Course;
import com.sms.service.CourseService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public List<Course> getAll() {
        return courseService.findAll();
    }

    @GetMapping("/{id}")
    public Course getOne(@PathVariable Long id) {
        return courseService.findById(id);
    }

    @PostMapping
    public Course create(@RequestBody Course course) {
        return courseService.create(course);
    }

    @PutMapping("/{id}")
    public Course update(@PathVariable Long id, @RequestBody Course course) {
        return courseService.update(id, course);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        courseService.delete(id);
    }
}
