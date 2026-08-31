package com.oa.roster.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 分页结果 VO：字段名与 Spring Data Page 的 JSON 结构保持一致
 * （content / totalElements），前端 Axios 解析无需改动。
 */
@Getter
@AllArgsConstructor
public class PageVO<T> {

    /** 当前页数据 */
    private List<T> content;

    /** 总条数（用于计算总页数） */
    private long totalElements;
}
