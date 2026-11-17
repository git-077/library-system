package com.library.module.file.controller;

import com.library.common.log.Audit;
import com.library.common.result.Result;
import com.library.module.file.dto.UploadVO;
import com.library.module.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传接口（仅管理员）。
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    /** 上传图书封面，返回站内相对地址 */
    @PostMapping("/cover")
    @PreAuthorize("hasRole('ADMIN')")
    @Audit(module = "图书", action = "上传封面")
    public Result<UploadVO> uploadCover(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.storeCover(file);
        return Result.ok(new UploadVO(url, file.getSize(), file.getOriginalFilename()));
    }
}
