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

function requireLogin(reason) {
  if (reason) {
    try { alert(reason); } catch (_) {}
  }
  const back = encodeURIComponent(location.pathname + location.search + location.hash);
  location.href = `/login?return_to=${back}`;
}

document.querySelector("#refresh-posts").addEventListener("click", () => loadPosts());
document.querySelector("#refresh-listings").addEventListener("click", () => loadListings());

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
}

async function runSearch(q, type) {
  lastSearchQuery = q;
  searchResultsPanel.hidden = false;
  searchPostsNode.innerHTML = skeletonRows(3);
  searchListingsNode.innerHTML = "";
  try {
    const data = await apiCall(`/api/v1/search?q=${encodeURIComponent(q)}&type=${type}`, "GET");
    renderSearchSection(searchPostsNode, data.posts || [], "post");
    renderSearchSection(searchListingsNode, data.listings || [], "listing");
  } catch (err) {
    searchPostsNode.innerHTML = `<p>${escapeHtml(err.message)}</p>`;
  }
}

function renderSearchSection(container, items, kind) {
  container.innerHTML = "";
  if (items.length === 0) {
    container.innerHTML = `<p class="empty-state">${escapeHtml(t("search.no_result"))}</p>`;
    return;
  }
  const title = document.createElement("div");
  title.className = "group-title";
  title.textContent = kind === "post" ? t("search.tab_posts") : t("search.tab_listings");
  container.appendChild(title);
  for (const item of items) {
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
    container.appendChild(el);
  }
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
  try {
    const data = await apiCall("/api/v1/posts?page=1&page_size=20", "GET");
    const items = Array.isArray(data.items) ? data.items : [];
    postsContainer.innerHTML = "";
    updateStat("stat-posts", data.total ?? items.length);
    if (items.length === 0) {
      postsContainer.innerHTML = `<div class="empty-state"><div class="hint-icon">📝</div><p>${escapeHtml(t("common.empty_posts_hint"))}</p></div>`;
      return;
    }
    for (const post of items) {
      renderPost(post);
    }
  } catch (err) {
    postsContainer.innerHTML = `<p>${escapeHtml(t("common.error_load_posts"))}: ${escapeHtml(err.message)}</p>`;
  }
}

function updateStat(id, value) {
  const el = document.getElementById(id);
  if (el) el.textContent = String(value);
}

function renderPost(post) {
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
      alert(err.message);
    }
  });

  postsContainer.appendChild(node);
}

async function loadListings() {
  listingsContainer.innerHTML = skeletonRows(3);
  try {
    const data = await apiCall("/api/v1/listings?page=1&page_size=20", "GET");
    const items = Array.isArray(data.items) ? data.items : [];
    listingsContainer.innerHTML = "";
    updateStat("stat-listings", data.total ?? items.length);
    if (items.length === 0) {
      listingsContainer.innerHTML = `<div class="empty-state"><div class="hint-icon">🛒</div><p>${escapeHtml(t("common.empty_listings"))}</p></div>`;
      return;
    }
    for (const listing of items) {
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
      listingsContainer.appendChild(node);
    }
  } catch (err) {
    listingsContainer.innerHTML = `<p>${escapeHtml(t("common.error_load_listings"))}: ${escapeHtml(err.message)}</p>`;
  }
}

async function apiCall(url, method = "GET", body) {
  const init = {
    method,
    headers: { "Content-Type": "application/json" },
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
      alert(t("report.submitted"));
    } catch (err) {
      alert(err.message);
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
    alert(`${t("listing.btn_buy")} #${order.id} · ${t("order.status.pending_payment")}`);
  } catch (err) {
    alert(err.message);
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
loadListings();
