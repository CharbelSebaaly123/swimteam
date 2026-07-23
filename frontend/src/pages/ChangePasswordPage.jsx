import { useState } from 'react';
import { api } from '../api';
import { useAuth } from '../auth';
import { AppShell } from '../components/AppShell';

export function ChangePasswordPage() {
  const { token } = useAuth();
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  async function onSubmit(event) {
    event.preventDefault();
    setError('');
    setMessage('');

    if (newPassword !== confirmPassword) {
      setError('New password and confirmation do not match');
      return;
    }

    setBusy(true);
    try {
      const result = await api.changePassword(token, { currentPassword, newPassword });
      setMessage(result.message || 'Password updated successfully');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      setError(err.message || 'Unable to change password');
    } finally {
      setBusy(false);
    }
  }

  return (
    <AppShell
      title="Change password"
      subtitle="Coaches and members can update their password with the current one for verification."
    >
      <form className="profile-form narrow" onSubmit={onSubmit}>
        <label>
          Current password
          <input
            type="password"
            autoComplete="current-password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            required
          />
        </label>
        <label>
          New password
          <input
            type="password"
            autoComplete="new-password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            minLength={6}
            required
          />
        </label>
        <label>
          Confirm new password
          <input
            type="password"
            autoComplete="new-password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            minLength={6}
            required
          />
        </label>

        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}
        {message && <p className="form-success">{message}</p>}

        <button className="btn primary" type="submit" disabled={busy}>
          {busy ? 'Updating…' : 'Update password'}
        </button>
      </form>
    </AppShell>
  );
}
