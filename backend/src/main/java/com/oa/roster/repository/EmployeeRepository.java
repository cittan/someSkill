package com.oa.roster.repository;

import com.oa.roster.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * 行级权限在服务端收窄：非 ALL 范围角色调用前会被强制覆盖 deptId，
     * 前端传入的 deptId 不被信任。
     */
    @Query("""
            select e from Employee e
            where (:deptId is null or e.deptId = :deptId)
              and (:keyword is null or e.name like %:keyword% or e.empNo like %:keyword%)
            """)
    Page<Employee> query(@Param("deptId") Long deptId,
                         @Param("keyword") String keyword,
                         Pageable pageable);

    /** 导入时批量预查库内工号，避免逐行查库 */
    List<Employee> findByEmpNoIn(Collection<String> empNos);
}
