const API_BASE = import.meta.env.VITE_API_URL || '';

export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

async function request(path, { method = 'GET', body, token } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });

  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = { message: text };
    }
  }

  if (!response.ok) {
    throw new ApiError(data?.message || 'Request failed', response.status);
  }
  return data;
}

export const api = {
  signup: (payload) => request('/api/auth/signup', { method: 'POST', body: payload }),
  login: (payload) => request('/api/auth/login', { method: 'POST', body: payload }),
  changePassword: (token, payload) =>
    request('/api/auth/change-password', { method: 'POST', body: payload, token }),
  getMyProfile: (token) => request('/api/profiles/me', { token }),
  updateMyProfile: (token, payload) =>
    request('/api/profiles/me', { method: 'PUT', body: payload, token }),
  getMembers: (token, { sort = 'name', direction = 'asc' } = {}) =>
    request(`/api/coach/members?sort=${encodeURIComponent(sort)}&direction=${encodeURIComponent(direction)}`, {
      token,
    }),
  getMember: (token, userId) => request(`/api/coach/members/${userId}`, { token }),
  getMetrics: (token) => request('/api/coach/metrics', { token }),
  getAgeGroupReport: (token) => request('/api/coach/reports/age-groups', { token }),
};

/** E.164 international phone, e.g. +14155552671 */
export const E164_PHONE_PATTERN = /^\+[1-9]\d{6,14}$/;
