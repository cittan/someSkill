package com.oa.roster.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 花名册视图对象。
 * 关键设计：HIDDEN 的字段不赋值（null），配合 @JsonInclude(NON_NULL)
 * 让敏感字段彻底不出现在 JSON 里——"后端字段过滤"的实现核心，
 * 而不是返回一个 masked 空值让前端猜语义。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeVO {

    private Long id;
    private String empNo;
    private String name;
    private String deptName;
    private String position;
    private String status;       // 中文标签
    private LocalDate hireDate;

    // ---- INTERNAL ----
    private String email;
    private String attendanceNo;

    // ---- SENSITIVE ----
    private String phone;

    // ---- CONFIDENTIAL ----
    private String idCard;
    private String bankCard;

    // ---- SALARY ----
    private BigDecimal salary;
}
