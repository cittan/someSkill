package com.oa.roster.enums;

/**
 * 字段可见性：PLAIN=明文，MASKED=脱敏，HIDDEN=后端不返回（字段直接不序列化进 JSON）。
 */
public enum Visibility {
    PLAIN,
    MASKED,
    HIDDEN
}
