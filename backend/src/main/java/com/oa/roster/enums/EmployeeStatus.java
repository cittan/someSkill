package com.oa.roster.enums;

import com.oa.roster.common.BizException;

/**
 * 员工在职状态，含旧系统状态码转换（数据迁移的字段转换点之一）。
 */
public enum EmployeeStatus {

    ACTIVE("在职"),
    RESIGNED("离职"),
    PROBATION("试用期");

    private final String label;

    EmployeeStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 旧系统用数字码：1=在职 2=离职 3=试用期
     */
    public static EmployeeStatus fromLegacyCode(String legacyCode) {
        return switch (legacyCode == null ? "" : legacyCode.trim()) {
            case "1" -> ACTIVE;
            case "2" -> RESIGNED;
            case "3" -> PROBATION;
            default -> throw BizException.badRequest("旧系统状态码非法（仅支持 1/2/3）：" + legacyCode);
        };
    }

    public static String labelOf(String name) {
        try {
            return valueOf(name).getLabel();
        } catch (IllegalArgumentException e) {
            return name;
        }
    }
}
