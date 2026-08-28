import request from './request.js';

export function login(data) {
  return request.post('/auth/login', data);
}

/** 花名册列表（后端已按角色完成行级收窄 + 字段脱敏/隐藏） */
export function fetchRoster(params) {
  return request.get('/employees', { params });
}

/** 既有 Dashboard 员工管理接口（明文，前端拦截器负责展示层剔除） */
export function fetchDashboardEmployees() {
  return request.get('/dashboard/employees');
}
