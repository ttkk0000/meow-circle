const TOKEN_KEY = "meow_token";
const USER_KEY = "meow_user";

const t = (k) => window.MeowShared.t(k);

const userHint = document.querySelector("#user-hint");
const logoutBtn = document.querySelector("#logout-btn");
const loginLink = document.querySelector("#login-link");
const registerLink = document.querySelector("#register-link");
const dashboardLink = document.querySelector("#dashboard-link");

const postsContainer = document.querySelector("#posts");
const activeRenderJobs = new WeakMap();
const activeRequestControllers = new Map();

let cachedFeed = [];
let currentFilter = "rec";

function requireLogin(reason) {
  if (reason) notify(reason, "error");
  const back = encodeURIComponent(location.pathname + location.search + location.hash);
  location.href = `/login?return_to=${back}`;
}

function pickAspectClass(seed) {
  const aspects = ["aspect-[3/4]", "aspect-square", "aspect-[4/5]", "aspect-[9/16]"];
  let h = 0;
  const s = String(seed);
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) | 0;
  return aspects[Math.abs(h) % aspects.length];
}

function placeholderImg(seed) {
  const id = Math.abs(String(seed).split("").reduce((a, c) => a + c.charCodeAt(0), 0) % 1000);
  return `https://images.unsplash.com/photo-1511044568932-338cba0ad803?auto=format&fit=crop&w=600&q=80&sig=${id}`;
}

// ----- Filter chips -----
document.querySelectorAll(".filter-chip").forEach((btn) => {
  btn.addEventListener("click", () => {
    const f = btn.dataset.filter;
    if (f === "follow" && !getToken()) {
      notify(t("stitch.filter_follow_login"), "info");
      const back = encodeURIComponent(location.pathname + location.search + location.hash);
      location.href = `/login?return_to=${back}`;
      return;
    }
    currentFilter = f;
    document.querySelectorAll(".filter-chip").forEach((b) => {
      const on = b.dataset.filter === f;
      b.classList.toggle("bg-primary-container/10", on);
      b.classList.toggle("text-primary-container", on);
      b.classList.toggle("ring-2", on);
      b.classList.toggle("ring-primary-container/30", on);
      b.classList.toggle("text-gray-500", !on);
      b.classList.toggle("hover:bg-gray-100", !on);
    });
    renderPosts();
  });
});

// ----- Search -----
const searchInput = document.querySelector("#global-search");
const searchInputMobile = document.querySelector("#global-search-mobile");
const searchResultsPanel = document.querySelector("#search-results");
const searchPostsNode = document.querySelector("#search-posts");
const searchListingsNode = document.querySelector("#search-listings");
const closeSearchBtn = document.querySelector("#close-search");
const mobileSearchOpen = document.querySelector("#mobile-search-open");
const mobileSearchPanel = document.querySelector("#mobile-search-panel");

let currentSearchType = "all";
let lastSearchQuery = "";

function syncSearchInputs(from, to) {
  if (from && to && document.activeElement === from) to.value = from.value;
}

if (mobileSearchOpen && mobileSearchPanel) {
  mobileSearchOpen.addEventListener("click", () => {
    mobileSearchPanel.classList.toggle("hidden");
    if (!mobileSearchPanel.classList.contains("hidden") && searchInputMobile) {
      searchInputMobile.focus();
    }
  });
}

function bindSearchInput(el) {
  if (!el) return;
  let timer = null;
  el.addEventListener("input", () => {
    if (searchInputMobile && el === searchInput) searchInputMobile.value = el.value;
    if (searchInput && el === searchInputMobile) searchInput.value = el.value;
    clearTimeout(timer);
    const q = el.value.trim();
    if (!q) {
      hideSearchResults();
      return;
    }
    timer = setTimeout(() => runSearch(q, currentSearchType), 300);
  });
  el.addEventListener("keydown", (ev) => {
    if (ev.key === "Escape") {
      el.value = "";
      if (searchInput && el !== searchInput) searchInput.value = "";
      if (searchInputMobile && el !== searchInputMobile) searchInputMobile.value = "";
      hideSearchResults();
    }
  });
}
bindSearchInput(searchInput);
bindSearchInput(searchInputMobile);

if (closeSearchBtn) {
  closeSearchBtn.addEventListener("click", () => {
    hideSearchResults();
    if (searchInput) searchInput.value = "";
    if (searchInputMobile) searchInputMobile.value = "";
  });
}

document.querySelectorAll("[data-search-tab]").forEach((btn) => {
  btn.addEventListener("click", () => {
    document.querySelectorAll("[data-search-tab]").forEach((b) => {
      b.classList.remove("bg-primary-container", "text-white");
      b.classList.add("bg-surface-container-low", "text-gray-600");
    });
    btn.classList.add("bg-primary-container", "text-white");
    btn.classList.remove("bg-surface-container-low", "text-gray-600");
    currentSearchType = btn.dataset.searchTab;
    if (lastSearchQuery) runSearch(lastSearchQuery, currentSearchType);
  });
});

if (searchResultsPanel) {
  searchResultsPanel.addEventListener("click", (ev) => {
    if (ev.target === searchResultsPanel) hideSearchResults();
  });
}

function hideSearchResults() {
  if (searchResultsPanel) {
    searchResultsPanel.setAttribute("hidden", "");
    searchResultsPanel.classList.add("hidden");
  }
  lastSearchQuery = "";
  cancelRequest("search");
}

async function runSearch(q, type) {
  lastSearchQuery = q;
  if (searchResultsPanel) {
    searchResultsPanel.removeAttribute("hidden");
    searchResultsPanel.classList.remove("hidden");
  }
  searchPostsNode.innerHTML = skeletonCards(3);
  searchListingsNode.innerHTML = "";
  const signal = takeRequestSignal("search");
  try {
    const data = await apiCall(`/api/v1/search?q=${encodeURIComponent(q)}&type=${type}`, "GET", undefined, { signal });
    await renderSearchSection(searchPostsNode, data.posts || [], "post");
    await renderSearchSection(searchListingsNode, data.listings || [], "listing");
  } catch (err) {
    if (err && err.name === "AbortError") return;
    searchPostsNode.innerHTML = `<p class="text-sm text-red-600">${escapeHtml(err.message)}</p>`;
  }
}

async function renderSearchSection(container, items, kind) {
  container.innerHTML = "";
  if (items.length === 0) return;
  const title = document.createElement("div");
  title.className = "text-label-md font-semibold text-gray-500 mb-2";
  title.textContent = kind === "post" ? t("search.tab_posts") : t("search.tab_listings");
  container.appendChild(title);
  await renderInBatches(container, items, (item) => buildSearchCard(item, kind), { batchSize: 14, preserveExisting: true });
}

function buildSearchCard(item, kind) {
  const a = document.createElement("a");
  a.className =
    "block bg-white rounded-2xl p-4 shadow-[0_4px_16px_rgba(255,90,119,0.06)] hover:-translate-y-0.5 transition-transform border border-gray-100";
  if (kind === "post") {
    a.href = `/post.html?id=${item.id}`;
    a.innerHTML = `<div class="font-body-lg font-semibold text-on-surface line-clamp-2">${escapeHtml(item.title)}</div>
      <p class="text-body-md text-gray-500 line-clamp-2 mt-1">${escapeHtml(item.content || "")}</p>
      <p class="text-label-md text-gray-400 mt-2">${escapeHtml(t("meta.author"))} ${item.author_id}</p>`;
  } else {
    a.href = `/market.html#l${item.id}`;
    a.innerHTML = `<div class="font-body-lg font-semibold text-on-surface line-clamp-2">${escapeHtml(item.title)}</div>
      <p class="text-body-md text-gray-500 line-clamp-2 mt-1">${escapeHtml(item.description || "")}</p>
      <p class="text-label-md text-primary-container mt-2">${escapeHtml(t("meta.price"))} ${(item.price_cents / 100).toFixed(2)} ${escapeHtml(item.currency || "")}</p>`;
  }
  return a;
}

function skeletonCards(n) {
  return Array.from({ length: n })
    .map(
      () =>
        `<div class="bg-white rounded-[24px] h-48 animate-pulse shadow-sm border border-gray-100"></div>`
    )
    .join("");
}

// ----- Notifications -----
const sidebarNotifDot = document.querySelector("#sidebar-notif-dot");
const bottomNotifDot = document.querySelector("#bottom-notif-dot");

async function refreshNotifCount() {
  if (!getToken()) return;
  try {
    const data = await apiCall("/api/v1/notifications?unread=true", "GET");
    const c = data && typeof data.unread_count === "number" ? data.unread_count : (data.items || []).length;
    const on = c > 0;
    if (sidebarNotifDot) sidebarNotifDot.classList.toggle("hidden", !on);
    if (bottomNotifDot) bottomNotifDot.classList.toggle("hidden", !on);
  } catch (_) {
    /* ignore */
  }
}

if (logoutBtn) {
  logoutBtn.addEventListener("click", () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    applyAuthState();
    loadPosts();
  });
}

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

async function loadPosts() {
  if (!postsContainer) return;
  postsContainer.innerHTML = skeletonCards(6);
  const signal = takeRequestSignal("posts");
  try {
    const q = new URLSearchParams({ page: "1", page_size: "40", filter: currentFilter });
    const data = await apiCall(`/api/v1/posts?${q.toString()}`, "GET", undefined, { signal });
    const items = Array.isArray(data.items) ? data.items : [];
    cachedFeed = items;
    if (items.length === 0) {
      postsContainer.innerHTML = `<div class="col-span-full text-center py-16 text-gray-500">${escapeHtml(t("common.empty_posts_hint"))}</div>`;
      return;
    }
    renderPosts();
  } catch (err) {
    if (err && err.name === "AbortError") return;
    postsContainer.innerHTML = `<p class="col-span-full text-red-600">${escapeHtml(t("common.error_load_posts"))}: ${escapeHtml(err.message)}</p>`;
  }
}

function renderPosts() {
  if (!postsContainer) return;
  postsContainer.innerHTML = "";
  renderInBatches(postsContainer, cachedFeed, buildPostNode, { batchSize: 10 });
}

async function hydratePostThumb(imgEl, overlayEl, mediaId) {
  if (!mediaId || !imgEl) return;
  try {
    const m = await apiCall(`/api/v1/media/${mediaId}`, "GET");
    if (m.url) imgEl.src = m.url;
    if (m.kind === "video" && overlayEl) {
      overlayEl.classList.remove("hidden");
    }
  } catch (_) {
    /* keep placeholder */
  }
}

function buildPostNode(item) {
  const post = item.post || item;
  const author = item.author || {};
  const likeCount = item.like_count ?? 0;
  let liked = item.liked ?? false;
  const fm = item.first_media;
  const aspect = pickAspectClass(post.id);
  const firstTag = Array.isArray(post.tags) && post.tags.length ? post.tags[0] : "";
  const tagHtml = firstTag
    ? `<div class="flex flex-wrap gap-2 mb-2"><span class="bg-primary-container/10 text-primary-container px-2 py-0.5 rounded-full text-label-md font-label-md">#${escapeHtml(firstTag)}</span></div>`
    : "";
  const authorLabel = escapeHtml(author.nickname || author.username || `${t("meta.author")} ${post.author_id}`);
  const avatarHtml = author.avatar_url
    ? `<img alt="" class="w-6 h-6 rounded-full object-cover shrink-0" src="${escapeHtml(author.avatar_url)}" />`
    : `<div class="w-6 h-6 rounded-full bg-primary-container/20 shrink-0 flex items-center justify-center text-label-md text-primary-container font-bold">${escapeHtml(String(post.author_id).slice(-1))}</div>`;
  const thumbSrc = fm && fm.url ? escapeHtml(fm.url) : placeholderImg(post.id);
  const heartClass = liked ? "fill text-primary-container" : "text-gray-400";

  const article = document.createElement("article");
  article.className =
    "bg-white rounded-[24px] overflow-hidden shadow-[0_4px_20px_rgba(255,90,119,0.04)] hover:-translate-y-1 transition-transform duration-300 cursor-pointer border border-gray-50";

  article.innerHTML = `
    <div class="${aspect} relative p-1 group">
      <img alt="" class="w-full h-full object-cover rounded-t-[20px] rounded-b-[8px] bg-surface-container-low" src="${thumbSrc}" data-thumb />
      <div class="video-overlay absolute inset-0 flex items-center justify-center pointer-events-none hidden">
        <div class="w-12 h-12 bg-white/30 backdrop-blur-md rounded-full flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform">
          <span class="material-symbols-outlined text-white text-[28px] fill">play_arrow</span>
        </div>
      </div>
    </div>
    <div class="p-4">
      ${tagHtml}
      <h3 class="text-body-lg font-body-lg text-on-surface line-clamp-2 mb-2">${escapeHtml(post.title)}</h3>
      <div class="flex items-center justify-between mt-auto gap-2">
        <div class="flex items-center gap-2 min-w-0">
          ${avatarHtml}
          <span class="text-label-md font-label-md text-gray-500 truncate">${authorLabel}</span>
        </div>
        <button type="button" class="like-btn flex items-center gap-1 shrink-0 rounded-full px-1 py-0.5 hover:bg-primary-container/5" data-post-id="${post.id}" aria-label="like">
          <span class="material-symbols-outlined text-[16px] like-heart ${heartClass}">favorite</span>
          <span class="text-label-md font-label-md like-count-num ${liked ? "text-primary-container" : "text-gray-500"}">${escapeHtml(formatCompactCount(likeCount))}</span>
        </button>
      </div>
    </div>`;

  article.addEventListener("click", () => {
    window.location.href = `/post.html?id=${post.id}`;
  });

  const likeBtn = article.querySelector(".like-btn");
  if (likeBtn) {
    likeBtn.addEventListener("click", async (ev) => {
      ev.preventDefault();
      ev.stopPropagation();
      if (!getToken()) {
        notify(t("stitch.like_login"), "info");
        const back = encodeURIComponent(location.pathname + location.search + location.hash);
        location.href = `/login?return_to=${back}`;
        return;
      }
      try {
        const res = await apiCall(`/api/v1/posts/${post.id}/like`, "POST");
        liked = res.liked;
        const heart = likeBtn.querySelector(".like-heart");
        const countEl = likeBtn.querySelector(".like-count-num");
        if (countEl) countEl.textContent = formatCompactCount(res.like_count);
        if (heart) {
          heart.classList.toggle("fill", !!res.liked);
          heart.classList.toggle("text-primary-container", !!res.liked);
          heart.classList.toggle("text-gray-400", !res.liked);
        }
        if (countEl) {
          countEl.classList.toggle("text-primary-container", !!res.liked);
          countEl.classList.toggle("text-gray-500", !res.liked);
        }
      } catch (e) {
        notify(e.message, "error");
      }
    });
  }

  const img = article.querySelector("[data-thumb]");
  const overlay = article.querySelector(".video-overlay");
  if (fm && fm.kind === "video" && overlay) {
    overlay.classList.remove("hidden");
  }
  const mid = post.media_ids && post.media_ids[0];
  if (!(fm && fm.url) && mid) hydratePostThumb(img, overlay, mid);

  return article;
}

async function apiCall(url, method = "GET", body, options = {}) {
  const init = {
    method,
    headers: { "Content-Type": "application/json" },
    signal: options.signal,
  };
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
    if (response.status === 401 && token) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      requireLogin();
    }
    const message = (payload && payload.message) || "request failed";
    throw new Error(message);
  }
  return payload && payload.data !== undefined ? payload.data : payload;
}

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function applyAuthState() {
  const token = getToken();
  const rawUser = localStorage.getItem(USER_KEY);
  const user = rawUser ? JSON.parse(rawUser) : null;

  if (token && user) {
    if (logoutBtn) logoutBtn.classList.remove("hidden");
    if (loginLink) loginLink.classList.add("hidden");
    if (registerLink) registerLink.classList.add("hidden");
    if (dashboardLink) dashboardLink.classList.remove("hidden");
    if (userHint) userHint.textContent = t("user.greeting").replace("{name}", user.nickname || user.username);
    refreshNotifCount();
  } else {
    if (logoutBtn) logoutBtn.classList.add("hidden");
    if (loginLink) loginLink.classList.remove("hidden");
    if (registerLink) registerLink.classList.remove("hidden");
    if (dashboardLink) dashboardLink.classList.add("hidden");
    if (userHint) userHint.textContent = "";
    if (sidebarNotifDot) sidebarNotifDot.classList.add("hidden");
    if (bottomNotifDot) bottomNotifDot.classList.add("hidden");
  }
}

function notify(message, kind) {
  if (window.MeowShared && typeof window.MeowShared.toast === "function") {
    window.MeowShared.toast(message, kind);
    return;
  }
  alert(message);
}

async function renderInBatches(container, items, renderItem, options = {}) {
  const batchSize = options.batchSize || 12;
  const preserveExisting = options.preserveExisting === true;
  const jobId = Symbol("render-job");
  activeRenderJobs.set(container, jobId);
  if (!preserveExisting) container.innerHTML = "";
  for (let i = 0; i < items.length; i += batchSize) {
    if (activeRenderJobs.get(container) !== jobId) return;
    const frag = document.createDocumentFragment();
    const batch = items.slice(i, i + batchSize);
    for (const item of batch) {
      frag.appendChild(renderItem(item));
    }
    container.appendChild(frag);
    if (i + batchSize < items.length) {
      await nextFrame();
    }
  }
}

function nextFrame() {
  return new Promise((resolve) => requestAnimationFrame(() => resolve()));
}

function takeRequestSignal(key) {
  cancelRequest(key);
  const controller = new AbortController();
  activeRequestControllers.set(key, controller);
  return controller.signal;
}

function cancelRequest(key) {
  const controller = activeRequestControllers.get(key);
  if (controller) {
    controller.abort();
    activeRequestControllers.delete(key);
  }
}

function escapeHtml(input) {
  return String(input)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

applyAuthState();
loadPosts();
