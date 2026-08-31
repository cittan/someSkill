import { useEffect, useState } from 'react';
import { fetchRoster } from '../api/api';
import { useAuth } from '../context/AuthContext';
import type { EmployeeVO, Role } from '../types';

/* ------------------------------------------------------------
 * 角色驱动的列渲染：与后端 RoleEnum 矩阵保持一致。
 * 双保险：后端 HIDDEN 字段根本不返回，前端白名单再过滤一次。
 * ------------------------------------------------------------ */
type ColumnKey = keyof EmployeeVO;

const ALL_COLUMNS: Array<{ key: ColumnKey; label: string }> = [
  { key: 'empNo', label: '工号' },
  { key: 'name', label: '姓名' },
  { key: 'deptName', label: '部门' },
  { key: 'position', label: '职位' },
  { key: 'status', label: '状态' },
  { key: 'hireDate', label: '入职日期' },
  { key: 'email', label: '邮箱' },
  { key: 'attendanceNo', label: '考勤号' },
  { key: 'phone', label: '手机号' },
  { key: 'idCard', label: '身份证' },
  { key: 'bankCard', label: '银行卡' },
  { key: 'salary', label: '薪资' },
];

const ROLE_VISIBLE: Record<Role, ColumnKey[]> = {
  HR: ALL_COLUMNS.map((c) => c.key),
  EXECUTIVE: ['empNo', 'name', 'deptName', 'position', 'status', 'hireDate', 'email', 'attendanceNo', 'phone'],
  DEPT_ADMIN: ['empNo', 'name', 'deptName', 'position', 'status', 'hireDate', 'email', 'attendanceNo', 'phone', 'idCard', 'bankCard'],
  ATTENDANCE: ['empNo', 'name', 'deptName', 'position', 'status', 'hireDate', 'email', 'attendanceNo', 'phone'],
  EMPLOYEE: ['empNo', 'name', 'deptName', 'position', 'status', 'hireDate', 'email', 'attendanceNo', 'phone'],
};

const DEPTS: Array<{ id: number | null; name: string }> = [
  { id: null, name: '全部部门' },
  { id: 1, name: '技术部' },
  { id: 2, name: '产品部' },
  { id: 3, name: '市场部' },
];

const PAGE_SIZE = 20;

export default function Roster() {
  const { role } = useAuth();
  const [rows, setRows] = useState<EmployeeVO[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [deptId, setDeptId] = useState<number | null>(null);
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);

  const visibleColumns = ALL_COLUMNS.filter((c) =>
    (ROLE_VISIBLE[role ?? 'EMPLOYEE']).includes(c.key)
  );

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    fetchRoster({
      deptId: deptId ?? undefined,
      keyword: keyword || undefined,
      page,
      size: PAGE_SIZE,
    })
      .then((data) => {
        if (cancelled) return;
        setRows(data.content ?? []);
        setTotal(data.totalElements ?? 0);
      })
      .catch(() => {})
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [deptId, keyword, page]);

  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));

  return (
    <div className="page">
      <div className="toolbar">
        <select value={deptId ?? ''} onChange={(e) => { setPage(0); setDeptId(e.target.value ? Number(e.target.value) : null); }}>
          {DEPTS.map((d) => (
            <option key={d.name} value={d.id ?? ''}>{d.name}</option>
          ))}
        </select>
        <input
          placeholder="搜索姓名 / 工号"
          value={keyword}
          onChange={(e) => { setPage(0); setKeyword(e.target.value); }}
        />
        <span className="hint">
          当前期限：{role} · 行级/字段级权限已由后端处理，隐藏字段不会出现在响应中
        </span>
      </div>

      <table>
        <thead>
          <tr>
            {visibleColumns.map((c) => (
              <th key={c.key}>{c.label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <tr><td colSpan={visibleColumns.length}>加载中...</td></tr>
          ) : rows.length === 0 ? (
            <tr><td colSpan={visibleColumns.length}>暂无数据</td></tr>
          ) : (
            rows.map((row) => (
              <tr key={row.id}>
                {visibleColumns.map((c) => (
                  <td key={c.key}>{row[c.key] ?? '-'}</td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>

      <div className="pager">
        <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>上一页</button>
        <span>{page + 1} / {totalPages}</span>
        <button disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>下一页</button>
      </div>
    </div>
  );
}
