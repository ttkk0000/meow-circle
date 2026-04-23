const TOKEN_KEY = "meow_token";
const t = (k) => window.MeowShared.t(k);
const form = document.querySelector("#compose-form");

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

async function apiCall(url, method = "GET", body) {
  const init = { method, headers: { "Content-Type": "application/json" } };
  const token = getToken();
  if (!token) throw new Error("unauthorized");
  init.headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) init.body = JSON.stringify(body);
  const res = await fetch(url, init);
  let payload = null;
  try {
    payload = await res.json();
  } catch (_) {
    payload = null;
  }
  if (!res.ok) throw new Error((payload && payload.message) || "failed");
  return payload && payload.data !== undefined ? payload.data : payload;
}

function notify(m, k) {
  if (window.MeowShared && window.MeowShared.toast) window.MeowShared.toast(m, k);
  else alert(m);
}

function parseTags(raw) {
  if (!raw || !String(raw).trim()) return [];
  return String(raw)
    .split(/[,，]/)
    .map((s) => s.trim())
    .filter(Boolean);
}

function parseMediaIds(raw) {
  if (!raw || !String(raw).trim()) return [];
  return String(raw)
    .split(/[,，\s]+/)
    .map((s) => s.trim())
    .filter(Boolean)
    .map((s) => Number(s))
    .filter((n) => !Number.isNaN(n));
}

form.addEventListener("submit", async (ev) => {
  ev.preventDefault();
  if (!getToken()) {
    notify(t("alert.login_first"), "error");
    location.href = `/login?return_to=${encodeURIComponent(location.pathname)}`;
    return;
  }
  const fd = new FormData(form);
  const payload = {
    title: String(fd.get("title") || "").trim(),
    content: String(fd.get("content") || "").trim(),
    category: String(fd.get("category") || "daily_share"),
    tags: parseTags(fd.get("tags")),
    media_ids: parseMediaIds(fd.get("media_ids")),
  };
  try {
    const post = await apiCall("/api/v1/posts", "POST", payload);
    notify(t("alert.publish_success"), "success");
    location.href = `/post.html?id=${post.id}`;
  } catch (e) {
    notify(e.message, "error");
  }
});

if (!getToken()) {
  location.replace(`/login?return_to=${encodeURIComponent(location.pathname)}`);
}
