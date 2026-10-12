package com.sms.controller;

import com.sms.model.Material;
import com.sms.service.MaterialService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/materials")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping
    public List<Material> getAll() {
        return materialService.findAll();
    }

    @GetMapping("/{id}")
    public Material getOne(@PathVariable Long id) {
        return materialService.findById(id);
    }

    /** Multipart upload: a "material" part with the JSON details and a "file" part with the file itself. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Material create(@RequestPart("material") Material material,
                           @RequestPart("file") MultipartFile file) {
        return materialService.create(material, file);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Material update(@PathVariable Long id,
                           @RequestPart("material") Material material,
                           @RequestPart(value = "file", required = false) MultipartFile file) {
        return materialService.update(id, material, file);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        materialService.delete(id);
    }

    /** Opens or downloads the file. Uses a signed link instead of a login. */
    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable Long id, @RequestParam long expires, @RequestParam String sig) {
        return materialService.file(id, expires, sig);
    }
}
