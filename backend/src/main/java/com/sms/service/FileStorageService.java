package com.sms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

/**
 * Saves uploaded files (videos, materials, submission PDFs) in folders on disk and creates the links to open them.
 *
 * A browser's video player or a plain link can't send the login header, so file links are signed instead:
 * e.g. /api/videos/5/stream?expires=...&sig=... only works for that exact address and for a few hours.
 */
@Service
public class FileStorageService {

    private static final Duration LINK_LIFETIME = Duration.ofHours(6);

    /** Types that are safe to show inside the browser; everything else is downloaded. */
    private static final Set<String> SHOW_IN_BROWSER = Set.of(
            "application/pdf", "image/png", "image/jpeg", "image/gif", "image/webp");

    private final Path root;
    private final byte[] signingKey = new byte[32];

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) throws IOException {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
        new SecureRandom().nextBytes(signingKey);
    }

    /** Saves the file in the given folder (e.g. "videos") under a random name and returns that name. */
    public String store(String folder, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please choose a file");
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String name = UUID.randomUUID() + (extension != null ? "." + extension.toLowerCase() : "");
        try {
            Path dir = root.resolve(folder);
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(name));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the file");
        }
        return name;
    }

    public void delete(String folder, String name) {
        if (name == null) {
            return;
        }
        try {
            Files.deleteIfExists(root.resolve(folder).resolve(name));
        } catch (IOException ignored) {
            // the database row is already gone; a leftover file is harmless
        }
    }

    /** Sends a stored file. Videos, PDFs and pictures open in the browser; other files are downloaded. */
    public ResponseEntity<Resource> serve(String folder, String name, String contentType, String originalName) {
        return serve(folder, name, contentType, originalName, false);
    }

    /** Sends a stored file so the browser always saves it, whatever its type. */
    public ResponseEntity<Resource> serveAsDownload(String folder, String name, String originalName) {
        return serve(folder, name, null, originalName, true);
    }

    private ResponseEntity<Resource> serve(String folder, String name, String contentType, String originalName,
                                           boolean forceDownload) {
        Path dir = root.resolve(folder).normalize();
        Path path = dir.resolve(name).normalize();
        if (!path.startsWith(dir) || !Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        MediaType type = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
        boolean inline = !forceDownload
                && (type.getType().equals("video") || SHOW_IN_BROWSER.contains(type.getType() + "/" + type.getSubtype()));
        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(originalName != null ? originalName : name, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(inline ? type : MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new FileSystemResource(path));
    }

    /** Adds a signature to an address so it works without a login for a few hours. */
    public String signedLink(String path) {
        long expires = Instant.now().plus(LINK_LIFETIME).getEpochSecond();
        return path + "?expires=" + expires + "&sig=" + sign(path, expires);
    }

    /** Rejects the request unless the signature matches this address and hasn't expired. */
    public void checkLink(String path, long expires, String sig) {
        boolean valid = Instant.now().getEpochSecond() <= expires
                && MessageDigest.isEqual(sign(path, expires).getBytes(StandardCharsets.UTF_8),
                                         sig.getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This link has expired, please reload the page");
        }
    }

    public static boolean isPdf(MultipartFile file) {
        String name = file.getOriginalFilename();
        return "application/pdf".equals(file.getContentType())
                || (name != null && name.toLowerCase().endsWith(".pdf"));
    }

    private String sign(String path, long expires) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            byte[] hash = mac.doFinal((path + ":" + expires).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
