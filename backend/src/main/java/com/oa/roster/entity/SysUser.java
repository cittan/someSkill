package com.oa.roster.entity;

import com.oa.roster.enums.RoleEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 系统用户实体（对应表 sys_user）：登录账号 + 角色 + 所属部门。
 */
@Getter
@Setter
@NoArgsConstructor
public class SysUser {

    /** 主键，自增 */
    private Long id;

    /** 登录用户名，唯一 */
    private String username;

    /** 密码摘要（演示用 MD5，生产必须 BCrypt） */
    private String passwordHash;

    /** 角色：HR / EXECUTIVE / DEPT_ADMIN / ATTENDANT / EMPLOYEE，
     *  MyBatis 默认 EnumTypeHandler 按枚举名与 VARCHAR 互转 */
    private RoleEnum role;

    /** 所属部门 ID（关联 department.id），非 ALL 范围角色的行级权限边界 */
    private Long deptId;
}
