import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login } from '../api/api.js';
import { useAuth } from '../context/AuthContext.jsx';

/** 演示账号，密码统一 123456 */
const DEMO_ACCOUNTS = [
  { username: 'hr', label: 'HR（全部门·全字段明文）' },
  { username: 'ceo', label: '高层领导（全部门·部分字段）' },
  { username: 'admin1', label: '部门管理员（技术部·部分字段）' },
  { username: 'attn1', label: '考勤员（技术部·考勤号明文）' },
  { username: 'emp2', label: '普通员工（产品部·基本字段）' },
];

export default function Login() {
  const [username, setUsername] = useState('hr');
  const [password, setPassword] = useState('123456');
  const [loading, setLoading] = useState(false);
  const { login: doLogin } = useAuth();
  const navigate = useNavigate();

  const onSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const data = await login({ username, password });
      doLogin(data);
      navigate('/roster');
    } catch {
      // 错误已由响应拦截器统一弹窗，这里只需吞掉
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <form className="login-card" onSubmit={onSubmit}>
        <h2>OA 系统登录</h2>
        <label>
          演示账号
          <select value={username} onChange={(e) => setUsername(e.target.value)}>
            {DEMO_ACCOUNTS.map((a) => (
              <option key={a.username} value={a.username}>
                {a.label}
              </option>
            ))}
          </select>
        </label>
        <label>
          密码
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
        </label>
        <button type="submit" disabled={loading}>
          {loading ? '登录中...' : '登录'}
        </button>
      </form>
    </div>
  );
}
