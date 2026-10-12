package com.sms.service;

import com.sms.model.Submission;
import com.sms.repository.SubmissionRepository;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** The teacher's side of submissions: see them and grade them. Students hand them in via StudentPortalService. */
@Service
public class SubmissionService {

    public static final String FOLDER = "submissions";

    private final SubmissionRepository submissionRepository;
    private final FileStorageService storage;

    public SubmissionService(SubmissionRepository submissionRepository, FileStorageService storage) {
        this.submissionRepository = submissionRepository;
        this.storage = storage;
    }

    public List<Submission> findAll() {
        return submissionRepository.findAll().stream().map(this::withFileUrl).toList();
    }

    public Submission findById(Long id) {
        return withFileUrl(submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found")));
    }

    /** Sets (or clears, with null) the grade. */
    public Submission grade(Long id, Integer grade) {
        Submission submission = findById(id);
        submission.setGrade(grade);
        return withFileUrl(submissionRepository.save(submission));
    }

    public void delete(Long id) {
        Submission submission = findById(id);
        submissionRepository.delete(submission);
        storage.delete(FOLDER, submission.getFileName());
    }

    /** Opens the PDF for a signed link (see FileStorageService). */
    public ResponseEntity<Resource> file(Long id, long expires, String sig) {
        storage.checkLink(filePath(id), expires, sig);
        Submission submission = findById(id);
        if (submission.getFileName() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This submission has no file");
        }
        return storage.serve(FOLDER, submission.getFileName(), "application/pdf", submission.getOriginalFileName());
    }

    public String fileUrl(Submission submission) {
        return submission.getFileName() == null ? null : storage.signedLink(filePath(submission.getId()));
    }

    private static String filePath(Long id) {
        return "/api/submissions/" + id + "/file";
    }

    private Submission withFileUrl(Submission submission) {
        submission.setFileUrl(fileUrl(submission));
        return submission;
    }
}
