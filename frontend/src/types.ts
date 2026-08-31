/** 后端角色枚举（与 RoleEnum.java 保持一致） */
export type Role = 'HR' | 'EXECUTIVE' | 'DEPT_ADMIN' | 'ATTENDANCE' | 'EMPLOYEE';

/** 后端统一响应包装：拦截器解包后业务侧只拿到 data */
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

/** 花名册行（敏感字段按角色可选——HIDDEN 时后端根本不返回该 key） */
export interface EmployeeVO {
  id: number;
  empNo: string;
  name: string;
  deptName: string;
  position: string;
  status: string;
  hireDate: string;
  email?: string;
  attendanceNo?: string;
  phone?: string;
  idCard?: string;
  bankCard?: string;
  salary?: number;
}

/** 分页结构（与后端 PageVO 一致） */
export interface PageVO<T> {
  content: T[];
  totalElements: number;
}

/** 登录响应 */
export interface LoginVO {
  token: string;
  role: Role;
  username: string;
  deptId: number;
}

/** 花名册查询参数 */
export interface RosterQuery {
  deptId?: number;
  keyword?: string;
  page: number;
  size: number;
}
