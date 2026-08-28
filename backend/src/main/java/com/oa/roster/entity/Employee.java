package com.oa.roster.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "employee")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "emp_no", nullable = false, unique = true)
    private String empNo;

    @Column(nullable = false)
    private String name;

    @Column(name = "dept_id", nullable = false)
    private Long deptId;

    private String position;

    private String email;

    private String phone;

    @Column(name = "id_card")
    private String idCard;

    @Column(name = "bank_card")
    private String bankCard;

    private BigDecimal salary;

    @Column(name = "attendance_no")
    private String attendanceNo;

    /** 存枚举名：ACTIVE / RESIGNED / PROBATION */
    @Column(nullable = false)
    private String status;

    @Column(name = "hire_date")
    private LocalDate hireDate;
}
