package com.oa.roster.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 导入报告：部分导入策略的输出，总数/成功/失败/错误明细 */
@Data
public class ImportReport {

    private int total;
    private int successCount;
    private int failedCount;
    private List<RowError> errors = new ArrayList<>();

    public ImportReport(int total) {
        this.total = total;
    }

    public void addError(int rowIndex, String reason) {
        this.errors.add(new RowError(rowIndex, reason));
        this.failedCount++;
    }
}
