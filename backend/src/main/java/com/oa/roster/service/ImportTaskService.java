package com.oa.roster.service;

import com.oa.roster.common.BizException;
import com.oa.roster.config.ImportExecutorConfig;
import com.oa.roster.dto.ImportReport;
import com.oa.roster.dto.ImportTaskVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Excel 导入异步化：上传接口立即返回 taskId，导入在专用线程池执行，前端轮询进度。
 *
 * 设计要点（面试可讲）：
 * 1. MultipartFile 的输入流只在当前请求生命周期内有效，必须在请求线程内
 *    transferTo 落盘临时文件，异步线程读文件而非读 MultipartFile；
 * 2. 权限校验（仅 HR）在提交时完成——异步线程里没有 UserContext（ThreadLocal
 *    绑定请求线程），且"提交那一刻有没有权限"才是正确语义；
 * 3. 任务状态存内存 ConcurrentHashMap：演示级简化，重启即失。
 *    生产方案：任务表（taskId/状态/进度/报告落库）或 Redis，
 *    多实例部署时轮询请求要能路由到任意实例（共享存储）；
 * 4. 进度上报：ExcelImportService 每处理完一批回调 addProcessed，
 *    轮询接口据此渲染进度条 processedRows/total。
 */
@Slf4j
@Service
public class ImportTaskService {

    private final ExcelImportService excelImportService;
    private final ThreadPoolExecutor importExecutor;

    /** 任务存储：taskId -> 任务。演示级内存实现（生产应落库/Redis） */
    private final Map<String, ImportTask> tasks = new ConcurrentHashMap<>();

    public ImportTaskService(ExcelImportService excelImportService,
                             @Qualifier(ImportExecutorConfig.IMPORT_EXECUTOR) ThreadPoolExecutor importExecutor) {
        this.excelImportService = excelImportService;
        this.importExecutor = importExecutor;
    }

    /** 提交导入任务：请求线程内落盘文件并排队，立即返回 taskId */
    public ImportTaskVO submit(MultipartFile file, String operator) {
        if (file == null || file.isEmpty()) {
            throw BizException.badRequest("上传文件为空");
        }
        // 必须在请求线程内落盘：MultipartFile 的流请求结束即失效
        File temp;
        try {
            temp = File.createTempFile("import-", ".xlsx");
            file.transferTo(temp);
        } catch (IOException e) {
            log.error("导入文件落盘失败", e);
            throw BizException.badRequest("文件保存失败，请重试");
        }

        ImportTask task = new ImportTask(UUID.randomUUID().toString(), operator);
        tasks.put(task.taskId, task);
        try {
            importExecutor.execute(() -> run(task, temp));
        } catch (RejectedExecutionException e) {
            // 队列满：AbortPolicy 抛出的拒绝，清理已落盘文件并友好提示
            temp.delete();
            tasks.remove(task.taskId);
            throw BizException.badRequest("导入队列已满，请稍后再试");
        }
        return task.toVO();
    }

    /** 查询任务状态：仅任务提交者可见（防止他人窥探导入报告中的敏感错误明细） */
    public ImportTaskVO get(String taskId, String requester) {
        ImportTask task = tasks.get(taskId);
        if (task == null || !task.operator.equals(requester)) {
            throw BizException.badRequest("导入任务不存在");
        }
        return task.toVO();
    }

    /** 后台线程执行体：状态流转 RUNNING -> SUCCESS/FAILED */
    private void run(ImportTask task, File file) {
        task.markRunning();
        try {
            ImportReport report = excelImportService.importExcel(file, task::addProcessed);
            task.complete(report);
            log.info("导入任务完成: taskId={}, operator={}, total={}, success={}, failed={}",
                    task.taskId, task.operator,
                    report.getTotal(), report.getSuccessCount(), report.getFailedCount());
        } catch (Exception e) {
            task.fail("导入失败：" + e.getMessage());
            log.error("导入任务失败: taskId={}, operator={}", task.taskId, task.operator, e);
        } finally {
            // 临时文件清理（无论成败）
            if (!file.delete()) {
                file.deleteOnExit();
            }
        }
    }

    /** 内部任务模型：进度计数线程安全（导入线程写、轮询线程读） */
    static class ImportTask {
        final String taskId;
        final String operator;
        private volatile String status = "PENDING";
        private final AtomicInteger processedRows = new AtomicInteger(0);
        private volatile ImportReport report;
        private volatile String errorMessage;

        ImportTask(String taskId, String operator) {
            this.taskId = taskId;
            this.operator = operator;
        }

        void markRunning() {
            this.status = "RUNNING";
        }

        void addProcessed(int rows) {
            processedRows.addAndGet(rows);
        }

        void complete(ImportReport report) {
            this.report = report;
            this.status = "SUCCESS";
        }

        void fail(String message) {
            this.errorMessage = message;
            this.status = "FAILED";
        }

        ImportTaskVO toVO() {
            ImportTaskVO vo = new ImportTaskVO();
            vo.setTaskId(taskId);
            vo.setStatus(status);
            vo.setProcessedRows(processedRows.get());
            if (report != null) {
                vo.setTotal(report.getTotal());
                vo.setSuccessCount(report.getSuccessCount());
                vo.setFailedCount(report.getFailedCount());
                vo.setErrors(report.getErrors());
            }
            vo.setErrorMessage(errorMessage);
            return vo;
        }
    }
}
