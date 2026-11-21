// The auth page for the tutoring marketplace. One script, no library.
// The access token lives only in the `accessToken` variable inside this closure: never in
// localStorage, sessionStorage, a cookie, the URL or the page text. Logging out sets it to null.
(() => {
  'use strict';

  let accessToken = null;
  let expiresAt = 0;
  let countdown = null;

  const $ = (id) => document.getElementById(id);
  const els = {
    signinCard: $('signin-card'),
    sessionCard: $('session-card'),
    loginForm: $('login-form'),
    username: $('username'),
    password: $('password'),
    meUsername: $('me-username'),
    meRole: $('me-role'),
    meExpiry: $('me-expiry'),
    logout: $('logout'),
    createForm: $('create-form'),
    businessKey: $('business-key'),
    subject: $('subject'),
    lessonId: $('lesson-id'),
    lessonStatus: $('lesson-status'),
    read: $('read'),
    confirm: $('confirm'),
    result: $('result'),
    history: $('history'),
  };

  function newCorrelationId() {
    const random = window.crypto && crypto.randomUUID
      ? crypto.randomUUID()
      : String(Date.now()) + Math.random().toString(16).slice(2);
    return 'page-' + random.slice(0, 8);
  }

  function newBusinessKey() {
    return 'LES-2026-' + String(Math.floor(1000 + Math.random() * 9000));
  }

  // Every call: a correlation id, JSON, and the Bearer header only while signed in.
  async function call(method, path, body) {
    const correlationId = newCorrelationId();
    const headers = { 'Accept': 'application/json', 'X-Correlation-Id': correlationId };
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (accessToken) headers['Authorization'] = 'Bearer ' + accessToken;
    let response;
    try {
      response = await fetch(path, {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body),
        credentials: 'omit',
        cache: 'no-store',
      });
    } catch (networkError) {
      return { status: 0, data: null, correlationId };
    }
    const text = await response.text();
    let data = null;
    try { data = text ? JSON.parse(text) : null; } catch (notJson) { data = null; }
    return { status: response.status, data, correlationId };
  }

  function isError(data) {
    return data && typeof data.code === 'string' && typeof data.message === 'string';
  }

  // Draws the last response with textContent only. On an error it shows code, message and
  // correlationId from the JSON body, and nothing else from the server.
  function show(action, res, extra) {
    const box = els.result;
    box.replaceChildren();
    box.className = 'result ' + (res.status >= 200 && res.status < 300 ? 'ok'
      : res.status === 409 || res.status === 404 ? 'warn' : 'bad');

    const headline = document.createElement('p');
    headline.className = 'headline';
    headline.textContent = res.status === 0
      ? action + ': the server is not reachable'
      : action + ': HTTP ' + res.status;
    box.appendChild(headline);

    const rows = [];
    if (isError(res.data)) {
      rows.push(['code', res.data.code], ['message', res.data.message],
        ['correlationId', res.data.correlationId || res.correlationId]);
    } else if (res.data && typeof res.data === 'object') {
      if (res.data.status) rows.push(['status', res.data.status]);
      if (res.data.businessKey) rows.push(['businessKey', res.data.businessKey]);
      if (res.data.id) rows.push(['id', res.data.id]);
      if (res.data.username) rows.push(['username', res.data.username]);
      if (res.data.tokenType) rows.push(['tokenType', res.data.tokenType]);
      if (res.data.expiresIn) rows.push(['expiresIn', res.data.expiresIn + ' s (' + res.data.expiresIn / 60 + ' minutes)']);
      if (res.data.accessToken) rows.push(['accessToken', 'kept in memory, not shown']);
      rows.push(['correlationId', res.correlationId]);
    } else {
      rows.push(['correlationId', res.correlationId]);
    }
    const dl = document.createElement('dl');
    for (const [key, value] of rows) {
      const dt = document.createElement('dt');
      dt.textContent = key;
      const dd = document.createElement('dd');
      dd.textContent = String(value);
      dl.append(dt, dd);
    }
    box.appendChild(dl);

    if (extra) {
      const note = document.createElement('p');
      note.className = 'note';
      note.textContent = extra;
      box.appendChild(note);
    }

    const item = document.createElement('li');
    const code = document.createElement('span');
    code.className = 'code';
    code.textContent = (res.status || 'no answer') + (isError(res.data) ? ' ' + res.data.code : '');
    item.append(action + ' → ', code);
    els.history.prepend(item);
  }

  function setLesson(lesson) {
    if (!lesson) return;
    if (lesson.id) els.lessonId.value = lesson.id;
    if (lesson.status) els.lessonStatus.textContent = lesson.status;
  }

  function lessonId() {
    return els.lessonId.value.trim();
  }

  function showSignedIn(me, expiresIn) {
    els.meUsername.textContent = me.username;
    const roles = (me.authorities || []).map((a) => a.replace(/^ROLE_/, ''));
    els.meRole.textContent = roles.join(', ') || 'none';
    expiresAt = Date.now() + expiresIn * 1000;
    tick();
    clearInterval(countdown);
    countdown = setInterval(tick, 1000);
    els.signinCard.classList.add('hidden');
    els.sessionCard.classList.remove('hidden');
  }

  function showSignedOut() {
    clearInterval(countdown);
    els.meUsername.textContent = '–';
    els.meRole.textContent = '–';
    els.meExpiry.textContent = '–';
    els.sessionCard.classList.add('hidden');
    els.signinCard.classList.remove('hidden');
    els.username.focus();
  }

  function tick() {
    const left = Math.max(0, Math.round((expiresAt - Date.now()) / 1000));
    const minutes = Math.floor(left / 60);
    const seconds = String(left % 60).padStart(2, '0');
    els.meExpiry.textContent = left > 0
      ? minutes + ':' + seconds + ' (15 minutes from login)'
      : 'expired: log in again';
  }

  els.loginForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    const username = els.username.value;
    const password = els.password.value;
    els.password.value = '';
    accessToken = null;

    const res = await call('POST', '/api/v1/auth/login', { username, password });
    if (res.status !== 200 || !res.data || !res.data.accessToken) {
      show('Log in', res, 'No token was stored. The form is still here.');
      return;
    }
    accessToken = res.data.accessToken;
    const expiresIn = res.data.expiresIn;

    const me = await call('GET', '/api/me');
    if (me.status !== 200) {
      accessToken = null;
      show('Who am I', me);
      return;
    }
    showSignedIn(me.data, expiresIn);
    show('Log in', res, 'Signed in as ' + me.data.username + '. The role shown comes from GET /api/me.');
  });

  els.logout.addEventListener('click', () => {
    accessToken = null;
    expiresAt = 0;
    showSignedOut();
    els.result.replaceChildren();
    els.result.className = 'result';
    const p = document.createElement('p');
    p.className = 'muted';
    p.textContent = 'Logged out: the token is gone from this tab. Try "Read lesson" now; the answer is 401.';
    els.result.appendChild(p);
    const item = document.createElement('li');
    item.textContent = 'Log out → token dropped';
    els.history.prepend(item);
  });

  for (const button of document.querySelectorAll('[data-user]')) {
    button.addEventListener('click', () => {
      els.username.value = button.dataset.user;
      els.password.value = button.dataset.pass;
      els.password.focus();
    });
  }

  els.createForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    const res = await call('POST', '/api/v1/lessons', {
      businessKey: els.businessKey.value.trim(),
      subject: els.subject.value.trim(),
    });
    if (res.status === 201) {
      setLesson(res.data);
      els.businessKey.value = newBusinessKey();
    }
    show('Create lesson', res);
  });

  els.read.addEventListener('click', async () => {
    if (!lessonId()) {
      show('Read lesson', { status: 0, data: null, correlationId: '–' }, 'Create a lesson first, or paste its UUID.');
      return;
    }
    const res = await call('GET', '/api/v1/lessons/' + encodeURIComponent(lessonId()));
    if (res.status === 200) setLesson(res.data);
    show('Read lesson', res);
  });

  els.confirm.addEventListener('click', async () => {
    if (!lessonId()) {
      show('Confirm lesson', { status: 0, data: null, correlationId: '–' }, 'Create a lesson first, or paste its UUID.');
      return;
    }
    const id = encodeURIComponent(lessonId());
    const res = await call('POST', '/api/v1/lessons/' + id + '/confirm');
    if (res.status === 200) {
      setLesson(res.data);
      show('Confirm lesson', res, 'The lesson is now ' + res.data.status + '.');
      return;
    }
    if (res.status === 403) {
      // prove the row did not move: a student may still read it
      const after = await call('GET', '/api/v1/lessons/' + id);
      const unchanged = after.status === 200 && after.data && after.data.status
        ? 'The row did not change: status is still ' + after.data.status + '.'
        : 'The service was never called, so the row did not change.';
      if (after.status === 200) setLesson(after.data);
      show('Confirm lesson', res, unchanged);
      return;
    }
    show('Confirm lesson', res);
  });

  els.businessKey.value = newBusinessKey();
})();
