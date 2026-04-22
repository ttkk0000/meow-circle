const TOKEN_KEY = "meow_token";
const USER_KEY = "meow_user";

const t = (k) => window.MeowShared.t(k);

const userHint = document.querySelector("#user-hint");
const logoutBtn = document.querySelector("#logout-btn");
const loginLink = document.querySelector("#login-link");
const registerLink = document.querySelector("#register-link");
const dashboardLink = document.querySelector("#dashboard-link");

const postsContainer = document.querySelector("#posts");
const listingsContainer = document.querySelector("#listings");
const postTemplate = document.querySelector("#post-item-template");
const listingTemplate = document.querySelector("#listing-item-template");
const activeRenderJobs = new WeakMap();
const activeRequestControllers = new Map();

function requireLogin(reason) {
  if (reason) {
    notify(reason, "error");
  }
  const back = encodeURIComponent(location.pathname + location.search + location.hash);
  location.href = `/login?return_to=${back}`;
}

const refreshPostsBtn = document.querySelector("#refresh-posts");
const refreshListingsBtn = document.querySelector("#refresh-listings");
if (refreshPostsBtn) refreshPostsBtn.addEventListener("click", () => runRefresh(refreshPostsBtn, loadPosts));
if (refreshListingsBtn) refreshListingsBtn.addEventListener("click", () => runRefresh(refreshListingsBtn, loadListings));

const heroPrimary = document.querySelector("#hero-primary");
if (heroPrimary) {
  heroPrimary.addEventListener("click", () => {
    if (getToken()) {
      window.location.href = "/dashboard#compose-post";
    } else {
      requireLogin();
    }
  });
}

// ===== Global search =====
const searchInput = document.querySelector("#global-search");
const searchResultsPanel = document.querySelector("#search-results");
const searchPostsNode = document.querySelector("#search-posts");
const searchListingsNode = document.querySelector("#search-listings");
const closeSearchBtn = document.querySelector("#close-search");
let currentSearchType = "all";
let lastSearchQuery = "";

if (searchInput) {
  let timer = null;
  searchInput.addEventListener("input", () => {
    clearTimeout(timer);
    const q = searchInput.value.trim();
    if (!q) {
      hideSearchResults();
      return;
    }
    timer = setTimeout(() => runSearch(q, currentSearchType), 300);
  });
  searchInput.addEventListener("keydown", (ev) => {
    if (ev.key === "Escape") {
      searchInput.value = "";
      hideSearchResults();
    }
  });
}
if (closeSearchBtn) closeSearchBtn.addEventListener("click", () => { searchInput.value = ""; hideSearchResults(); });

document.querySelectorAll('[data-search-tab]').forEach((btn) => {
  btn.addEventListener("click", () => {
    document.querySelectorAll('[data-search-tab]').forEach((b) => b.classList.remove("active"));
    btn.classList.add("active");
    currentSearchType = btn.dataset.searchTab;
    if (lastSearchQuery) runSearch(lastSearchQuery, currentSearchType);
  });
});

function hideSearchResults() {
  if (searchResultsPanel) searchResultsPanel.hidden = true;
  lastSearchQuery = "";
  cancelRequest("search");
}

async function runSearch(q, type) {
  lastSearchQuery = q;
  searchResultsPanel.hidden = false;
  searchPostsNode.innerHTML = skeletonRows(3);
  searchListingsNode.innerHTML = "";
  const signal = takeRequestSignal("search");
  try {
    const data = await apiCall(`/api/v1/search?q=${encodeURIComponent(q)}&type=${type}`, "GET", undefined, { signal });
    await renderSearchSection(searchPostsNode, data.posts || [], "post");
    await renderSearchSection(searchListingsNode, data.listings || [], "listing");
  } catch (err) {
    if (err && err.name === "AbortError") return;
    searchPostsNode.innerHTML = `<p>${escapeHtml(err.message)}</p>`;
  }
}

async function renderSearchSection(container, items, kind) {
  container.innerHTML = "";
  if (items.length === 0) {
    container.innerHTML = renderEditorialEmptyState(t("search.no_result"), "Nº S");
    return;
  }
  const title = document.createElement("div");
  title.className = "group-title";
  title.textContent = kind === "post" ? t("search.tab_posts") : t("search.tab_listings");
  container.appendChild(title);
  await renderInBatches(container, items, (item) => buildSearchItemNode(item, kind), { batchSize: 14, preserveExisting: true });
}

function skeletonRows(n) {
  return Array.from({ length: n })
    .map(() => `<div class="skeleton" style="height:72px;margin-bottom:10px;"></div>`)
    .join("");
}

// ===== Notifications badge =====
const notifBtn = document.querySelector("#notif-btn");
const notifCount = document.querySelector("#notif-count");
if (notifBtn) {
  notifBtn.addEventListener("click", () => { window.location.href = "/dashboard#notifications"; });
}

async function refreshNotifCount() {
  if (!getToken() || !notifBtn) return;
  try {
    const data = await apiCall("/api/v1/notifications?unread=true", "GET");
    const c = (data && typeof data.unread_count === "number") ? data.unread_count : (data.items || []).length;
    if (c > 0) {
      notifCount.textContent = c > 99 ? "99+" : String(c);
      notifCount.hidden = false;
    } else {
      notifCount.hidden = true;
    }
  } catch (_) { /* ignore */ }
}

logoutBtn.addEventListener("click", () => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  applyAuthState();
  loadPosts();
});

async function loadPosts() {
  postsContainer.innerHTML = skeletonRows(3);
  const signal = takeRequestSignal("posts");
  try {
    const data = await apiCall("/api/v1/posts?page=1&page_size=20", "GET", undefined, { signal });
    const items = Array.isArray(data.items) ? data.items : [];
    postsContainer.innerHTML = "";
    updateStat("stat-posts", data.total ?? items.length);
    if (items.length === 0) {
      postsContainer.innerHTML = renderEditorialEmptyState(t("common.empty_posts_hint"), "Nº 01");
      return;
    }
    await renderInBatches(postsContainer, items, buildPostNode, { batchSize: 8 });
  } catch (err) {
    if (err && err.name === "AbortError") return;
    postsContainer.innerHTML = `<p>${escapeHtml(t("common.error_load_posts"))}: ${escapeHtml(err.message)}</p>`;
  }
}

function updateStat(id, value) {
  const el = document.getElementById(id);
  if (el) el.textContent = String(value);
}

function buildPostNode(post) {
  const node = postTemplate.content.cloneNode(true);
  window.MeowShared.applyI18n(node);
  node.querySelector(".item-title").textContent = post.title;
  node.querySelector(".category").textContent = post.category || "daily_share";
  node.querySelector(".item-content").textContent = post.content;
  node.querySelector(".meta").textContent =
    `${t("meta.author")} ${post.author_id} · ${t("meta.published_at")} ${formatTime(post.created_at)}`;

  const article = node.querySelector("article.item");
  const actionBar = document.createElement("div");
  actionBar.className = "item-actions";
  actionBar.appendChild(buildReportButton("post", post.id));
  article.insertBefore(actionBar, article.querySelector("details"));

  const details = node.querySelector("details");
  const commentsNode = node.querySelector(".comments");
  const commentForm = node.querySelector(".comment-form");
  let mediaNode = null;

  details.addEventListener("toggle", async () => {
    if (!details.open) return;
    try {
      const detail = await apiCall(`/api/v1/posts/${post.id}`, "GET");
      if (mediaNode) mediaNode.remove();
      const mediaItems = Array.isArray(detail.media) ? detail.media : [];
      if (mediaItems.length) {
        mediaNode = renderMediaGallery(mediaItems);
        details.insertBefore(mediaNode, commentsNode);
      }
      const comments = Array.isArray(detail.comments) ? detail.comments : [];
      commentsNode.innerHTML = comments.length
        ? comments
            .map(
              (comment) =>
                `<div class="comment">#${comment.id} ${escapeHtml(t("meta.author"))} ${comment.author_id}: ${escapeHtml(comment.content)}</div>`
            )
            .join("")
        : `<p>${escapeHtml(t("common.no_comments"))}</p>`;
    } catch (err) {
      commentsNode.innerHTML = `<p>${escapeHtml(err.message)}</p>`;
    }
  });

  commentForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!getToken()) {
      requireLogin(t("alert.login_to_comment"));
      return;
    }
    const fd = new FormData(commentForm);
    try {
      await apiCall(`/api/v1/posts/${post.id}/comments`, "POST", {
        content: String(fd.get("content") || ""),
      });
      commentForm.reset();
      details.open = true;
      details.dispatchEvent(new Event("toggle"));
    } catch (err) {
      notify(err.message, "error");
    }
  });

  return node;
}

async function loadListings() {
  listingsContainer.innerHTML = skeletonRows(3);
  const signal = takeRequestSignal("listings");
  try {
    const data = await apiCall("/api/v1/listings?page=1&page_size=20", "GET", undefined, { signal });
    const items = Array.isArray(data.items) ? data.items : [];
    listingsContainer.innerHTML = "";
    updateStat("stat-listings", data.total ?? items.length);
    if (items.length === 0) {
      listingsContainer.innerHTML = renderEditorialEmptyState(t("common.empty_listings"), "Nº 02");
      return;
    }
    await renderInBatches(listingsContainer, items, buildListingNode, { batchSize: 10 });
  } catch (err) {
    if (err && err.name === "AbortError") return;
    listingsContainer.innerHTML = `<p>${escapeHtml(t("common.error_load_listings"))}: ${escapeHtml(err.message)}</p>`;
  }
}

function buildListingNode(listing) {
  const node = listingTemplate.content.cloneNode(true);
  window.MeowShared.applyI18n(node);
  node.querySelector(".item-title").textContent = listing.title;
  node.querySelector(".type").textContent = listing.type || "product";
  node.querySelector(".item-content").textContent = listing.description || t("common.no_description");
  node.querySelector(".meta").textContent =
    `${t("meta.seller")} ${listing.seller_id} · ${t("meta.price")} ${(listing.price_cents / 100).toFixed(2)} ${listing.currency}`;
  const article = node.querySelector("article.item");
  const actions = document.createElement("div");
  actions.className = "item-actions";
  if (listing.price_cents > 0) {
    const buyBtn = document.createElement("button");
    buyBtn.type = "button";
    buyBtn.textContent = t("listing.btn_buy");
    buyBtn.addEventListener("click", () => buyListing(listing));
    actions.appendChild(buyBtn);
  }
  actions.appendChild(buildReportButton("listing", listing.id));
  article.appendChild(actions);

  if (Array.isArray(listing.media_ids) && listing.media_ids.length) {
    const detailsEl = document.createElement("details");
    detailsEl.innerHTML = `<summary>${escapeHtml(t("post.view_comments"))}</summary>`;
    const holder = document.createElement("div");
    detailsEl.appendChild(holder);
    let loaded = false;
    detailsEl.addEventListener("toggle", async () => {
      if (!detailsEl.open || loaded) return;
      try {
        const detail = await apiCall(`/api/v1/listings/${listing.id}`, "GET");
        const mediaItems = Array.isArray(detail.media) ? detail.media : [];
        if (mediaItems.length) holder.appendChild(renderMediaGallery(mediaItems));
        loaded = true;
      } catch (err) {
        holder.innerHTML = `<p>${escapeHtml(err.message)}</p>`;
      }
    });
    article.appendChild(detailsEl);
  }
  return node;
}

async function apiCall(url, method = "GET", body, options = {}) {
  const init = {
    method,
    headers: { "Content-Type": "application/json" },
    signal: options.signal,
  };
  const token = getToken();
  if (token) {
    init.headers.Authorization = `Bearer ${token}`;
  }
  if (body !== undefined) {
    init.body = JSON.stringify(body);
  }
  const response = await fetch(url, init);
  let payload = null;
  try {
    payload = await response.json();
  } catch (_) {
    payload = null;
  }
  if (!response.ok) {
    if (response.status === 401 && token) {
      /* Token expired: force login again preserving return_to */
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
    if (logoutBtn) logoutBtn.hidden = false;
    if (loginLink) loginLink.hidden = true;
    if (registerLink) registerLink.hidden = true;
    if (dashboardLink) dashboardLink.hidden = false;
    if (notifBtn) notifBtn.hidden = false;
    userHint.textContent = t("user.greeting").replace("{name}", user.nickname || user.username);
    refreshNotifCount();
  } else {
    if (logoutBtn) logoutBtn.hidden = true;
    if (loginLink) loginLink.hidden = false;
    if (registerLink) registerLink.hidden = false;
    if (dashboardLink) dashboardLink.hidden = true;
    if (notifBtn) notifBtn.hidden = true;
    userHint.textContent = "";
  }
}

function formatTime(value) {
  if (!value) return t("common.unknown");
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return t("common.unknown");
  return date.toLocaleString();
}

function renderMediaGallery(items) {
  const wrap = document.createElement("div");
  wrap.className = "media-gallery";
  for (const m of items) {
    if (m.kind === "video") {
      const v = document.createElement("video");
      v.src = m.url;
      v.controls = true;
      v.preload = "metadata";
      wrap.appendChild(v);
    } else {
      const img = document.createElement("img");
      img.src = m.url;
      img.alt = "";
      wrap.appendChild(img);
    }
  }
  return wrap;
}

function buildReportButton(kind, id) {
  const btn = document.createElement("button");
  btn.type = "button";
  btn.className = "link";
  btn.textContent = t("report.btn");
  btn.addEventListener("click", async () => {
    if (!getToken()) {
      requireLogin(t("alert.login_first"));
      return;
    }
    const reason = prompt(t("report.placeholder"));
    if (!reason) return;
    try {
      await apiCall("/api/v1/reports", "POST", { target_kind: kind, target_id: id, reason });
      notify(t("report.submitted"), "success");
    } catch (err) {
      notify(err.message, "error");
    }
  });
  return btn;
}

async function buyListing(listing) {
  if (!getToken()) {
    requireLogin(t("alert.login_first"));
    return;
  }
  try {
    const order = await apiCall("/api/v1/orders", "POST", { listing_id: listing.id });
    notify(`${t("listing.btn_buy")} #${order.id} · ${t("order.status.pending_payment")}`, "success");
  } catch (err) {
    notify(err.message, "error");
  }
}

function notify(message, kind) {
  if (window.MeowShared && typeof window.MeowShared.toast === "function") {
    window.MeowShared.toast(message, kind);
    return;
  }
  alert(message);
}

function renderEditorialEmptyState(message, mark) {
  return `<div class="empty-state empty-state-editorial"><div class="empty-mark" aria-hidden="true"><span class="index-num">${escapeHtml(mark)}</span></div><p>${escapeHtml(message)}</p></div>`;
}

async function runRefresh(button, loader) {
  if (!button || typeof loader !== "function") return;
  button.classList.add("is-loading");
  try {
    await loader();
    button.classList.remove("is-loading");
    button.classList.add("is-success");
    setTimeout(() => button.classList.remove("is-success"), 720);
  } catch (err) {
    button.classList.remove("is-loading");
    button.classList.add("is-error");
    setTimeout(() => button.classList.remove("is-error"), 720);
    notify(err.message, "error");
  }
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

function initKittyShowcase() {
  const cards = Array.from(document.querySelectorAll(".kitty-photo-card"));
  if (cards.length === 0) return;
  const pool = [
    {
      src: "https://images.unsplash.com/photo-1511044568932-338cba0ad803?auto=format&fit=crop&w=900&q=80",
      alt: "好奇地看向镜头的猫",
      caption: "“今天是认真营业的小店长”",
      likes: 246,
      comments: 38,
    },
    {
      src: "https://images.unsplash.com/photo-1495360010541-f48722b34f7d?auto=format&fit=crop&w=900&q=80",
      alt: "在毯子上休息的猫",
      caption: "“午后打盹，主打一个治愈”",
      likes: 312,
      comments: 54,
    },
    {
      src: "https://images.unsplash.com/photo-1573865526739-10659fec78a5?auto=format&fit=crop&w=900&q=80",
      alt: "坐在窗边的橘猫",
      caption: "“窗边观察员，已上线巡逻”",
      likes: 189,
      comments: 29,
    },
    {
      src: "https://images.unsplash.com/photo-1519052537078-e6302a4968d4?auto=format&fit=crop&w=900&q=80",
      alt: "在木桌上趴着的灰白猫",
      caption: "“值班结束，申请摸摸奖励”",
      likes: 278,
      comments: 41,
    },
    {
      src: "https://images.unsplash.com/photo-1536589961747-e239b2c7d4d0?auto=format&fit=crop&w=900&q=80",
      alt: "看向窗外的黑白猫",
      caption: "“今天也在认真研究窗外新闻”",
      likes: 334,
      comments: 47,
    },
  ];

  let tick = 0;
  const swapCard = (card, data) => {
    const img = card.querySelector("img");
    const caption = card.querySelector(".caption");
    const meta = card.querySelector(".kitty-card-meta");
    if (!img || !caption || !meta) return;
    img.classList.add("is-swapping");
    setTimeout(() => {
      img.src = data.src;
      img.alt = data.alt;
      caption.textContent = data.caption;
      meta.innerHTML = `<span>❤ ${data.likes}</span><span>💬 ${data.comments}</span>`;
      img.classList.remove("is-swapping");
    }, 170);
  };

  setInterval(() => {
    tick += 1;
    cards.forEach((card, idx) => {
      const data = pool[(tick + idx) % pool.length];
      swapCard(card, data);
    });
  }, 7000);
}

function buildSearchItemNode(item, kind) {
  const el = document.createElement("article");
  el.className = "item";
  const head = document.createElement("div");
  head.className = "item-head";
  head.innerHTML = `<h3 class="item-title">${escapeHtml(item.title)}</h3><span class="pill">${escapeHtml(kind)}</span>`;
  const body = document.createElement("p");
  body.className = "item-content";
  body.textContent = kind === "post" ? (item.content || "") : (item.description || "");
  const meta = document.createElement("p");
  meta.className = "meta";
  if (kind === "post") {
    meta.textContent = `${t("meta.author")} ${item.author_id} · ${formatTime(item.created_at)}`;
  } else {
    meta.textContent = `${t("meta.seller")} ${item.seller_id} · ${t("meta.price")} ${(item.price_cents / 100).toFixed(2)} ${item.currency}`;
  }
  el.append(head, body, meta);
  return el;
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
initKittyShowcase();
loadPosts();
loadListings();
