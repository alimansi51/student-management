// Small wrapper around fetch that adds the logged-in user's credentials (HTTP Basic) to every request.

const KEY = 'sms_session';

/** { token, role: 'ADMIN' | 'STUDENT', name, username, mustChangePassword } or null when logged out */
export function getSession() {
  try {
    return JSON.parse(sessionStorage.getItem(KEY));
  } catch {
    return null;
  }
}

/** Where a logged-in user belongs. Students on a one-time password must change it first. */
export function landingPage(session) {
  if (session.role === 'ADMIN') return '/';
  return session.mustChangePassword ? '/change-password' : '/student';
}

export async function login(username, password) {
  // encode as UTF-8 so names with non-English letters work too
  const bytes = new TextEncoder().encode(`${username.trim()}:${password}`);
  const token = btoa(String.fromCharCode(...bytes));
  const res = await fetch('/api/auth/me', { headers: { Authorization: `Basic ${token}` } });
  if (!res.ok) throw new Error('Invalid username or password');
  const me = await res.json();
  const session = { token, ...me };
  sessionStorage.setItem(KEY, JSON.stringify(session));
  return session;
}

export function logout() {
  sessionStorage.removeItem(KEY);
}

export async function api(path, { method = 'GET', body } = {}) {
  const res = await fetch(`/api${path}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Basic ${getSession()?.token}`,
    },
    body: body ? JSON.stringify(body) : undefined,
  });

  if (res.status === 401) {
    logout();
    window.location.href = '/login';
    throw new Error('Session expired');
  }
  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const data = await res.json();
      if (data.message) message = data.message;
    } catch {
      // response had no JSON body
    }
    throw new Error(message);
  }

  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

/** Sends a file upload (FormData). Uses XMLHttpRequest because fetch can't report upload progress. */
export function upload(path, { method = 'POST', formData, onProgress }) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open(method, `/api${path}`);
    xhr.setRequestHeader('Authorization', `Basic ${getSession()?.token}`);
    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable) onProgress?.(Math.round((e.loaded * 100) / e.total));
    };
    xhr.onload = () => {
      if (xhr.status === 401) {
        logout();
        window.location.href = '/login';
        reject(new Error('Session expired'));
        return;
      }
      let data = null;
      try {
        data = JSON.parse(xhr.responseText);
      } catch {
        // response had no JSON body
      }
      if (xhr.status >= 200 && xhr.status < 300) resolve(data);
      else reject(new Error(data?.message || `Upload failed (${xhr.status})`));
    };
    xhr.onerror = () => reject(new Error('Upload failed. Is the backend running?'));
    xhr.send(formData);
  });
}
