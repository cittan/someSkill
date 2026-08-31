import axios, { AxiosRequestConfig, AxiosResponse } from 'axios';
import type { ApiResponse, EmployeeVO, Role } from '../types';

/* ------------------------------------------------------------
 * 极简全局提示（生产项目一般接 antd message / react-hot-toast）
 * ------------------------------------------------------------ */
let toastTimer: ReturnType<typeof setTimeout> | null = null;

function toast(msg: string): void {
  // const + ?? 使 el 类型恒为 HTMLDivElement：let 的窄化无法穿透 setTimeout 回调
  // （TS 认为回调执行前 let 变量可能被重新赋值），const 的窄化才可以保留。
  const existing = document.querySelector<HTMLDivElement>('.toast');
  const el = existing ?? document.createElement('div');
  if (!existing) {
    el.className = 'toast';
    document.body.appendChild(el);
  }
  el.textContent = msg;
  el.classList.add('show');
  if (toastTimer) clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.remove('show'), 2500);
}

/* ------------------------------------------------------------
 * Axios 实例 + 拦截器（简历第二条的前端核心）
 *
 * 1. 请求拦截器：统一注入 X-Auth-Token，业务代码不再手写 header；
 * 2. 响应拦截器：统一解析后端包装结构 {code, message, data}，
 *    业务侧拿到的直接就是 data，错误统一弹窗、统一 reject；
 * 3. Dashboard 字段拦截：既有 dashboard 接口返回明文全量字段，
 *    此处按当前登录角色在展示层剔除敏感字段后写入响应。
 *    注意：仅展示层控制（网络面板仍可见明文），安全方案以
 *    花名册接口的后端过滤+脱敏为准。
 * ------------------------------------------------------------ */

const instance = axios.create({
  baseURL: '/api',
  timeout: 15000,
});

// Dashboard 员工管理页各角色需要剔除的敏感字段
const DASHBOARD_STRIP_BY_ROLE: Record<Role, string[]> = {
  HR: [],
  EXECUTIVE: ['idCard', 'bankCard', 'salary'],
  DEPT_ADMIN: ['idCard', 'bankCard', 'salary'],
  ATTENDANCE: ['idCard', 'bankCard', 'salary', 'phone'],
  EMPLOYEE: ['idCard', 'bankCard', 'salary', 'phone'],
};

function readRole(): Role {
  const role = localStorage.getItem('role') as Role | null;
  return role ?? 'EMPLOYEE';
}

function stripDashboardRows(rows: EmployeeVO[]): EmployeeVO[] {
  const stripKeys = DASHBOARD_STRIP_BY_ROLE[readRole()];
  rows.forEach((row) => stripKeys.forEach((key) => delete (row as unknown as Record<string, unknown>)[key]));
  return rows;
}

// 请求拦截器：token 注入
instance.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers['X-Auth-Token'] = token;
  }
  return config;
});

// 响应拦截器：统一解析 + 错误兜底 + dashboard 字段拦截
// 注意：拦截器刻意返回解包后的 data 而非完整 response（所以下方包装层
// 把返回类型修正为业务数据 T），这里用类型断言对齐 Axios 的签名。
instance.interceptors.response.use(
  (response): AxiosResponse | Promise<never> => {
    const { code, message, data } = response.data as ApiResponse<unknown>;
    if (code !== 200) {
      toast(message || '请求失败');
      return Promise.reject(new Error(message || '请求失败'));
    }
    // Dashboard 员工管理页：按角色剔除响应体中的敏感字段后返回
    if (response.config.url?.startsWith('/dashboard/employees')) {
      return stripDashboardRows(data as EmployeeVO[]) as unknown as AxiosResponse;
    }
    return data as unknown as AxiosResponse;
  },
  (error: { response?: { status?: number; data?: ApiResponse<unknown> } }) => {
    const status = error.response?.status;
    const message = error.response?.data?.message;
    if (status === 401) {
      // 登录失效：清空本地态并跳回登录页
      localStorage.removeItem('token');
      localStorage.removeItem('role');
      if (!window.location.hash.includes('/login')) {
        window.location.hash = '#/login';
      }
    }
    toast(message || '网络异常，请稍后重试');
    return Promise.reject(error);
  }
);

/**
 * 类型安全包装：拦截器已把响应解包为 data 本体，
 * 因此这里的返回类型直接是业务数据 T 而非 AxiosResponse<T>。
 */
const request = {
  get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.get(url, config) as unknown as Promise<T>;
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return instance.post(url, data, config) as unknown as Promise<T>;
  },
};

export default request;
