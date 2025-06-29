package com.sonnk.product.configurations;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.sonnk.product.objects.properties.CloudinaryProperties;
import com.sonnk.product.utils.img.CloudinaryUtil;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CloudinaryProperties.class)
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary(CloudinaryProperties props) {
        Cloudinary instance = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", props.getCloudName(),
                "api_key", props.getApiKey(),
                "api_secret", props.getApiSecret()
        ));

        // Gán vào class util static
        CloudinaryUtil.init(instance);

        return instance;
    }
}
