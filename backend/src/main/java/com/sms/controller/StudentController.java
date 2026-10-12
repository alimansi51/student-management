package com.sms.controller;

import com.sms.dto.StudentViews.*;
import com.sms.service.StudentPortalService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

/** Endpoints for the logged-in student (only reachable with the STUDENT role). */
@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final StudentPortalService portal;

    public StudentController(StudentPortalService portal) {
        this.portal = portal;
    }

    @GetMapping("/me")
    public Profile me(Principal principal) {
        return portal.profile(principal.getName());
    }

    @GetMapping("/courses")
    public List<CourseView> courses(Principal principal) {
        return portal.courses(principal.getName());
    }

    @GetMapping("/videos")
    public List<VideoView> videos(Principal principal) {
        return portal.videos(principal.getName());
    }

    @GetMapping("/materials")
    public List<MaterialView> materials(Principal principal) {
        return portal.materials(principal.getName());
    }

    @GetMapping("/assignments")
    public List<AssignmentView> assignments(Principal principal) {
        return portal.assignments(principal.getName());
    }

    /** Multipart form: "assignmentId" and "file" (a PDF). */
    @PostMapping(value = "/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AssignmentView submit(Principal principal,
                                 @RequestParam Long assignmentId,
                                 @RequestParam("file") MultipartFile file) {
        return portal.submit(principal.getName(), assignmentId, file);
    }

    @PutMapping("/password")
    public void changePassword(Principal principal, @RequestBody ChangePasswordRequest request) {
        portal.changePassword(principal.getName(), request);
    }
}
