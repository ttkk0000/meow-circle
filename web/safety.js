/* Guest-only safety channel. No account cookies, analytics, or persistent localStorage. */
(() => {
  'use strict';
  const $ = id => document.getElementById(id);
  const storageKey = 'mnd.safety.tab.v1';
  const kinds = new Set(['entries', 'pets', 'cases', 'offers']);
  const types = new Set(['safety', 'privacy', 'rights_notice', 'supplement']);
  const maxID = 9223372036854775807n;
  let state = { token: '', expires: '', verifiedEmail: '', phase: 'editing', raw: '', key: '', receipt: null, related: null };
  let busy = false;
  let sendAfter = 0;
  let remember = false;
  const labels = { safety: '安全求助／不符举报', privacy: '隐私问题', rights_notice: '正式权利通知', supplement: '补充材料' };

  function id(value, allowZero = false) {
    const text = String(value);
    if (!/^(0|[1-9][0-9]*)$/.test(text) || BigInt(text) > maxID || (!allowZero && text === '0')) throw new Error('编号格式不正确，请重新打开原链接或查询回执。');
    return BigInt(text);
  }

  // Protect JSON integer tokens before JSON.parse can round an int64. Quoted text
  // is copied verbatim, including escaped quotes, and is never regex-rewritten.
  function parseExact(raw) {
    let out = '', i = 0;
    while (i < raw.length) {
      const c = raw[i];
      if (c === '"') {
        const start = i++;
        while (i < raw.length) {
          if (raw[i] === '\\') { i += 2; continue; }
          if (raw[i++] === '"') break;
        }
        out += raw.slice(start, i);
      } else if (c === '-' || /[0-9]/.test(c)) {
        const token = raw.slice(i).match(/^-?(?:0|[1-9][0-9]*)(?:\.[0-9]+)?(?:[eE][+-]?[0-9]+)?/);
        if (!token) throw new Error('响应格式不正确');
        const number = token[0];
        out += /^-?[0-9]+$/.test(number) && (BigInt(number) > BigInt(Number.MAX_SAFE_INTEGER) || BigInt(number) < BigInt(Number.MIN_SAFE_INTEGER)) ? JSON.stringify(number) : number;
        i += number.length;
      } else { out += c; i++; }
    }
    return JSON.parse(out);
  }

  function wire(value) {
    if (typeof value === 'bigint') return String(value);
    if (Array.isArray(value)) return '[' + value.map(wire).join(',') + ']';
    if (value && typeof value === 'object') return '{' + Object.entries(value).map(([k, v]) => JSON.stringify(k) + ':' + wire(v)).join(',') + '}';
    return JSON.stringify(value);
  }
  function key() {
    return Array.from(crypto.getRandomValues(new Uint8Array(20)), b => b.toString(16).padStart(2, '0')).join('');
  }
  function message(text) { $('status').textContent = text; }
  function persist() {
    try {
      if (remember && state.token) sessionStorage.setItem(storageKey, JSON.stringify(state));
      else sessionStorage.removeItem(storageKey);
    } catch (_) { message('标签页存储不可用；当前请求仍保留在本页内。请勿刷新或关闭页面。'); }
  }
  function receipt(value) {
    if (!value || typeof value.state !== 'string' || typeof value.type !== 'string') throw new Error('回执格式不完整，需核对原请求。');
    return { id: String(id(value.id)), version: String(id(value.version)), state: value.state, type: value.type,
      evidence_state: String(value.evidence_state || ''), review_due: String(value.review_due || '') };
  }
  async function api(path, method = 'GET', raw) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 20000);
    try {
      const response = await fetch('/api/v2/safety/' + path, { method, credentials: 'omit', cache: 'no-store', referrerPolicy: 'no-referrer', signal: controller.signal,
        headers: { 'Content-Type': 'application/json', ...(state.token ? { 'X-Safety-Token': state.token } : {}) }, body: raw });
      const data = parseExact(await response.text());
      if (!response.ok || data.ok !== true) {
        const failure = new Error('请求未成功，请核对输入或稍后重试。' + (typeof data.error_code === 'string' ? '（' + data.error_code + '）' : ''));
        // Only an explicit rejection envelope is a definite unsuccessful write.
        failure.definite = response.status >= 400 && response.status < 500 && data.ok === false;
        failure.status = response.status;
        throw failure;
      }
      return data.data;
    } finally { clearTimeout(timeout); }
  }
  async function session() {
    if (state.token) {
      if (!(Date.parse(state.expires) > Date.now())) throw new Error('本次凭据已到期。旧事项无法凭编号恢复；请联系实际受理方，不要把重发当成原案查询。');
      return;
    }
    const access = await api('session', 'POST', '{}');
    if (!access || !/^[A-Za-z0-9_-]{40,100}$/.test(access.token) || !(Date.parse(access.expires_at) > Date.now())) throw new Error('未获得有效查询凭据，请稍后重试。');
    state.token = access.token; state.expires = access.expires_at; persist();
  }
  function target() {
    const text = $('target').value.trim();
    if (!text) return { kind: '', id: 0n };
    let url;
    try { url = new URL(text, location.origin); } catch (_) { throw new Error('请填写有效的公开内容链接。'); }
    const parts = url.pathname.split('/');
    if (url.origin !== location.origin || url.username || url.password || url.search || url.hash || parts.length !== 4 || parts[1] !== 'share' || !kinds.has(parts[2])) throw new Error('只接受同一网站的公开分享链接；请移除查询参数和凭据。');
    return { kind: parts[2], id: id(parts[3]) };
  }
  function render() {
    const phase = state.phase;
    $('request-form').hidden = phase !== 'editing';
    $('fields').disabled = busy;
    const formal = $('type').value === 'rights_notice';
    $('formal').hidden = !formal;
    $('formal').querySelectorAll('input,textarea,button').forEach(control => { control.disabled = busy || !formal; });
    $('preview-panel').hidden = !['preview', 'submitting', 'unknown', 'rejected'].includes(phase);
    $('receipt-panel').hidden = !state.receipt;
    $('submit').hidden = phase !== 'preview';
    $('edit').hidden = !['preview', 'rejected'].includes(phase);
    $('reconcile').hidden = phase !== 'unknown';
    $('retry').hidden = !['unknown', 'rejected'].includes(phase);
    for (const name of ['submit', 'edit', 'reconcile', 'retry', 'refresh', 'supplement', 'new-request', 'forget', 'remember']) $(name).disabled = busy;
    $('supplement').disabled = busy || phase !== 'accepted';
    $('new-request').disabled = busy || !['accepted', 'editing'].includes(phase);
    $('send-code').disabled = busy || !formal || Date.now() < sendAfter;
    if (state.receipt) {
      const r = state.receipt;
      const due = Date.parse(r.review_due);
      $('receipt-text').textContent = `回执 ${r.id} · 版本 ${r.version}\n服务端状态：${r.state}\n材料状态：${r.evidence_state || '待查询'}\n复核期限：${Number.isFinite(due) ? new Date(due).toLocaleString() : '待查询'}\n状态以最新查询为准，提交不代表已处理完成。`;
    }
    if (state.raw) {
      try {
        const input = parseExact(state.raw);
        $('preview-text').textContent = `${labels[input.type]}\n相关内容：${input.target_kind || '未指定'} ${input.target_id || ''}\n关联回执：${input.related_case_id || '无'}\n\n${input.reason}\n\n证据说明：${input.evidence_description || '无'}${input.type === 'rights_notice' ? `\n主体：${input.legal_name}\n权利：${input.right_type}\n声明：${input.statement}` : ''}\n\n本次请求编号：${state.key}`;
      } catch (_) { message('原请求不可读，请保留凭据并联系实际受理方。'); }
    }
  }
  async function action(job) {
    if (busy) return;
    busy = true; render();
    try { await job(); } catch (error) { message(error.message || '请求未成功，当前输入已保留。'); }
    finally { busy = false; persist(); render(); }
  }
  function accept(data) {
    state.receipt = receipt(data); state.phase = 'accepted';
    message('已获得真实回执。可查询进度或基于此回执补充。');
  }
  async function submitOriginal() {
    const wasUnknown = state.phase === 'unknown';
    state.phase = 'submitting'; persist(); render();
    try { accept(await api('tickets', 'POST', state.raw)); }
    catch (error) {
      const rejected = error.definite && !wasUnknown;
      state.phase = rejected ? 'rejected' : 'unknown';
      message(rejected ? error.message + ' 原内容已保留；补充事项遇到版本冲突时，请返回查询最新回执后再补充。' : '提交结果不明。原请求编号与内容已保留；请先核对原请求。查询未找到或凭据失效也不能证明之前提交失败。');
    }
  }

  $('request-form').addEventListener('submit', event => {
    event.preventDefault();
    action(async () => {
      const type = $('type').value;
      if (!types.has(type)) throw new Error('请选择有效事项类型。');
      const t = target();
      const formal = type === 'rights_notice';
      const reason = $('reason').value.trim(), evidence = $('evidence').value.trim();
      if (!reason || !$('confirmed').checked) throw new Error('请填写情况说明，并主动勾选确认。');
      if (formal && (state.verifiedEmail !== $('email').value.trim().toLowerCase() || !state.verifiedEmail || !$('name').value.trim() || !$('right-type').value.trim() || !$('statement').value.trim() || !evidence)) throw new Error('请完成联系邮箱验证、主体、权利类型、真实性声明与证据说明。');
      if (type === 'supplement' && !state.related) throw new Error('请先查询本设备已有事项，再从回执选择补充。');
      await session();
      const requestKey = key();
      const body = { confirmed: true, client_request_id: requestKey, type, target_kind: t.kind, target_id: t.id,
        related_case_id: type === 'supplement' ? id(state.related.id) : 0n,
        version: type === 'supplement' ? id(state.related.version) : 0n, reason, evidence_description: evidence,
        legal_name: formal ? $('name').value.trim() : '', right_type: formal ? $('right-type').value.trim() : '', statement: formal ? $('statement').value.trim() : '', reauth_token: '', media_ids: [] };
      state.raw = wire(body); state.key = requestKey; state.phase = 'preview';
      message('请核对原文，点击确认后才会提交。');
    });
  });
  $('submit').addEventListener('click', () => action(async () => { if (state.phase === 'preview') await submitOriginal(); }));
  $('retry').addEventListener('click', () => action(async () => {
    if (!['unknown', 'rejected'].includes(state.phase) || !state.raw) return;
    if (state.phase === 'unknown' && !confirm('仅用相同凭据、请求编号和原内容重试这一次事项，不建立新事项。是否继续？')) return;
    await submitOriginal();
  }));
  $('reconcile').addEventListener('click', () => action(async () => {
    if (state.phase !== 'unknown') return;
    try { accept(await api('requests/' + encodeURIComponent(state.key))); }
    catch (_) { message('暂未获得原请求回执。原内容及编号仍保留，不把未找到解释为提交失败；稍后可继续核对或按原内容重试。'); }
  }));
  $('edit').addEventListener('click', () => {
    if (busy || !['preview', 'rejected'].includes(state.phase)) return;
    state.phase = 'editing'; state.raw = ''; state.key = ''; persist(); render();
    message('可修改后重新核对。本次返回编辑由你主动发起。');
  });
  $('refresh').addEventListener('click', () => action(async () => {
    if (!state.receipt) return;
    state.receipt = receipt(await api('tickets/' + String(id(state.receipt.id))));
    if (state.related && ['editing', 'rejected'].includes(state.phase) && state.related.id === state.receipt.id)
      state.related = { id: state.receipt.id, version: state.receipt.version };
    message('已更新该回执进度。');
  }));
  function editNew(supplement) {
    if (busy || state.phase !== 'accepted') return;
    state.related = supplement ? { id: state.receipt.id, version: state.receipt.version } : null;
    state.raw = ''; state.key = ''; state.phase = 'editing';
    $('type').value = supplement ? 'supplement' : 'safety';
    for (const field of ['reason', 'evidence', 'code', 'name', 'right-type', 'statement']) $(field).value = '';
    $('confirmed').checked = false;
    if (!supplement) { state.receipt = null; $('target').value = ''; }
    persist(); render(); message(supplement ? '基于当前回执版本补充；提交前请核对内容。' : '已开始另一个事项，旧回执不会被删除。');
    $('type').focus();
  }
  $('supplement').addEventListener('click', () => editNew(true));
  $('new-request').addEventListener('click', () => editNew(false));
  $('type').addEventListener('change', render);
  $('email').addEventListener('input', () => { state.verifiedEmail = ''; $('contact-status').textContent = '邮箱已变更，请重新验证。'; persist(); });
  $('send-code').addEventListener('click', () => action(async () => {
    const email = $('email').value.trim().toLowerCase();
    if (!email || !$('email').reportValidity()) throw new Error('请先填写有效邮箱。');
    if (Date.now() < sendAfter) return;
    await session();
    const sent = await api('contact-otp', 'POST', JSON.stringify({ email }));
    state.verifiedEmail = '';
    const seconds = Number.isSafeInteger(sent?.retry_after_seconds) && sent.retry_after_seconds > 0 ? Math.min(sent.retry_after_seconds, 86400) : 60;
    sendAfter = Date.now() + seconds * 1000;
    setTimeout(() => render(), seconds * 1000);
    $('contact-status').textContent = `发送请求已受理，请查看邮箱；${seconds} 秒后可再次请求。未收到时不要填写猜测验证码。`;
  }));
  $('verify-code').addEventListener('click', () => action(async () => {
    const email = $('email').value.trim().toLowerCase(), code = $('code').value.trim();
    if (!email || !code || !$('email').reportValidity()) throw new Error('请填写邮箱与收到的验证码。');
    await session();
    await api('contact-verify', 'POST', JSON.stringify({ email, code }));
    state.verifiedEmail = email; $('code').value = ''; $('contact-status').textContent = '此次联系邮箱已验证。';
  }));
  $('remember').addEventListener('change', () => { remember = $('remember').checked; persist(); });
  $('forget').addEventListener('click', () => {
    if (busy || !confirm('这只会清除本机页面凭据，不会删除服务端事项。尚未核对的请求可能无法恢复查询。确定清除？')) return;
    remember = false; state = { token: '', expires: '', verifiedEmail: '', phase: 'editing', raw: '', key: '', receipt: null, related: null };
    $('remember').checked = false; $('request-form').reset(); sendAfter = 0;
    $('contact-status').textContent = ''; persist(); render(); message('本机页面凭据已清除，服务端事项未删除。');
  });
  try {
    const saved = sessionStorage.getItem(storageKey);
    if (saved) {
      const restored = JSON.parse(saved);
      if (!/^[A-Za-z0-9_-]{40,100}$/.test(restored.token) || !['editing', 'preview', 'submitting', 'unknown', 'rejected', 'accepted'].includes(restored.phase)) throw new Error('invalid saved session');
      if (restored.raw) {
        const input = parseExact(restored.raw);
        if (!types.has(input.type) || !/^[A-Za-z0-9_-]{8,100}$/.test(restored.key) || input.client_request_id !== restored.key) throw new Error('invalid saved request');
        id(input.target_id, true); id(input.related_case_id, true); id(input.version, true);
      }
      if (restored.receipt) restored.receipt = receipt(restored.receipt);
      state = restored;
      if (state.phase === 'submitting') state.phase = 'unknown';
      remember = true; $('remember').checked = true;
      $('email').value = state.verifiedEmail || '';
      if (state.raw) {
        const input = parseExact(state.raw);
        $('type').value = input.type;
        $('target').value = input.target_kind ? `/share/${input.target_kind}/${input.target_id}` : '';
        for (const [field, name] of [['reason', 'reason'], ['evidence', 'evidence_description'], ['name', 'legal_name'], ['right-type', 'right_type'], ['statement', 'statement']]) $(field).value = input[name] || '';
        $('confirmed').checked = input.confirmed === true;
      }
      message('已恢复本标签页原请求。提交结果不明时请先核对原请求。');
    }
  } catch (_) {
    state.phase = 'blocked';
    message('保存的页面状态不可读，已停止提交。请保留原回执并联系实际受理方；清除本机凭据不能证明原请求失败。');
  }
  render();
})();
