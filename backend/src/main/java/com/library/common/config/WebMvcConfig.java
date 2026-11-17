package com.library.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 把上传目录挂到 {@code /uploads/**} 上，让上传的封面能被浏览器直接访问。
 *
 * <p>用 file: 协议指向绝对路径，而不是 classpath —— 图片是运行期数据，
 * 打包进 jar 会导致重启即丢失。生产环境建议把该目录挂载到宿主机卷，
 * 或直接换成对象存储（只需替换 {@code FileStorageService} 的实现）。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${library.upload.dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        String location = dir.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                // 封面文件名带 UUID，内容不会变，可以放心缓存一天
                .setCachePeriod(86400);
    }
}
