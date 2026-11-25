class SessionStore {
  static KEY = 'sfb_session';

  static get() {
    const raw = localStorage.getItem(SessionStore.KEY);
    return raw ? JSON.parse(raw) : null;
  }

  static set(session) {
    localStorage.setItem(SessionStore.KEY, JSON.stringify(session));
  }

  static clear() {
    localStorage.removeItem(SessionStore.KEY);
  }
}

class ApiClient {
  async request(method, path, body) {
    const session = SessionStore.get();
    const headers = { 'Content-Type': 'application/json' };
    if (session && session.token) {
      headers['X-Auth-Token'] = session.token;
    }
    const response = await fetch(path, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });

    const data = await response.json().catch(() => ({}));

    // Only treat a 401 as "your session expired" when we actually sent a
    // token and it was rejected. A 401 with no token attached (e.g. a
    // failed /api/login or /api/register itself) is a normal app-level
    // error - let it fall through and show the real message instead of
    // silently redirecting and losing it.
    if (response.status === 401 && session && session.token) {
      SessionStore.clear();
      window.location.href = 'login.html';
      throw new Error('session expired');
    }

    if (!response.ok) {
      throw new Error(data.error || 'Request failed (' + response.status + ')');
    }
    return data;
  }

  get(path) {
    return this.request('GET', path);
  }

  post(path, body) {
    return this.request('POST', path, body === undefined ? {} : body);
  }
}

window.SessionStore = SessionStore;
window.apiClient = new ApiClient();
