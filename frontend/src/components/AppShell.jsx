import { Link, NavLink } from 'react-router-dom';
import { useAuth } from '../auth';

export function AppShell({ children, title, subtitle }) {
  const { user, logout, isCoach } = useAuth();

  return (
    <div className="app-shell">
      <header className="topbar">
        <Link to={isCoach ? '/coach' : '/profile'} className="brand">
          <span className="brand-mark" aria-hidden="true" />
          <span className="brand-text">SwimTeam</span>
        </Link>
        {user && (
          <nav className="topnav" aria-label="Primary">
            {isCoach ? (
              <NavLink to="/coach" end>
                Dashboard
              </NavLink>
            ) : (
              <NavLink to="/profile">My Profile</NavLink>
            )}
            <button type="button" className="linkish" onClick={logout}>
              Sign out
            </button>
          </nav>
        )}
      </header>

      <main className="page">
        {(title || subtitle) && (
          <div className="page-header">
            {title && <h1>{title}</h1>}
            {subtitle && <p>{subtitle}</p>}
          </div>
        )}
        {children}
      </main>

      <footer className="site-footer">
        <span>{user ? `Signed in as ${user.username}` : 'SwimTeam Profiles'}</span>
      </footer>
    </div>
  );
}
