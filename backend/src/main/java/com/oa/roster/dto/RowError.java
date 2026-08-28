package com.oa.roster.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 单行校验错误：行号 + 原因，直接可展示给导入操作者 */
@Data
@AllArgsConstructor
public class RowError {

    private int rowIndex;
    private String reason;
}
