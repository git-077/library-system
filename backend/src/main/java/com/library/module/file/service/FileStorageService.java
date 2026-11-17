package com.library.module.file.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 本地文件存储：把上传的图片落到磁盘，返回站内可访问的相对路径。
 *
 * <p>项目起步阶段用本地目录（{@code library.upload.dir}，默认 {@code ./uploads}），
 * 挂载到 {@code /uploads/**} 静态资源。将来换对象存储只需要换一个实现类，
 * 接口签名（返回 URL 字符串）不变，业务侧与前端都不用改。
 */
public interface FileStorageService {

    /**
     * 保存封面图。
     *
     * @return 可直接写进 {@code book.cover_url} 的相对地址，形如 {@code /uploads/covers/20261116/xxx.jpg}
     */
    String storeCover(MultipartFile file);
}
