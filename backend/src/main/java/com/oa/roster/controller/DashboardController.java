package com.oa.roster.controller;

import com.oa.roster.common.ApiResponse;
import com.oa.roster.common.UserContext;
import com.oa.roster.dto.EmployeeVO;
import com.oa.roster.entity.Employee;
import com.oa.roster.enums.EmployeeStatus;
import com.oa.roster.mapper.DepartmentMapper;
import com.oa.roster.mapper.EmployeeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 既有 Dashboard 员工管理接口（历史实现）：返回明文全量字段，
 * 字段权限由前端 Axios 响应拦截器在展示层剔除。
 *
 * 注意：明文数据仍会到达浏览器（网络面板可见），这只是展示层控制；
 * 安全方案以花名册接口（/api/employees，后端过滤+脱敏）为准。
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;

    @GetMapping("/employees")
    public ApiResponse<List<EmployeeVO>> list() {
        UserContext.require(); // 仅校验登录，不做字段级权限
        Map<Long, String> deptNames = departmentMapper.selectAll().stream()
                .collect(Collectors.toMap(d -> d.getId(), d -> d.getName()));
        List<EmployeeVO> plain = employeeMapper.selectAll().stream()
                .map(e -> toPlainVO(e, deptNames))
                .collect(Collectors.toList());
        return ApiResponse.ok(plain);
    }

    private EmployeeVO toPlainVO(Employee e, Map<Long, String> deptNames) {
        EmployeeVO vo = new EmployeeVO();
        vo.setId(e.getId());
        vo.setEmpNo(e.getEmpNo());
        vo.setName(e.getName());
        vo.setDeptName(deptNames.get(e.getDeptId()));
        vo.setPosition(e.getPosition());
        vo.setStatus(EmployeeStatus.labelOf(e.getStatus()));
        vo.setHireDate(e.getHireDate());
        vo.setEmail(e.getEmail());
        vo.setAttendanceNo(e.getAttendanceNo());
        vo.setPhone(e.getPhone());
        vo.setIdCard(e.getIdCard());
        vo.setBankCard(e.getBankCard());
        vo.setSalary(e.getSalary());
        return vo;
    }
}
