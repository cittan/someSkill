import { useEffect, useState } from 'react';
import { fetchDashboardEmployees } from '../api/api.js';

/**
 * Dashboard 员工管理页（仅前端实现）：
 * 后端接口返回明文全量字段，响应在 Axios 响应拦截器中按当前角色
 * 剔除敏感字段后才写入本页状态——即"前端用 axios 拦截响应体字段"的方案。
 *
 * 注意：F12 网络面板仍能看到明文原始响应，此方案仅为展示层控制；
 * 真正的数据安全以花名册（后端过滤 + 脱敏）为准。
 */
const COLUMNS = [
  { key: 'empNo', label: '工号' },
  { key: 'name', label: '姓名' },
  { key: 'deptName', label: '部门' },
  { key: 'position', label: '职位' },
  { key: 'status', label: '状态' },
  { key: 'phone', label: '手机号' },
  { key: 'idCard', label: '身份证' },
  { key: 'bankCard', label: '银行卡' },
  { key: 'salary', label: '薪资' },
];

export default function Dashboard() {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDashboardEmployees()
      .then(setRows) // 拿到的数据已在拦截器中剔除敏感字段
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="page">
      <p className="hint">
        本页数据来自既有 dashboard 接口（明文返回），敏感字段已在 Axios 响应拦截器中按角色剔除。
      </p>
      <table>
        <thead>
          <tr>
            {COLUMNS.map((c) => (
              <th key={c.key}>{c.label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <tr><td colSpan={COLUMNS.length}>加载中...</td></tr>
          ) : rows.length === 0 ? (
            <tr><td colSpan={COLUMNS.length}>暂无数据</td></tr>
          ) : (
            rows.map((row) => (
              <tr key={row.id}>
                {COLUMNS.map((c) => (
                  <td key={c.key}>{row[c.key] ?? '-'}</td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
