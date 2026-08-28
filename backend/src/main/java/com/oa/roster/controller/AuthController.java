package com.oa.roster.controller;

import com.oa.roster.common.ApiResponse;
import com.oa.roster.service.AuthService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Data
    public static class LoginDTO {
        private String username;
        private String password;
    }

    /** 登录成功返回 token + 角色类型，前端据此驱动花名册的列渲染 */
    @PostMapping("/login")
    public ApiResponse<AuthService.LoginVO> login(@RequestBody LoginDTO dto) {
        return ApiResponse.ok(authService.login(dto.getUsername(), dto.getPassword()));
    }
}
