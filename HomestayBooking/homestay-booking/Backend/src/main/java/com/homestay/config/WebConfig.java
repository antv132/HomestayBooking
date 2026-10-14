package com.homestay.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Chuẩn hóa thành đường dẫn tuyệt đối: đường dẫn tương đối ("uploads/")
        // phụ thuộc CWD lúc chạy app — nếu CWD sai sẽ trỏ nhầm thư mục và lỗi 500.
        Path absolute = Paths.get(uploadDir).toAbsolutePath().normalize();
        String location = absolute.toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
