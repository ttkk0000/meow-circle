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

async function load() {
  if (!getToken()) {
    guestHint.classList.remove("hidden");
    guestLogin.classList.remove("hidden");
    profileName.textContent = "Guest";
    return;
  }
  guestHint.classList.add("hidden");
  guestLogin.classList.add("hidden");
  try {
    const user = await apiCall("/api/v1/me", "GET");
    profileName.textContent = user.nickname || user.username || `User ${user.id}`;
    profileBio.textContent = user.bio || "";
    if (user.avatar_url) {
      profileAvatar.innerHTML = `<img src="${escapeHtml(user.avatar_url)}" alt="" class="w-full h-full object-cover" />`;
    } else {
      profileAvatar.textContent = (user.nickname || user.username || "?").slice(0, 1).toUpperCase();
    }

    const { items } = await apiCall("/api/v1/me/posts", "GET");
    const posts = items || [];
    statPosts.textContent = String(posts.length);
    myPosts.innerHTML = "";
    if (!posts.length) {
      myPosts.innerHTML = `<div class="col-span-full text-center py-8 text-gray-500">${escapeHtml(t("common.empty_my_posts"))}</div>`;
      return;
    }
    for (const p of posts) {
      const div = document.createElement("div");
      div.className =
        "masonry-item bg-white rounded-2xl overflow-hidden shadow-[0_4px_16px_rgba(0,0,0,0.04)] border border-gray-100 cursor-pointer hover:shadow-lg transition-shadow";
      div.innerHTML = `<div class="p-4"><h3 class="font-body-lg text-on-surface line-clamp-2">${escapeHtml(p.title)}</h3>
        <p class="text-label-md text-gray-400 mt-2">${escapeHtml(p.category || "")}</p></div>`;
      div.addEventListener("click", () => {
        location.href = `/post.html?id=${p.id}`;
      });
      myPosts.appendChild(div);
    }
  } catch (e) {
    profileBio.textContent = e.message;
  }
}

load();
