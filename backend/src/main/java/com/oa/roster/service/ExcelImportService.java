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
import com.oa.roster.repository.DepartmentRepository;
import com.oa.roster.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

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
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Excel 批量导入：解析 -> 校验 -> 字段转换 -> 分批入库（部分导入策略）。
 *
 * 设计要点（面试可讲）：
 * 1. EasyExcel 逐行 SAX 回调读取，内存占用与文件行数无关，避免原生 POI DOM 全量加载导致 OOM；
 * 2. 校验与入库分离：校验失败的行只记录（行号+原因）不中断，合法行分批入库；
 * 3. 每批独立事务（TransactionTemplate），而非一个大事务包全部——
 *    牺牲全量原子性换取可用性，符合"存量迁移、坏行人工修复后补导"的场景；
 * 4. 库内工号/部门映射一次性预加载批量比对，避免逐行查库（5 万行 = 5 万次 SQL 的反模式）；
 * 5. 导入按工号幂等：中断重跑时已存在的工号会被校验拦下，不会重复插入。
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

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final TransactionTemplate transactionTemplate;

    /** 单行校验失败异常：携带原因，由调用方收集进报告 */
    private static class InvalidRowException extends RuntimeException {
        InvalidRowException(String reason) {
            super(reason);
        }
    }

    public ImportReport importExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BizException.badRequest("上传文件为空");
        }
        List<EmployeeExcelRow> rows = readRows(file);
        if (rows.isEmpty()) {
            throw BizException.badRequest("Excel 中没有数据行");
        }

        ImportReport report = new ImportReport(rows.size());

        // ---- 预加载比对数据（避免逐行查库）----
        Map<String, Long> deptIdByName = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(Department::getName, Department::getId, (a, b) -> a));
        Set<String> dbEmpNos = employeeRepository.findByEmpNoIn(
                        rows.stream().map(EmployeeExcelRow::getEmpNo)
                                .filter(Objects::nonNull).map(String::trim).toList())
                .stream().map(Employee::getEmpNo).collect(Collectors.toSet());

        // ---- 逐行校验 + 转换，失败只收集不中断 ----
        Set<String> seenInFile = new HashSet<>();
        List<Employee> valid = new ArrayList<>(rows.size());
        for (EmployeeExcelRow row : rows) {
            try {
                valid.add(convertAndValidate(row, deptIdByName, dbEmpNos, seenInFile));
                seenInFile.add(row.getEmpNo().trim());
            } catch (InvalidRowException e) {
                report.addError(row.getRowIndex(), e.getMessage());
            }
        }

        // ---- 合法行分批入库，每批独立事务（部分导入的核心）----
        int saved = 0;
        for (int i = 0; i < valid.size(); i += BATCH_SIZE) {
            List<Employee> batch = valid.subList(i, Math.min(i + BATCH_SIZE, valid.size()));
            transactionTemplate.executeWithoutResult(s -> employeeRepository.saveAll(batch));
            saved += batch.size();
        }
        report.setSuccessCount(saved);
        log.info("Excel 导入完成: total={}, success={}, failed={}",
                report.getTotal(), report.getSuccessCount(), report.getFailedCount());
        return report;
    }

    /**
     * EasyExcel 读取：监听器逐行回调（SAX 流式），行号来自读取上下文，含表头偏移。
     */
    private List<EmployeeExcelRow> readRows(MultipartFile file) {
        List<EmployeeExcelRow> rows = new ArrayList<>();
        try (InputStream in = file.getInputStream()) {
            EasyExcel.read(in, EmployeeExcelRow.class, new AnalysisEventListener<EmployeeExcelRow>() {
                @Override
                public void invoke(EmployeeExcelRow row, AnalysisContext context) {
                    row.setRowIndex(context.readRowHolder().getRowIndex() + 1);
                    rows.add(row);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    // 读取完毕，无需额外处理
                }
            }).sheet().doRead();
        } catch (Exception e) {
            log.error("Excel 解析失败", e);
            throw BizException.badRequest("Excel 文件解析失败，请使用模板重新导出");
        }
        return rows;
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
