import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth';
import { AppShell } from '../components/AppShell';

export function CoachDashboardPage() {
  const { token } = useAuth();
  const [metrics, setMetrics] = useState(null);
  const [members, setMembers] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const [m, roster] = await Promise.all([api.getMetrics(token), api.getMembers(token)]);
        if (!cancelled) {
          setMetrics(m);
          setMembers(roster);
        }
      } catch (err) {
        if (!cancelled) setError(err.message || 'Failed to load coach data');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [token]);

  return (
    <AppShell
      title="Coach dashboard"
      subtitle="Full roster access with completion flags and team metrics."
    >
      {loading && <p className="muted">Loading dashboard…</p>}
      {error && <p className="form-error" role="alert">{error}</p>}

      {!loading && !error && metrics && (
        <>
          <section className="metrics-strip" aria-label="Team metrics">
            <div>
              <span className="metric-label">Members</span>
              <strong className="metric-value">{metrics.totalMembers}</strong>
            </div>
            <div>
              <span className="metric-label">Completed</span>
              <strong className="metric-value">{metrics.completedProfiles}</strong>
            </div>
            <div>
              <span className="metric-label">Incomplete</span>
              <strong className="metric-value">{metrics.incompleteProfiles}</strong>
            </div>
            <div>
              <span className="metric-label">Completion rate</span>
              <strong className="metric-value">{metrics.completionRatePercent}%</strong>
            </div>
            <div>
              <span className="metric-label">Avg PB (sec)</span>
              <strong className="metric-value">
                {metrics.membersWithPersonalBest ? metrics.averagePersonalBestSeconds : '—'}
              </strong>
            </div>
          </section>

          <section className="roster-section">
            <h2>Roster</h2>
            {members.length === 0 ? (
              <p className="muted">No members have signed up yet.</p>
            ) : (
              <div className="table-wrap">
                <table className="roster-table">
                  <thead>
                    <tr>
                      <th>Member</th>
                      <th>Stroke</th>
                      <th>PB (s)</th>
                      <th>Status</th>
                      <th />
                    </tr>
                  </thead>
                  <tbody>
                    {members.map((m) => (
                      <tr key={m.userId}>
                        <td>
                          <div className="member-cell">
                            <strong>
                              {[m.firstName, m.lastName].filter(Boolean).join(' ') || m.username}
                            </strong>
                            <span>{m.email}</span>
                          </div>
                        </td>
                        <td>{m.strokeSpecialty || '—'}</td>
                        <td>{m.personalBestSeconds ?? '—'}</td>
                        <td>
                          <span className={`flag ${m.profileCompleted ? 'yes' : 'no'}`}>
                            {m.profileCompleted ? 'Complete' : 'Incomplete'}
                          </span>
                        </td>
                        <td>
                          <Link className="text-link" to={`/coach/members/${m.userId}`}>
                            View
                          </Link>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </>
      )}
    </AppShell>
  );
}

export function CoachMemberDetailPage() {
  const { userId } = useParams();
  const { token } = useAuth();
  const [profile, setProfile] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await api.getMember(token, userId);
        if (!cancelled) setProfile(data);
      } catch (err) {
        if (!cancelled) setError(err.message || 'Failed to load member');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [token, userId]);

  return (
    <AppShell
      title="Member detail"
      subtitle="Full profile visible to coaches only."
    >
      <p>
        <Link className="text-link" to="/coach">
          ← Back to roster
        </Link>
      </p>

      {loading && <p className="muted">Loading…</p>}
      {error && <p className="form-error" role="alert">{error}</p>}

      {profile && (
        <article className="detail-panel">
          <div className={`status-banner ${profile.profileCompleted ? 'complete' : 'incomplete'}`}>
            <strong>{profile.profileCompleted ? 'Profile complete' : 'Profile incomplete'}</strong>
            <span>
              {profile.firstName} {profile.lastName} (@{profile.username})
            </span>
          </div>

          <dl className="detail-grid">
            <div>
              <dt>Email</dt>
              <dd>{profile.email}</dd>
            </div>
            <div>
              <dt>Phone</dt>
              <dd>{profile.phone || '—'}</dd>
            </div>
            <div>
              <dt>Date of birth</dt>
              <dd>{profile.dateOfBirth || '—'}</dd>
            </div>
            <div>
              <dt>Address</dt>
              <dd>{profile.address || '—'}</dd>
            </div>
            <div>
              <dt>Emergency contact</dt>
              <dd>
                {profile.emergencyContactName || '—'}
                {profile.emergencyContactPhone ? ` · ${profile.emergencyContactPhone}` : ''}
              </dd>
            </div>
            <div>
              <dt>Stroke specialty</dt>
              <dd>{profile.strokeSpecialty || '—'}</dd>
            </div>
            <div>
              <dt>Personal best</dt>
              <dd>{profile.personalBestSeconds != null ? `${profile.personalBestSeconds}s` : '—'}</dd>
            </div>
            <div>
              <dt>Height / Weight</dt>
              <dd>
                {profile.heightCm != null ? `${profile.heightCm} cm` : '—'}
                {' / '}
                {profile.weightKg != null ? `${profile.weightKg} kg` : '—'}
              </dd>
            </div>
            <div className="full">
              <dt>Notes</dt>
              <dd>{profile.notes || '—'}</dd>
            </div>
          </dl>
        </article>
      )}
    </AppShell>
  );
}
