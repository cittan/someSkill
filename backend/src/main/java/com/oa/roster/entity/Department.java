package com.oa.roster.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 部门实体（对应表 department）：组织架构级小表，导入时全量加载做内存映射。
 */
@Getter
@Setter
@NoArgsConstructor
public class Department {

    /** 主键，自增 */
    private Long id;

    /** 部门名称，唯一；Excel 导入时按名称映射成此 ID */
    private String name;
}
