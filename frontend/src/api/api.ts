import request from './request';
import type { EmployeeVO, ImportTaskVO, LoginVO, PageVO, RosterQuery } from '../types';

export function login(data: { username: string; password: string }): Promise<LoginVO> {
  return request.post('/auth/login', data);
}

/** 花名册列表（后端已按角色完成行级收窄 + 字段脱敏/隐藏） */
export function fetchRoster(params: RosterQuery): Promise<PageVO<EmployeeVO>> {
  return request.get('/employees', { params });
}

/** 既有 Dashboard 员工管理接口（明文，前端拦截器负责展示层剔除） */
export function fetchDashboardEmployees(): Promise<EmployeeVO[]> {
  return request.get('/dashboard/employees');
}

/** 上传 Excel：异步导入，立即返回 taskId（multipart，浏览器自动设 Content-Type） */
export function uploadExcel(file: File): Promise<ImportTaskVO> {
  const form = new FormData();
  form.append('file', file);
  return request.post('/employees/import', form);
}

/** 轮询导入任务进度/报告 */
export function fetchImportTask(taskId: string): Promise<ImportTaskVO> {
  return request.get(`/employees/import/tasks/${taskId}`);
}
