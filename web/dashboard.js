const TOKEN_KEY = "meow_token";
const USER_KEY = "meow_user";

const t = (k) => window.MeowShared.t(k);

const notLogin = document.querySelector("#not-login");
const dashboard = document.querySelector("#dashboard");
const userHint = document.querySelector("#user-hint");
const logoutBtn = document.querySelector("#logout-btn");

const postForm = document.querySelector("#post-form");
const listingForm = document.querySelector("#listing-form");
const myPostsNode = document.querySelector("#my-posts");
const myListingsNode = document.querySelector("#my-listings");
const mediaForm = document.querySelector("#media-form");
const mediaFile = document.querySelector("#media-file");
const mediaList = document.querySelector("#media-list");
const myOrdersNode = document.querySelector("#my-orders");
let ordersRole = "buyer";

const profileForm = document.querySelector("#profile-form");
const notificationsList = document.querySelector("#notifications-list");
const convListNode = document.querySelector("#conv-list");
const convMessagesNode = document.querySelector("#conv-messages");
const messageForm = document.querySelector("#message-form");
const notifBtn = document.querySelector("#notif-btn");
const notifCount = document.querySelector("#notif-count");
let activePeerId = null;

logoutBtn.addEventListener("click", () => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  window.location.href = "/";
});

document.querySelectorAll(".nav-btn").forEach((btn) => {
  btn.addEventListener("click", () => {
    document.querySelectorAll(".nav-btn").forEach((b) => b.classList.remove("active"));
    btn.classList.add("active");
    const target = btn.dataset.section;
    document.querySelectorAll(".panel").forEach((panel) => {
      panel.hidden = panel.dataset.panel !== target;
    });
    if (target === "my-posts") loadMyPosts();
    if (target === "my-listings") loadMyListings();
    if (target === "media") loadMedia();
    if (target === "my-orders") loadMyOrders();
    if (target === "profile") loadProfile();
    if (target === "notifications") loadNotifications();
    if (target === "messages") loadConversations();
  });
});

function activateSection(name) {
  const btn = document.querySelector(`.nav-btn[data-section="${name}"]`);
  if (btn) btn.click();
}

if (window.location.hash) {
  const target = window.location.hash.slice(1);
  setTimeout(() => activateSection(target), 0);
}

if (notifBtn) {
  notifBtn.addEventListener("click", () => activateSection("notifications"));
}

document.querySelector("#refresh-my-posts").addEventListener("click", () => loadMyPosts());
document.querySelector("#refresh-my-listings").addEventListener("click", () => loadMyListings());
document.querySelector("#refresh-media").addEventListener("click", () => loadMedia());
document.querySelector("#refresh-my-orders").addEventListener("click", () => loadMyOrders());

document.querySelector("#orders-tab-buyer").addEventListener("click", () => switchOrdersTab("buyer"));
document.querySelector("#orders-tab-seller").addEventListener("click", () => switchOrdersTab("seller"));

function switchOrdersTab(role) {
  ordersRole = role;
  document.querySelector("#orders-tab-buyer").classList.toggle("active", role === "buyer");
  document.querySelector("#orders-tab-seller").classList.toggle("active", role === "seller");
  loadMyOrders();
}

mediaForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const file = mediaFile.files && mediaFile.files[0];
  if (!file) return;
  try {
    const fd = new FormData();
    fd.append("file", file);
    await apiUpload("/api/v1/media", fd);
    mediaForm.reset();
    await loadMedia();
  } catch (err) {
    alert(err.message);
  }
});

async function loadMedia() {
  try {
    const data = await apiCall("/api/v1/media", "GET");
    const items = Array.isArray(data.items) ? data.items : [];
    mediaList.innerHTML = items.length ? "" : `<p>${escapeHtml(t("media.empty"))}</p>`;
    for (const m of items) {
      const el = document.createElement("article");
      el.className = "item";
      const preview = m.kind === "video"
        ? `<video src="${escapeHtml(m.url)}" controls preload="metadata" style="max-width:100%;border-radius:8px"></video>`
        : `<img src="${escapeHtml(m.url)}" alt="" style="max-width:100%;border-radius:8px" />`;
      el.innerHTML = `
        ${preview}
        <p class="meta">#${m.id} · ${escapeHtml(m.kind)} · ${escapeHtml(m.mime)} · ${(m.size/1024).toFixed(1)} KB · ${escapeHtml(m.status)}</p>
        <div class="item-actions">
          <button type="button" class="copy-id" data-id="${m.id}">${escapeHtml(t("media.copy_id"))}</button>
          <button type="button" class="danger" data-id="${m.id}">${escapeHtml(t("common.delete"))}</button>
        </div>
      `;
      el.querySelector(".copy-id").addEventListener("click", () => {
        navigator.clipboard && navigator.clipboard.writeText(String(m.id));
      });
      el.querySelector("button.danger").addEventListener("click", async () => {
        if (!confirm(`${t("common.delete")} #${m.id}?`)) return;
        try {
          await apiCall(`/api/v1/media/${m.id}`, "DELETE");
          await loadMedia();
        } catch (err) {
          alert(err.message);
        }
      });
      mediaList.appendChild(el);
    }
  } catch (err) {
    mediaList.innerHTML = `<p>${escapeHtml(err.message)}</p>`;
  }
}

async function loadMyOrders() {
  try {
    const data = await apiCall(`/api/v1/me/orders?role=${encodeURIComponent(ordersRole)}`, "GET");
    const items = Array.isArray(data.items) ? data.items : [];
    myOrdersNode.innerHTML = items.length ? "" : `<p>${escapeHtml(t("order.empty"))}</p>`;
    for (const o of items) {
      const el = document.createElement("article");
      el.className = "item";
      const price = `${(o.amount_cents / 100).toFixed(2)} ${escapeHtml(o.currency || "CNY")}`;
      const statusLabel = t(`order.status.${o.status}`);
      el.innerHTML = `
        <p><strong>#${o.id}</strong> ${escapeHtml(o.listing_title || "")} · ${price}</p>
        <p class="meta">${escapeHtml(t("meta.author"))} ${o.buyer_id} → ${escapeHtml(t("meta.seller"))} ${o.seller_id} · <b>${escapeHtml(statusLabel)}</b></p>
        <div class="item-actions" data-order-id="${o.id}"></div>
      `;
      const actions = el.querySelector(".item-actions");
      orderActions(o).forEach((btn) => actions.appendChild(btn));
      myOrdersNode.appendChild(el);
    }
  } catch (err) {
    myOrdersNode.innerHTML = `<p>${escapeHtml(err.message)}</p>`;
  }
}

function orderActions(order) {
  const buttons = [];
  const add = (labelKey, action, danger) => {
    const b = document.createElement("button");
    b.type = "button";
    b.textContent = t(labelKey);
    if (danger) b.className = "danger";
    b.addEventListener("click", () => doOrderAction(order.id, action));
    buttons.push(b);
  };
  const isBuyer = ordersRole === "buyer";
  const isSeller = ordersRole === "seller";

  if (isBuyer && order.status === "pending_payment") {
    add("order.btn_pay", "pay");
    add("order.btn_cancel", "cancel", true);
  }
  if (isSeller && order.status === "paid") {
    add("order.btn_ship", "ship");
    add("order.btn_refund", "refund", true);
  }
  if (isBuyer && order.status === "shipped") {
    add("order.btn_complete", "complete");
  }
  if (isSeller && order.status === "shipped") {
    add("order.btn_refund", "refund", true);
  }
  return buttons;
}

async function doOrderAction(id, action) {
  try {
    const body = action === "pay" ? { method: "mock" } : undefined;
    await apiCall(`/api/v1/orders/${id}/${action}`, "POST", body);
    await loadMyOrders();
  } catch (err) {
    alert(err.message);
  }
}

postForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const fd = new FormData(postForm);
    const tags = splitCsv(String(fd.get("tags") || ""));
    const mediaIds = splitCsv(String(fd.get("media_ids") || "")).map(Number).filter(Boolean);
    await apiCall("/api/v1/posts", "POST", {
      title: String(fd.get("title") || ""),
      content: String(fd.get("content") || ""),
      category: String(fd.get("category") || "daily_share"),
      tags,
      media_ids: mediaIds,
    });
    postForm.reset();
    alert(t("alert.publish_success"));
  } catch (err) {
    alert(err.message);
  }
});

listingForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const fd = new FormData(listingForm);
    const mediaIds = splitCsv(String(fd.get("media_ids") || "")).map(Number).filter(Boolean);
    await apiCall("/api/v1/listings", "POST", {
      type: String(fd.get("type") || "product"),
      title: String(fd.get("title") || ""),
      description: String(fd.get("description") || ""),
      price_cents: Number(fd.get("price_cents") || 0),
      currency: String(fd.get("currency") || "CNY"),
      media_ids: mediaIds,
    });
    listingForm.reset();
    alert(t("alert.publish_success"));
  } catch (err) {
    alert(err.message);
  }
});

function splitCsv(raw) {
  return raw.split(",").map((item) => item.trim()).filter(Boolean);
}

async function loadMyPosts() {
  try {
    const data = await apiCall("/api/v1/me/posts", "GET");
    const items = Array.isArray(data.items) ? data.items : [];
    myPostsNode.innerHTML = items.length ? "" : `<p>${escapeHtml(t("common.empty_my_posts"))}</p>`;
    for (const post of items) {
      const el = document.createElement("article");
      el.className = "item";
      el.innerHTML = `
        <div class="item-head">
          <h3 class="item-title">${escapeHtml(post.title)}</h3>
          <span class="pill category">${escapeHtml(post.category || "")}</span>
        </div>
        <p class="item-content">${escapeHtml(post.content)}</p>
        <p class="meta">${escapeHtml(t("meta.published_at"))} ${formatTime(post.created_at)} · ${escapeHtml(t("meta.last_reply"))} ${formatTime(post.last_reply_at)}</p>
        <div class="item-actions">
          <button class="danger" data-id="${post.id}">${escapeHtml(t("common.delete"))}</button>
        </div>
      `;
      el.querySelector("button.danger").addEventListener("click", async () => {
        if (!confirm(t("common.confirm_delete_post") + ` #${post.id}`)) return;
        try {
          await apiCall(`/api/v1/posts/${post.id}`, "DELETE");
          await loadMyPosts();
        } catch (err) {
          alert(err.message);
        }
      });
      myPostsNode.appendChild(el);
    }
  } catch (err) {
    myPostsNode.innerHTML = `<p>${escapeHtml(err.message)}</p>`;
  }
}

async function loadMyListings() {
  try {
    const data = await apiCall("/api/v1/me/listings", "GET");
    const items = Array.isArray(data.items) ? data.items : [];
    myListingsNode.innerHTML = items.length ? "" : `<p>${escapeHtml(t("common.empty_my_listings"))}</p>`;
    for (const listing of items) {
      const el = document.createElement("article");
      el.className = "item";
      el.innerHTML = `
        <div class="item-head">
          <h3 class="item-title">${escapeHtml(listing.title)}</h3>
          <span class="pill type">${escapeHtml(listing.type || "")}</span>
        </div>
        <p class="item-content">${escapeHtml(listing.description || "")}</p>
        <p class="meta">${escapeHtml(t("meta.price"))} ${(listing.price_cents / 100).toFixed(2)} ${escapeHtml(listing.currency)} · ${formatTime(listing.created_at)}</p>
        <div class="item-actions">
          <button class="danger" data-id="${listing.id}">${escapeHtml(t("common.delete"))}</button>
        </div>
      `;
      el.querySelector("button.danger").addEventListener("click", async () => {
        if (!confirm(t("common.confirm_delete_listing") + ` #${listing.id}`)) return;
        try {
          await apiCall(`/api/v1/listings/${listing.id}`, "DELETE");
          await loadMyListings();
        } catch (err) {
          alert(err.message);
        }
      });
      myListingsNode.appendChild(el);
    }
  } catch (err) {
    myListingsNode.innerHTML = `<p>${escapeHtml(err.message)}</p>`;
  }
}

async function apiUpload(url, formData) {
  const init = { method: "POST", body: formData, headers: {} };
  const token = getToken();
  if (token) init.headers.Authorization = `Bearer ${token}`;
  const response = await fetch(url, init);
  let payload = null;
  try { payload = await response.json(); } catch (_) {}
  if (!response.ok) {
    throw new Error((payload && payload.message) || "upload failed");
  }
  return payload && payload.data !== undefined ? payload.data : payload;
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
    const message = (payload && payload.message) || "request failed";
    throw new Error(message);
  }
  return payload && payload.data !== undefined ? payload.data : payload;
}

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function formatTime(value) {
  if (!value) return t("common.unknown");
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return t("common.unknown");
  return date.toLocaleString();
}

function escapeHtml(input) {
  return String(input)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

// ===== Profile =====
async function loadProfile() {
  try {
    const me = await apiCall("/api/v1/auth/me", "GET");
    profileForm.nickname.value = me.nickname || "";
    profileForm.avatar_url.value = me.avatar_url || "";
    profileForm.bio.value = me.bio || "";
    document.querySelector("#profile-name").textContent = me.nickname || me.username;
    document.querySelector("#profile-handle").textContent = "@" + me.username;
    const avatar = document.querySelector("#profile-avatar");
    if (me.avatar_url) { avatar.src = me.avatar_url; avatar.alt = me.username; } else { avatar.removeAttribute("src"); }
  } catch (err) {
    alert(err.message);
  }
}

if (profileForm) {
  profileForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const fd = new FormData(profileForm);
    try {
      const updated = await apiCall("/api/v1/me", "PATCH", {
        nickname: String(fd.get("nickname") || ""),
        avatar_url: String(fd.get("avatar_url") || ""),
        bio: String(fd.get("bio") || ""),
      });
      localStorage.setItem(USER_KEY, JSON.stringify(updated));
      userHint.textContent = t("user.greeting").replace("{name}", updated.nickname || updated.username);
      alert(t("profile.saved"));
      loadProfile();
    } catch (err) { alert(err.message); }
  });
}

// ===== Notifications =====
async function loadNotifications() {
  try {
    const data = await apiCall("/api/v1/notifications", "GET");
    const items = Array.isArray(data.items) ? data.items : [];
    notificationsList.innerHTML = items.length ? "" : `<div class="empty-state"><div class="hint-icon">🔔</div><p>${escapeHtml(t("notify.empty"))}</p></div>`;
    for (const n of items) {
      const el = document.createElement("div");
      el.className = "notification" + (n.read ? "" : " unread");
      el.innerHTML = `
        <span class="kind-badge">${escapeHtml(t("notify.kind_" + n.kind) || n.kind)}</span>
        <div class="notif-body">
          <div><strong>${escapeHtml(n.title)}</strong></div>
          <div>${escapeHtml(n.body || "")}</div>
          <div class="meta">${formatTime(n.created_at)}</div>
        </div>
      `;
      if (!n.read) {
        el.addEventListener("click", async () => {
          try { await apiCall(`/api/v1/notifications/${n.id}/read`, "POST"); } catch (_) {}
          loadNotifications();
          refreshNotifBadge();
        });
      }
      notificationsList.appendChild(el);
    }
    refreshNotifBadge();
  } catch (err) {
    notificationsList.innerHTML = `<p>${escapeHtml(err.message)}</p>`;
  }
}

const markAllBtn = document.querySelector("#mark-all-notifications");
if (markAllBtn) markAllBtn.addEventListener("click", async () => {
  try { await apiCall("/api/v1/notifications/read-all", "POST"); } catch (_) {}
  loadNotifications();
  refreshNotifBadge();
});
const refreshNotificationsBtn = document.querySelector("#refresh-notifications");
if (refreshNotificationsBtn) refreshNotificationsBtn.addEventListener("click", loadNotifications);

async function refreshNotifBadge() {
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
  } catch (_) {}
}

// ===== Messages =====
async function loadConversations() {
  try {
    const data = await apiCall("/api/v1/me/conversations", "GET");
    const items = Array.isArray(data.items) ? data.items : [];
    convListNode.innerHTML = items.length ? "" : `<p class="empty-state">${escapeHtml(t("message.empty"))}</p>`;
    for (const c of items) {
      const btn = document.createElement("button");
      btn.type = "button";
      btn.dataset.peerId = c.peer.id;
      btn.innerHTML = `
        <div class="user-row">
          <div>
            <div class="name">${escapeHtml(c.peer.nickname || c.peer.username)}</div>
            <div class="handle">${escapeHtml((c.last_message || "").slice(0, 40))}</div>
          </div>
          ${c.unread_count ? `<span class="badge accent" style="margin-left:auto;">${c.unread_count}</span>` : ""}
        </div>
      `;
      btn.addEventListener("click", () => openConversation(c.peer.id));
      convListNode.appendChild(btn);
      if (activePeerId === c.peer.id) btn.classList.add("active");
    }
  } catch (err) {
    convListNode.innerHTML = `<p>${escapeHtml(err.message)}</p>`;
  }
}

async function openConversation(peerId) {
  activePeerId = peerId;
  document.querySelectorAll("#conv-list button").forEach((b) =>
    b.classList.toggle("active", Number(b.dataset.peerId) === peerId));
  messageForm.hidden = false;
  try {
    const data = await apiCall(`/api/v1/me/conversations/${peerId}`, "GET");
    const me = JSON.parse(localStorage.getItem(USER_KEY) || "{}");
    convMessagesNode.innerHTML = "";
    for (const m of data.messages || []) {
      const bubble = document.createElement("div");
      bubble.className = "message-bubble " + (m.sender_id === me.id ? "mine" : "theirs");
      bubble.textContent = m.content;
      convMessagesNode.appendChild(bubble);
    }
    convMessagesNode.scrollTop = convMessagesNode.scrollHeight;
    refreshNotifBadge();
  } catch (err) {
    convMessagesNode.innerHTML = `<p>${escapeHtml(err.message)}</p>`;
  }
}

if (messageForm) {
  messageForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!activePeerId) return;
    const fd = new FormData(messageForm);
    try {
      await apiCall("/api/v1/messages", "POST", {
        recipient_id: activePeerId,
        content: String(fd.get("content") || ""),
      });
      messageForm.reset();
      openConversation(activePeerId);
    } catch (err) { alert(err.message); }
  });
}

const startConvBtn = document.querySelector("#start-conversation");
if (startConvBtn) startConvBtn.addEventListener("click", () => {
  const peerInput = document.querySelector("#new-message-peer");
  const peerId = Number(peerInput.value || 0);
  if (peerId <= 0) return;
  openConversation(peerId);
});
const refreshConvBtn = document.querySelector("#refresh-conversations");
if (refreshConvBtn) refreshConvBtn.addEventListener("click", loadConversations);

function boot() {
  const token = getToken();
  const rawUser = localStorage.getItem(USER_KEY);
  if (!token || !rawUser) {
    notLogin.hidden = false;
    dashboard.hidden = true;
    return;
  }
  const user = JSON.parse(rawUser);
  userHint.textContent = t("user.greeting").replace("{name}", user.nickname || user.username);
  notLogin.hidden = true;
  dashboard.hidden = false;
  loadMyPosts();
  refreshNotifBadge();
  setInterval(refreshNotifBadge, 30000);
}

boot();
