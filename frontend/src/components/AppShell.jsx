import { Link, NavLink } from 'react-router-dom';
import { useAuth } from '../auth';

function displayLabel(user) {
  if (!user) return '';
  const full = [user.firstName, user.lastName].filter(Boolean).join(' ').trim();
  if (full && user.nickname) return `${full} (“${user.nickname}”)`;
  if (full) return full;
  if (user.nickname) return user.nickname;
  return user.username;
}

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
              <>
                <NavLink to="/coach" end>
                  Dashboard
                </NavLink>
                <NavLink to="/coach/profile">My Profile</NavLink>
              </>
            ) : (
              <NavLink to="/profile">My Profile</NavLink>
            )}
            <NavLink to="/change-password">Password</NavLink>
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
        <span>{user ? `Signed in as ${displayLabel(user)}` : 'SwimTeam Profiles'}</span>
      </footer>
    </div>
  );
}
