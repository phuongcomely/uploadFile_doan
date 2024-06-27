package com.example.uploadflle.repository;

import com.example.uploadflle.enity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
    List<Review> findByImage_Id(Integer imageId);
}