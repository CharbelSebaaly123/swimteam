import { useEffect, useState } from 'react';
import { api, fetchPhotoObjectUrl } from '../api';
import { useAuth } from '../auth';
import { AppShell } from '../components/AppShell';
import { resizeImageForUpload } from '../imageUpload';

export function CoachProfilePage() {
  const { token, patchUser } = useAuth();
  const [form, setForm] = useState({ firstName: '', lastName: '', nickname: '' });
  const [hasPhoto, setHasPhoto] = useState(false);
  const [photoUrl, setPhotoUrl] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [photoBusy, setPhotoBusy] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  useEffect(() => {
    let cancelled = false;
    let objectUrl;

    (async () => {
      try {
        const profile = await api.getCoachProfile(token);
        if (cancelled) return;
        setForm({
          firstName: profile.firstName || '',
          lastName: profile.lastName || '',
          nickname: profile.nickname || '',
        });
        setHasPhoto(Boolean(profile.hasPhoto));
        if (profile.hasPhoto) {
          objectUrl = await fetchPhotoObjectUrl(token, { coachSelf: true });
          if (!cancelled) setPhotoUrl(objectUrl);
        }
      } catch (err) {
        if (!cancelled) setError(err.message || 'Failed to load coach profile');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [token]);

  async function onSubmit(event) {
    event.preventDefault();
    setSaving(true);
    setError('');
    setMessage('');
    try {
      const saved = await api.updateCoachProfile(token, {
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        nickname: form.nickname.trim() || null,
      });
      setForm({
        firstName: saved.firstName || '',
        lastName: saved.lastName || '',
        nickname: saved.nickname || '',
      });
      patchUser({
        firstName: saved.firstName,
        lastName: saved.lastName,
        nickname: saved.nickname,
        hasPhoto: saved.hasPhoto,
      });
      setMessage('Coach profile saved.');
    } catch (err) {
      setError(err.message || 'Unable to save coach profile');
    } finally {
      setSaving(false);
    }
  }

  async function onPhotoSelected(event) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    setPhotoBusy(true);
    setError('');
    setMessage('');
    try {
      const compressed = await resizeImageForUpload(file);
      const result = await api.uploadCoachPhoto(token, compressed);
      if (photoUrl) URL.revokeObjectURL(photoUrl);
      const next = await fetchPhotoObjectUrl(token, { coachSelf: true });
      setPhotoUrl(next);
      setHasPhoto(true);
      patchUser({ hasPhoto: true });
      setMessage(
        result.message
          ? `${result.message} (${Math.round(result.sizeBytes / 1024)} KB)`
          : 'Photo uploaded.',
      );
    } catch (err) {
      setError(err.message || 'Unable to upload photo');
    } finally {
      setPhotoBusy(false);
    }
  }

  return (
    <AppShell
      title="Coach profile"
      subtitle="Your name, nickname, and photo are shown to the team."
    >
      {loading ? (
        <p className="muted">Loading…</p>
      ) : (
        <form className="profile-form" onSubmit={onSubmit}>
          <fieldset>
            <legend>Photo</legend>
            <div className="photo-upload">
              <div className="photo-preview">
                {photoUrl ? <img src={photoUrl} alt="Coach" /> : <span>No photo yet</span>}
              </div>
              <div className="photo-actions">
                <label className="btn secondary file-btn">
                  {photoBusy ? 'Working…' : hasPhoto ? 'Replace photo' : 'Upload photo'}
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/gif,image/webp"
                    onChange={onPhotoSelected}
                    disabled={photoBusy}
                    hidden
                  />
                </label>
              </div>
            </div>
          </fieldset>

          <fieldset>
            <legend>Identity</legend>
            <div className="grid-2">
              <label>
                First name *
                <input
                  value={form.firstName}
                  onChange={(e) => setForm((f) => ({ ...f, firstName: e.target.value }))}
                  required
                />
              </label>
              <label>
                Last name *
                <input
                  value={form.lastName}
                  onChange={(e) => setForm((f) => ({ ...f, lastName: e.target.value }))}
                  required
                />
              </label>
              <label className="full">
                Nickname
                <input
                  value={form.nickname}
                  onChange={(e) => setForm((f) => ({ ...f, nickname: e.target.value }))}
                  placeholder="Wahsh"
                />
              </label>
            </div>
          </fieldset>

          {error && (
            <p className="form-error" role="alert">
              {error}
            </p>
          )}
          {message && <p className="form-success">{message}</p>}

          <button className="btn primary" type="submit" disabled={saving}>
            {saving ? 'Saving…' : 'Save profile'}
          </button>
        </form>
      )}
    </AppShell>
  );
}
