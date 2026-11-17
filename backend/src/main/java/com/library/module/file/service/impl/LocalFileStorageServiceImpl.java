package com.library.module.file.service.impl;

import com.library.common.exception.BizException;
import com.library.module.file.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 本地磁盘存储实现。
 *
 * <p>校验分三层，缺一层都会被绕过或留下隐患：
 * <ol>
 *   <li><b>扩展名白名单</b>：只收 jpg/jpeg/png/webp，避免 .jsp/.svg（SVG 可内嵌脚本）落盘；</li>
 *   <li><b>大小上限</b>：默认 2MB，封面 640px 宽足够，超了就是误传原图；</li>
 *   <li><b>文件头魔数</b>：真正读前几个字节确认是图片，防止把任意文件改名成 .jpg 上传。</li>
 * </ol>
 * 文件名由服务端用 UUID 生成，完全不用原始文件名，从根上避免路径穿越（../../etc/passwd）。
 */
@Slf4j
@Service
public class LocalFileStorageServiceImpl implements FileStorageService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp");

    /** 封面 URL 前缀，与 WebMvcConfig 中的静态资源映射保持一致 */
    private static final String COVER_URL_PREFIX = "/uploads/covers/";

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${library.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${library.upload.cover-max-size:2097152}")
    private long coverMaxSize;

    @Override
    public String storeCover(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的图片");
        }
        if (file.getSize() > coverMaxSize) {
            throw new BizException("封面不能超过 " + (coverMaxSize / 1024 / 1024) + "MB");
        }
        String ext = resolveExtension(file.getOriginalFilename());
        byte[] head = readHead(file);
        if (!looksLikeImage(head, ext)) {
            throw new BizException("文件内容不是有效的图片，请上传 jpg / png / webp");
        }

        // 按天分目录：单目录文件过万时 ls / 备份都会变慢，顺带便于按时间归档
        String day = LocalDate.now().format(DAY);
        Path targetDir = Paths.get(uploadDir, "covers", day).toAbsolutePath().normalize();
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = targetDir.resolve(fileName);
        try {
            Files.createDirectories(targetDir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("封面保存失败 dir={}", targetDir, e);
            throw new BizException("图片保存失败，请稍后重试");
        }
        log.info("封面上传成功 {} ({} KB)", target, file.getSize() / 1024);
        return COVER_URL_PREFIX + day + "/" + fileName;
    }

    /** 取扩展名；不认识的一律拒绝，不做「猜类型」 */
    private String resolveExtension(String originalName) {
        String ext = StringUtils.getFilenameExtension(originalName);
        if (ext == null) {
            throw new BizException("图片缺少扩展名，请上传 jpg / png / webp");
        }
        ext = ext.toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BizException("只支持 jpg / png / webp 格式的图片");
        }
        return "jpeg".equals(ext) ? "jpg" : ext;
    }

    private byte[] readHead(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] head = new byte[12];
            int read = in.readNBytes(head, 0, head.length);
            return read == head.length ? head : java.util.Arrays.copyOf(head, Math.max(read, 0));
        } catch (IOException e) {
            throw new BizException("图片读取失败");
        }
    }

    /** 魔数判断：JPEG(FF D8 FF) / PNG(89 50 4E 47) / WEBP(RIFF....WEBP) */
    private boolean looksLikeImage(byte[] head, String ext) {
        if (head.length < 4) {
            return false;
        }
        boolean jpeg = (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF;
        boolean png = (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G';
        boolean webp = head.length >= 12 && head[0] == 'R' && head[1] == 'I'
                && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
        return switch (ext) {
            case "jpg" -> jpeg;
            case "png" -> png;
            case "webp" -> webp;
            default -> false;
        };
    }
}
