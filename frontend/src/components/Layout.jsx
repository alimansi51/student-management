import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { getSession, logout } from '../api';

/** Sidebar + page area, shared by the admin and the student screens. */
export default function Layout({ title, links }) {
  const navigate = useNavigate();
  const session = getSession();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="layout">
      <aside className="sidebar">
        <h1 className="brand">{title}</h1>
        <p className="signed-in">Signed in as<br /><strong>{session?.name}</strong></p>
        <nav>
          {links.map((l) => (
            <NavLink key={l.to} to={l.to} end={l.end}>{l.label}</NavLink>
          ))}
        </nav>
        <button className="btn btn-light logout" onClick={handleLogout}>Log out</button>
      </aside>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
