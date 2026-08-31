package com.oa.roster.mapper;

import com.oa.roster.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 员工表 Mapper：SQL 见 resources/mapper/EmployeeMapper.xml。
 */
@Mapper
public interface EmployeeMapper {

    /**
     * 条件分页查询（花名册列表）。
     * 行级权限说明：服务端在调用前已按角色强制覆盖 deptId，此处参数不可被前端左右。
     */
    List<Employee> selectByCondition(@Param("deptId") Long deptId,
                                     @Param("keyword") String keyword,
                                     @Param("offset") long offset,
                                     @Param("limit") int limit);

    /** 与 selectByCondition 同条件计数，用于分页 total */
    long countByCondition(@Param("deptId") Long deptId,
                          @Param("keyword") String keyword);

    /** 主键查询（详情） */
    Employee selectById(@Param("id") Long id);

    /** 全量查询（仅 Dashboard 历史明文接口使用） */
    List<Employee> selectAll();

    /** 批量预查库内已存在的工号（导入幂等校验，一次 IN 查询避免逐行查库） */
    List<Employee> selectByEmpNoIn(@Param("empNos") List<String> empNos);

    /**
     * 批量插入（Excel 导入分批入库）。
     * foreach 拼成多值 INSERT，一条 SQL 完成一批，配合每批独立事务。
     */
    int insertBatch(@Param("list") List<Employee> list);
}
