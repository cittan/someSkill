package com.oa.roster.enums;

/**
 * 字段敏感级别（列权限的载体）：
 * PUBLIC       工号/姓名/部门/职位/状态/入职日期
 * INTERNAL     邮箱/考勤号（部门内共享）
 * SENSITIVE    手机号
 * CONFIDENTIAL 身份证/银行卡
 * SALARY       薪资
 */
public enum SensitiveLevel {
    PUBLIC,
    INTERNAL,
    SENSITIVE,
    CONFIDENTIAL,
    SALARY
}
