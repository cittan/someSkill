package com.oa.roster.service;

/**
 * 脱敏工具：所有规则收敛一处，避免散落在各业务代码里口径不一致。
 */
public final class MaskUtils {

    private MaskUtils() {
    }

    /** 手机号：138****5678 */
    public static String phone(String phone) {
        return mask(phone, 3, 4);
    }

    /** 身份证：保留前 3 后 4 */
    public static String idCard(String idCard) {
        return mask(idCard, 3, 4);
    }

    /** 银行卡：保留前 4 后 4 */
    public static String bankCard(String bankCard) {
        return mask(bankCard, 4, 4);
    }

    /** 邮箱：保留前 2 位 + 域名，如 zh****@oa.com */
    public static String email(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return email;
        }
        String prefix = email.substring(0, Math.min(2, at));
        return prefix + "****" + email.substring(at);
    }

    /** 考勤号：保留前 3 后 2，如 ATT****01 */
    public static String attendanceNo(String no) {
        return mask(no, 3, 2);
    }

    private static String mask(String source, int keepPrefix, int keepSuffix) {
        if (source == null || source.length() <= keepPrefix + keepSuffix) {
            return source;
        }
        return source.substring(0, keepPrefix)
                + "*".repeat(source.length() - keepPrefix - keepSuffix)
                + source.substring(source.length() - keepSuffix);
    }
}
