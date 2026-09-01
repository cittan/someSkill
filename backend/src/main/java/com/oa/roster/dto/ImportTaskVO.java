package com.oa.roster.dto;

import lombok.Data;

import java.util.List;

/** 异步导入任务的状态视图：前端轮询的响应体 */
@Data
public class ImportTaskVO {

    private String taskId;
    /** PENDING(已排队) / RUNNING(执行中) / SUCCESS / FAILED */
    private String status;
    /** 已处理行数（进度条分子） */
    private int processedRows;
    /** 总行数（进度条分母，流式模式下任务完成前可能为 0） */
    private int total;
    private int successCount;
    private int failedCount;
    /** 完成后的行级错误明细（进行中为 null） */
    private List<RowError> errors;
    /** FAILED 时的失败原因 */
    private String errorMessage;
}
