import { createContext, useCallback, useContext, useMemo, useState } from 'react';

/**
 * 登录态上下文：token + 角色类型。
 * 后端登录接口返回角色枚举（HR/EXECUTIVE/DEPT_ADMIN/ATTENDANCE/EMPLOYEE），
 * 前端所有按角色渲染的组件从这里读取。
 */
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => ({
    token: localStorage.getItem('token'),
    role: localStorage.getItem('role'),
    username: localStorage.getItem('username'),
  }));

  const login = useCallback(({ token, role, username }) => {
    localStorage.setItem('token', token);
    localStorage.setItem('role', role);
    localStorage.setItem('username', username);
    setAuth({ token, role, username });
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('username');
    setAuth({ token: null, role: null, username: null });
  }, []);

  const value = useMemo(() => ({ ...auth, login, logout }), [auth, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
