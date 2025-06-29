package com.sonnk.product.utils.img;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class CloudinaryUtil {
    private static Cloudinary cloudinary;

    public static void init(Cloudinary instance) {
        cloudinary = instance;
    }

    private static final Set<String> ALLOWED_TYPES = Set.of("image/png", "image/jpeg", "image/jpg", "image/webp");

    public static Optional<String> uploadImage(MultipartFile file) {
        if (file.isEmpty()) {
            return Optional.empty();
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported file type: " + contentType);
        }

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            String url = (String) uploadResult.get("secure_url");
            return Optional.ofNullable(url);
        } catch (IOException e) {
            // Có thể log lỗi tại đây
            throw new RuntimeException("Failed to upload image to Cloudinary", e);
        }
    }
}
