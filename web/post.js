const TOKEN_KEY = "meow_token";
const USER_KEY = "meow_user";
const t = (k) => window.MeowShared.t(k);

const params = new URLSearchParams(location.search);
const postId = params.get("id");

const errBox = document.querySelector("#err-box");
const shell = document.querySelector("#post-shell");
const heroImg = document.querySelector("#hero-img");
const heroVideo = document.querySelector("#hero-video");
const postTitle = document.querySelector("#post-title");
const postBody = document.querySelector("#post-body");
const postTags = document.querySelector("#post-tags");
const postMeta = document.querySelector("#post-meta");
const commentsNode = document.querySelector("#comments");
const commentCount = document.querySelector("#comment-count");
const commentForm = document.querySelector("#comment-form");
const btnBack = document.querySelector("#btn-back");
const btnReport = document.querySelector("#btn-report");
const authorAvatar = document.querySelector("#author-avatar");
const authorLine = document.querySelector("#author-line");
const btnLike = document.querySelector("#btn-like");
const likeIcon = document.querySelector("#like-icon");
const likeCountEl = document.querySelector("#like-count");
const btnFollow = document.querySelector("#btn-follow");

let detailLikeCount = 0;
let detailLiked = false;
let currentAuthorId = null;
let followingAuthor = false;

function formatCompactCount(n) {
  const x = Math.floor(Number(n) || 0);
  if (x >= 10000) {
    const w = x / 10000;
    const s = w >= 10 ? String(Math.round(w)) : w.toFixed(1).replace(/\.0$/, "");
    return `${s}w`;
  }
  if (x >= 1000) {
    const k = x / 1000;
    const s = k >= 10 ? String(Math.round(k)) : k.toFixed(1).replace(/\.0$/, "");
    return `${s}k`;
  }
  return String(x);
}

function applyLikeUI() {
  if (!likeCountEl || !likeIcon) return;
  likeCountEl.textContent = formatCompactCount(detailLikeCount);
  likeIcon.classList.toggle("fill", detailLiked);
  likeIcon.classList.toggle("text-primary-container", detailLiked);
  likeIcon.classList.toggle("text-gray-400", !detailLiked);
}

function getMe() {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch (_) {
    return null;
  }
}

function applyFollowUI() {
  if (!btnFollow) return;
  const me = getMe();
  if (!me || !currentAuthorId || Number(me.id) === Number(currentAuthorId)) {
    btnFollow.classList.add("hidden");
    return;
  }
  btnFollow.classList.remove("hidden");
  btnFollow.textContent = followingAuthor ? t("stitch.following") : t("stitch.follow");
  btnFollow.classList.toggle("bg-primary-container/10", followingAuthor);
}

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

async function apiCall(url, method = "GET", body) {
  const init = { method, headers: { "Content-Type": "application/json" } };
  const token = getToken();
  if (token) init.headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) init.body = JSON.stringify(body);
  const response = await fetch(url, init);
  let payload = null;
  try {
    payload = await response.json();
  } catch (_) {
    payload = null;
  }
  if (!response.ok) {
    const message = (payload && payload.message) || "request failed";
    throw new Error(message);
  }
  return payload && payload.data !== undefined ? payload.data : payload;
}

function escapeHtml(s) {
  return String(s)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function notify(msg, kind) {
  if (window.MeowShared && window.MeowShared.toast) window.MeowShared.toast(msg, kind);
  else alert(msg);
}

function formatTime(value) {
  if (!value) return t("common.unknown");
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return t("common.unknown");
  return date.toLocaleString();
}

if (btnFollow) {
  btnFollow.addEventListener("click", async (ev) => {
    ev.preventDefault();
    if (!currentAuthorId) return;
    if (!getToken()) {
      notify(t("stitch.follow_login"), "error");
      location.href = `/login?return_to=${encodeURIComponent(location.pathname + location.search)}`;
      return;
    }
    try {
      if (followingAuthor) {
        await apiCall(`/api/v1/me/follow/${currentAuthorId}`, "DELETE");
        followingAuthor = false;
        notify(t("stitch.unfollowed_ok"), "success");
      } else {
        await apiCall(`/api/v1/me/follow/${currentAuthorId}`, "POST");
        followingAuthor = true;
        notify(t("stitch.followed_ok"), "success");
      }
      applyFollowUI();
    } catch (e) {
      notify(e.message, "error");
    }
  });
}

if (btnLike) {
  btnLike.addEventListener("click", async () => {
    if (!postId) return;
    if (!getToken()) {
      notify(t("stitch.like_login"), "error");
      location.href = `/login?return_to=${encodeURIComponent(location.pathname + location.search)}`;
      return;
    }
    try {
      const res = await apiCall(`/api/v1/posts/${postId}/like`, "POST");
      detailLiked = !!res.liked;
      detailLikeCount = Number(res.like_count) || 0;
      applyLikeUI();
    } catch (e) {
      notify(e.message, "error");
    }
  });
}

btnBack.addEventListener("click", () => {
  if (history.length > 1) history.back();
  else location.href = "/";
});

btnReport.addEventListener("click", async () => {
  if (!postId) return;
  if (!getToken()) {
    notify(t("alert.login_first"), "error");
    location.href = `/login?return_to=${encodeURIComponent(location.pathname + location.search)}`;
    return;
  }
  const reason = prompt(t("report.placeholder"));
  if (!reason) return;
  try {
    await apiCall("/api/v1/reports", "POST", { target_kind: "post", target_id: Number(postId), reason });
    notify(t("report.submitted"), "success");
  } catch (e) {
    notify(e.message, "error");
  }
});

function renderMedia(media) {
  heroImg.classList.add("hidden");
  heroVideo.classList.add("hidden");
  if (!media || !media.length) {
    heroImg.classList.remove("hidden");
    heroImg.src =
      "https://images.unsplash.com/photo-1511044568932-338cba0ad803?auto=format&fit=crop&w=1200&q=80";
    return;
  }
  const m = media[0];
  if (m.kind === "video") {
    heroVideo.classList.remove("hidden");
    heroVideo.src = m.url;
  } else {
    heroImg.classList.remove("hidden");
    heroImg.src = m.url;
  }
}

function renderComments(list) {
  commentsNode.innerHTML = "";
  commentCount.textContent = `(${list.length})`;
  if (!list.length) {
    commentsNode.innerHTML = `<p class="text-label-md text-gray-400">${escapeHtml(t("common.no_comments"))}</p>`;
    return;
  }
  for (const c of list) {
    const row = document.createElement("div");
    row.className = "flex gap-3";
    const initial = String(c.author_id).slice(-1);
    row.innerHTML = `<div class="w-9 h-9 rounded-full bg-secondary-container/25 flex items-center justify-center font-bold text-secondary shrink-0">${escapeHtml(initial)}</div>
      <div class="flex-1 min-w-0">
        <span class="font-label-md text-on-surface-variant">${escapeHtml(t("meta.author"))} ${c.author_id}</span>
        <p class="font-body-md text-on-surface mt-1 leading-snug">${escapeHtml(c.content)}</p>
        <span class="font-label-md text-on-surface-variant/40 mt-1 inline-block">${escapeHtml(formatTime(c.created_at))}</span>
      </div>`;
    commentsNode.appendChild(row);
  }
}

commentForm.addEventListener("submit", async (ev) => {
  ev.preventDefault();
  if (!getToken()) {
    notify(t("alert.login_to_comment"), "error");
    location.href = `/login?return_to=${encodeURIComponent(location.pathname + location.search)}`;
    return;
  }
  const fd = new FormData(commentForm);
  try {
    await apiCall(`/api/v1/posts/${postId}/comments`, "POST", { content: String(fd.get("content") || "") });
    commentForm.reset();
    const detail = await apiCall(`/api/v1/posts/${postId}`, "GET");
    renderComments(detail.comments || []);
  } catch (e) {
    notify(e.message, "error");
  }
});

async function load() {
  if (!postId) {
    errBox.textContent = "missing id";
    errBox.classList.remove("hidden");
    return;
  }
  try {
    const detail = await apiCall(`/api/v1/posts/${postId}`, "GET");
    const post = detail.post;
    postTitle.textContent = post.title;
    postBody.textContent = post.content;
    const au = detail.author;
    if (au && authorLine && authorAvatar) {
      authorLine.textContent = au.nickname || au.username || `${t("meta.author")} ${post.author_id}`;
      if (au.avatar_url) {
        authorAvatar.innerHTML = `<img alt="" class="w-full h-full object-cover rounded-full" src="${escapeHtml(au.avatar_url)}" />`;
        authorAvatar.classList.remove("text-primary-container", "font-bold");
      } else {
        authorAvatar.textContent = String(post.author_id).slice(-1);
        authorAvatar.classList.add("text-primary-container", "font-bold");
      }
    } else if (authorLine && authorAvatar) {
      authorLine.textContent = `${t("meta.author")} ${post.author_id}`;
      authorAvatar.textContent = String(post.author_id).slice(-1);
    }
    detailLikeCount = Number(detail.like_count) || 0;
    detailLiked = !!detail.liked;
    applyLikeUI();
    currentAuthorId = au && au.id != null ? au.id : post.author_id;
    followingAuthor = !!detail.following_author;
    applyFollowUI();
    postMeta.textContent = `${t("meta.published_at")} ${formatTime(post.created_at)} · ${post.category || ""}`;
    postTags.innerHTML = (post.tags || [])
      .map(
        (tag) =>
          `<span class="px-3 py-1 rounded-full bg-primary-container/10 text-primary-container font-label-md">#${escapeHtml(tag)}</span>`
      )
      .join("");
    renderMedia(detail.media);
    renderComments(detail.comments || []);
    shell.classList.remove("hidden");
  } catch (e) {
    errBox.textContent = e.message;
    errBox.classList.remove("hidden");
  }
}

load();
