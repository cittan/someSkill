import { createContext, useCallback, useContext, useMemo, useState, ReactNode } from 'react';
import type { Role } from '../types';

/** 登录态：token + 角色 + 用户名（role 由后端登录接口下发） */
export interface AuthState {
  token: string | null;
  role: Role | null;
  username: string | null;
}

export interface AuthContextValue extends AuthState {
  login: (vo: { token: string; role: Role; username: string }) => void;
  logout: () => void;
}

/**
 * 登录态上下文：token + 角色类型。
 * 后端登录接口返回角色枚举（HR/EXECUTIVE/DEPT_ADMIN/ATTENDANCE/EMPLOYEE），
 * 前端所有按角色渲染的组件从这里读取。
 */
const AuthContext = createContext<AuthContextValue | null>(null);

function readStored(): AuthState {
  return {
    token: localStorage.getItem('token'),
    role: (localStorage.getItem('role') as Role | null) ?? null,
    username: localStorage.getItem('username'),
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthState>(readStored);

  const login = useCallback((vo: { token: string; role: Role; username: string }) => {
    localStorage.setItem('token', vo.token);
    localStorage.setItem('role', vo.role);
    localStorage.setItem('username', vo.username);
    setAuth({ token: vo.token, role: vo.role, username: vo.username });
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('username');
    setAuth({ token: null, role: null, username: null });
  }, []);

  const value = useMemo<AuthContextValue>(() => ({ ...auth, login, logout }), [auth, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth 必须在 AuthProvider 内使用');
  }
  return ctx;
}
