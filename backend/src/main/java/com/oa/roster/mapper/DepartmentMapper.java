package com.oa.roster.mapper;

import com.oa.roster.entity.Department;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** 部门表 Mapper：SQL 见 resources/mapper/DepartmentMapper.xml */
@Mapper
public interface DepartmentMapper {

    /** 全量查询：组织架构级小表，导入与列表页一次性加载做内存映射 */
    List<Department> selectAll();
}
