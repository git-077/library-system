package com.library.module.file.dto;

/**
 * 上传结果：url 直接写进 book.cover_url 即可
 */
public record UploadVO(String url, long size, String originalName) {
}
