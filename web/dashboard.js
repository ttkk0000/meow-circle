const TOKEN_KEY = "meow_token";
const USER_KEY = "meow_user";
const DRAFT_POST_KEY = "meow_draft_post";
const DRAFT_LISTING_KEY = "meow_draft_listing_v1";

const t = (k) => window.MeowShared.t(k);

let editingPostId = null;
let editingListingId = null;
const postTagState = { tags: [] };
let postMediaIds = [];
let listingMediaIds = [];
/** First media by id; filled when loading my posts (for cover thumbs). */
let mediaCacheById = new Map();
let mediaFilter = "all";
let mediaQuery = "";
let listingFilter = "all";
let lastMediaItems = [];
let lastListingItems = [];

const notLogin = document.querySelector("#not-login");
const dashboard = document.querySelector("#dashboard");
const userHint = document.querySelector("#user-hint");
const logoutBtn = document.querySelector("#logout-btn");

const postForm = document.querySelector("#post-form");
const listingForm = document.querySelector("#listing-form");
const postContentEl = document.querySelector("#post-content");
const postMediaIdsField = document.querySelector("#post-media-ids");
const listingMediaIdsField = document.querySelector("#listing-media-ids");
const postTagBox = document.querySelector("#post-tag-box");
const postTagInput = document.querySelector("#post-tag-input");
const postDropzone = document.querySelector("#post-dropzone");
const postFileInput = document.querySelector("#post-file-input");
const postMediaPreview = document.querySelector("#post-media-preview");
const listingDropzone = document.querySelector("#listing-dropzone");
const listingFileInput = document.querySelector("#listing-file-input");
const listingMediaPreview = document.querySelector("#listing-media-preview");
const postEditBadge = document.querySelector("#post-edit-badge");
const postCancelEdit = document.querySelector("#post-cancel-edit");
const postDraftBtn = document.querySelector("#post-draft-btn");
const listingEditBadge = document.querySelector("#listing-edit-badge");
const listingCancelEdit = document.querySelector("#listing-cancel-edit");
const listingDraftBtn = document.querySelector("#listing-draft-btn");
const myPostsNode = document.querySelector("#my-posts");
const myListingsNode = document.querySelector("#my-listings");
const mediaForm = document.querySelector("#media-form");
const mediaFile = document.querySelector("#media-file");
const mediaList = document.querySelector("#media-list");
const mediaSearchInput = document.querySelector("#media-search");
const myListingsMeta = document.querySelector("#my-listings-meta");
const myOrdersTbody = document.querySelector("#my-orders-tbody");
const myOrdersEmpty = document.querySelector("#my-orders-empty");
const ordersStatRevenue = document.querySelector("#orders-stat-revenue");
const ordersStatActive = document.querySelector("#orders-stat-active");
const ordersStatUrgent = document.querySelector("#orders-stat-urgent");
const ordersStatTrend = document.querySelector("#orders-stat-trend");
const ordersStatSatisfaction = document.querySelector("#orders-stat-satisfaction");
const ordersStatSatisfactionSub = document.querySelector("#orders-stat-satisfaction-sub");
const ordersPagination = document.querySelector("#orders-pagination");
const ordersFocusSearch = document.querySelector("#orders-focus-search");
const notifCategories = document.querySelector("#notif-categories");
const listingYuanInput = document.querySelector("#listing-price-yuan");
const listingCentsField = document.querySelector("#listing-price-cents-field");
const postPublishBtn = document.querySelector("#post-publish-btn");
const postBrowseBtn = document.querySelector("#post-browse-btn");
const postVisibilityBtn = document.querySelector("#post-visibility-btn");
const listingPublishBtn = document.querySelector("#listing-publish-btn");
const inbarSettings = document.querySelector("#inbar-settings");
const inbarMail = document.querySelector("#inbar-mail");
const adminGlobalSearch = document.querySelector("#admin-global-search");
const headerAvatar = document.querySelector("#header-avatar");
const myPostsEmpty = document.querySelector("#my-posts-empty");
const myPostsStatTotal = document.querySelector("#my-posts-stat-total");
const myPostsStatViews = document.querySelector("#my-posts-stat-views");
const postLivePreview = document.querySelector("#post-live-preview");
const listingLivePreview = document.querySelector("#listing-live-preview");

let lastOrdersAll = [];
let ordersStatusFilter = "all";
let activePanel = "compose-post";
let myPostsFilter = "all";
let lastNotifItems = [];
let notifFilterKind = "all";
let notifUnreadOnly = false;
let lastLoadedPosts = [];
let lastConversationItems = [];
let convMessageFilter = "all";

const PREF_KEY = "meow_profile_prefs";
const POST_VISIBILITY_KEY = "meow_post_visibility_public";
let ordersRole = "buyer";

const profileForm = document.querySelector("#profile-form");
const notificationsList = document.querySelector("#notifications-list");
const convListNode = document.querySelector("#conv-list");
const convMessagesNode = document.querySelector("#conv-messages");
const messageForm = document.querySelector("#message-form");
const notifBtn = document.querySelector("#notif-btn");
const notifCount = document.querySelector("#notif-count");
const notifUnreadDot = document.querySelector("#notif-unread-dot");
const toggleUnreadOnlyBtn = document.querySelector("#toggle-unread-only");
let activePeerId = null;
const activeRenderJobs = new WeakMap();
const activeRequestControllers = new Map();
const virtualListCleanups = new WeakMap();

function readProfilePrefs() {
  try {
    return JSON.parse(localStorage.getItem(PREF_KEY) || "{}");
  } catch (_) {
    return {};
  }
}

function applyProfilePrefsVisuals() {
  const p = readProfilePrefs();
  const compact = !!p.compact;
  if (myPostsNode) myPostsNode.classList.toggle("gap-4", compact);
  if (myPostsNode) myPostsNode.classList.toggle("gap-6", !compact);
  if (myListingsNode) myListingsNode.classList.toggle("compact", compact);
}

logoutBtn?.addEventListener("click", () => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  window.location.href = "/";
});

document.querySelectorAll(".nav-btn").forEach((btn) => {
  btn.addEventListener("click", () => {
    document.querySelectorAll(".nav-btn").forEach((b) => b.classList.remove("active"));
    btn.classList.add("active");
    const target = btn.dataset.section;
    activePanel = target || "compose-post";
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
    syncAdminSearchPlaceholder();
  });
});

function syncPostMediaField() {
  if (postMediaIdsField) postMediaIdsField.value = postMediaIds.join(", ");
}
function syncListingMediaField() {
  if (listingMediaIdsField) listingMediaIdsField.value = listingMediaIds.join(", ");
}

function syncListingYuanFromCents(cents) {
  const c = Math.max(0, Math.round(Number(cents) || 0));
  const field = document.querySelector("#listing-price-cents-field");
  const yuan = document.querySelector("#listing-price-yuan");
  if (field) field.value = String(c);
  if (yuan) yuan.value = (c / 100).toFixed(2);
}

const LISTING_CURRENCIES = new Set(["CNY", "USD", "HKD"]);

function syncListingPricePrefix() {
  const sel = document.querySelector("#listing-currency");
  const pre = document.querySelector("#listing-price-prefix");
  if (!pre) return;
  const raw = sel && sel.value ? String(sel.value) : "CNY";
  const v = LISTING_CURRENCIES.has(raw) ? raw : "CNY";
  if (sel && sel.value !== v) sel.value = v;
  pre.textContent = v === "USD" ? "$" : v === "HKD" ? "HK$" : "¥";
}

function refreshPostPreview() {
  if (!postLivePreview || !postForm) return;
  const titleEl = postForm.querySelector('[name="title"]');
  const title = String((titleEl && titleEl.value) || "").trim();
  const content = String((postContentEl && postContentEl.value) || "").trim();
  const h = postLivePreview.querySelector("h4");
  const p = postLivePreview.querySelector("p");
  if (h) h.textContent = title || "Untitled post";
  if (p) p.textContent = content || "Write something to preview how this card looks in feed.";
}

function refreshListingPreview() {
  if (!listingLivePreview || !listingForm) return;
  const titleEl = listingForm.querySelector('[name="title"]');
  const descEl = listingForm.querySelector('[name="description"]');
  const ccyEl = listingForm.querySelector('[name="currency"]');
  const title = String((titleEl && titleEl.value) || "").trim();
  const desc = String((descEl && descEl.value) || "").trim();
  const ccy = String((ccyEl && ccyEl.value) || "CNY");
  const price = Number((listingCentsField && listingCentsField.value) || 0);
  const h = listingLivePreview.querySelector("h4");
  const ps = listingLivePreview.querySelectorAll("p");
  if (h) h.textContent = title || "Untitled listing";
  if (ps[0]) ps[0].textContent = desc || "Describe what you sell and condition details.";
  if (ps[1]) ps[1].textContent = `${(price / 100).toFixed(2)} ${ccy}`;
}

function setListingCurrency(code) {
  const sel = document.querySelector("#listing-currency");
  if (!sel) return;
  const v = LISTING_CURRENCIES.has(String(code || "")) ? String(code) : "CNY";
  sel.value = v;
  syncListingPricePrefix();
}

const ADMIN_SEARCH_PLACEHOLDER_BY_PANEL = {
  "my-orders": "dashboard.search_placeholder_orders",
  "my-posts": "dashboard.search_placeholder_posts",
  "my-listings": "dashboard.search_placeholder_listings",
  messages: "dashboard.search_placeholder_messages",
};

const STITCH_SCREEN_BY_PANEL = {
  "compose-post": "ADMIN_COMPOSE_POST",
  "compose-listing": "ADMIN_COMPOSE_LISTING",
  media: "ADMIN_MEDIA",
  "my-posts": "ADMIN_MY_POSTS",
  "my-listings": "ADMIN_MY_LISTINGS",
  "my-orders": "ADMIN_MY_ORDERS",
  profile: "ADMIN_PROFILE",
  notifications: "ADMIN_NOTIFICATIONS",
  messages: "ADMIN_MESSAGES",
};

function syncStitchScreenByPanel(panel) {
  const shared = window.MeowShared;
  if (!shared || typeof shared.setStitchScreen !== "function") return;
  const key = STITCH_SCREEN_BY_PANEL[panel || "compose-post"] || STITCH_SCREEN_BY_PANEL["compose-post"];
  const screenId = shared.STITCH_WEB_SCREENS && shared.STITCH_WEB_SCREENS[key];
  if (screenId) shared.setStitchScreen(screenId);
}

function syncAdminSearchPlaceholder() {
  syncStitchScreenByPanel(activePanel);
  if (!adminGlobalSearch) return;
  const key = ADMIN_SEARCH_PLACEHOLDER_BY_PANEL[activePanel] || "dashboard.search_placeholder";
  adminGlobalSearch.placeholder = t(key);
}

function getAdminSearchQuery() {
  return (adminGlobalSearch && adminGlobalSearch.value ? adminGlobalSearch.value : "").trim().toLowerCase();
}

function setPostTagList(tags) {
  postTagState.tags = Array.isArray(tags) ? tags.filter(Boolean) : [];
  if (!postTagBox) return;
  postTagBox.innerHTML = "";
  for (const tag of postTagState.tags) {
    const chip = document.createElement("span");
    chip.className = "stitch-tag";
    chip.appendChild(document.createTextNode(`#${tag} `));
    const x = document.createElement("button");
    x.type = "button";
    x.setAttribute("aria-label", "remove");
    x.appendChild(document.createTextNode("×"));
    x.addEventListener("click", () => {
      postTagState.tags = postTagState.tags.filter((x2) => x2 !== tag);
      setPostTagList(postTagState.tags);
    });
    chip.appendChild(x);
    postTagBox.appendChild(chip);
  }
}

function addPostTag(raw) {
  const v = String(raw || "")
    .trim()
    .replace(/^#+/, "");
  if (!v || postTagState.tags.includes(v)) return;
  postTagState.tags.push(v);
  setPostTagList(postTagState.tags);
  if (postTagInput) postTagInput.value = "";
}

function wrapSelection(ta, before, after) {
  if (!ta) return;
  const start = ta.selectionStart;
  const end = ta.selectionEnd;
  const val = ta.value;
  const mid = val.slice(start, end);
  ta.value = val.slice(0, start) + before + mid + after + val.slice(end);
  const pos = start + before.length + mid.length + after.length;
  ta.focus();
  ta.setSelectionRange(pos, pos);
}

function insertAtLineStart(ta, prefix) {
  if (!ta) return;
  const start = ta.selectionStart;
  const val = ta.value;
  const lineStart = val.lastIndexOf("\n", start - 1) + 1;
  ta.value = val.slice(0, lineStart) + prefix + val.slice(lineStart);
  const newPos = start + prefix.length;
  ta.focus();
  ta.setSelectionRange(newPos, newPos);
}

function transformSelectedLines(ta, lineFn) {
  if (!ta) return;
  const start = ta.selectionStart;
  const end = ta.selectionEnd;
  const val = ta.value;
  const block = val.slice(start, end);
  if (block.length === 0) return;
  const lines = block.split("\n");
  const out = lines.map(lineFn).join("\n");
  const newVal = val.slice(0, start) + out + val.slice(end);
  ta.value = newVal;
  const newEnd = start + out.length;
  ta.focus();
  ta.setSelectionRange(start, newEnd);
}

function initPostFormatBar() {
  document.querySelectorAll(".stitch-format-bar [data-cmd]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const cmd = btn.getAttribute("data-cmd");
      const ta = postContentEl;
      if (cmd === "bold") wrapSelection(ta, "**", "**");
      if (cmd === "italic") wrapSelection(ta, "*", "*");
      if (cmd === "underline") wrapSelection(ta, "_", "_");
      if (cmd === "ul") insertAtLineStart(ta, "- ");
      if (cmd === "ol") insertAtLineStart(ta, "1. ");
      if (cmd === "align_left") {
        transformSelectedLines(ta, (line) => line.replace(/^(  |\t)/, ""));
      }
      if (cmd === "align_center") {
        transformSelectedLines(ta, (line) => (line.length ? "  " + line : line));
      }
      if (cmd === "link") {
        const url = window.prompt(t("form.link_url_prompt") || "Link URL", "https://");
        if (url) wrapSelection(ta, "[", `](${url})`);
      }
    });
  });
}

function initTagInput() {
  if (!postTagInput) return;
  postTagInput.addEventListener("keydown", (ev) => {
    if (ev.key === "Enter" || ev.key === ",") {
      ev.preventDefault();
      addPostTag(postTagInput.value);
    }
  });
  postTagInput.addEventListener("blur", () => {
    if (postTagInput.value.trim()) addPostTag(postTagInput.value);
  });
}

function setCategoryRadio(name) {
  const r = document.querySelector(`#post-form input[name="category"][value="${name}"]`);
  if (r) {
    r.checked = true;
  }
}

function setListingTypeRadio(name) {
  const r = document.querySelector(`#listing-form input[name="type"][value="${name}"]`);
  if (r) r.checked = true;
}

function renderPostMediaThumbs() {
  if (!postMediaPreview) return;
  postMediaPreview.innerHTML = "";
  for (const id of postMediaIds) {
    const idEl = document.createElement("span");
    idEl.className =
      "inline-flex items-center gap-1 rounded-full bg-surface-container px-2 py-1 text-xs font-medium text-on-surface-variant";
    idEl.appendChild(document.createTextNode(`#${id} `));
    const rm = document.createElement("button");
    rm.type = "button";
    rm.className = "text-primary-container";
    rm.appendChild(document.createTextNode("×"));
    rm.addEventListener("click", () => {
      postMediaIds = postMediaIds.filter((i) => i !== id);
      syncPostMediaField();
      renderPostMediaThumbs();
    });
    idEl.appendChild(rm);
    postMediaPreview.appendChild(idEl);
  }
}

function renderListingMediaThumbs() {
  if (!listingMediaPreview) return;
  listingMediaPreview.innerHTML = "";
  for (const id of listingMediaIds) {
    const idEl = document.createElement("span");
    idEl.className =
      "inline-flex items-center gap-1 rounded-full bg-surface-container px-2 py-1 text-xs font-medium";
    idEl.appendChild(document.createTextNode(`#${id} `));
    const rm = document.createElement("button");
    rm.type = "button";
    rm.className = "text-primary-container";
    rm.appendChild(document.createTextNode("×"));
    rm.addEventListener("click", () => {
      listingMediaIds = listingMediaIds.filter((i) => i !== id);
      syncListingMediaField();
      renderListingMediaThumbs();
    });
    idEl.appendChild(rm);
    listingMediaPreview.appendChild(idEl);
  }
}

async function uploadFilesToMedia(files) {
  const out = [];
  for (const file of files) {
    if (!file) continue;
    const fd = new FormData();
    fd.append("file", file);
    const m = await apiUpload("/api/v1/media", fd);
    if (m && m.id) out.push(m.id);
  }
  return out;
}

function bindDropzone(dz, fileInput, onFiles) {
  if (!dz || !fileInput) return;
  dz.addEventListener("click", (ev) => {
    if (ev.target === fileInput) return;
    fileInput.click();
  });
  fileInput.addEventListener("change", () => {
    const f = fileInput.files ? Array.from(fileInput.files) : [];
    if (f.length) onFiles(f);
    fileInput.value = "";
  });
  ["dragenter", "dragover"].forEach((type) => {
    dz.addEventListener(type, (e) => {
      e.preventDefault();
      e.stopPropagation();
      dz.classList.add("stitch-dropzone--active");
    });
  });
  ["dragleave", "drop"].forEach((type) => {
    dz.addEventListener(type, (e) => {
      e.preventDefault();
      e.stopPropagation();
      if (type === "dragleave") dz.classList.remove("stitch-dropzone--active");
    });
  });
  dz.addEventListener("drop", (e) => {
    dz.classList.remove("stitch-dropzone--active");
    const f = e.dataTransfer && e.dataTransfer.files ? Array.from(e.dataTransfer.files) : [];
    if (f.length) onFiles(f);
  });
}

function resetPostForm(clearDraft) {
  editingPostId = null;
  if (postForm) postForm.reset();
  if (postContentEl) postContentEl.value = "";
  postMediaIds = [];
  syncPostMediaField();
  renderPostMediaThumbs();
  setPostTagList([]);
  setCategoryRadio("daily_share");
  if (postEditBadge) {
    postEditBadge.classList.add("hidden");
    postEditBadge.classList.remove("inline-flex", "flex");
  }
  if (postCancelEdit) postCancelEdit.classList.add("hidden");
  if (clearDraft) {
    try {
      localStorage.removeItem(DRAFT_POST_KEY);
    } catch (_) {}
  }
}

function resetListingForm(clearDraft) {
  editingListingId = null;
  if (listingForm) listingForm.reset();
  setListingCurrency("CNY");
  syncListingYuanFromCents(0);
  listingMediaIds = [];
  syncListingMediaField();
  renderListingMediaThumbs();
  setListingTypeRadio("product");
  if (listingEditBadge) {
    listingEditBadge.classList.add("hidden");
    listingEditBadge.classList.remove("inline-flex", "flex");
  }
  if (listingCancelEdit) listingCancelEdit.classList.add("hidden");
  if (clearDraft) {
    try {
      localStorage.removeItem(DRAFT_LISTING_KEY);
    } catch (_) {}
  }
}

function loadPostDraft() {
  try {
    const raw = localStorage.getItem(DRAFT_POST_KEY);
    if (!raw) return;
    const d = JSON.parse(raw);
    const pTitle = postForm && postForm.querySelector('[name="title"]');
    if (pTitle && d.title) pTitle.value = d.title;
    if (postContentEl) postContentEl.value = d.content || "";
    if (d.category) setCategoryRadio(d.category);
    if (Array.isArray(d.media_ids)) {
      postMediaIds = d.media_ids.map(Number).filter((n) => n > 0);
      syncPostMediaField();
      renderPostMediaThumbs();
    }
    if (Array.isArray(d.tags)) setPostTagList(d.tags);
  } catch (_) {}
}

function loadListingDraft() {
  try {
    const raw = localStorage.getItem(DRAFT_LISTING_KEY);
    if (!raw) return;
    const d = JSON.parse(raw);
    if (listingForm) {
      if (d.title) listingForm.title.value = d.title;
      if (d.description) listingForm.description.value = d.description;
      if (d.price_cents != null) syncListingYuanFromCents(d.price_cents);
      setListingCurrency(d.currency || "CNY");
    }
    if (d.type) setListingTypeRadio(d.type);
    if (Array.isArray(d.media_ids)) {
      listingMediaIds = d.media_ids.map(Number).filter((n) => n > 0);
      syncListingMediaField();
      renderListingMediaThumbs();
    }
  } catch (_) {}
}

function initStitchDashboard() {
  syncPostMediaField();
  syncListingMediaField();
  initPostFormatBar();
  initTagInput();
  bindDropzone(postDropzone, postFileInput, async (files) => {
    try {
      const ids = await uploadFilesToMedia(files);
      postMediaIds = postMediaIds.concat(ids);
      syncPostMediaField();
      renderPostMediaThumbs();
    } catch (err) {
      notify(err.message, "error");
    }
  });
  bindDropzone(listingDropzone, listingFileInput, async (files) => {
    try {
      const ids = await uploadFilesToMedia(files);
      listingMediaIds = listingMediaIds.concat(ids);
      syncListingMediaField();
      renderListingMediaThumbs();
    } catch (err) {
      notify(err.message, "error");
    }
  });
  if (postPublishBtn && postForm) {
    postPublishBtn.addEventListener("click", () => postForm.requestSubmit());
  }
  if (postForm) postForm.addEventListener("input", refreshPostPreview);
  if (postBrowseBtn) {
    postBrowseBtn.addEventListener("click", (e) => {
      e.stopPropagation();
      if (postFileInput) postFileInput.click();
    });
  }
  (function initPostVisibilityToggle() {
    if (!postVisibilityBtn) return;
    let on = true;
    try {
      on = localStorage.getItem(POST_VISIBILITY_KEY) !== "0";
    } catch (_) {}
    const apply = (v) => {
      const knob = postVisibilityBtn.querySelector("span");
      postVisibilityBtn.setAttribute("aria-pressed", v ? "true" : "false");
      if (knob) {
        knob.classList.toggle("translate-x-5", v);
        knob.classList.remove("translate-x-0.5");
        if (!v) knob.classList.add("translate-x-0.5");
        if (v) knob.classList.remove("translate-x-0.5");
      }
      postVisibilityBtn.classList.toggle("bg-primary-container", v);
      postVisibilityBtn.classList.toggle("bg-surface-variant", !v);
    };
    apply(on);
    postVisibilityBtn.addEventListener("click", () => {
      on = !on;
      try {
        localStorage.setItem(POST_VISIBILITY_KEY, on ? "1" : "0");
      } catch (_) {}
      apply(on);
    });
  })();
  if (inbarMail) {
    inbarMail.addEventListener("click", () => activateSection("messages"));
  }
  if (adminGlobalSearch) {
    let searchDebounce;
    adminGlobalSearch.addEventListener("input", () => {
      clearTimeout(searchDebounce);
      searchDebounce = setTimeout(() => {
        if (activePanel === "my-orders") renderOrdersView();
        if (activePanel === "my-posts") renderMyPostsView();
        if (activePanel === "my-listings") renderMyListingsView();
        if (activePanel === "media") renderMediaView();
        if (activePanel === "messages") void renderFilteredConversations();
      }, 200);
    });
  }
  document.querySelectorAll(".order-filter-chip").forEach((chip) => {
    chip.addEventListener("click", () => {
      ordersStatusFilter = chip.getAttribute("data-order-filter") || "all";
      document.querySelectorAll(".order-filter-chip").forEach((c) => {
        c.classList.remove("active", "bg-primary-container", "text-white", "shadow-[0_4px_10px_rgba(255,90,119,0.2)]");
        c.classList.add("bg-surface-container", "text-on-surface-variant");
      });
      chip.classList.add("active", "bg-primary-container", "text-white", "shadow-[0_4px_10px_rgba(255,90,119,0.2)]");
      chip.classList.remove("bg-surface-container", "text-on-surface-variant");
      renderOrdersView();
    });
  });
  const exportCsv = document.querySelector("#orders-export-csv");
  if (exportCsv) {
    exportCsv.addEventListener("click", () => {
      const items = getFilteredOrderRows();
      if (!items.length) {
        notify(t("order.empty_csv") || "No rows", "error");
        return;
      }
      const headers = [
        t("order.col_id"),
        t("order.stitch_col_customer") || "Counterparty",
        t("order.col_date"),
        t("order.col_total"),
        t("order.col_status"),
      ];
      const lines = [headers.map((h) => `"${String(h).replaceAll('"', '""')}"`).join(",")];
      for (const o of items) {
        const row = [
          o.id,
          ordersRole === "buyer" ? o.seller_id : o.buyer_id,
          o.created_at,
          (o.amount_cents / 100).toFixed(2) + " " + (o.currency || "CNY"),
          t("order.status." + o.status) || o.status,
        ];
        lines.push(row.map((c) => `"${String(c).replaceAll('"', '""')}"`).join(","));
      }
      const blob = new Blob([lines.join("\n")], { type: "text/csv;charset=utf-8" });
      const a = document.createElement("a");
      a.href = URL.createObjectURL(blob);
      a.download = "kitty-orders-" + new Date().toISOString().slice(0, 10) + ".csv";
      a.click();
      URL.revokeObjectURL(a.href);
    });
  }
  document.querySelectorAll(".my-posts-filter").forEach((b) => {
    b.addEventListener("click", () => {
      myPostsFilter = b.getAttribute("data-posts-filter") || "all";
      document.querySelectorAll(".my-posts-filter").forEach((x) => {
        x.classList.remove(
          "bg-primary-container/10",
          "text-primary-container",
          "shadow-sm",
          "bg-primary-container",
          "text-white"
        );
        x.classList.add("bg-surface-container-lowest", "text-on-surface-variant", "hover:bg-surface-container");
      });
      b.classList.remove("bg-surface-container-lowest", "text-on-surface-variant", "hover:bg-surface-container");
      b.classList.add("bg-primary-container/10", "text-primary-container", "shadow-sm");
      renderMyPostsView();
    });
  });
  document.querySelectorAll(".media-filter-chip").forEach((b) => {
    b.addEventListener("click", () => {
      mediaFilter = b.getAttribute("data-media-filter") || "all";
      document.querySelectorAll(".media-filter-chip").forEach((x) => {
        x.classList.remove("active", "bg-primary-container", "text-white", "shadow-sm");
        x.classList.add("text-on-surface-variant");
      });
      b.classList.add("active", "bg-primary-container", "text-white", "shadow-sm");
      b.classList.remove("text-on-surface-variant");
      renderMediaView();
    });
  });
  document.querySelectorAll(".listing-filter-chip").forEach((b) => {
    b.addEventListener("click", () => {
      listingFilter = b.getAttribute("data-listing-filter") || "all";
      document.querySelectorAll(".listing-filter-chip").forEach((x) => {
        const on = x === b;
        x.classList.toggle("bg-primary-container", on);
        x.classList.toggle("text-white", on);
        x.classList.toggle("text-on-surface-variant", !on);
      });
      renderMyListingsView();
    });
  });
  if (mediaSearchInput) {
    let mediaSearchDebounce;
    mediaSearchInput.addEventListener("input", () => {
      clearTimeout(mediaSearchDebounce);
      mediaSearchDebounce = setTimeout(() => {
        mediaQuery = String(mediaSearchInput.value || "").trim().toLowerCase();
        renderMediaView();
      }, 160);
    });
  }
  const profileCancel = document.querySelector("#profile-cancel");
  if (profileCancel) {
    profileCancel.addEventListener("click", () => {
      void loadProfile();
    });
  }
  document.querySelectorAll(".profile-pref-toggle").forEach((btn) => {
    btn.addEventListener("click", () => {
      const k = btn.dataset.pref;
      if (!k) return;
      let p = {};
      try {
        p = JSON.parse(localStorage.getItem(PREF_KEY) || "{}");
      } catch (_) {}
      const cur = btn.getAttribute("aria-pressed") === "true";
      const next = !cur;
      p[k] = next;
      btn.setAttribute("aria-pressed", next ? "true" : "false");
      const knob = btn.querySelector("span");
      if (knob) {
        knob.classList.toggle("translate-x-5", next);
        knob.classList.toggle("translate-x-0.5", !next);
      }
      btn.classList.toggle("bg-primary-container", next);
      btn.classList.toggle("bg-surface-variant", !next);
      try {
        localStorage.setItem(PREF_KEY, JSON.stringify(p));
      } catch (_) {}
      applyProfilePrefsVisuals();
    });
  });
  try {
    const p = JSON.parse(localStorage.getItem(PREF_KEY) || "{}");
    document.querySelectorAll(".profile-pref-toggle").forEach((btn) => {
      const k = btn.dataset.pref;
      if (k == null) return;
      const on = !!p[k];
      btn.setAttribute("aria-pressed", on ? "true" : "false");
      const knob = btn.querySelector("span");
      if (knob) {
        knob.classList.toggle("translate-x-5", on);
        knob.classList.toggle("translate-x-0.5", !on);
      }
      btn.classList.toggle("bg-primary-container", on);
      btn.classList.toggle("bg-surface-variant", !on);
    });
  } catch (_) {}
  applyProfilePrefsVisuals();
  if (listingPublishBtn && listingForm) {
    listingPublishBtn.addEventListener("click", () => listingForm.requestSubmit());
  }
  if (listingYuanInput && listingCentsField) {
    listingYuanInput.addEventListener("input", () => {
      const cents = (() => {
        const s = String(listingYuanInput.value || "");
        const n = parseFloat(s.replace(/[^\d.]/g, "")) || 0;
        return Math.max(0, Math.round(n * 100));
      })();
      listingCentsField.value = String(cents);
      refreshListingPreview();
    });
  }
  const listingCurrency = document.querySelector("#listing-currency");
  if (listingCurrency) {
    listingCurrency.addEventListener("change", () => {
      syncListingPricePrefix();
      refreshListingPreview();
    });
    syncListingPricePrefix();
  }
  if (listingForm) listingForm.addEventListener("input", refreshListingPreview);
  if (postDraftBtn) {
    postDraftBtn.addEventListener("click", () => {
      if (!postForm) return;
      const fd = new FormData(postForm);
      const payload = {
        title: String(fd.get("title") || ""),
        content: String((postContentEl && postContentEl.value) || fd.get("content") || ""),
        category: String(
          (document.querySelector('#post-form input[name="category"]:checked') || {}).value || "daily_share"
        ),
        media_ids: postMediaIds.slice(),
        tags: postTagState.tags.slice(),
        savedAt: new Date().toISOString(),
      };
      try {
        localStorage.setItem(DRAFT_POST_KEY, JSON.stringify(payload));
        notify(t("alert.draft_saved") || "草稿已保存", "success");
      } catch (err) {
        notify(err.message, "error");
      }
    });
  }
  if (postCancelEdit) {
    postCancelEdit.addEventListener("click", () => {
      resetPostForm(false);
      loadPostDraft();
    });
  }
  if (listingDraftBtn) {
    listingDraftBtn.addEventListener("click", () => {
      if (!listingForm) return;
      const fd = new FormData(listingForm);
      const payload = {
        title: String(fd.get("title") || ""),
        description: String(fd.get("description") || ""),
        type: String((document.querySelector('#listing-form input[name="type"]:checked') || {}).value || "product"),
        price_cents: Number(fd.get("price_cents") || 0),
        currency: String(fd.get("currency") || "CNY"),
        media_ids: listingMediaIds.slice(),
        savedAt: new Date().toISOString(),
      };
      try {
        localStorage.setItem(DRAFT_LISTING_KEY, JSON.stringify(payload));
        notify(t("alert.draft_saved") || "草稿已保存", "success");
      } catch (err) {
        notify(err.message, "error");
      }
    });
  }
  if (listingCancelEdit) {
    listingCancelEdit.addEventListener("click", () => {
      resetListingForm(false);
      loadListingDraft();
    });
  }
  document.querySelectorAll(".conv-filter-chip").forEach((b) => {
    b.addEventListener("click", () => {
      convMessageFilter = b.getAttribute("data-conv-filter") || "all";
      document.querySelectorAll(".conv-filter-chip").forEach((x) => {
        x.classList.remove("bg-primary-container", "text-white", "shadow-sm");
        x.classList.add("bg-surface-container-lowest", "text-on-surface-variant", "hover:bg-surface-container");
      });
      b.classList.remove("bg-surface-container-lowest", "text-on-surface-variant", "hover:bg-surface-container");
      b.classList.add("bg-primary-container", "text-white", "shadow-sm");
      void renderFilteredConversations();
    });
  });
}

function activateSection(name) {
  const btn = document.querySelector(`.nav-btn[data-section="${name}"]`);
  if (btn) btn.click();
}

document.querySelector("#sidebar-create-new")?.addEventListener("click", () => {
  activateSection("compose-post");
});

if (window.location.hash) {
  const target = window.location.hash.slice(1);
  setTimeout(() => activateSection(target), 0);
}

if (notifBtn) {
  notifBtn.addEventListener("click", () => activateSection("notifications"));
}
if (inbarSettings) {
  inbarSettings.addEventListener("click", () => {
    const sel = document.querySelector("#settings-bar select");
    if (sel) sel.focus();
  });
}

const refreshMyPostsBtn = document.querySelector("#refresh-my-posts");
const refreshMyListingsBtn = document.querySelector("#refresh-my-listings");
const refreshMediaBtn = document.querySelector("#refresh-media");
const refreshMyOrdersBtn = document.querySelector("#refresh-my-orders");
if (refreshMyPostsBtn) refreshMyPostsBtn.addEventListener("click", () => runRefresh(refreshMyPostsBtn, loadMyPosts));
if (refreshMyListingsBtn) refreshMyListingsBtn.addEventListener("click", () => runRefresh(refreshMyListingsBtn, loadMyListings));
if (refreshMediaBtn) refreshMediaBtn.addEventListener("click", () => runRefresh(refreshMediaBtn, loadMedia));

const myPostsLoadMore = document.querySelector("#my-posts-load-more");
if (myPostsLoadMore) {
  myPostsLoadMore.addEventListener("click", () => {
    notify(t("dashboard.posts_all_loaded"), "info");
  });
}
if (refreshMyOrdersBtn) refreshMyOrdersBtn.addEventListener("click", () => runRefresh(refreshMyOrdersBtn, loadMyOrders));

const ordersTabBuyer = document.querySelector("#orders-tab-buyer");
const ordersTabSeller = document.querySelector("#orders-tab-seller");
if (ordersTabBuyer) ordersTabBuyer.addEventListener("click", () => switchOrdersTab("buyer"));
if (ordersTabSeller) ordersTabSeller.addEventListener("click", () => switchOrdersTab("seller"));
if (ordersFocusSearch) {
  ordersFocusSearch.addEventListener("click", () => {
    if (adminGlobalSearch) {
      adminGlobalSearch.focus();
      adminGlobalSearch.select();
    }
  });
}

function switchOrdersTab(role) {
  ordersRole = role;
  if (ordersTabBuyer) {
    const on = role === "buyer";
    ordersTabBuyer.classList.toggle("order-role-tab--on", on);
  }
  if (ordersTabSeller) {
    const on = role === "seller";
    ordersTabSeller.classList.toggle("order-role-tab--on", on);
  }
  loadMyOrders();
}

if (mediaForm && mediaFile) {
  mediaFile.addEventListener("change", () => {
    if (mediaFile.files && mediaFile.files[0]) {
      mediaForm.requestSubmit();
    }
  });
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
      notify(err.message, "error");
    }
  });
}

async function loadMedia() {
  if (!mediaList) return;
  const signal = takeRequestSignal("media");
  try {
    const data = await apiCall("/api/v1/media", "GET", undefined, { signal });
    lastMediaItems = Array.isArray(data.items) ? data.items : [];
    renderMediaView();
  } catch (err) {
    if (err && err.name === "AbortError") return;
    mediaList.innerHTML = `<p>${escapeHtml(err.message)}</p>`;
  }
}

function getFilteredMediaItems() {
  let items = lastMediaItems.slice();
  if (mediaFilter === "photos") items = items.filter((m) => m.kind === "image");
  if (mediaFilter === "videos") items = items.filter((m) => m.kind === "video");
  const qFromPanel = mediaQuery || "";
  const qFromHeader = getAdminSearchQuery();
  const q = (qFromPanel || qFromHeader || "").trim().toLowerCase();
  if (q) {
    items = items.filter((m) => {
      const name = String(m.filename || "").toLowerCase();
      const id = String(m.id || "");
      const kind = String(m.kind || "").toLowerCase();
      return name.includes(q) || id.includes(q) || kind.includes(q);
    });
  }
  return items;
}

function renderMediaView() {
  if (!mediaList) return;
  const items = getFilteredMediaItems();
  mediaList.innerHTML = items.length ? "" : renderEditorialEmptyState(t("media.empty"), "Nº M");
  if (!items.length) return;
  void renderInBatches(mediaList, items, buildMediaNode, { batchSize: 10 });
}

function filterOrdersBySearch(items) {
  if (!adminGlobalSearch) return items;
  const q = (adminGlobalSearch.value || "").trim().toLowerCase();
  if (!q) return items;
  return items.filter(
    (o) => String(o.id).includes(q) || (o.listing_title || "").toLowerCase().includes(q)
  );
}

function orderMatchesStatusFilter(o, f) {
  if (f === "all" || !f) return true;
  if (f === "pending") return o.status === "pending_payment";
  if (f === "processing") return o.status === "paid" || o.status === "shipped";
  if (f === "completed") return o.status === "completed";
  return true;
}

function getFilteredOrderRows() {
  const base = filterOrdersBySearch(lastOrdersAll);
  return base.filter((o) => orderMatchesStatusFilter(o, ordersStatusFilter));
}

function updateOrderStats(items) {
  if (!ordersStatRevenue) return;
  const moneyStatuses = new Set(["pending_payment", "paid", "shipped", "completed"]);
  let gmv = 0;
  let nMoney = 0;
  for (const o of items) {
    if (moneyStatuses.has(o.status)) {
      gmv += o.amount_cents || 0;
      nMoney += 1;
    }
  }
  const activeCount = items.filter(
    (o) => o.status === "pending_payment" || o.status === "paid" || o.status === "shipped"
  ).length;
  const urgent = items.filter(
    (o) => o.status === "pending_payment" || (ordersRole === "seller" && o.status === "paid")
  ).length;
  ordersStatRevenue.textContent = "¥" + (gmv / 100).toFixed(2);
  if (ordersStatActive) ordersStatActive.textContent = String(activeCount);
  if (ordersStatUrgent) {
    const tpl = t("order.stat_urgent_msg");
    ordersStatUrgent.textContent = (tpl && tpl.includes("{n}") ? tpl.replace("{n}", String(urgent)) : `${t("order.stat_action")} ${urgent}`);
  }
  if (ordersStatTrend) {
    const msg = t("order.stat_trend_na");
    ordersStatTrend.innerHTML = `<span class="material-symbols-outlined text-sm">trending_up</span><span class="font-label-md text-label-md">${escapeHtml(
      msg
    )}</span>`;
  }
  if (ordersStatSatisfaction) {
    ordersStatSatisfaction.textContent = "—";
  }
  if (ordersStatSatisfactionSub) {
    ordersStatSatisfactionSub.className = "mt-2 flex items-center gap-1 text-tertiary text-label-md";
    ordersStatSatisfactionSub.innerHTML = `<span class="material-symbols-outlined text-sm">star</span><span class="font-label-md">${escapeHtml(
      t("order.stat_satisfaction_sub")
    )}</span>`;
  }
}

function orderIdDisplay(id) {
  return `#ORD-${String(id).padStart(4, "0")}`;
}

function orderCounterpartName(o) {
  const counterId = ordersRole === "buyer" ? o.seller_id : o.buyer_id;
  return t("order.counterparty_id").replace("{id}", String(counterId)) || `User #${counterId}`;
}

function orderRowInitials(o) {
  const title = (o.listing_title || "").trim();
  if (title.length >= 2) return title.slice(0, 2).toUpperCase();
  if (title.length === 1) return (title + title).toUpperCase();
  return "U" + (Number(o.id) % 9);
}

function orderStatusPillClass(st) {
  if (st === "pending_payment") return "stitch-order-pill stitch-order-pill--error";
  if (st === "paid" || st === "shipped") return "stitch-order-pill stitch-order-pill--process";
  if (st === "completed") return "stitch-order-pill stitch-order-pill--done";
  if (st === "cancelled" || st === "refunded") return "stitch-order-pill stitch-order-pill--neutral";
  return "stitch-order-pill stitch-order-pill--neutral";
}

function formatDateOnly(value) {
  if (!value) return t("common.unknown");
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return t("common.unknown");
  return date.toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
}

function buildOrderTableRow(o) {
  const tr = document.createElement("tr");
  tr.className = "group border-b border-surface-variant transition-colors hover:bg-surface-container/30";
  const statusLabel = t(`order.status.${o.status}`) || o.status;
  const total = "¥" + (o.amount_cents / 100).toFixed(2);
  const nameLine = orderCounterpartName(o);
  const initials = orderRowInitials(o);
  const subline = o.listing_title || t("order.listing_fallback");

  const td0 = document.createElement("td");
  td0.className = "p-4 pl-6 font-bold text-on-background";
  td0.textContent = orderIdDisplay(o.id);

  const td1 = document.createElement("td");
  td1.className = "p-4";
  td1.innerHTML = `<div class="flex items-center gap-3">
  <div class="flex h-8 w-8 shrink-0 items-center justify-center overflow-hidden rounded-full bg-surface-container-highest text-xs font-bold text-primary-container">${escapeHtml(
    initials
  )}</div>
  <div class="min-w-0">
    <div class="font-medium text-on-surface">${escapeHtml(nameLine)}</div>
    <div class="text-xs text-on-surface-variant line-clamp-1">${escapeHtml(subline)}</div>
  </div>
</div>`;

  const td2 = document.createElement("td");
  td2.className = "p-4 text-on-surface-variant";
  td2.textContent = formatDateOnly(o.created_at);

  const td3 = document.createElement("td");
  td3.className = "p-4 font-medium";
  td3.textContent = total;

  const td4 = document.createElement("td");
  td4.className = "p-4";
  const pill = document.createElement("span");
  pill.className = `${orderStatusPillClass(o.status)} inline-flex max-w-full items-center rounded-full px-2.5 py-1 text-xs font-bold`;
  pill.setAttribute("data-status", o.status);
  pill.textContent = statusLabel;
  td4.appendChild(pill);

  const td5 = document.createElement("td");
  td5.className = "p-4 pr-6 text-right";
  const act = orderActions(o);
  if (act.length === 0) {
    const ph = document.createElement("div");
    ph.className = "inline-flex h-9 w-9 items-center justify-center rounded-full text-stone-300";
    ph.innerHTML = '<span class="material-symbols-outlined text-lg">more_horiz</span>';
    ph.setAttribute("aria-hidden", "true");
    td5.appendChild(ph);
  } else {
    const det = document.createElement("details");
    det.className = "stitch-order-popover relative inline-block text-left";
    const sum = document.createElement("summary");
    sum.className = "stitch-order-more list-none";
    sum.innerHTML = '<span class="material-symbols-outlined">more_horiz</span>';
    const menu = document.createElement("div");
    menu.className =
      "absolute right-0 z-30 mt-1 min-w-[9.5rem] rounded-xl border border-surface-variant bg-surface-container-lowest py-1 text-left shadow-lg";
    act.forEach((b) => {
      const isDanger = (b.getAttribute("class") || "").includes("danger");
      b.className = `stitch-order-menu-btn block w-full px-3 py-2.5 text-left text-sm ${
        isDanger ? "text-error hover:bg-error/10" : "text-on-surface hover:bg-surface-container"
      }`;
      b.addEventListener("click", () => {
        det.removeAttribute("open");
      });
      menu.appendChild(b);
    });
    det.append(sum, menu);
    td5.appendChild(det);
  }

  tr.append(td0, td1, td2, td3, td4, td5);
  return tr;
}

function renderOrdersView() {
  if (!myOrdersTbody) return;
  const items = getFilteredOrderRows();
  updateOrderStats(items);
  myOrdersTbody.innerHTML = "";
  if (myOrdersEmpty) {
    if (!items.length) myOrdersEmpty.classList.remove("hidden");
    else myOrdersEmpty.classList.add("hidden");
  }
  if (ordersPagination) {
    if (!items.length) {
      ordersPagination.classList.add("hidden");
    } else {
      ordersPagination.classList.remove("hidden");
      const n = lastOrdersAll.length;
      const m = items.length;
      const line = t("order.footer_range")
        .replace("{m}", String(m))
        .replace("{n}", String(n));
      ordersPagination.innerHTML = `<span class="pl-1">${escapeHtml(line)}</span>
        <div class="inline-flex items-center gap-0.5 pr-1 opacity-50" title="">
          <span class="inline-flex h-8 w-8 items-center justify-center rounded-full text-stone-400" aria-hidden="true"><span class="material-symbols-outlined text-sm">chevron_left</span></span>
          <span class="inline-flex h-8 w-8 items-center justify-center rounded-full bg-primary-container text-sm font-bold text-white shadow-[0_2px_8px_rgba(255,90,119,0.3)]" aria-hidden="true">1</span>
          <span class="inline-flex h-8 w-8 items-center justify-center rounded-full text-stone-400" aria-hidden="true"><span class="material-symbols-outlined text-sm">chevron_right</span></span>
        </div>`;
    }
  }
  if (!items.length) return;
  for (const o of items) {
    myOrdersTbody.appendChild(buildOrderTableRow(o));
  }
}

async function loadMyOrders() {
  const signal = takeRequestSignal(`orders:${ordersRole}`);
  if (!myOrdersTbody) return;
  try {
    const data = await apiCall(`/api/v1/me/orders?role=${encodeURIComponent(ordersRole)}`, "GET", undefined, { signal });
    const items = Array.isArray(data.items) ? data.items : [];
    lastOrdersAll = items;
    renderOrdersView();
  } catch (err) {
    if (err && err.name === "AbortError") return;
    lastOrdersAll = [];
    updateOrderStats([]);
    if (ordersPagination) ordersPagination.classList.add("hidden");
    if (myOrdersEmpty) myOrdersEmpty.classList.add("hidden");
    if (myOrdersTbody) {
      myOrdersTbody.innerHTML = "";
      const tr = document.createElement("tr");
      const td = document.createElement("td");
      td.colSpan = 6;
      td.className = "p-6 text-center text-on-surface-variant";
      td.textContent = err.message;
      tr.appendChild(td);
      myOrdersTbody.appendChild(tr);
    }
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
    notify(err.message, "error");
  }
}

postForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const fd = new FormData(postForm);
    const category = String(
      (document.querySelector('#post-form input[name="category"]:checked') || {}).value || "daily_share"
    );
    const body = {
      title: String(fd.get("title") || ""),
      content: String((postContentEl && postContentEl.value) || ""),
      category,
      tags: postTagState.tags.slice(),
      media_ids: postMediaIds.slice(),
    };
    if (editingPostId) {
      await apiCall(`/api/v1/posts/${editingPostId}`, "PATCH", body);
      notify(t("alert.update_success") || t("alert.publish_success"), "success");
    } else {
      await apiCall("/api/v1/posts", "POST", body);
      notify(t("alert.publish_success"), "success");
    }
    resetPostForm(true);
    try {
      localStorage.removeItem(DRAFT_POST_KEY);
    } catch (_) {}
  } catch (err) {
    notify(err.message, "error");
  }
});

listingForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const fd = new FormData(listingForm);
    const type = String((document.querySelector('#listing-form input[name="type"]:checked') || {}).value || "product");
    const body = {
      type,
      title: String(fd.get("title") || ""),
      description: String(fd.get("description") || ""),
      price_cents: Number(fd.get("price_cents") || 0),
      currency: String(fd.get("currency") || "CNY"),
      media_ids: listingMediaIds.slice(),
    };
    if (editingListingId) {
      await apiCall(`/api/v1/listings/${editingListingId}`, "PATCH", body);
      notify(t("alert.update_success") || t("alert.publish_success"), "success");
    } else {
      await apiCall("/api/v1/listings", "POST", body);
      notify(t("alert.publish_success"), "success");
    }
    resetListingForm(true);
    try {
      localStorage.removeItem(DRAFT_LISTING_KEY);
    } catch (_) {}
  } catch (err) {
    notify(err.message, "error");
  }
});

function splitCsv(raw) {
  return raw.split(",").map((item) => item.trim()).filter(Boolean);
}

function getPostDraftObject() {
  try {
    const raw = localStorage.getItem(DRAFT_POST_KEY);
    if (!raw) return null;
    const d = JSON.parse(raw);
    return {
      id: 0,
      isDraft: true,
      title: d.title || t("dashboard.draft_untitled"),
      content: d.content || "",
      category: d.category,
      created_at: d.savedAt,
      last_reply_at: null,
    };
  } catch {
    return null;
  }
}

function updateMyPostStats(publishedCount, draftCount) {
  const total = publishedCount + draftCount;
  if (myPostsStatTotal) {
    myPostsStatTotal.textContent = String(total);
  }
  if (myPostsStatViews) {
    myPostsStatViews.textContent = t("dashboard.posts_views_na");
  }
}

function postRowMatchesFilter(post) {
  if (myPostsFilter === "all") return true;
  if (myPostsFilter === "drafts") return !!post.isDraft;
  if (myPostsFilter === "published") return !post.isDraft;
  return true;
}

function filterPostsBySearchList(list) {
  if (!adminGlobalSearch) return list;
  const q = (adminGlobalSearch.value || "").trim().toLowerCase();
  if (!q) return list;
  return list.filter(
    (p) =>
      (p.title || "").toLowerCase().includes(q) || (p.content || "").toLowerCase().includes(String(q).slice(0, 500))
  );
}

function renderMyPostsView() {
  if (!myPostsNode) return;
  let list = lastLoadedPosts.filter(postRowMatchesFilter);
  list = filterPostsBySearchList(list);
  myPostsNode.innerHTML = "";
  if (myPostsEmpty) {
    if (!list.length) myPostsEmpty.classList.remove("hidden");
    else myPostsEmpty.classList.add("hidden");
  }
  if (!list.length) return;
  void renderInBatches(myPostsNode, list, buildMyPostNode, { batchSize: 12 });
}

async function loadMyPosts() {
  const signal = takeRequestSignal("my-posts");
  try {
    const [data, mediaData] = await Promise.all([
      apiCall("/api/v1/me/posts", "GET", undefined, { signal }),
      apiCall("/api/v1/media", "GET", undefined, { signal }).catch(() => null),
    ]);
    mediaCacheById = new Map();
    if (mediaData && Array.isArray(mediaData.items)) {
      for (const m of mediaData.items) {
        if (m && m.id) mediaCacheById.set(m.id, m);
      }
    }
    const items = Array.isArray(data.items) ? data.items : [];
    const draft = getPostDraftObject();
    lastLoadedPosts = draft ? [draft, ...items] : items.slice();
    updateMyPostStats(items.length, draft ? 1 : 0);
    renderMyPostsView();
  } catch (err) {
    if (err && err.name === "AbortError") return;
    lastLoadedPosts = [];
    mediaCacheById = new Map();
    if (myPostsStatTotal) myPostsStatTotal.textContent = "—";
    if (myPostsNode) myPostsNode.innerHTML = `<p class="p-4 text-center text-on-surface-variant">${escapeHtml(err.message)}</p>`;
    if (myPostsEmpty) myPostsEmpty.classList.add("hidden");
  }
}

async function loadMyListings() {
  const signal = takeRequestSignal("my-listings");
  try {
    const [data, mediaData] = await Promise.all([
      apiCall("/api/v1/me/listings", "GET", undefined, { signal }),
      apiCall("/api/v1/media", "GET", undefined, { signal }).catch(() => null),
    ]);
    if (mediaData && Array.isArray(mediaData.items)) {
      mediaCacheById = new Map(mediaData.items.map((m) => [Number(m.id), m]));
    }
    lastListingItems = Array.isArray(data.items) ? data.items : [];
    renderMyListingsView();
  } catch (err) {
    if (err && err.name === "AbortError") return;
    myListingsNode.innerHTML = `<p>${escapeHtml(err.message)}</p>`;
  }
}

function getFilteredListings() {
  let items = lastListingItems.slice();
  if (listingFilter !== "all") items = items.filter((l) => String(l.type || "") === listingFilter);
  const q = getAdminSearchQuery();
  if (!q) return items;
  return items.filter((l) => {
    const title = String(l.title || "").toLowerCase();
    const desc = String(l.description || "").toLowerCase();
    const id = String(l.id || "");
    const type = String(l.type || "").toLowerCase();
    return title.includes(q) || desc.includes(q) || id.includes(q) || type.includes(q);
  });
}

function renderMyListingsView() {
  if (!myListingsNode) return;
  const items = getFilteredListings();
  if (myListingsMeta) {
    myListingsMeta.textContent = `${items.length} / ${lastListingItems.length} listings`;
  }
  myListingsNode.innerHTML = items.length ? "" : renderEditorialEmptyState(t("common.empty_my_listings"), "Nº L");
  if (!items.length) return;
  void renderInBatches(myListingsNode, items, buildMyListingNode, { batchSize: 10 });
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
    if (me.avatar_url) {
      avatar.src = me.avatar_url;
      avatar.alt = me.username;
      if (headerAvatar) headerAvatar.src = me.avatar_url;
    } else {
      avatar.removeAttribute("src");
      if (headerAvatar) headerAvatar.removeAttribute("src");
    }
    if (userHint) userHint.textContent = t("user.greeting").replace("{name}", me.nickname || me.username);
  } catch (err) {
    notify(err.message, "error");
  }
}

if (profileForm) {
  const avatarInput = profileForm.querySelector('[name="avatar_url"]');
  if (avatarInput) {
    avatarInput.addEventListener("input", () => {
      const v = String(avatarInput.value || "").trim();
      const avatar = document.querySelector("#profile-avatar");
      if (!avatar) return;
      if (v) {
        avatar.src = v;
      } else {
        avatar.removeAttribute("src");
      }
    });
  }
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
      notify(t("profile.saved"), "success");
      await loadProfile();
    } catch (err) { notify(err.message, "error"); }
  });
}

// ===== Notifications =====
function notifKindIconMaterial(kind) {
  switch (String(kind)) {
    case "all":
      return "all_inbox";
    case "system":
      return "campaign";
    case "comment":
      return "chat_bubble";
    case "order":
      return "receipt_long";
    case "message":
      return "mail";
    case "report_handled":
      return "gavel";
    default:
      return "notifications";
  }
}

function buildNotifCategoryNav(items) {
  if (!notifCategories) return;
  const list = Array.isArray(items) ? items : [];
  const kinds = new Set();
  for (const n of list) {
    if (n.kind) kinds.add(n.kind);
  }
  const keys = ["all", ...Array.from(kinds).sort()];
  if (notifFilterKind !== "all" && !keys.includes(notifFilterKind)) {
    notifFilterKind = "all";
  }
  const countFor = (k) =>
    k === "all" ? list.length : list.filter((n) => String(n.kind) === String(k)).length;

  notifCategories.innerHTML = "";
  const head = document.createElement("h3");
  head.className =
    "mb-2 pl-1 text-label-md font-semibold uppercase tracking-wider text-on-surface-variant";
  head.textContent = t("notify.categories_label");
  notifCategories.appendChild(head);

  for (const k of keys) {
    const b = document.createElement("button");
    b.type = "button";
    b.dataset.filter = k;
    const active = notifFilterKind === k;
    const label = k === "all" ? t("notify.cat_all") : t("notify.kind_" + k) || k;
    const icon = notifKindIconMaterial(k);
    const cnt = countFor(k);
    b.className =
      "notif-cat flex w-full items-center justify-between gap-2 rounded-xl px-3 py-2.5 text-left text-sm transition-colors " +
      (active
        ? "bg-primary-fixed/30 font-semibold text-primary-container ring-1 ring-primary-container/20"
        : "text-on-surface-variant hover:bg-surface-container-low");
    b.innerHTML = `
      <span class="flex min-w-0 flex-1 items-center gap-2">
        <span class="material-symbols-outlined shrink-0 text-[18px] leading-none ${active ? "" : "opacity-85"}">${icon}</span>
        <span class="truncate">${escapeHtml(label)}</span>
      </span>
      <span class="shrink-0 rounded-full px-2 py-0.5 text-center text-[10px] font-bold leading-none tabular-nums ${
        active ? "bg-primary-container text-white" : "bg-surface-container-highest/90 text-on-surface-variant"
      }">${cnt}</span>`;
    b.addEventListener("click", () => {
      notifFilterKind = k;
      buildNotifCategoryNav(lastNotifItems);
      renderNotifList();
    });
    notifCategories.appendChild(b);
  }
}

function renderNotifList() {
  if (!notificationsList) return;
  let items =
    notifFilterKind === "all"
      ? lastNotifItems.slice()
      : lastNotifItems.filter((n) => n.kind === notifFilterKind);
  if (notifUnreadOnly) items = items.filter((n) => !n.read);
  notificationsList.innerHTML = items.length ? "" : renderEditorialEmptyState(t("notify.empty"), "Nº N");
  for (const n of items) {
    const unread = !n.read;
    const kindLabel = escapeHtml(t("notify.kind_" + n.kind) || n.kind);
    const icon = notifKindIconMaterial(n.kind);
    const iconFill =
      n.kind === "system" || n.kind === "order" || n.kind === "message"
        ? " style=\"font-variation-settings: 'FILL' 1\""
        : "";
    const avatar = (n.actor_avatar_url && String(n.actor_avatar_url).trim()) || "";
    const thumb = (n.image_url && String(n.image_url).trim()) || "";
    const overlayIcon = `<div class="absolute -bottom-0.5 -right-0.5 flex h-6 w-6 items-center justify-center rounded-full border border-outline-variant/40 bg-white shadow-sm">
          <span class="material-symbols-outlined text-[14px] leading-none text-primary-container"${iconFill}>${icon}</span>
        </div>`;
    const leadColumn = avatar
      ? `<div class="relative h-12 w-12 shrink-0">
          <img src="${escapeHtml(avatar)}" alt="" class="h-12 w-12 rounded-full object-cover ring-2 ring-white" loading="lazy" decoding="async" />
          ${overlayIcon}
        </div>`
      : `<div class="relative flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-primary-fixed/25 text-primary-container">
          <span class="material-symbols-outlined text-[22px] leading-none"${iconFill}>${icon}</span>
        </div>`;
    const thumbCol = thumb
      ? `<div class="hidden shrink-0 self-start sm:block">
          <img src="${escapeHtml(thumb)}" alt="" class="h-16 w-16 rounded-xl border border-outline-variant/25 object-cover shadow-sm" loading="lazy" decoding="async" />
        </div>`
      : "";
    const el = document.createElement("div");
    el.className =
      "stitch-notif-card notification stitch-notif relative overflow-hidden rounded-2xl border p-4 transition-all md:p-5 " +
      (unread
        ? "cursor-pointer border-primary-container/20 bg-white shadow-[0_6px_24px_rgba(255,90,119,0.07)] hover:-translate-y-0.5 hover:border-primary-container/35 hover:shadow-[0_12px_36px_rgba(255,90,119,0.12)]"
        : "cursor-default border-outline-variant/15 bg-surface-container-lowest/95 hover:bg-surface-container-lowest");
    el.innerHTML = `
      <div class="pointer-events-none absolute bottom-0 left-0 top-0 w-1 rounded-full ${
        unread ? "bg-primary-container" : "bg-surface-variant/70"
      }" aria-hidden="true"></div>
      <div class="relative z-10 flex gap-3 pl-2 md:gap-4">
        ${leadColumn}
        <div class="min-w-0 flex-1">
          <div class="mb-1.5 flex flex-wrap items-center justify-between gap-2">
            <span class="inline-flex rounded-full border border-outline-variant/35 bg-surface-container/80 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-on-surface-variant">${kindLabel}</span>
            <span class="flex shrink-0 items-center gap-0.5 text-[11px] font-medium text-stone-400">
              <span class="material-symbols-outlined text-[14px]">schedule</span>
              ${escapeHtml(formatTime(n.created_at))}
            </span>
          </div>
          <div class="flex flex-wrap items-start justify-between gap-2">
            <h4 class="m-0 max-w-full flex-1 font-body-md font-semibold leading-snug text-on-background">${escapeHtml(n.title)}</h4>
            ${
              unread
                ? `<span class="shrink-0 rounded-full bg-primary-fixed/30 px-2 py-0.5 text-[10px] font-bold text-primary-container">${escapeHtml(t("notify.badge_new"))}</span>`
                : ""
            }
          </div>
          ${
            n.body
              ? `<p class="mb-0 mt-2 rounded-xl border border-surface-variant/40 bg-surface-container-low/80 px-3 py-2 text-sm leading-relaxed text-on-surface-variant">${escapeHtml(n.body)}</p>`
              : ""
          }
        </div>
        ${thumbCol}
      </div>
    `;
    if (unread) {
      el.setAttribute("role", "button");
      el.tabIndex = 0;
      const markRead = async () => {
        try {
          await apiCall(`/api/v1/notifications/${n.id}/read`, "POST");
        } catch (_) {}
        loadNotifications();
        refreshNotifBadge();
      };
      el.addEventListener("click", () => void markRead());
      el.addEventListener("keydown", (e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          void markRead();
        }
      });
    }
    notificationsList.appendChild(el);
  }
  refreshNotifBadge();
}

if (toggleUnreadOnlyBtn) {
  toggleUnreadOnlyBtn.addEventListener("click", () => {
    notifUnreadOnly = !notifUnreadOnly;
    toggleUnreadOnlyBtn.classList.toggle("bg-primary-container", notifUnreadOnly);
    toggleUnreadOnlyBtn.classList.toggle("text-white", notifUnreadOnly);
    renderNotifList();
  });
}

async function loadNotifications() {
  const signal = takeRequestSignal("notifications");
  try {
    const data = await apiCall("/api/v1/notifications", "GET", undefined, { signal });
    const items = Array.isArray(data.items) ? data.items : [];
    lastNotifItems = items;
    buildNotifCategoryNav(lastNotifItems);
    renderNotifList();
  } catch (err) {
    if (err && err.name === "AbortError") return;
    if (notificationsList) notificationsList.innerHTML = `<p class="p-4">${escapeHtml(err.message)}</p>`;
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
    if (notifUnreadDot) notifUnreadDot.hidden = c <= 0;
    if (notifCount) {
      if (c > 0) {
        notifCount.textContent = c > 99 ? "99+" : String(c);
        notifCount.hidden = false;
      } else {
        notifCount.hidden = true;
      }
    }
  } catch (_) {}
}

// ===== Messages =====
function conversationLooksTradeRelated(c) {
  const blob = `${c.last_message || ""} ${(c.peer && (c.peer.username || c.peer.nickname)) || ""}`.toLowerCase();
  const keys = [
    "order",
    "ord-",
    "listing",
    "¥",
    "$",
    "cny",
    "usd",
    "pay",
    "ship",
    "refund",
    "交易",
    "订单",
    "商品",
    "发货",
    "退款",
    "支付",
    "购买",
  ];
  return keys.some((k) => blob.includes(k));
}

async function renderFilteredConversations() {
  if (!convListNode) return;
  let items = lastConversationItems.slice();
  if (convMessageFilter === "unread") items = items.filter((c) => (c.unread_count || 0) > 0);
  if (convMessageFilter === "trades") items = items.filter(conversationLooksTradeRelated);
  if (activePanel === "messages" && adminGlobalSearch) {
    const q = (adminGlobalSearch.value || "").trim().toLowerCase();
    if (q) {
      items = items.filter((c) => {
        const peer = `${(c.peer && (c.peer.nickname || c.peer.username)) || ""}`;
        const idStr = c.peer && c.peer.id != null ? String(c.peer.id) : "";
        const lm = (c.last_message || "").toLowerCase();
        return peer.toLowerCase().includes(q) || lm.includes(q) || idStr.includes(q);
      });
    }
  }
  teardownVirtualList(convListNode);
  convListNode.innerHTML = items.length ? "" : renderEditorialEmptyState(t("message.empty"), "Nº D");
  if (!items.length) return;
  if (items.length > 80) {
    renderVirtualConversations(items);
    return;
  }
  await renderInBatches(convListNode, items, buildConversationNode, { batchSize: 12 });
}

async function loadConversations() {
  const signal = takeRequestSignal("conversations");
  try {
    const data = await apiCall("/api/v1/me/conversations", "GET", undefined, { signal });
    const items = Array.isArray(data.items) ? data.items : [];
    lastConversationItems = items;
    await renderFilteredConversations();
    if (!activePeerId && items.length) {
      const first = items[0];
      if (first && first.peer && first.peer.id) {
        await openConversation(first.peer.id);
      }
    }
  } catch (err) {
    if (err && err.name === "AbortError") return;
    lastConversationItems = [];
    teardownVirtualList(convListNode);
    convListNode.innerHTML = `<p>${escapeHtml(err.message)}</p>`;
  }
}

async function openConversation(peerId) {
  activePeerId = peerId;
  document.querySelectorAll("#conv-list button").forEach((b) =>
    b.classList.toggle("active", Number(b.dataset.peerId) === peerId));
  messageForm.hidden = false;
  try {
    const data = await apiCall(`/api/v1/me/conversations/${peerId}`, "GET", undefined, { signal: takeRequestSignal(`conversation:${peerId}`) });
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
    } catch (err) { notify(err.message, "error"); }
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
if (refreshConvBtn) refreshConvBtn.addEventListener("click", () => runRefresh(refreshConvBtn, loadConversations));

function boot() {
  const token = getToken();
  const rawUser = localStorage.getItem(USER_KEY);
  if (!token || !rawUser) {
    notLogin.hidden = false;
    dashboard.hidden = true;
    // Send the user to the dedicated login page, preserving return_to so
    // they bounce back into the dashboard after signing in.
    const back = encodeURIComponent("/dashboard" + window.location.hash);
    window.location.replace(`/login?return_to=${back}`);
    return;
  }
  const user = JSON.parse(rawUser);
  userHint.textContent = t("user.greeting").replace("{name}", user.nickname || user.username);
  if (headerAvatar) {
    if (user.avatar_url) {
      headerAvatar.src = user.avatar_url;
      headerAvatar.alt = user.nickname || user.username || "";
    } else {
      headerAvatar.removeAttribute("src");
    }
  }
  notLogin.hidden = true;
  dashboard.hidden = false;
  initStitchDashboard();
  loadPostDraft();
  loadListingDraft();
refreshPostPreview();
refreshListingPreview();
  refreshNotifBadge();
  setInterval(refreshNotifBadge, 30000);
  if (window.MeowShared && typeof window.MeowShared.applyI18n === "function") {
    window.MeowShared.applyI18n(dashboard);
  }
  syncAdminSearchPlaceholder();
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

function buildMediaNode(m) {
  const el = document.createElement("article");
  el.className =
    "item stitch-media-card group relative flex cursor-pointer flex-col overflow-hidden rounded-2xl border border-white bg-surface-container-lowest shadow-[0_8px_30px_rgba(0,0,0,0.04)] transition-all hover:-translate-y-1 hover:shadow-[0_12px_40px_rgba(255,90,119,0.1)]";
  const isVideo = m.kind === "video";
  const typeIcon = isVideo ? "play_circle" : "image";
  const typeColor = isVideo ? "text-secondary-container" : "text-primary-container";
  const thumb = isVideo
    ? `<video class="h-full w-full rounded-[1.25rem] object-cover" src="${escapeHtml(m.url)}" muted playsinline preload="metadata"></video>`
    : `<img alt="" class="h-full w-full rounded-[1.25rem] object-cover" src="${escapeHtml(m.url)}" />`;
  const safeName = escapeHtml(m.filename || `#${m.id}`);
  const metaLine = `${(m.size / 1024).toFixed(1)} KB · ${formatTime(m.created_at)}`;
  el.innerHTML = `
    <div class="relative aspect-square overflow-hidden p-2">
      <div class="relative h-full w-full overflow-hidden rounded-[1.25rem] bg-surface-container-low">${thumb}</div>
      <div class="absolute right-4 top-4 flex h-8 w-8 items-center justify-center rounded-full bg-white/80 shadow-sm backdrop-blur-md ${typeColor}">
        <span class="material-symbols-outlined text-[18px]">${typeIcon}</span>
      </div>
      <div class="pointer-events-none absolute inset-2 flex items-center justify-center gap-3 rounded-[1.25rem] bg-black/40 opacity-0 transition-opacity group-hover:pointer-events-auto group-hover:opacity-100">
        <button type="button" class="media-open h-10 w-10 rounded-full bg-white text-on-surface shadow-sm transition-transform hover:scale-110" title="Open">
          <span class="material-symbols-outlined text-[20px]">visibility</span>
        </button>
        <button type="button" class="media-copy h-10 w-10 rounded-full bg-white text-on-surface shadow-sm transition-transform hover:scale-110" title="${escapeHtml(t("media.copy_id"))}">
          <span class="material-symbols-outlined text-[20px]">content_copy</span>
        </button>
        <button type="button" class="media-delete h-10 w-10 rounded-full bg-white text-error shadow-sm transition-transform hover:scale-110" title="${escapeHtml(t("common.delete"))}">
          <span class="material-symbols-outlined text-[20px]">delete</span>
        </button>
      </div>
    </div>
    <div class="flex items-start justify-between px-4 pb-4 pt-1">
      <div class="min-w-0 truncate pr-2">
        <h4 class="truncate font-label-md text-on-surface">${safeName}</h4>
        <span class="text-[11px] text-on-surface-variant">#${m.id} · ${escapeHtml(String(m.kind || ""))} · ${escapeHtml(metaLine)}</span>
      </div>
    </div>
  `;
  el.querySelector(".media-open").addEventListener("click", (ev) => {
    ev.stopPropagation();
    window.open(m.url, "_blank", "noopener,noreferrer");
  });
  el.querySelector(".media-copy").addEventListener("click", (ev) => {
    ev.stopPropagation();
    if (navigator.clipboard) navigator.clipboard.writeText(String(m.id));
  });
  el.querySelector(".media-delete").addEventListener("click", async (ev) => {
    ev.stopPropagation();
    if (!confirm(`${t("common.delete")} #${m.id}?`)) return;
    try {
      await apiCall(`/api/v1/media/${m.id}`, "DELETE");
      await loadMedia();
    } catch (err) {
      notify(err.message, "error");
    }
  });
  return el;
}

function buildMyPostNode(post) {
  const el = document.createElement("article");
  el.className =
    "group flex cursor-pointer flex-col overflow-hidden rounded-2xl border border-white/60 bg-surface-container-lowest shadow-[0_8px_30px_rgba(0,0,0,0.04)] transition-transform duration-300 hover:-translate-y-1 hover:shadow-[0_12px_40px_rgba(255,90,119,0.1)]";
  if (post.isDraft) el.classList.add("opacity-90");
  const firstId = !post.isDraft && post.media_ids && post.media_ids[0] ? post.media_ids[0] : null;
  const media = firstId ? mediaCacheById.get(firstId) : null;
  const coverUrl = media && media.url ? media.url : null;
  const hero = coverUrl
    ? `<img src="${escapeHtml(coverUrl)}" alt="" class="h-48 w-full object-cover transition-transform duration-500 group-hover:scale-105" />`
    : `<div class="flex h-48 w-full items-center justify-center bg-surface-container"><span class="material-symbols-outlined text-4xl text-stone-300">image</span></div>`;
  const isDraft = !!post.isDraft;
  const badgeWrap = isDraft
    ? "bg-stone-100/90 text-stone-600 border border-stone-200"
    : "bg-white/90 text-on-surface";
  const dotClass = isDraft ? "bg-stone-400" : "bg-tertiary-container animate-pulse";
  const badgeText = isDraft ? t("dashboard.draft_badge") : t("dashboard.published_badge");
  const preview = (post.content || "").slice(0, 180);
  el.innerHTML = `
    <div class="relative h-48 w-full overflow-hidden rounded-t-2xl">
      ${hero}
      <div class="absolute right-3 top-3 flex items-center gap-1 rounded-full px-3 py-1 shadow-sm backdrop-blur-sm ${badgeWrap}">
        <span class="h-2 w-2 rounded-full ${dotClass}"></span>
        <span class="text-xs font-semibold tracking-tight">${escapeHtml(badgeText)}</span>
      </div>
    </div>
    <div class="flex flex-1 flex-col gap-2 p-4">
      <h3 class="m-0 line-clamp-2 text-lg font-bold leading-tight text-on-surface ${isDraft ? "italic" : ""}">${escapeHtml(post.title || "")}</h3>
      <p class="m-0 line-clamp-2 text-sm text-on-surface-variant">${escapeHtml(preview)}</p>
      <div class="mt-auto flex items-center justify-between border-t border-surface-variant pt-4">
        <div class="flex items-center gap-4 text-stone-400">
          <div class="flex items-center gap-1"><span class="material-symbols-outlined text-[18px]">visibility</span><span class="text-sm">0</span></div>
          <div class="flex items-center gap-1"><span class="material-symbols-outlined text-[18px]">favorite</span><span class="text-sm">0</span></div>
          <div class="flex items-center gap-1"><span class="material-symbols-outlined text-[18px]">chat_bubble</span><span class="text-sm">0</span></div>
        </div>
        <div class="flex gap-2">
          <button type="button" data-action="edit" class="flex h-8 w-8 items-center justify-center rounded-full bg-surface-container text-stone-500 transition-colors hover:bg-primary-container/10 hover:text-primary-container" title="${escapeHtml(
            post.isDraft ? t("dashboard.draft_continue") : t("common.edit")
          )}">
            <span class="material-symbols-outlined text-[18px]">edit</span>
          </button>
          <button type="button" data-action="delete" class="flex h-8 w-8 items-center justify-center rounded-full bg-surface-container text-stone-500 transition-colors hover:bg-error/10 hover:text-error" title="${escapeHtml(
            t("common.delete")
          )}">
            <span class="material-symbols-outlined text-[18px]">delete</span>
          </button>
        </div>
      </div>
    </div>
  `;
  el.querySelector('[data-action="edit"]').addEventListener("click", (ev) => {
    ev.stopPropagation();
    if (post.isDraft) {
      loadPostDraft();
      activateSection("compose-post");
      return;
    }
    void beginEditPost(post.id);
  });
  el.querySelector('[data-action="delete"]').addEventListener("click", async (ev) => {
    ev.stopPropagation();
    if (post.isDraft) {
      if (!confirm(t("common.confirm_delete_draft") || t("common.confirm_delete_post"))) return;
      try {
        localStorage.removeItem(DRAFT_POST_KEY);
        await loadMyPosts();
      } catch (err) {
        notify(err.message, "error");
      }
      return;
    }
    if (!confirm(t("common.confirm_delete_post") + ` #${post.id}`)) return;
    try {
      await apiCall(`/api/v1/posts/${post.id}`, "DELETE");
      await loadMyPosts();
    } catch (err) {
      notify(err.message, "error");
    }
  });
  if (!post.isDraft) {
    el.addEventListener("click", () => {
      window.open(`/post.html?id=${post.id}`, "_blank");
    });
  }
  return el;
}

function buildMyListingNode(listing) {
  const el = document.createElement("article");
  el.className =
    "group flex flex-col overflow-hidden rounded-2xl border border-white/70 bg-surface-container-lowest shadow-[0_8px_30px_rgba(0,0,0,0.04)] transition-transform hover:-translate-y-1 hover:shadow-[0_12px_40px_rgba(255,90,119,0.1)]";
  const thumb = Array.isArray(listing.media_ids) && listing.media_ids.length
    ? mediaCacheById.get(Number(listing.media_ids[0]))
    : null;
  const hero = thumb && thumb.url
    ? `<img src="${escapeHtml(thumb.url)}" alt="" class="h-44 w-full object-cover transition-transform duration-500 group-hover:scale-105" />`
    : `<div class="h-44 w-full bg-surface-container flex items-center justify-center"><span class="material-symbols-outlined text-4xl text-stone-300">inventory_2</span></div>`;
  const price = `${(listing.price_cents / 100).toFixed(2)} ${escapeHtml(listing.currency || "CNY")}`;
  el.innerHTML = `
    <div class="relative h-44 w-full overflow-hidden rounded-t-2xl">
      ${hero}
      <span class="absolute right-3 top-3 rounded-full bg-white/90 px-3 py-1 text-xs font-semibold text-on-surface shadow-sm">${escapeHtml(listing.type || "")}</span>
    </div>
    <div class="flex flex-1 flex-col gap-2 p-4">
      <h3 class="m-0 line-clamp-2 text-lg font-bold text-on-surface">${escapeHtml(listing.title)}</h3>
      <p class="m-0 line-clamp-2 text-sm text-on-surface-variant">${escapeHtml(listing.description || "")}</p>
      <p class="m-0 text-sm font-semibold text-primary-container">${escapeHtml(t("meta.price"))} ${price}</p>
      <p class="m-0 text-xs text-on-surface-variant">${formatTime(listing.created_at)}</p>
      <div class="mt-auto flex items-center justify-end gap-2 pt-2">
        <button type="button" class="flex h-8 w-8 items-center justify-center rounded-full bg-surface-container text-stone-500 transition-colors hover:bg-primary-container/10 hover:text-primary-container" data-action="edit" title="${escapeHtml(t("common.edit"))}">
          <span class="material-symbols-outlined text-[18px]">edit</span>
        </button>
        <button type="button" class="flex h-8 w-8 items-center justify-center rounded-full bg-surface-container text-stone-500 transition-colors hover:bg-error/10 hover:text-error danger" data-id="${listing.id}" title="${escapeHtml(t("common.delete"))}">
          <span class="material-symbols-outlined text-[18px]">delete</span>
        </button>
      </div>
    </div>
  `;
  el.querySelector('[data-action="edit"]').addEventListener("click", () => {
    void beginEditListing(listing.id);
  });
  el.querySelector("button.danger").addEventListener("click", async () => {
    if (!confirm(t("common.confirm_delete_listing") + ` #${listing.id}`)) return;
    try {
      await apiCall(`/api/v1/listings/${listing.id}`, "DELETE");
      await loadMyListings();
    } catch (err) {
      notify(err.message, "error");
    }
  });
  return el;
}

async function beginEditPost(postId) {
  try {
    const data = await apiCall(`/api/v1/posts/${postId}`, "GET");
    const p = data.post;
    if (!p) return;
    editingPostId = postId;
    const titleInput = postForm.querySelector('[name="title"]');
    if (titleInput) titleInput.value = p.title;
    if (postContentEl) postContentEl.value = p.content || "";
    if (p.category) setCategoryRadio(String(p.category));
    postMediaIds = (p.media_ids || []).map(Number).filter((n) => n > 0);
    syncPostMediaField();
    renderPostMediaThumbs();
    setPostTagList(p.tags || []);
    if (postEditBadge) {
      postEditBadge.classList.remove("hidden");
      postEditBadge.classList.add("inline-flex");
    }
    if (postCancelEdit) postCancelEdit.classList.remove("hidden");
    activateSection("compose-post");
  } catch (err) {
    notify(err.message, "error");
  }
}

async function beginEditListing(listingId) {
  try {
    const data = await apiCall(`/api/v1/listings/${listingId}`, "GET");
    const l = data.listing;
    if (!l) return;
    editingListingId = listingId;
    const f = listingForm;
    const tEl = f.querySelector('[name="title"]');
    if (tEl) tEl.value = l.title;
    const dEl = f.querySelector('[name="description"]');
    if (dEl) dEl.value = l.description || "";
    syncListingYuanFromCents(l.price_cents);
    setListingCurrency(l.currency || "CNY");
    if (l.type) setListingTypeRadio(String(l.type));
    listingMediaIds = (l.media_ids || []).map(Number).filter((n) => n > 0);
    syncListingMediaField();
    renderListingMediaThumbs();
    if (listingEditBadge) {
      listingEditBadge.classList.remove("hidden");
      listingEditBadge.classList.add("inline-flex");
    }
    if (listingCancelEdit) listingCancelEdit.classList.remove("hidden");
    activateSection("compose-listing");
  } catch (err) {
    notify(err.message, "error");
  }
}

function buildConversationNode(c) {
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
  if (activePeerId === c.peer.id) btn.classList.add("active");
  return btn;
}

async function renderInBatches(container, items, renderItem, options = {}) {
  const batchSize = options.batchSize || 12;
  const jobId = Symbol("render-job");
  activeRenderJobs.set(container, jobId);
  container.innerHTML = "";
  for (let i = 0; i < items.length; i += batchSize) {
    if (activeRenderJobs.get(container) !== jobId) return;
    const frag = document.createDocumentFragment();
    const batch = items.slice(i, i + batchSize);
    for (const item of batch) {
      frag.appendChild(renderItem(item));
    }
    container.appendChild(frag);
    if (i + batchSize < items.length) await nextFrame();
  }
}

function nextFrame() {
  return new Promise((resolve) => requestAnimationFrame(() => resolve()));
}

function renderVirtualConversations(items) {
  teardownVirtualList(convListNode);
  convListNode.innerHTML = "";
  const rowHeight = 62;
  const overscan = 6;
  const topSpacer = document.createElement("div");
  const bottomSpacer = document.createElement("div");
  const viewport = document.createElement("div");
  convListNode.append(topSpacer, viewport, bottomSpacer);
  let currentStart = -1;
  let currentEnd = -1;

  const renderWindow = () => {
    const scrollTop = convListNode.scrollTop;
    const viewportHeight = convListNode.clientHeight || 420;
    const visibleStart = Math.max(0, Math.floor(scrollTop / rowHeight) - overscan);
    const visibleEnd = Math.min(items.length, Math.ceil((scrollTop + viewportHeight) / rowHeight) + overscan);
    if (visibleStart === currentStart && visibleEnd === currentEnd) return;
    currentStart = visibleStart;
    currentEnd = visibleEnd;
    topSpacer.style.height = `${visibleStart * rowHeight}px`;
    bottomSpacer.style.height = `${Math.max(0, (items.length - visibleEnd) * rowHeight)}px`;
    viewport.innerHTML = "";
    const frag = document.createDocumentFragment();
    for (let i = visibleStart; i < visibleEnd; i++) {
      frag.appendChild(buildConversationNode(items[i]));
    }
    viewport.appendChild(frag);
  };

  const onScroll = () => requestAnimationFrame(renderWindow);
  convListNode.addEventListener("scroll", onScroll, { passive: true });
  renderWindow();
  virtualListCleanups.set(convListNode, () => {
    convListNode.removeEventListener("scroll", onScroll);
  });
}

function teardownVirtualList(container) {
  const cleanup = virtualListCleanups.get(container);
  if (cleanup) {
    cleanup();
    virtualListCleanups.delete(container);
  }
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

boot();
