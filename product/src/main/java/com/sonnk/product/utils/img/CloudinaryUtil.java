package com.sonnk.product.utils.img;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class CloudinaryUtil {
    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @PostConstruct
    public void init() {
        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
        // Gán vào biến static nếu cần dùng ở static method
        CloudinaryUtil.instance = cloudinary;
        log.info("Cloudinary initialized with cloudName={}", cloudName);
    }

    private static Cloudinary instance;

    public static Cloudinary getInstance() {
        return instance;
    }

    private static final Set<String> ALLOWED_TYPES = Set.of("image/png", "image/jpeg", "image/jpg", "image/webp");

    public static Optional<String> uploadImage(MultipartFile file) {
        if (file.isEmpty()) {
            log.warn("Upload failed: file is empty");
            return Optional.empty();
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            log.error("Unsupported file type: {}", contentType);
            throw new IllegalArgumentException("Unsupported file type: " + contentType);
        }

        try {
            log.info("Uploading image to Cloudinary: {}", contentType);
            Map<?, ?> uploadResult = instance.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            String url = (String) uploadResult.get("secure_url");
            log.info("Upload success: {}", url);
            return Optional.ofNullable(url);
        } catch (IOException e) {
            log.error("IOException while uploading to Cloudinary", e);
            throw new RuntimeException("Failed to upload image to Cloudinary", e);
        }
    }
}
