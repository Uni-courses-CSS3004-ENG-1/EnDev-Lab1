// Every HTTP call the page makes, and the only place the access token lives.
//
// Two tokens keep a person signed in:
//  - the access token (15 minutes) lives in the `accessToken` variable of this module only: never in
//    localStorage, sessionStorage, a readable cookie, the URL or the page text.
//  - the refresh token (14 days) is an httpOnly cookie the server sets on /api/v1/auth. JavaScript
//    cannot read it. On page load, and a minute before the access token runs out, POST /refresh
//    trades it for a new access token. So a reload does not log anyone out.
// dropToken() is the one place the access token is forgotten.

import type { Account, ApiError, ApiResult, Me, TokenResponse } from './types';

let accessToken: string | null = null;
let refreshTimer: number | undefined;

// ---------- the session the React components read (never the token itself) ----------

export type Session = {
  status: 'loading' | 'signedOut' | 'signedIn';
  me: Me | null;
  account: Account | null;
};

let session: Session = { status: 'loading', me: null, account: null };
const listeners = new Set<() => void>();

function setSession(next: Session) {
  session = next;
  listeners.forEach((listener) => listener());
}

export function subscribeSession(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function getSession() {
  return session;
}

// a session-ended message for the page to show once, as a toast
let endedMessage: string | null = null;
export function takeEndedMessage() {
  const message = endedMessage;
  endedMessage = null;
  return message;
}

// ---------- the call ----------

function newCorrelationId() {
  const random = crypto.randomUUID ? crypto.randomUUID() : String(Date.now()) + Math.random().toString(16).slice(2);
  return 'web-' + random.slice(0, 8);
}

export function isError(data: unknown): data is ApiError {
  return !!data && typeof (data as ApiError).code === 'string' && typeof (data as ApiError).message === 'string';
}

const AUTH_PATHS = '/api/v1/auth/';

// Every call: a correlation id for the server logs, JSON, and the Bearer header while signed in.
// The /auth paths never get the Bearer header: they work from the password or the refresh cookie.
export async function call<T>(method: string, path: string, body?: unknown, retry = true): Promise<ApiResult<T>> {
  const correlationId = newCorrelationId();
  const headers: Record<string, string> = { 'Accept': 'application/json', 'X-Correlation-Id': correlationId };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (accessToken && !path.startsWith(AUTH_PATHS)) headers['Authorization'] = 'Bearer ' + accessToken;

  let response: Response;
  try {
    response = await fetch(path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      credentials: 'same-origin',
      cache: 'no-store',
    });
  } catch {
    return { status: 0, data: null, correlationId };
  }

  const text = await response.text();
  let data: unknown = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = null; }

  // A signed-in call answered 401: the access token ran out. Try the refresh cookie once, then retry.
  if (response.status === 401 && accessToken && !path.startsWith(AUTH_PATHS)) {
    if (retry && await refreshAccessToken()) return call<T>(method, path, body, false);
    dropToken('Your session has ended. Please log in again.');
  }
  return { status: response.status, data, correlationId } as ApiResult<T>;
}

// ---------- log in, sign up, stay signed in, log out ----------

export async function login(username: string, password: string) {
  forgetToken();
  const res = await call<TokenResponse>('POST', '/api/v1/auth/login', { username, password });
  return res.status === 200 ? startSession(res) : res;
}

export async function register(form: Record<string, unknown>) {
  forgetToken();
  const res = await call<TokenResponse>('POST', '/api/v1/auth/register', form);
  return res.status === 201 ? startSession(res) : res;
}

// On page load: if the refresh cookie is still good, the person is signed in without typing anything.
export async function restoreSession() {
  const res = await call<TokenResponse>('POST', '/api/v1/auth/refresh');
  if (res.status === 200) await startSession(res);
  else setSession({ status: 'signedOut', me: null, account: null });
}

export async function logout() {
  await call('POST', '/api/v1/auth/logout');
  dropToken(null);
}

// Reload the profile, e.g. after something changed on the server.
export async function reloadAccount() {
  const account = await call<Account>('GET', '/api/v1/account');
  if (account.status === 200 && session.status === 'signedIn') {
    setSession({ ...session, account: account.data as Account });
  }
}

// before a new login: an old token must never travel with the next person's password
function forgetToken() {
  accessToken = null;
}

// The role on the page comes from GET /api/me, that is from the signed token, never from a form.
async function startSession(tokenRes: ApiResult<TokenResponse>): Promise<ApiResult<unknown>> {
  useToken(tokenRes.data as TokenResponse);
  const who = await call<Me>('GET', '/api/me');
  if (who.status !== 200) {
    dropToken(null);
    return who;
  }
  const account = await call<Account>('GET', '/api/v1/account');
  setSession({
    status: 'signedIn',
    me: who.data as Me,
    account: account.status === 200 ? (account.data as Account) : null,
  });
  return tokenRes;
}

// keep the new access token, and renew it a minute before it runs out
function useToken(token: TokenResponse) {
  accessToken = token.accessToken;
  window.clearTimeout(refreshTimer);
  refreshTimer = window.setTimeout(async () => {
    if (!(await refreshAccessToken())) dropToken('Your session has ended. Please log in again.');
  }, Math.max(10, token.expiresIn - 60) * 1000);
}

async function refreshAccessToken() {
  const res = await call<TokenResponse>('POST', '/api/v1/auth/refresh');
  if (res.status !== 200) return false;
  useToken(res.data as TokenResponse);
  return true;
}

// The one place the access token is forgotten: log out, or a session the server no longer accepts.
export function dropToken(message: string | null) {
  accessToken = null;
  window.clearTimeout(refreshTimer);
  endedMessage = message;
  setSession({ status: 'signedOut', me: null, account: null });
}
