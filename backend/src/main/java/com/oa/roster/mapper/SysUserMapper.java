package com.oa.roster.mapper;

import com.oa.roster.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 系统用户表 Mapper：SQL 见 resources/mapper/SysUserMapper.xml */
@Mapper
public interface SysUserMapper {

    /** 登录：按用户名查询 */
    SysUser selectByUsername(@Param("username") String username);

    /** token 解析：按主键查询 */
    SysUser selectById(@Param("id") Long id);
}
