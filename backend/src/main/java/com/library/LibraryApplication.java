package com.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 图书管理系统启动类
 * Mapper 通过 @Mapper 注解逐个声明，无需 @MapperScan 全量扫描
 * @EnableScheduling 开启定时任务（逾期扫描，见 module.borrow.task）
 */
@EnableScheduling
@SpringBootApplication
public class LibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);
    }
}
