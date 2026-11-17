package com.library.module.auth.controller;

import com.library.common.exception.BizException;
import com.library.common.exception.GlobalExceptionHandler;
import com.library.module.auth.dto.LoginDTO;
import com.library.module.auth.dto.LoginVO;
import com.library.module.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证接口契约测试（standalone MockMvc，不启动 Spring 容器、不连数据库）。
 * <p>
 * 重点校验：统一响应结构 {code,message,data,timestamp}、参数校验拦截、业务异常转换。
 * 这类测试在 CI 上跑得很快，可作为「接口格式被改坏」的护栏。
 */
@DisplayName("AuthController 接口契约")
class AuthControllerTest {

    private MockMvc mockMvc;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = Mockito.mock(AuthService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("登录成功：返回 code=200 与 token，且响应含统一字段")
    void loginSuccess_shouldReturnToken() throws Exception {
        given(authService.login(any(), any())).willReturn(new LoginVO("jwt-token-value", 1L, "admin", "admin"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"admin123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.token").value("jwt-token-value"))
                .andExpect(jsonPath("$.data.role").value("admin"))
                .andExpect(jsonPath("$.timestamp").isNumber());
    }

    @Test
    @DisplayName("用户名为空：被参数校验拦截，返回 code=400 且不进入 Service")
    void loginWithBlankUsername_shouldFailValidation() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","password":"admin123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verify(authService, never()).login(any(), any());
    }

    @Test
    @DisplayName("密码错误：Service 抛业务异常，转换为 code=400 + 原始文案")
    void loginWithWrongPassword_shouldReturnBizMessage() throws Exception {
        given(authService.login(any(), any())).willThrow(new BizException("用户名或密码错误"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"wrong"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("注册：用户名过短被校验拦截，不会写入用户")
    void registerWithTooShortUsername_shouldFailValidation() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"a","password":"123456"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("登录请求体字段能正确绑定到 DTO")
    void loginShouldBindRequestFields() throws Exception {
        given(authService.login(any(), any())).willReturn(new LoginVO("t", 1L, "reader", "user"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"  reader  ","password":"123456"}
                        """));

        ArgumentCaptor<LoginDTO> captor = ArgumentCaptor.forClass(LoginDTO.class);
        verify(authService).login(captor.capture(), any());
        // Controller 不做 trim，trim 在 Service 层完成；此处断言原始值已到达服务层
        assertThat(captor.getValue().getPassword()).isEqualTo("123456");
        assertThat(captor.getValue().getUsername()).contains("reader");
    }

    @Test
    @DisplayName("未登录访问 /api/auth/me：Service 抛 401 业务异常，HTTP 状态与 body.code 均为 401")
    void meWithoutLogin_shouldReturn401() throws Exception {
        given(authService.currentUser()).willThrow(new BizException(401, "未登录"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("修改密码：新密码过短被参数校验拦截，不会进入 Service")
    void changePasswordWithShortNewPassword_shouldFailValidation() throws Exception {
        mockMvc.perform(post("/api/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"oldPassword":"admin123","newPassword":"123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verify(authService, never()).changePassword(any());
    }

    @Test
    @DisplayName("修改密码：原密码错误由 Service 抛业务异常，返回 code=400")
    void changePasswordWithWrongOldPassword_shouldReturn400() throws Exception {
        Mockito.doThrow(new BizException("原密码不正确"))
                .when(authService).changePassword(any());

        mockMvc.perform(post("/api/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"oldPassword":"wrongOld","newPassword":"newPass123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("原密码不正确"));
    }

    @Test
    @DisplayName("登录被锁定：HTTP 429 与 body.code 429 保持一致")
    void loginLocked_shouldReturn429() throws Exception {
        given(authService.login(any(), any())).willThrow(
                new BizException(429, "密码错误次数过多，账号已临时锁定 15 分钟"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"admin123"}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(429));
    }
}
