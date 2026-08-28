package com.oa.roster.enums;

import java.util.Map;

/**
 * RBAC 核心：角色 = 行级数据范围 + 字段可见性矩阵。
 *
 * 面试要点：权限模型不一定要 user/role/permission 五张表——
 * 当角色集合稳定（OA 的 5 类角色）、权限规则是"策略"而非"可配置资源"时，
 * 用枚举把规则收敛到一处，比查库拼权限更直观、更易测试、零查库开销。
 *
 * 矩阵（行=角色，列=敏感级别）：
 * | 级别         | HR | 高层 | 部门管理员 | 考勤员 | 员工 |
 * |-------------|----|------|-----------|--------|------|
 * | PUBLIC      | 明文| 明文 | 明文      | 明文   | 明文 |
 * | INTERNAL    | 明文| 明文 | 明文      | 明文   | 脱敏 |
 * | SENSITIVE   | 明文| 脱敏 | 脱敏      | 脱敏   | 脱敏 |
 * | CONFIDENTIAL| 明文| 隐藏 | 脱敏      | 隐藏   | 隐藏 |
 * | SALARY      | 明文| 隐藏 | 隐藏      | 隐藏   | 隐藏 |
 */
public enum RoleEnum {

    /** HR：全部部门 + 全部字段明文（数据Owner） */
    HR(DeptScope.ALL, Map.of(
            SensitiveLevel.PUBLIC, Visibility.PLAIN,
            SensitiveLevel.INTERNAL, Visibility.PLAIN,
            SensitiveLevel.SENSITIVE, Visibility.PLAIN,
            SensitiveLevel.CONFIDENTIAL, Visibility.PLAIN,
            SensitiveLevel.SALARY, Visibility.PLAIN)),

    /** 高层领导：全部部门，但薪资/身份证/银行卡对高层也隐藏 */
    EXECUTIVE(DeptScope.ALL, Map.of(
            SensitiveLevel.PUBLIC, Visibility.PLAIN,
            SensitiveLevel.INTERNAL, Visibility.PLAIN,
            SensitiveLevel.SENSITIVE, Visibility.MASKED,
            SensitiveLevel.CONFIDENTIAL, Visibility.HIDDEN,
            SensitiveLevel.SALARY, Visibility.HIDDEN)),

    /** 部门管理员：本部门，可见本部门员工脱敏后的证件信息（办理入离职材料） */
    DEPT_ADMIN(DeptScope.CURRENT_DEPT, Map.of(
            SensitiveLevel.PUBLIC, Visibility.PLAIN,
            SensitiveLevel.INTERNAL, Visibility.PLAIN,
            SensitiveLevel.SENSITIVE, Visibility.MASKED,
            SensitiveLevel.CONFIDENTIAL, Visibility.MASKED,
            SensitiveLevel.SALARY, Visibility.HIDDEN)),

    /** 考勤员：本部门 + 考勤号明文；跨部门访问时按普通员工处理 */
    ATTENDANCE(DeptScope.CURRENT_DEPT, Map.of(
            SensitiveLevel.PUBLIC, Visibility.PLAIN,
            SensitiveLevel.INTERNAL, Visibility.PLAIN,
            SensitiveLevel.SENSITIVE, Visibility.MASKED,
            SensitiveLevel.CONFIDENTIAL, Visibility.HIDDEN,
            SensitiveLevel.SALARY, Visibility.HIDDEN)),

    /** 普通员工：本部门，仅基本字段 + 脱敏联系方式 */
    EMPLOYEE(DeptScope.CURRENT_DEPT, Map.of(
            SensitiveLevel.PUBLIC, Visibility.PLAIN,
            SensitiveLevel.INTERNAL, Visibility.MASKED,
            SensitiveLevel.SENSITIVE, Visibility.MASKED,
            SensitiveLevel.CONFIDENTIAL, Visibility.HIDDEN,
            SensitiveLevel.SALARY, Visibility.HIDDEN));

    private final DeptScope scope;
    private final Map<SensitiveLevel, Visibility> matrix;

    RoleEnum(DeptScope scope, Map<SensitiveLevel, Visibility> matrix) {
        this.scope = scope;
        this.matrix = matrix;
    }

    public DeptScope getScope() {
        return scope;
    }

    public Visibility visibilityOf(SensitiveLevel level) {
        // 未知级别默认隐藏，权限收敛的默认拒绝
        return matrix.getOrDefault(level, Visibility.HIDDEN);
    }
}
