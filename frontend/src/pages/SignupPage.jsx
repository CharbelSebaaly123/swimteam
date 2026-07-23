import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth';
import { AppShell } from '../components/AppShell';

export function SignupPage() {
  const { user, signup, isCoach } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
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
      await signup(username.trim(), email.trim(), password);
      navigate('/profile');
    } catch (err) {
      setError(err.message || 'Unable to sign up');
    } finally {
      setBusy(false);
    }
  }

  return (
    <AppShell>
      <section className="auth-layout">
        <div className="auth-panel">
          <p className="eyebrow">Join the team</p>
          <h1>Member sign up</h1>
          <p className="lede">Create your account, then complete your athlete profile.</p>

          <form className="stack-form" onSubmit={onSubmit}>
            <label>
              Username
              <input
                autoComplete="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                minLength={3}
                required
              />
            </label>
            <label>
              Email
              <input
                type="email"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </label>
            <label>
              Password
              <input
                type="password"
                autoComplete="new-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                minLength={6}
                required
              />
            </label>
            {error && <p className="form-error" role="alert">{error}</p>}
            <button className="btn primary" type="submit" disabled={busy}>
              {busy ? 'Creating…' : 'Create account'}
            </button>
          </form>

          <p className="auth-switch">
            Already registered? <Link to="/login">Sign in</Link>
          </p>
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
