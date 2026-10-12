package com.sms.controller;

import com.sms.model.Submission;
import com.sms.service.SubmissionService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Teacher endpoints for submissions. Students hand in their PDFs through /api/student/submissions. */
@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @GetMapping
    public List<Submission> getAll() {
        return submissionService.findAll();
    }

    @GetMapping("/{id}")
    public Submission getOne(@PathVariable Long id) {
        return submissionService.findById(id);
    }

    /** Body: { "grade": 85 } or { "grade": null } to clear it */
    @PutMapping("/{id}/grade")
    public Submission grade(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        return submissionService.grade(id, body.get("grade"));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        submissionService.delete(id);
    }

    /** Opens the submitted PDF. Uses a signed link instead of a login. */
    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable Long id, @RequestParam long expires, @RequestParam String sig) {
        return submissionService.file(id, expires, sig);
    }
}
