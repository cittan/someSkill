package com.oa.roster.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 员工实体（对应表 employee，列名由 MyBatis 驼峰映射 emp_no -> empNo）。
 */
@Getter
@Setter
@NoArgsConstructor
public class Employee {

    /** 主键，自增 */
    private Long id;

    /** 工号：全表唯一，导入幂等的依据（唯一索引兜底并发插入） */
    private String empNo;

    /** 姓名 */
    private String name;

    /** 所属部门 ID（关联 department.id），行级数据权限按此收窄 */
    private Long deptId;

    /** 职位 */
    private String position;

    /** 邮箱：敏感级别 INTERNAL */
    private String email;

    /** 手机号：敏感级别 SENSITIVE，按角色明文/脱敏 */
    private String phone;

    /** 身份证号：敏感级别 CONFIDENTIAL，仅 HR 可见明文 */
    private String idCard;

    /** 银行卡号：敏感级别 CONFIDENTIAL */
    private String bankCard;

    /** 薪资：敏感级别 SALARY，仅 HR 可见，必须 BigDecimal（浮点有精度误差） */
    private BigDecimal salary;

    /** 考勤号：敏感级别 INTERNAL，考勤员本部门明文 */
    private String attendanceNo;

    /** 员工状态：存枚举名 ACTIVE / RESIGNED / PROBATION（旧系统 1/2/3 转换而来） */
    private String status;

    /** 入职日期：旧系统三种格式统一解析为 LocalDate */
    private LocalDate hireDate;
}
