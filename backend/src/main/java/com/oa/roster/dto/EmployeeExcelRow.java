package com.oa.roster.dto;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * Excel 导入行模型：全部用 String 接收，
 * 避免手机号/工号等长数字被读成科学计数法（经典坑），转换与校验统一在服务层做。
 */
@Data
public class EmployeeExcelRow {

    @ExcelIgnore
    private int rowIndex;   // Excel 直观行号（含表头偏移），用于错误报告定位

    @ExcelProperty("工号")
    private String empNo;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("部门")
    private String deptName;

    @ExcelProperty("职位")
    private String position;

    @ExcelProperty("手机号")
    private String phone;

    @ExcelProperty("身份证号")
    private String idCard;

    @ExcelProperty("银行卡号")
    private String bankCard;

    @ExcelProperty("薪资")
    private String salary;

    @ExcelProperty("考勤号")
    private String attendanceNo;

    /** 旧系统状态码：1=在职 2=离职 3=试用期 */
    @ExcelProperty("状态")
    private String status;

    /** 旧系统日期格式：yyyy/M/d */
    @ExcelProperty("入职日期")
    private String hireDate;
}
