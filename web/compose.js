const TOKEN_KEY = "meow_token";
const t = (k) => window.MeowShared.t(k);
const form = document.querySelector("#compose-form");
const DRAFT_KEY = "meow_compose_draft";
const mediaIdsInput = document.querySelector("#compose-media-ids");
const mediaPicked = document.querySelector("#compose-media-picked");
const saveDraftBtn = document.querySelector("#compose-save-draft");
const uploadImageInput = document.querySelector("#compose-upload-image");
const uploadVideoInput = document.querySelector("#compose-upload-video");
const titleInput = form ? form.querySelector('input[name="title"]') : null;
const contentInput = form ? form.querySelector('textarea[name="content"]') : null;
const categoryInput = form ? form.querySelector('select[name="category"]') : null;
const tagsInput = form ? form.querySelector('input[name="tags"]') : null;

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

async function uploadMedia(file) {
  const token = getToken();
  if (!token) throw new Error("unauthorized");
  const fd = new FormData();
  fd.set("file", file);
  const res = await fetch("/api/v1/media", {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body: fd,
  });
  let payload = null;
  try {
    payload = await res.json();
  } catch (_) {
    payload = null;
  }
  if (!res.ok) throw new Error((payload && payload.message) || "upload failed");
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

function setMediaIds(ids) {
  if (!mediaIdsInput) return;
  mediaIdsInput.value = ids.join(",");
  if (mediaPicked) {
    mediaPicked.textContent = ids.length
      ? `${t("media.selected").replace("{n}", String(ids.length))}: ${ids.join(", ")}`
      : "";
  }
}

function readDraft() {
  try {
    const raw = localStorage.getItem(DRAFT_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch (_) {
    return null;
  }
}

function writeDraft() {
  const fd = new FormData(form);
  const draft = {
    title: String(fd.get("title") || ""),
    content: String(fd.get("content") || ""),
    category: String(fd.get("category") || "daily_share"),
    tags: String(fd.get("tags") || ""),
    media_ids: String(fd.get("media_ids") || ""),
  };
  localStorage.setItem(DRAFT_KEY, JSON.stringify(draft));
  notify(t("alert.draft_saved"), "success");
}

async function onPickMedia(file) {
  if (!file) return;
  try {
    const media = await uploadMedia(file);
    const ids = parseMediaIds(mediaIdsInput ? mediaIdsInput.value : "");
    ids.push(Number(media.id));
    setMediaIds(ids.filter((n) => !Number.isNaN(n)));
    notify(t("alert.update_success"), "success");
  } catch (err) {
    notify(err.message, "error");
  }
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
    localStorage.removeItem(DRAFT_KEY);
    notify(t("alert.publish_success"), "success");
    location.href = `/post.html?id=${post.id}`;
  } catch (e) {
    notify(e.message, "error");
  }
});

if (!getToken()) {
  location.replace(`/login?return_to=${encodeURIComponent(location.pathname)}`);
}

saveDraftBtn?.addEventListener("click", writeDraft);
uploadImageInput?.addEventListener("change", () => onPickMedia(uploadImageInput.files && uploadImageInput.files[0]));
uploadVideoInput?.addEventListener("change", () => onPickMedia(uploadVideoInput.files && uploadVideoInput.files[0]));

const draft = readDraft();
if (draft) {
  if (titleInput) titleInput.value = draft.title || "";
  if (contentInput) contentInput.value = draft.content || "";
  if (categoryInput) categoryInput.value = draft.category || "daily_share";
  if (tagsInput) tagsInput.value = draft.tags || "";
  if (mediaIdsInput) mediaIdsInput.value = draft.media_ids || "";
}
setMediaIds(parseMediaIds(mediaIdsInput ? mediaIdsInput.value : ""));
