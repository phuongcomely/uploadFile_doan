package com.example.uploadflle.service;

import com.example.uploadflle.enity.User;
import com.example.uploadflle.exception.BadRequestException;
import com.example.uploadflle.exception.ResourceNotFoundException;
import com.example.uploadflle.repository.UserRepository;
import com.example.uploadflle.request.LoginRequest;
import com.example.uploadflle.request.RegisterRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final HttpSession session;

    public void login(LoginRequest request) {
        // Tìm kiếm user theo email
        // Nếu không tìm thấy thì throw exception
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Email hoặc mật khẩu không đúng"));

        // Nếu tìm thấy thì kiểm tra password
        // Nếu password không đúng thì throw exception
        if (!bCryptPasswordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Email hoặc mật khẩu không đúng");
        }

        // Lưu thông tin vào trong session
        session.setAttribute("currentUser", user);
    }

    // Về nhà làm
    public void register(RegisterRequest request) {
        // Kiểm tra xem email đã tồn tại chưa
        // Nếu tồn tại rồi thì throw exception
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email đã tồn tại");
        }

        // Kiểm tra xem password có trùng với confirm password không
        // Nếu không trùng thì throw exception
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu không trùng khớp");
        }

        // Mã hóa password
        String encodedPassword = bCryptPasswordEncoder.encode(request.getPassword());

        // Tạo user
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(encodedPassword)
                .build();

        // Lưu user vào database
        userRepository.save(user);
    }

    public void logout() {

        session.setAttribute("currentUser", null);
    }
}
