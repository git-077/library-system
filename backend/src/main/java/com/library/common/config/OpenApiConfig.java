package com.library.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档配置（springdoc-openapi）。
 * <p>
 * 访问 <a href="http://localhost:8080/swagger-ui.html">/swagger-ui.html</a> 查看，
 * 右上角 Authorize 填入登录接口返回的 token（不需要 "Bearer " 前缀）即可调试需要鉴权的接口。
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "BearerAuth";

    @Bean
    public OpenAPI libraryOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("图书管理系统 API")
                        .description("Spring Boot 3 + MyBatis-Plus 实现的图书管理系统后端接口。"
                                + "统一响应结构：{code, message, data, timestamp}，"
                                + "code=200 表示成功，其余为业务错误码。")
                        .version("v1.0.0")
                        .contact(new Contact().name("Library Team"))
                        .license(new License().name("MIT")))
                // 全局声明 Bearer 鉴权：所有接口默认要求 Authorization: Bearer <token>
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("登录接口返回的 token")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
    }
}
