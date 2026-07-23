import { useEffect, useState } from 'react';
import { E164_PHONE_PATTERN, api, fetchPhotoObjectUrl } from '../api';
import { useAuth } from '../auth';
import { AppShell } from '../components/AppShell';
import { resizeImageForUpload } from '../imageUpload';

const EMPTY = {
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  dateOfBirth: '',
  address: '',
  emergencyContactName: '',
  emergencyContactPhone: '',
  strokeSpecialty: '',
  personalBestSeconds: '',
  heightCm: '',
  weightKg: '',
  notes: '',
};

function toForm(profile) {
  if (!profile) return { ...EMPTY };
  return {
    firstName: profile.firstName || '',
    lastName: profile.lastName || '',
    email: profile.email || '',
    phone: profile.phone || '',
    dateOfBirth: profile.dateOfBirth || '',
    address: profile.address || '',
    emergencyContactName: profile.emergencyContactName || '',
    emergencyContactPhone: profile.emergencyContactPhone || '',
    strokeSpecialty: profile.strokeSpecialty || '',
    personalBestSeconds: profile.personalBestSeconds ?? '',
    heightCm: profile.heightCm ?? '',
    weightKg: profile.weightKg ?? '',
    notes: profile.notes || '',
  };
}

function toPayload(form) {
  return {
    firstName: form.firstName.trim(),
    lastName: form.lastName.trim(),
    email: form.email.trim(),
    phone: form.phone.trim(),
    dateOfBirth: form.dateOfBirth,
    address: form.address.trim() || null,
    emergencyContactName: form.emergencyContactName.trim() || null,
    emergencyContactPhone: form.emergencyContactPhone.trim() || null,
    strokeSpecialty: form.strokeSpecialty.trim() || null,
    personalBestSeconds: form.personalBestSeconds === '' ? null : Number(form.personalBestSeconds),
    heightCm: form.heightCm === '' ? null : Number(form.heightCm),
    weightKg: form.weightKg === '' ? null : Number(form.weightKg),
    notes: form.notes.trim() || null,
  };
}

export function ProfilePage() {
  const { token } = useAuth();
  const [form, setForm] = useState(EMPTY);
  const [completed, setCompleted] = useState(false);
  const [hasPhoto, setHasPhoto] = useState(false);
  const [photoUrl, setPhotoUrl] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [photoBusy, setPhotoBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;
    let objectUrl;

    (async () => {
      try {
        const profile = await api.getMyProfile(token);
        if (cancelled) return;
        setForm(toForm(profile));
        setCompleted(Boolean(profile.profileCompleted));
        setHasPhoto(Boolean(profile.hasPhoto));
        if (profile.hasPhoto) {
          objectUrl = await fetchPhotoObjectUrl(token);
          if (!cancelled) setPhotoUrl(objectUrl);
        }
      } catch (err) {
        if (!cancelled) setError(err.message || 'Failed to load profile');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [token]);

  function updateField(name, value) {
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  async function refreshPhotoPreview() {
    if (photoUrl) {
      URL.revokeObjectURL(photoUrl);
      setPhotoUrl(null);
    }
    const next = await fetchPhotoObjectUrl(token);
    setPhotoUrl(next);
    setHasPhoto(Boolean(next));
  }

  async function onPhotoSelected(event) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;

    setPhotoBusy(true);
    setError('');
    setMessage('');
    try {
      const compressed = await resizeImageForUpload(file, { maxDimension: 800, quality: 0.75 });
      const result = await api.uploadMyPhoto(token, compressed);
      await refreshPhotoPreview();
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

  async function onRemovePhoto() {
    setPhotoBusy(true);
    setError('');
    setMessage('');
    try {
      await api.deleteMyPhoto(token);
      if (photoUrl) URL.revokeObjectURL(photoUrl);
      setPhotoUrl(null);
      setHasPhoto(false);
      setMessage('Photo removed.');
    } catch (err) {
      setError(err.message || 'Unable to remove photo');
    } finally {
      setPhotoBusy(false);
    }
  }

  async function onSubmit(event) {
    event.preventDefault();
    setSaving(true);
    setError('');
    setMessage('');

    if (!E164_PHONE_PATTERN.test(form.phone.trim())) {
      setError('Phone must be international E.164 format, e.g. +14155552671');
      setSaving(false);
      return;
    }
    if (
      form.emergencyContactPhone.trim() &&
      !E164_PHONE_PATTERN.test(form.emergencyContactPhone.trim())
    ) {
      setError('Emergency contact phone must be international E.164 format, e.g. +14155552671');
      setSaving(false);
      return;
    }
    if (!form.dateOfBirth) {
      setError('Date of birth is required');
      setSaving(false);
      return;
    }

    try {
      const saved = await api.updateMyProfile(token, toPayload(form));
      setForm(toForm(saved));
      setCompleted(Boolean(saved.profileCompleted));
      setHasPhoto(Boolean(saved.hasPhoto));
      setMessage(
        saved.profileCompleted
          ? 'Profile saved and marked complete.'
          : 'Profile saved. Fill required fields to complete.',
      );
    } catch (err) {
      setError(err.message || 'Unable to save profile');
    } finally {
      setSaving(false);
    }
  }

  return (
    <AppShell
      title="My profile"
      subtitle="Email, international phone, and date of birth are required. Address is optional. Photos are resized and compressed on upload."
    >
      <div className={`status-banner ${completed ? 'complete' : 'incomplete'}`}>
        <strong>{completed ? 'Profile complete' : 'Profile incomplete'}</strong>
        <span>
          {completed
            ? 'Your coach can see a completed flag on the roster.'
            : 'Required: name, email, international phone, date of birth, emergency contact, stroke specialty.'}
        </span>
      </div>

      {loading ? (
        <p className="muted">Loading profile…</p>
      ) : (
        <form className="profile-form" onSubmit={onSubmit}>
          <fieldset>
            <legend>Profile photo</legend>
            <div className="photo-upload">
              <div className="photo-preview" aria-hidden={!hasPhoto}>
                {photoUrl ? (
                  <img src={photoUrl} alt="Your profile" />
                ) : (
                  <span>No photo yet</span>
                )}
              </div>
              <div className="photo-actions">
                <p className="muted">
                  Images are resized to max 800px and converted to compressed JPEG in the browser,
                  then checked again on the server.
                </p>
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
                {hasPhoto && (
                  <button
                    type="button"
                    className="btn ghost"
                    onClick={onRemovePhoto}
                    disabled={photoBusy}
                  >
                    Remove photo
                  </button>
                )}
              </div>
            </div>
          </fieldset>

          <fieldset>
            <legend>Personal information</legend>
            <div className="grid-2">
              <label>
                First name *
                <input
                  value={form.firstName}
                  onChange={(e) => updateField('firstName', e.target.value)}
                  required
                />
              </label>
              <label>
                Last name *
                <input
                  value={form.lastName}
                  onChange={(e) => updateField('lastName', e.target.value)}
                  required
                />
              </label>
              <label>
                Email *
                <input
                  type="email"
                  value={form.email}
                  onChange={(e) => updateField('email', e.target.value)}
                  required
                />
              </label>
              <label>
                Phone (international) *
                <input
                  value={form.phone}
                  onChange={(e) => updateField('phone', e.target.value)}
                  placeholder="+14155552671"
                  pattern="^\+[1-9]\d{6,14}$"
                  title="E.164 format, e.g. +14155552671"
                  required
                />
              </label>
              <label>
                Date of birth *
                <input
                  type="date"
                  value={form.dateOfBirth}
                  onChange={(e) => updateField('dateOfBirth', e.target.value)}
                  required
                />
              </label>
              <label className="full">
                Address (optional)
                <input value={form.address} onChange={(e) => updateField('address', e.target.value)} />
              </label>
            </div>
          </fieldset>

          <fieldset>
            <legend>Emergency contact</legend>
            <div className="grid-2">
              <label>
                Contact name *
                <input
                  value={form.emergencyContactName}
                  onChange={(e) => updateField('emergencyContactName', e.target.value)}
                />
              </label>
              <label>
                Contact phone (international) *
                <input
                  value={form.emergencyContactPhone}
                  onChange={(e) => updateField('emergencyContactPhone', e.target.value)}
                  placeholder="+14155559876"
                  pattern="^\+[1-9]\d{6,14}$"
                  title="E.164 format, e.g. +14155559876"
                />
              </label>
            </div>
          </fieldset>

          <fieldset>
            <legend>Swim metrics</legend>
            <div className="grid-2">
              <label>
                Stroke specialty *
                <select
                  value={form.strokeSpecialty}
                  onChange={(e) => updateField('strokeSpecialty', e.target.value)}
                >
                  <option value="">Select stroke</option>
                  <option>Freestyle</option>
                  <option>Backstroke</option>
                  <option>Breaststroke</option>
                  <option>Butterfly</option>
                  <option>Individual Medley</option>
                </select>
              </label>
              <label>
                Personal best (seconds)
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  value={form.personalBestSeconds}
                  onChange={(e) => updateField('personalBestSeconds', e.target.value)}
                />
              </label>
              <label>
                Height (cm)
                <input
                  type="number"
                  min="1"
                  value={form.heightCm}
                  onChange={(e) => updateField('heightCm', e.target.value)}
                />
              </label>
              <label>
                Weight (kg)
                <input
                  type="number"
                  step="0.1"
                  min="1"
                  value={form.weightKg}
                  onChange={(e) => updateField('weightKg', e.target.value)}
                />
              </label>
              <label className="full">
                Notes
                <textarea
                  rows={3}
                  value={form.notes}
                  onChange={(e) => updateField('notes', e.target.value)}
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
