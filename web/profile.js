const TOKEN_KEY = "meow_token";
const t = (k) => window.MeowShared.t(k);

const guestHint = document.querySelector("#guest-hint");
const guestLogin = document.querySelector("#guest-login");
const profileName = document.querySelector("#profile-name");
const profileBio = document.querySelector("#profile-bio");
const profileAvatar = document.querySelector("#profile-avatar");
const statPosts = document.querySelector("#stat-posts");
const myPosts = document.querySelector("#my-posts");
const btnShare = document.querySelector("#btn-share");
const btnEdit = document.querySelector("#btn-edit-profile");
const editModal = document.querySelector("#profile-edit-modal");
const editForm = document.querySelector("#profile-edit-form");
const btnEditCancel = document.querySelector("#btn-edit-cancel");
const profileCover = document.querySelector("#profile-cover");
const PROFILE_BG_KEY = "mnd_profile_bg";
let currentUser = null;

const PROFILE_BACKGROUNDS = {
  picnic: "assets/stitch-remote/screens/5e8d58afba4142068ea51010b1e17e3a.png",
  desk: "assets/stitch-remote/screens/5e8d58afba4142068ea51010b1e17e3a.png",
  arcade: "assets/stitch-remote/screens/207a704fbe6e4962b73725ee6d2b88da.png",
  garden: "assets/stitch-remote/screens/6d735a2d4f714b29ad07607685e2de49.png",
};

const DEMO_USER = {
  id: 9001,
  username: "peachlatte",
  nickname: "桃子和拿铁",
  bio: "两只猫的日常记录员，偶尔带 doggie 出镜。M&D 的猫猫优先示例主页。",
  avatar_url: "assets/stitch-remote/screens/5e8d58afba4142068ea51010b1e17e3a.png",
};

const DEMO_POSTS = [
  { id: 9001, title: "猫猫第一次学会开门，家里从此没有秘密", category: "daily_share" },
  { id: 9002, title: "猫猫新手村：晚上一直叫怎么办", category: "help" },
  { id: 9003, title: "周末猫狗野餐，有没有一起的？", category: "activity" },
];

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

async function apiCall(url, method = "GET", body) {
  const init = { method, headers: { "Content-Type": "application/json" } };
  const token = getToken();
  if (token) init.headers.Authorization = `Bearer ${token}`;
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

function escapeHtml(s) {
  return String(s)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

btnShare.addEventListener("click", async () => {
  const url = location.href;
  try {
    await navigator.clipboard.writeText(url);
    notify(t("stitch.profile_share_ok"), "success");
  } catch (_) {
    notify(url, "info");
  }
});

function openEditModal() {
  if (!currentUser || !editModal || !editForm) return;
  editForm.nickname.value = currentUser.nickname || currentUser.username || "";
  editForm.avatar_url.value = currentUser.avatar_url || "";
  editForm.bio.value = currentUser.bio || "";
  editModal.classList.remove("hidden");
}

function closeEditModal() {
  editModal?.classList.add("hidden");
}

btnEdit?.addEventListener("click", openEditModal);
btnEditCancel?.addEventListener("click", closeEditModal);
editModal?.addEventListener("click", (e) => {
  if (e.target === editModal) closeEditModal();
});

editForm?.addEventListener("submit", async (e) => {
  e.preventDefault();
  try {
    const fd = new FormData(editForm);
    await apiCall("/api/v1/me", "PATCH", {
      nickname: String(fd.get("nickname") || "").trim(),
      avatar_url: String(fd.get("avatar_url") || "").trim(),
      bio: String(fd.get("bio") || "").trim(),
    });
    notify(t("profile.saved"), "success");
    closeEditModal();
    await load();
  } catch (err) {
    notify(err.message, "error");
  }
});

function setProfileBg(bg) {
  const key = PROFILE_BACKGROUNDS[bg] ? bg : "picnic";
  if (profileCover) {
    profileCover.style.background = `linear-gradient(180deg, rgba(43,23,34,.05), rgba(43,23,34,.46)), url("${PROFILE_BACKGROUNDS[key]}") center / cover`;
  }
  localStorage.setItem(PROFILE_BG_KEY, key);
  document.querySelectorAll("[data-profile-bg]").forEach((button) => {
    const active = button.dataset.profileBg === key;
    button.classList.toggle("bg-primary-container", active);
    button.classList.toggle("text-white", active);
    button.classList.toggle("bg-surface-container", !active);
    button.classList.toggle("text-on-surface-variant", !active);
  });
}

function renderProfile(user, posts) {
  currentUser = user;
  profileName.textContent = user.nickname || user.username || `User ${user.id}`;
  profileBio.textContent = user.bio || "";
  if (user.avatar_url) {
    profileAvatar.innerHTML = `<img src="${escapeHtml(user.avatar_url)}" alt="" class="w-full h-full object-cover" />`;
  } else {
    profileAvatar.textContent = (user.nickname || user.username || "?").slice(0, 1).toUpperCase();
  }
  statPosts.textContent = String(posts.length);
  myPosts.innerHTML = "";
  if (!posts.length) {
    myPosts.innerHTML = `<div class="col-span-full text-center py-8 text-gray-500">${escapeHtml(t("common.empty_my_posts"))}</div>`;
    return;
  }
  for (const p of posts) {
    const div = document.createElement("div");
    div.className =
      "masonry-item bg-white rounded-lg overflow-hidden shadow-[0_4px_16px_rgba(0,0,0,0.04)] border border-gray-100 cursor-pointer hover:shadow-lg transition-shadow";
    div.innerHTML = `<div class="p-4"><h3 class="font-body-lg text-on-surface line-clamp-2">${escapeHtml(p.title)}</h3>
      <p class="text-label-md text-gray-400 mt-2">${escapeHtml(p.category || "")}</p></div>`;
    div.addEventListener("click", () => {
      location.href = `/post.html?id=${p.id}`;
    });
    myPosts.appendChild(div);
  }
}

async function load() {
  setProfileBg(localStorage.getItem(PROFILE_BG_KEY) || "picnic");
  if (!getToken()) {
    guestHint.classList.remove("hidden");
    guestLogin.classList.remove("hidden");
    renderProfile(DEMO_USER, DEMO_POSTS);
    return;
  }
  guestHint.classList.add("hidden");
  guestLogin.classList.add("hidden");
  try {
    const user = await apiCall("/api/v1/me", "GET");
    const { items } = await apiCall("/api/v1/me/posts", "GET");
    const posts = items || [];
    renderProfile(user, posts);
  } catch (e) {
    renderProfile(DEMO_USER, DEMO_POSTS);
  }
}

document.querySelectorAll("[data-profile-bg]").forEach((button) => {
  button.addEventListener("click", () => setProfileBg(button.dataset.profileBg));
});

load();
