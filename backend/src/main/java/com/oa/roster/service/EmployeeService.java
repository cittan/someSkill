package com.oa.roster.service;

import com.oa.roster.common.BizException;
import com.oa.roster.dto.EmployeeVO;
import com.oa.roster.dto.PageVO;
import com.oa.roster.entity.Department;
import com.oa.roster.entity.Employee;
import com.oa.roster.entity.SysUser;
import com.oa.roster.enums.DeptScope;
import com.oa.roster.enums.EmployeeStatus;
import com.oa.roster.enums.RoleEnum;
import com.oa.roster.enums.SensitiveLevel;
import com.oa.roster.enums.Visibility;
import com.oa.roster.mapper.DepartmentMapper;
import com.oa.roster.mapper.EmployeeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 花名册查询：行级数据权限 + 字段级权限（过滤/脱敏）都在服务端完成。
 *
 * 安全要点（面试可讲）：
 * 1. 行级收窄在服务端强制覆盖查询条件，前端传入的 deptId 不被信任；
 * 2. HIDDEN 字段不赋值 + @JsonInclude(NON_NULL)，敏感字段根本不出现在响应 JSON，
 *    而不是返回空值让前端隐藏——数据不落地到浏览器才是真安全；
 * 3. 跨部门访问降级：非 ALL 范围角色访问外部门员工时，按普通员工的字段规则处理。
 */
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;

    public PageVO<EmployeeVO> listForUser(SysUser user, Long deptId, String keyword, int page, int size) {
        // 行级权限：非 ALL 范围角色，无论前端传什么都强制收敛到本部门
        Long effectiveDeptId = user.getRole().getScope() == DeptScope.ALL
                ? deptId
                : user.getDeptId();

        int safeSize = Math.min(Math.max(size, 1), 100);
        long offset = (long) Math.max(page, 0) * safeSize;

        List<Employee> rows = employeeMapper.selectByCondition(
                effectiveDeptId, trimToNull(keyword), offset, safeSize);
        long total = employeeMapper.countByCondition(effectiveDeptId, trimToNull(keyword));

        Map<Long, String> deptNames = loadDeptNames();
        List<EmployeeVO> content = rows.stream()
                .map(e -> toVO(e, user.getRole(), deptNames))
                .collect(Collectors.toList());
        return new PageVO<>(content, total);
    }

    public EmployeeVO detailForUser(SysUser user, Long id) {
        Employee employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw new BizException(404, "员工不存在");
        }
        RoleEnum effectiveRole = effectiveRole(user, employee);
        return toVO(employee, effectiveRole, loadDeptNames());
    }

    /**
     * 跨部门降级：HR/高层保持原角色；其他角色访问外部门员工时按普通员工处理
     * （对应需求：A 部门考勤员只能看 A 部门的考勤号，看 B 部门按普通员工规则）。
     */
    private RoleEnum effectiveRole(SysUser user, Employee target) {
        if (user.getRole().getScope() == DeptScope.ALL) {
            return user.getRole();
        }
        return target.getDeptId().equals(user.getDeptId())
                ? user.getRole()
                : RoleEnum.EMPLOYEE;
    }

    /** 部门表很小（组织架构级数据），一次性全量加载做内存映射，避免列表 N+1 查询 */
    private Map<Long, String> loadDeptNames() {
        return departmentMapper.selectAll().stream()
                .collect(Collectors.toMap(Department::getId, Department::getName, (a, b) -> a));
    }

    /**
     * 实体 -> VO：按角色的字段可见性矩阵决定 明文/脱敏/不返回。
     */
    private EmployeeVO toVO(Employee e, RoleEnum role, Map<Long, String> deptNames) {
        EmployeeVO vo = new EmployeeVO();
        // PUBLIC：所有角色可见
        vo.setId(e.getId());
        vo.setEmpNo(e.getEmpNo());
        vo.setName(e.getName());
        vo.setDeptName(deptNames.get(e.getDeptId()));
        vo.setPosition(e.getPosition());
        vo.setStatus(EmployeeStatus.labelOf(e.getStatus()));
        vo.setHireDate(e.getHireDate());

        Visibility internal = role.visibilityOf(SensitiveLevel.INTERNAL);
        if (internal == Visibility.PLAIN) {
            vo.setEmail(e.getEmail());
            vo.setAttendanceNo(e.getAttendanceNo());
        } else if (internal == Visibility.MASKED) {
            vo.setEmail(MaskUtils.email(e.getEmail()));
            vo.setAttendanceNo(MaskUtils.attendanceNo(e.getAttendanceNo()));
        }
        // HIDDEN：不赋值，JSON 中无该字段

        Visibility sensitive = role.visibilityOf(SensitiveLevel.SENSITIVE);
        if (sensitive == Visibility.PLAIN) {
            vo.setPhone(e.getPhone());
        } else if (sensitive == Visibility.MASKED) {
            vo.setPhone(MaskUtils.phone(e.getPhone()));
        }

        Visibility confidential = role.visibilityOf(SensitiveLevel.CONFIDENTIAL);
        if (confidential == Visibility.PLAIN) {
            vo.setIdCard(e.getIdCard());
            vo.setBankCard(e.getBankCard());
        } else if (confidential == Visibility.MASKED) {
            vo.setIdCard(MaskUtils.idCard(e.getIdCard()));
            vo.setBankCard(MaskUtils.bankCard(e.getBankCard()));
        }

        if (role.visibilityOf(SensitiveLevel.SALARY) == Visibility.PLAIN) {
            vo.setSalary(e.getSalary());
        }
        return vo;
    }

    private String trimToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
