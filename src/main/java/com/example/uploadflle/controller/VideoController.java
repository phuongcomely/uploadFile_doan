package com.example.uploadflle.controller;

import com.example.uploadflle.enity.Video;
import com.example.uploadflle.repository.VideoRepository;
import com.example.uploadflle.request.UpsertVideoRequest;
import com.example.uploadflle.service.UploadVideoService;
import com.example.uploadflle.service.VideoService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;

@Controller
public class VideoController {
    private static final String UPLOAD_DIR = "public/video_uploads/";
    private static final String FFMPG_PATH = "/usr/local/bin/ffmpeg"; // Đường dẫn đến ffmpeg
    private static final int VIDEO_WIDTH = 1280; // Chiều rộng cố định
    private static final int VIDEO_HEIGHT = 720; // Chiều cao cố định
    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    private final VideoRepository videoRepository;
    private final UploadVideoService uploadVideoService;
    private final VideoService videoService;
    private final Path videoLocation = Paths.get("public/video_uploads/");
    public VideoController(VideoRepository videoRepository, UploadVideoService uploadVideoService, VideoService videoService) {
        this.videoRepository = videoRepository;
        this.uploadVideoService = uploadVideoService;
        this.videoService = videoService;
    }


    @GetMapping("/videos/{filename}")
    public ResponseEntity<Resource> getVideo(@PathVariable String filename) {
        try {
            Path filePath = videoLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("Could not read the file!");
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not read the file!", e);
        }
    }
    @GetMapping("admin/video")
    public String getVideo(Model model){
        List<Video> listVideo = videoService.getAllVideo();

        model.addAttribute("listVideo",listVideo );
        return "admin/video/index";
    }
    @GetMapping("admin/video/{id}/detail")
    public String getVideoDetail(Model model, @PathVariable Integer id){
        Video video = videoService.getVideoById(id);
        List<Video> listVideo = videoService.getAllVideo();

        model.addAttribute("video", video);
        model.addAttribute("listVideo", listVideo);
        return "admin/video/detail";
    }
    @GetMapping("admin/video/create")
    public String getCreateVideo(Model model){


        model.addAttribute("request", new UpsertVideoRequest());
        return "admin/video/create";
    }
    @PostMapping("/admin/video/create")
    public String createVideo(@Valid @ModelAttribute("request") UpsertVideoRequest request, BindingResult result, Model model) {
        if (request.getVideoUrl() == null || request.getVideoUrl().isEmpty()) {
            result.addError(new FieldError("request", "videoUrl", "File video không được để trống!"));
        }

        if (result.hasErrors()) {
            return "admin/video/create";
        }

        MultipartFile video = request.getVideoUrl();
        String storageFileName = UUID.randomUUID().toString() + "_" + video.getOriginalFilename();  // Create a unique file name

        try {
            String uploadDir = "public/video_uploads/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            try (InputStream inputStream = video.getInputStream()) {
                Files.copy(inputStream, uploadPath.resolve(storageFileName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            result.addError(new FieldError("request", "videoUrl", "Có lỗi xảy ra khi tải tệp lên!"));
            model.addAttribute("errorMessage", "Có lỗi xảy ra khi tải tệp lên!");
            return "admin/video/create";
        }

        Video videoEntity = new Video();
        videoEntity.setFileName(request.getFileName());
        videoEntity.setDescription(request.getDescription());
        videoEntity.setVideoUrl(storageFileName);

        try {
            videoRepository.save(videoEntity);
        } catch (Exception e) {
            result.addError(new FieldError("request", "videoUrl", "Có lỗi xảy ra khi lưu thông tin video!"));
            model.addAttribute("errorMessage", "Có lỗi xảy ra khi lưu thông tin video!");
            return "admin/video/create";
        }

        return "redirect:/admin/video";
    }

    @PostMapping("admin/video/{id}/update")
    public ResponseEntity<?> updateVideo(@PathVariable int id,
                                         @RequestParam("title") String title,
                                         @RequestParam("description") String description,
                                         @RequestParam(value = "videoUrl", required = false) MultipartFile videoFile) {
        try {
            videoService.updateVideo(id, title, description, videoFile);
            return ResponseEntity.ok().body("{\"success\": true}");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"success\": false, \"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @DeleteMapping("admin/video/{id}/delete")
    public ResponseEntity<?> deleteVideo(@PathVariable int id) {
        try {
            videoService.deleteVideo(id);
            return ResponseEntity.ok().body("{\"success\": true}");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"success\": false, \"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
