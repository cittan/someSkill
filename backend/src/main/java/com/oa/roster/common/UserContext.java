package com.oa.roster.common;

import com.oa.roster.entity.SysUser;

/**
 * 当前登录用户上下文：登录拦截器写入，请求结束清理，避免线程复用导致串号。
 */
public final class UserContext {

    private static final ThreadLocal<SysUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(SysUser user) {
        HOLDER.set(user);
    }

    public static SysUser get() {
        return HOLDER.get();
    }

    public static SysUser require() {
        SysUser user = HOLDER.get();
        if (user == null) {
            throw new BizException(401, "未登录或登录已过期");
        }
        return user;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
