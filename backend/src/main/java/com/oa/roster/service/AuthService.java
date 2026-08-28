package com.oa.roster.service;

import com.oa.roster.common.BizException;
import com.oa.roster.entity.SysUser;
import com.oa.roster.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录与 token 管理。
 *
 * 面试要点：
 * 1. token 存内存 Map 仅供单机演示，生产应放 Redis（支持过期时间、多实例共享、踢人下线）；
 * 2. 密码演示用 MD5，生产必须 BCrypt（自带盐、慢哈希抗撞库）。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserRepository userRepository;

    /** token -> userId */
    private final Map<String, Long> tokenUserIds = new ConcurrentHashMap<>();

    public record LoginVO(String token, String role, String username, Long deptId) {
    }

    public LoginVO login(String username, String password) {
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException(401, "用户名或密码错误"));
        String digest = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
        if (!digest.equals(user.getPasswordHash())) {
            throw new BizException(401, "用户名或密码错误");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenUserIds.put(token, user.getId());
        return new LoginVO(token, user.getRole().name(), user.getUsername(), user.getDeptId());
    }

    public SysUser resolve(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Long userId = tokenUserIds.get(token);
        return userId == null ? null : userRepository.findById(userId).orElse(null);
    }
}
