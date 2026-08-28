import axios from 'axios';

/* ------------------------------------------------------------
 * 极简全局提示（生产项目一般接 antd message / react-hot-toast）
 * ------------------------------------------------------------ */
let toastTimer = null;

function toast(msg) {
  let el = document.querySelector('.toast');
  if (!el) {
    el = document.createElement('div');
    el.className = 'toast';
    document.body.appendChild(el);
  }
  el.textContent = msg;
  el.classList.add('show');
  clearTimeout(toastTimer);
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

// Dashboard 员工管理页各角色需要剔除的敏感字段
const DASHBOARD_STRIP_BY_ROLE = {
  HR: [],
  EXECUTIVE: ['idCard', 'bankCard', 'salary'],
  DEPT_ADMIN: ['idCard', 'bankCard', 'salary'],
  ATTENDANCE: ['idCard', 'bankCard', 'salary', 'phone'],
  EMPLOYEE: ['idCard', 'bankCard', 'salary', 'phone'],
};

function stripDashboardRows(rows) {
  const role = localStorage.getItem('role') || 'EMPLOYEE';
  const stripKeys = DASHBOARD_STRIP_BY_ROLE[role] || ['idCard', 'bankCard', 'salary', 'phone'];
  const list = Array.isArray(rows) ? rows : (rows?.content ?? []);
  list.forEach((row) => stripKeys.forEach((key) => delete row[key]));
  return rows;
}

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
});

// 请求拦截器：token 注入
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers['X-Auth-Token'] = token;
  }
  return config;
});

// 响应拦截器：统一解析 + 错误兜底 + dashboard 字段拦截
request.interceptors.response.use(
  (response) => {
    const { code, message, data } = response.data;
    if (code !== 200) {
      toast(message || '请求失败');
      return Promise.reject(new Error(message || '请求失败'));
    }
    // Dashboard 员工管理页：按角色剔除响应体中的敏感字段后返回
    if (response.config.url.startsWith('/dashboard/employees')) {
      return stripDashboardRows(data);
    }
    return data;
  },
  (error) => {
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

export default request;
