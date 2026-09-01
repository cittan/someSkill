package com.oa.roster.controller;

import com.oa.roster.common.ApiResponse;
import com.oa.roster.common.BizException;
import com.oa.roster.common.UserContext;
import com.oa.roster.dto.EmployeeVO;
import com.oa.roster.dto.ImportTaskVO;
import com.oa.roster.dto.PageVO;
import com.oa.roster.entity.SysUser;
import com.oa.roster.enums.RoleEnum;
import com.oa.roster.service.ImportTaskService;
import com.oa.roster.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final ImportTaskService importTaskService;

    /** 花名册列表：行级/字段级权限均由服务端处理，前端只管渲染 */
    @GetMapping
    public ApiResponse<PageVO<EmployeeVO>> list(@RequestParam(required = false) Long deptId,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        SysUser user = UserContext.require();
        return ApiResponse.ok(employeeService.listForUser(user, deptId, keyword, page, size));
    }

    /** 花名册详情：跨部门访问自动降级为普通员工字段规则 */
    @GetMapping("/{id}")
    public ApiResponse<EmployeeVO> detail(@PathVariable Long id) {
        SysUser user = UserContext.require();
        return ApiResponse.ok(employeeService.detailForUser(user, id));
    }

    /**
     * Excel 批量导入（异步）：校验 HR 权限后立即返回 taskId，导入在专用线程池执行。
     * 功能级权限在【提交时】校验——异步线程没有 UserContext（ThreadLocal 绑定请求线程）。
     */
    @PostMapping("/import")
    public ApiResponse<ImportTaskVO> importExcel(@RequestParam("file") MultipartFile file) {
        SysUser user = UserContext.require();
        if (user.getRole() != RoleEnum.HR) {
            throw BizException.forbidden("仅 HR 可执行数据导入");
        }
        return ApiResponse.ok(importTaskService.submit(file, user.getUsername()));
    }

    /** 导入任务进度轮询：仅任务提交者可见 */
    @GetMapping("/import/tasks/{taskId}")
    public ApiResponse<ImportTaskVO> importTaskStatus(@PathVariable String taskId) {
        SysUser user = UserContext.require();
        return ApiResponse.ok(importTaskService.get(taskId, user.getUsername()));
    }
}
