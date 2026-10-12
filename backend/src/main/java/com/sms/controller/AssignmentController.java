package com.sms.controller;

import com.sms.model.Assignment;
import com.sms.service.AssignmentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public List<Assignment> getAll() {
        return assignmentService.findAll();
    }

    @GetMapping("/{id}")
    public Assignment getOne(@PathVariable Long id) {
        return assignmentService.findById(id);
    }

    @PostMapping
    public Assignment create(@RequestBody Assignment assignment) {
        return assignmentService.create(assignment);
    }

    @PutMapping("/{id}")
    public Assignment update(@PathVariable Long id, @RequestBody Assignment assignment) {
        return assignmentService.update(id, assignment);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        assignmentService.delete(id);
    }
}
