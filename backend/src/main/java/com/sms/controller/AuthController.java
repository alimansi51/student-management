package com.sms.controller;

import com.sms.model.User;
import com.sms.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** The frontend calls this at login to check the credentials and find out who is logging in. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return Map.of("role", "ADMIN", "name", "Administrator", "username", auth.getName(),
                    "mustChangePassword", false);
        }
        User student = userService.findByUsername(auth.getName());
        return Map.of("role", "STUDENT", "name", student.getFullName(), "username", student.getUsername(),
                "mustChangePassword", userService.mustChangePassword(student));
    }
}
