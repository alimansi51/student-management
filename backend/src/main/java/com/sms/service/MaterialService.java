package com.sms.service;

import com.sms.model.Course;
import com.sms.model.Material;
import com.sms.repository.MaterialRepository;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MaterialService {

    public static final String FOLDER = "materials";

    private final MaterialRepository materialRepository;
    private final CourseService courseService;
    private final FileStorageService storage;

    public MaterialService(MaterialRepository materialRepository, CourseService courseService,
                           FileStorageService storage) {
        this.materialRepository = materialRepository;
        this.courseService = courseService;
        this.storage = storage;
    }

    public List<Material> findAll() {
        return materialRepository.findAll().stream().map(this::withFileUrl).toList();
    }

    public Material findById(Long id) {
        return withFileUrl(materialRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material not found")));
    }

    public Material create(Material material, MultipartFile file) {
        material.setId(null);
        material.setCourse(requireCourse(material));
        material.setUploadedAt(LocalDateTime.now());
        saveFile(material, file);
        return withFileUrl(materialRepository.save(material));
    }

    /** Updates the details. The file is only replaced if a new one was chosen. */
    public Material update(Long id, Material changes, MultipartFile file) {
        Material material = findById(id);
        material.setTitle(changes.getTitle());
        material.setDescription(changes.getDescription());
        material.setCourse(requireCourse(changes));

        String oldFile = null;
        if (file != null && !file.isEmpty()) {
            oldFile = material.getFileName();
            saveFile(material, file);
            material.setUploadedAt(LocalDateTime.now());
        }
        Material saved = materialRepository.save(material);
        storage.delete(FOLDER, oldFile);
        return withFileUrl(saved);
    }

    public void delete(Long id) {
        Material material = findById(id);
        materialRepository.delete(material);
        storage.delete(FOLDER, material.getFileName());
    }

    /** Downloads the file for a signed link (see FileStorageService). Materials are always saved, never opened in the tab. */
    public ResponseEntity<Resource> file(Long id, long expires, String sig) {
        storage.checkLink(filePath(id), expires, sig);
        Material material = findById(id);
        return storage.serveAsDownload(FOLDER, material.getFileName(), material.getOriginalFileName());
    }

    public String fileUrl(Material material) {
        return material.getFileName() == null ? null : storage.signedLink(filePath(material.getId()));
    }

    private static String filePath(Long id) {
        return "/api/materials/" + id + "/file";
    }

    private void saveFile(Material material, MultipartFile file) {
        material.setFileName(storage.store(FOLDER, file));
        material.setOriginalFileName(file.getOriginalFilename());
        material.setContentType(file.getContentType());
        material.setFileSize(file.getSize());
    }

    private Course requireCourse(Material material) {
        if (material.getCourse() == null || material.getCourse().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course is required");
        }
        return courseService.findById(material.getCourse().getId());
    }

    private Material withFileUrl(Material material) {
        material.setFileUrl(fileUrl(material));
        return material;
    }
}
