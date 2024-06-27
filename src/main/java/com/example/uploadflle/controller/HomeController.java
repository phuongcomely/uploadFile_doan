package com.example.uploadflle.controller;

import com.example.uploadflle.enity.Image;
import com.example.uploadflle.enity.User;
import com.example.uploadflle.enity.Video;
import com.example.uploadflle.exception.ResourceNotFoundException;
import com.example.uploadflle.repository.ImageRepository;
import com.example.uploadflle.repository.VideoRepository;
import com.example.uploadflle.request.UpsertImageRequest;
import com.example.uploadflle.request.UpsertVideoRequest;
import com.example.uploadflle.service.ImageService;
import com.example.uploadflle.service.UploadVideoService;
import com.example.uploadflle.service.VideoService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class HomeController {

    private final ImageService imageService;
    private final ImageRepository imageRepository;
    private final VideoRepository videoRepository;
    private final HttpSession session;
    private final UploadVideoService uploadVideoService;

    private static final String UPLOAD_DIR = "public/image_uploads/";
    private static final int IMAGE_WIDTH = 200; // Chiều rộng cố định
    private static final int IMAGE_HEIGHT = 200; // Chiều cao cố định
    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);
    @GetMapping("/image")
    public String getHomePage(Model model){
        List<Image> listImage = imageService.getAllImage();

        model.addAttribute("listImage", listImage);
            return "admin/image/index";
    }
    @GetMapping("/image/create")
    public String showCreateImage(Model model){
        UpsertImageRequest request = new UpsertImageRequest();

        model.addAttribute("request", request);
        return "admin/image/create";
    }
    @PostMapping("/image/create")
    public String getCreateImage(@Valid @ModelAttribute("request") UpsertImageRequest request, BindingResult result, Model model) {
        // Kiểm tra nếu URL ảnh bị trống
        if (request.getImageUrl() == null || request.getImageUrl().isEmpty()) {
            result.addError(new FieldError("request", "imageUrl", "File ảnh không được để trống!"));
        }

        // Nếu có lỗi xác thực, trả về form
        if (result.hasErrors()) {
            model.addAttribute("request", request);
            return "admin/image/create";
        }

        // Lưu file ảnh
        MultipartFile image = request.getImageUrl();
        String storageFileName = image.getOriginalFilename();
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            try (InputStream inputStream = image.getInputStream()) {
                Path filePath = uploadPath.resolve(storageFileName);
                // Sử dụng Thumbnailator để thay đổi kích thước ảnh và lưu lại
                Thumbnails.of(inputStream)
                        .size(IMAGE_WIDTH, IMAGE_HEIGHT)
                        .toFile(filePath.toFile());
            }
        } catch (Exception e) {
            // Sử dụng logger để xử lý ngoại lệ
            logger.error("Lỗi khi lưu file ảnh: " + e.getMessage(), e);
        }

        // Lưu dữ liệu ảnh vào repository
        Image imageEntity = new Image();
        imageEntity.setTitle(request.getTitle());
        imageEntity.setDescription(request.getDescription());
        imageEntity.setCategory(request.getCategory());
        imageEntity.setImageUrl(storageFileName);
        imageRepository.save(imageEntity);

        return "redirect:/admin/image";
    }

    @GetMapping("/image/{id}/detail")
    public String getDetailImage(@PathVariable Integer id, Model model) {
        Image image1 = imageService.getImageById(id);
        model.addAttribute("image1", image1);

        Image image = new Image();
        image.setTitle(image1.getTitle());
        image.setDescription(image1.getDescription());
        image.setCategory(image1.getCategory());
        image.setImageUrl(image1.getImageUrl()); // Thêm dòng này để hiển thị URL hình ảnh hiện tại

        model.addAttribute("image", image);

        return "admin/image/detail";
    }
    @PostMapping("/image/{id}/detail")
    public String updateImage(@PathVariable Integer id, @ModelAttribute("image") UpsertImageRequest request, @RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin ảnh với id = " + id));

        // Cập nhật các trường thông tin
        image.setTitle(request.getTitle());
        image.setDescription(request.getDescription());
        image.setCategory(request.getCategory());

        // Kiểm tra và xử lý file ảnh tải lên
        if (file != null && !file.isEmpty()) {
            String fileName = file.getOriginalFilename();
            try {
                // Lưu file vào hệ thống
                Path path = Paths.get("image_uploads/" + fileName);
                Files.createDirectories(path.getParent()); // Tạo thư mục nếu chưa tồn tại
                Files.write(path, file.getBytes());
                image.setImageUrl(fileName); // Cập nhật URL hình ảnh mới
            } catch (IOException e) {
                throw new RuntimeException("Lỗi khi lưu tệp ảnh", e);
            }
        }

        imageRepository.save(image);
        redirectAttributes.addFlashAttribute("message", "Cập nhật thông tin thành công!");
        return "redirect:/admin/image";
    }
    @DeleteMapping("/image/{id}/detail")
    public ResponseEntity<?> deleteImage(@PathVariable Integer id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin ảnh với id = " + id));

        // Xóa hình ảnh từ hệ thống lưu trữ
        String imageUrl = image.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                Path imagePath = Paths.get("image_uploads/" + imageUrl);
                Files.deleteIfExists(imagePath); // Xóa file ảnh từ hệ thống
            } catch (IOException e) {
                throw new RuntimeException("Lỗi khi xóa tệp ảnh", e);
            }
        }

        // Xóa hình ảnh từ cơ sở dữ liệu
        imageRepository.delete(image);

        return ResponseEntity.ok().body("Xóa hình ảnh thành công!");
    }



//    @PostMapping("/video/create")
//    public String createVideo(@Valid @ModelAttribute("request") UpsertVideoRequest request, BindingResult result, Model model) {
//        if (request.getVideoUrl() == null || request.getVideoUrl().isEmpty()) {
//            result.addError(new FieldError("request", "videoUrl", "File video không được để trống!"));
//        }
//
//        if (result.hasErrors()) {
//            return "admin/video/create";
//        }
//
//        MultipartFile video = request.getVideoUrl();
//        String storageFileName = UUID.randomUUID().toString() + "_" + video.getOriginalFilename();  // Create a unique file name
//
//        try {
//            String uploadDir = "public/video_uploads/";
//            Path uploadPath = Paths.get(uploadDir);
//            if (!Files.exists(uploadPath)) {
//                Files.createDirectories(uploadPath);
//            }
//
//            try (InputStream inputStream = video.getInputStream()) {
//                Files.copy(inputStream, uploadPath.resolve(storageFileName), StandardCopyOption.REPLACE_EXISTING);
//            }
//        } catch (IOException e) {
//            result.addError(new FieldError("request", "videoUrl", "Có lỗi xảy ra khi tải tệp lên!"));
//            model.addAttribute("errorMessage", "Có lỗi xảy ra khi tải tệp lên!");
//            return "admin/video/create";
//        }
//
//        Video videoEntity = new Video();
//        videoEntity.setFileName(request.getFileName());
//        videoEntity.setDescription(request.getDescription());
//        videoEntity.setVideoUrl(storageFileName);
//
//        try {
//            videoRepository.save(videoEntity);
//        } catch (Exception e) {
//            result.addError(new FieldError("request", "videoUrl", "Có lỗi xảy ra khi lưu thông tin video!"));
//            model.addAttribute("errorMessage", "Có lỗi xảy ra khi lưu thông tin video!");
//            return "admin/video/create";
//        }
//
//        return "redirect:/admin/video";
//    }



}
