import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth';
import { AppShell } from '../components/AppShell';

export function LoginPage() {
  const { user, login, isCoach } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  if (user) {
    return <Navigate to={isCoach ? '/coach' : '/profile'} replace />;
  }

  async function onSubmit(event) {
    event.preventDefault();
    setError('');
    setBusy(true);
    try {
      const data = await login(username.trim(), password);
      navigate(data.role === 'COACH' ? '/coach' : '/profile');
    } catch (err) {
      setError(err.message || 'Unable to sign in');
    } finally {
      setBusy(false);
    }
  }

  return (
    <AppShell>
      <section className="auth-layout">
        <div className="auth-panel">
          <p className="eyebrow">Team access</p>
          <h1>Sign in to SwimTeam</h1>
          <p className="lede">Members manage their profile. Coaches see the full roster.</p>

          <form className="stack-form" onSubmit={onSubmit}>
            <label>
              Username
              <input
                autoComplete="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
              />
            </label>
            <label>
              Password
              <input
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </label>
            {error && <p className="form-error" role="alert">{error}</p>}
            <button className="btn primary" type="submit" disabled={busy}>
              {busy ? 'Signing in…' : 'Sign in'}
            </button>
          </form>

          <p className="auth-switch">
            New member? <Link to="/signup">Create an account</Link>
          </p>
          <p className="hint">Coach demo: admin / admin123 (Labib “Wahsh” Waked)</p>
        </div>
        <div className="auth-visual" aria-hidden="true">
          <div className="wave wave-a" />
          <div className="wave wave-b" />
          <div className="wave wave-c" />
        </div>
      </section>
    </AppShell>
  );
}
