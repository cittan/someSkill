import { Navigate, NavLink, Route, Routes } from 'react-router-dom';
import { useAuth } from './context/AuthContext.jsx';
import Login from './pages/Login.jsx';
import Roster from './pages/Roster.jsx';
import Dashboard from './pages/Dashboard.jsx';

function Guard({ children }) {
  const { token } = useAuth();
  return token ? children : <Navigate to="/login" replace />;
}

export default function App() {
  const { role, username, logout } = useAuth();

  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        path="/"
        element={
          <Guard>
            <div className="layout">
              <header className="topbar">
                <span className="brand">OA 员工花名册</span>
                <nav>
                  <NavLink to="/roster">花名册</NavLink>
                  <NavLink to="/dashboard">Dashboard 员工管理</NavLink>
                </nav>
                <span className="user">
                  {username}（{role}）
                  <button onClick={logout}>退出</button>
                </span>
              </header>
              <main>
                <Routes>
                  <Route index element={<Navigate to="/roster" replace />} />
                  <Route path="/roster" element={<Roster />} />
                  <Route path="/dashboard" element={<Dashboard />} />
                </Routes>
              </main>
            </div>
          </Guard>
        }
      />
    </Routes>
  );
}
