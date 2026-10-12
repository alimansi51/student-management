package com.sms.service;

import com.sms.model.Course;
import com.sms.model.User;
import com.sms.model.Submission;
import com.sms.repository.CourseRepository;
import com.sms.repository.SubmissionRepository;
import com.sms.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    // no look-alike characters (0/O, 1/l/I) so passwords are easy to read out
    private static final String PASSWORD_CHARS = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int PASSWORD_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;
    private final SubmissionRepository submissionRepository;
    private final FileStorageService storage;

    public UserService(UserRepository userRepository, CourseRepository courseRepository,
                       PasswordEncoder passwordEncoder, SubmissionRepository submissionRepository,
                       FileStorageService storage) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.passwordEncoder = passwordEncoder;
        this.submissionRepository = submissionRepository;
        this.storage = storage;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    /**
     * Creates the student with a generated username (firstname.lastname) and a random one-time password.
     * The student has to choose their own password the first time they log in.
     */
    public User create(User user) {
        requireNames(user);
        user.setId(null);
        user.setUsername(generateUsername(user.getFirstName(), user.getLastName()));
        String password = generatePassword();
        user.setPassword(passwordEncoder.encode(password));
        user.setMustChangePassword(true);
        User saved = userRepository.save(user);
        saved.setInitialPassword(password);
        return saved;
    }

    /** Updates the student's details. The username and password stay the same. */
    public User update(Long id, User changes) {
        requireNames(changes);
        User user = findById(id);
        user.setFirstName(changes.getFirstName());
        user.setLastName(changes.getLastName());
        user.setEmail(changes.getEmail());
        user.setPhone(changes.getPhone());
        return userRepository.save(user);
    }

    /** Un-enrolls the user from every course first; their submissions are removed by the database and their PDFs here. */
    @Transactional
    public void delete(Long id) {
        User user = findById(id);
        for (Course course : courseRepository.findByStudentsId(id)) {
            course.getStudents().removeIf(s -> s.getId().equals(id));
        }
        List<Submission> submissions = submissionRepository.findByStudentId(id);
        userRepository.delete(user);
        submissions.forEach(s -> storage.delete(SubmissionService.FOLDER, s.getFileName()));
    }

    /**
     * Admin action for a student who forgot their password. Gives them a new one-time password,
     * which they must change on their next login. Returns the new login details.
     */
    public Map<String, String> resetPassword(Long id) {
        User user = findById(id);
        String password = generatePassword();
        user.setPassword(passwordEncoder.encode(password));
        user.setMustChangePassword(true);
        userRepository.save(user);
        return Map.of("name", user.getFullName(), "username", user.getUsername(), "password", password);
    }

    public void changePassword(User user, String currentPassword, String newPassword) {
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is wrong");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be at least 6 characters");
        }
        if (newPassword.equals(currentPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be different from the current one");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    public boolean mustChangePassword(User user) {
        return Boolean.TRUE.equals(user.getMustChangePassword());
    }

    private void requireNames(User user) {
        if (isBlank(user.getFirstName()) || isBlank(user.getLastName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "First and last name are required");
        }
        user.setFirstName(user.getFirstName().trim());
        user.setLastName(user.getLastName().trim());
    }

    /** "John", "Smith" -> john.smith (john.smith2, john.smith3... if already taken) */
    private String generateUsername(String firstName, String lastName) {
        String base = clean(firstName) + "." + clean(lastName);
        String username = base;
        for (int n = 2; userRepository.existsByUsername(username); n++) {
            username = base + n;
        }
        return username;
    }

    /** Lowercase and drop spaces/symbols: "Mary Ann" -> "maryann" */
    private static String clean(String name) {
        return name.trim().toLowerCase().replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private static String generatePassword() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
