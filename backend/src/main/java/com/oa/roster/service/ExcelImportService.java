package com.oa.roster.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.oa.roster.common.BizException;
import com.oa.roster.dto.EmployeeExcelRow;
import com.oa.roster.dto.ImportReport;
import com.oa.roster.entity.Department;
import com.oa.roster.entity.Employee;
import com.oa.roster.enums.EmployeeStatus;
import com.oa.roster.mapper.DepartmentMapper;
import com.oa.roster.mapper.EmployeeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Excel 批量导入：流式"攒批即处理"——监听器攒满 500 行立即校验+转换+独立事务入库。
 *
 * 设计要点（面试可讲）：
 * 1. EasyExcel SAX 逐行回调读取，且【不全量收集】：buffer 攒满即处理并清空，
 *    内存占用 O(batch)，与文件总行数无关——初版"读完 5 万行再统一处理"的全量收集
 *    会抵消流式收益（rows + valid 两份列表峰值 200MB+），本次重构消除了该瓶颈；
 * 2. 校验与入库分离：校验失败的行只记录（行号+原因）不中断，合法行当批入库；
 * 3. 每批独立事务（TransactionTemplate），而非一个大事务包全部——
 *    牺牲全量原子性换取可用性，符合"存量迁移、坏行人工修复后补导"的场景；
 * 4. 库内工号预查按【批】进行（每批 500 个工号一次 IN 查询），
 *    既避免逐行查库，也避免几十万工号拼一个超大 IN 子句；
 * 5. 导入按工号幂等：中断重跑时已存在的工号会被校验拦下，只会补进新行。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private static final int BATCH_SIZE = 500;

    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern ID_CARD = Pattern.compile("^\\d{17}[\\dXx]$");

    /** 旧系统导出的日期格式不止一种，逐一尝试解析 */
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy-M-d"),
            DateTimeFormatter.ofPattern("yyyy年M月d日"));

    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final TransactionTemplate transactionTemplate;

    /** 单行校验失败异常：携带原因，由调用方收集进报告 */
    private static class InvalidRowException extends RuntimeException {
        InvalidRowException(String reason) {
            super(reason);
        }
    }

    /**
     * 导入入口（异步任务调用）：读磁盘文件，每处理完一批通过 progressListener 上报累计行数。
     *
     * @param progressListener 进度回调，参数为已处理行数（含校验失败行）；可为 null
     */
    public ImportReport importExcel(File file, java.util.function.IntConsumer progressListener) {
        ImportReport report = new ImportReport(0);
        // 部门表是组织架构级小表，一次性加载建内存映射（避免逐行查库）
        Map<String, Long> deptIdByName = departmentMapper.selectAll().stream()
                .collect(Collectors.toMap(Department::getName, Department::getId, (a, b) -> a));
        // 文件内工号去重：跨批次共享状态
        Set<String> seenInFile = new HashSet<>();
        // 数组包装使匿名类/lambda 内部可写（total 计数 / 已处理行数 / 是否已开始入库的标记）；
        // 声明在 try 之外，catch 块需要读取 anyBatchPersisted 区分中断阶段
        int[] total = {0};
        int[] processed = {0};
        boolean[] anyBatchPersisted = {false};

        try (InputStream in = new FileInputStream(file)) {
            List<EmployeeExcelRow> buffer = new ArrayList<>(BATCH_SIZE);

            EasyExcel.read(in, EmployeeExcelRow.class, new AnalysisEventListener<EmployeeExcelRow>() {
                @Override
                public void invoke(EmployeeExcelRow row, AnalysisContext context) {
                    row.setRowIndex(context.readRowHolder().getRowIndex() + 1);
                    buffer.add(row);
                    total[0]++;
                    if (buffer.size() == BATCH_SIZE) {
                        anyBatchPersisted[0] = true;
                        processBatch(buffer, deptIdByName, seenInFile, report);
                        reportProgress(buffer.size());
                        buffer.clear();
                    }
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    // 收尾：不足一批的零头同样处理
                    if (!buffer.isEmpty()) {
                        anyBatchPersisted[0] = true;
                        processBatch(buffer, deptIdByName, seenInFile, report);
                        reportProgress(buffer.size());
                        buffer.clear();
                    }
                }

                private void reportProgress(int batchSize) {
                    processed[0] += batchSize;
                    if (progressListener != null) {
                        progressListener.accept(processed[0]);
                    }
                }
            }).sheet().doRead();

            // 读取与处理结束后回填总数（流式模式下事先未知）
            report.setTotal(total[0]);
        } catch (Exception e) {
            if (anyBatchPersisted[0]) {
                // 已有批次入库后中断：已提交批次保留（部分导入语义），
                // 包装上抛由全局异常处理器兜底，修复后凭工号幂等重跑补齐剩余行
                log.error("导入中途失败（已提交批次保留，可凭工号幂等重跑）", e);
                throw new RuntimeException("导入中途失败，已入库部分保留，请修复后重新上传", e);
            }
            // 解析阶段（尚未入库任何批次）失败：提示用户检查文件
            log.error("Excel 解析失败", e);
            throw BizException.badRequest("Excel 文件解析失败，请使用模板重新导出");
        }
        if (report.getTotal() == 0) {
            throw BizException.badRequest("Excel 中没有数据行");
        }
        log.info("Excel 导入完成: total={}, success={}, failed={}",
                report.getTotal(), report.getSuccessCount(), report.getFailedCount());
        return report;
    }

    /**
     * 处理一批（<=500 行）：本批工号预查 -> 逐行校验+转换 -> 合法行独立事务入库。
     * 校验失败只收集进报告不中断；批内原子（MyBatis foreach 一条多值 INSERT + 事务）。
     */
    private void processBatch(List<EmployeeExcelRow> batch,
                              Map<String, Long> deptIdByName,
                              Set<String> seenInFile,
                              ImportReport report) {
        // 本批工号一次 IN 预查（幂等拦截依据），批级而非全文件级
        List<String> empNosInBatch = batch.stream().map(EmployeeExcelRow::getEmpNo)
                .filter(Objects::nonNull).map(String::trim).toList();
        Set<String> dbEmpNos = empNosInBatch.isEmpty()
                ? Set.of()
                : employeeMapper.selectByEmpNoIn(empNosInBatch).stream()
                        .map(Employee::getEmpNo).collect(Collectors.toSet());

        // 逐行校验 + 转换，失败只收集不中断
        List<Employee> valid = new ArrayList<>(batch.size());
        for (EmployeeExcelRow row : batch) {
            try {
                valid.add(convertAndValidate(row, deptIdByName, dbEmpNos, seenInFile));
                seenInFile.add(row.getEmpNo().trim());
            } catch (InvalidRowException e) {
                report.addError(row.getRowIndex(), e.getMessage());
            }
        }

        // 合法行独立事务入库（部分导入的核心）：一批 = 一条多值 INSERT
        if (!valid.isEmpty()) {
            transactionTemplate.executeWithoutResult(s -> employeeMapper.insertBatch(valid));
            report.setSuccessCount(report.getSuccessCount() + valid.size());
        }
    }

    /**
     * 校验链 + 字段转换（旧系统 -> 新系统）：
     * 必填 -> 格式 -> 部门映射 -> 文件内/库内工号去重 -> 状态码/日期/薪资转换
     */
    private Employee convertAndValidate(EmployeeExcelRow row,
                                        Map<String, Long> deptIdByName,
                                        Set<String> dbEmpNos,
                                        Set<String> seenInFile) {
        String empNo = requireText(row.getEmpNo(), "工号");
        String name = requireText(row.getName(), "姓名");
        String deptName = requireText(row.getDeptName(), "部门");
        String phone = requireText(row.getPhone(), "手机号");

        if (!PHONE.matcher(phone).matches()) {
            throw new InvalidRowException("手机号格式非法: " + phone);
        }
        String idCard = trimToNull(row.getIdCard());
        if (idCard != null && !ID_CARD.matcher(idCard).matches()) {
            throw new InvalidRowException("身份证号格式非法（应为 18 位）: " + idCard);
        }

        Long deptId = deptIdByName.get(deptName);
        if (deptId == null) {
            throw new InvalidRowException("部门不存在（请先在系统中维护部门）: " + deptName);
        }
        if (seenInFile.contains(empNo)) {
            throw new InvalidRowException("工号在文件内重复: " + empNo);
        }
        if (dbEmpNos.contains(empNo)) {
            throw new InvalidRowException("工号已存在于系统（导入幂等拦截）: " + empNo);
        }

        // ---- 字段转换：旧系统编码/格式 -> 新系统模型 ----
        EmployeeStatus status;
        try {
            status = EmployeeStatus.fromLegacyCode(row.getStatus());
        } catch (BizException e) {
            throw new InvalidRowException(e.getMessage());
        }
        LocalDate hireDate = parseDate(trimToNull(row.getHireDate()));
        BigDecimal salary = parseSalary(trimToNull(row.getSalary()));

        Employee e = new Employee();
        e.setEmpNo(empNo);
        e.setName(name);
        e.setDeptId(deptId);
        e.setPosition(trimToNull(row.getPosition()));
        e.setPhone(phone);
        e.setIdCard(idCard);
        e.setBankCard(trimToNull(row.getBankCard()));
        e.setSalary(salary);
        e.setAttendanceNo(trimToNull(row.getAttendanceNo()));
        e.setStatus(status.name());
        e.setHireDate(hireDate);
        return e;
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidRowException(field + " 不能为空");
        }
        return value.trim();
    }

    private LocalDate parseDate(String value) {
        if (value == null) {
            return null;
        }
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignore) {
                // 尝试下一种旧系统日期格式
            }
        }
        throw new InvalidRowException("入职日期格式无法解析: " + value);
    }

    private BigDecimal parseSalary(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new InvalidRowException("薪资格式非法: " + value);
        }
    }

    private String trimToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
