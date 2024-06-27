package com.example.uploadflle.resouce;

import com.example.uploadflle.enity.Video;
import com.example.uploadflle.request.UpsertVideoRequest;
import com.example.uploadflle.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/video")
@RequiredArgsConstructor
public class VideoResouce {
    private final VideoService videoService;

//    @PostMapping("/{id}/upload-video")
//    public ResponseEntity<?> uploadVideo(@RequestParam("video") MultipartFile file, @PathVariable Integer id) {
//        videoService.uploadVideo(id, file);
//        return ResponseEntity.ok().build(); // status code 200
//    }
//    @PostMapping
//    public ResponseEntity<?> createVideo(@RequestBody UpsertVideoRequest request) {
//       Video video = videoService.createVideo(request);
//        return ResponseEntity.ok(video); // status code 200
//    }
//    // Cập nhật review - PUT
//    @PutMapping("/{id}")
//    public ResponseEntity<?> updateVideo(@RequestBody UpsertVideoRequest request, @PathVariable Integer id) {
//       Video video= videoService.updateVideo(id, request);
//        return ResponseEntity.ok(video); // status code 200
//    }
//
//    // Xóa review - DELETE
//    @DeleteMapping("/{id}")
//    public ResponseEntity<?> deleteVideo(@PathVariable Integer id) {
//        videoService.deleteVideo(id);
//        return ResponseEntity.noContent().build(); // status code 204
//    }
}
