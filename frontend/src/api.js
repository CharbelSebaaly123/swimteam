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

async function requestMultipart(path, { method = 'POST', formData, token } = {}) {
  const headers = {};
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body: formData,
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

export function photoUrlForMember(token, userId) {
  // Used with fetch + blob for authenticated <img> display
  return {
    path: userId == null ? '/api/profiles/me/photo' : `/api/coach/members/${userId}/photo`,
    token,
  };
}

export async function fetchPhotoObjectUrl(token, userId) {
  const path = userId == null ? '/api/profiles/me/photo' : `/api/coach/members/${userId}/photo`;
  const response = await fetch(`${API_BASE}${path}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (response.status === 404) {
    return null;
  }
  if (!response.ok) {
    const text = await response.text();
    let message = 'Failed to load photo';
    try {
      message = JSON.parse(text)?.message || message;
    } catch {
      /* ignore */
    }
    throw new ApiError(message, response.status);
  }
  const blob = await response.blob();
  return URL.createObjectURL(blob);
}

export const api = {
  signup: (payload) => request('/api/auth/signup', { method: 'POST', body: payload }),
  login: (payload) => request('/api/auth/login', { method: 'POST', body: payload }),
  changePassword: (token, payload) =>
    request('/api/auth/change-password', { method: 'POST', body: payload, token }),
  getMyProfile: (token) => request('/api/profiles/me', { token }),
  updateMyProfile: (token, payload) =>
    request('/api/profiles/me', { method: 'PUT', body: payload, token }),
  uploadMyPhoto: (token, file) => {
    const formData = new FormData();
    formData.append('file', file);
    return requestMultipart('/api/profiles/me/photo', { formData, token });
  },
  deleteMyPhoto: (token) => request('/api/profiles/me/photo', { method: 'DELETE', token }),
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
