let adminKey = "";
const t = (k) => window.MeowShared.t(k);

const authForm = document.querySelector("#auth-form");
const keyInput = document.querySelector("#admin-key");
const summaryNode = document.querySelector("#summary");
const postsNode = document.querySelector("#admin-posts");
const commentsNode = document.querySelector("#admin-comments");
const listingsNode = document.querySelector("#admin-listings");

document.querySelector("#refresh-posts").addEventListener("click", () => loadPosts());
document.querySelector("#refresh-comments").addEventListener("click", () => loadComments());
document.querySelector("#refresh-listings").addEventListener("click", () => loadListings());
document.querySelector("#refresh-media").addEventListener("click", () => loadAdminMedia());
document.querySelector("#refresh-reports").addEventListener("click", () => loadAdminReports());
document.querySelector("#refresh-orders").addEventListener("click", () => loadAdminOrders());
document.querySelector("#refresh-audit").addEventListener("click", () => loadAdminAudit());

const adminMediaNode = document.querySelector("#admin-media");
const adminReportsNode = document.querySelector("#admin-reports");
const adminOrdersNode = document.querySelector("#admin-orders");
const adminAuditNode = document.querySelector("#admin-audit");

authForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  adminKey = keyInput.value.trim();
  if (!adminKey) return;
  localStorage.setItem("admin_key", adminKey);
  await loadAll();
});

async function loadAll() {
  await Promise.all([
    loadSummary(),
    loadPosts(),
    loadComments(),
    loadListings(),
    loadAdminMedia(),
    loadAdminReports(),
    loadAdminOrders(),
    loadAdminAudit(),
  ]);
}

async function loadAdminAudit() {
  try {
    const data = await adminFetch("/api/v1/admin/audit-logs?limit=200");
    const items = Array.isArray(data.items) ? data.items : [];
    adminAuditNode.innerHTML = items.length ? "" : `<p>${escapeHtml(t("admin.audit_empty"))}</p>`;
    for (const log of items) {
      const el = document.createElement("article");
      el.className = "item";
      const when = new Date(log.created_at).toLocaleString();
      el.innerHTML = `
        <p><span class="pill">${escapeHtml(log.action)}</span> · <strong>${escapeHtml(log.actor)}</strong> · ${escapeHtml(when)}</p>
        <p class="meta">${escapeHtml(log.target_kind || "-")} #${log.target_id || "-"} · ip ${escapeHtml(log.ip || "-")}</p>
        ${log.note ? `<p>${escapeHtml(log.note)}</p>` : ""}
      `;
      adminAuditNode.appendChild(el);
    }
  } catch (err) {
    adminAuditNode.innerHTML = `<p>${escapeHtml(err.message)}</p>`;
  }
}

async function loadAdminMedia() {
  try {
    const data = await adminFetch("/api/v1/admin/media");
    const items = Array.isArray(data.items) ? data.items : [];
    adminMediaNode.innerHTML = items.length ? "" : `<p>${escapeHtml(t("common.no_items"))}</p>`;
    for (const m of items) {
      const el = document.createElement("article");
      el.className = "item";
      const preview = m.kind === "video"
        ? `<video src="${escapeHtml(m.url)}" controls preload="metadata" style="max-width:280px;border-radius:8px"></video>`
        : `<img src="${escapeHtml(m.url)}" alt="" style="max-width:280px;border-radius:8px" />`;
      el.innerHTML = `
        ${preview}
        <p>#${m.id} · owner ${m.owner_id} · ${escapeHtml(m.kind)} · ${escapeHtml(m.mime)} · ${escapeHtml(m.status)}</p>
        <div class="row">
          <button data-act="approve">${escapeHtml(t("admin.btn_approve"))}</button>
          <button data-act="reject">${escapeHtml(t("admin.btn_reject"))}</button>
          <button class="danger" data-act="delete">${escapeHtml(t("common.delete"))}</button>
        </div>
      `;
      el.querySelectorAll("button").forEach((btn) => {
        btn.addEventListener("click", async () => {
          try {
            const act = btn.dataset.act;
            if (act === "delete") {
              if (!confirm(`${t("common.delete")} media #${m.id}?`)) return;
              await adminFetch(`/api/v1/admin/media/${m.id}`, { method: "DELETE" });
            } else {
              await adminFetch(`/api/v1/admin/media/${m.id}/${act}`, { method: "POST" });
            }
            await loadAdminMedia();
          } catch (err) {
            notify(err.message, "error");
          }
        });
      });
      adminMediaNode.appendChild(el);
    }
  } catch (err) {
    adminMediaNode.innerHTML = `<p>${escapeHtml(err.message)}</p>`;
  }
}

async function loadAdminReports() {
  try {
    const data = await adminFetch("/api/v1/admin/reports");
    const items = Array.isArray(data.items) ? data.items : [];
    adminReportsNode.innerHTML = items.length ? "" : `<p>${escapeHtml(t("common.no_items"))}</p>`;
    for (const r of items) {
      const el = document.createElement("article");
      el.className = "item";
      el.innerHTML = `
        <p><strong>#${r.id}</strong> ${escapeHtml(r.target_kind)} #${r.target_id} · reporter ${r.reporter_id} · <b>${escapeHtml(r.status)}</b></p>
        <p>${escapeHtml(r.reason)}</p>
        <div class="row">
          <button data-act="resolve">${escapeHtml(t("admin.btn_resolve"))}</button>
          <button class="danger" data-act="dismiss">${escapeHtml(t("admin.btn_dismiss"))}</button>
        </div>
      `;
      el.querySelectorAll("button").forEach((btn) => {
        btn.addEventListener("click", async () => {
          try {
            const act = btn.dataset.act;
            const body = act === "resolve" ? { delete_target: true } : {};
            await adminFetch(`/api/v1/admin/reports/${r.id}/${act}`, {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify(body),
            });
            await loadAll();
          } catch (err) {
            notify(err.message, "error");
          }
        });
      });
      adminReportsNode.appendChild(el);
    }
  } catch (err) {
    adminReportsNode.innerHTML = `<p>${escapeHtml(err.message)}</p>`;
  }
}

async function loadAdminOrders() {
  try {
    const data = await adminFetch("/api/v1/admin/orders");
    const items = Array.isArray(data.items) ? data.items : [];
    adminOrdersNode.innerHTML = items.length ? "" : `<p>${escapeHtml(t("common.no_items"))}</p>`;
    for (const o of items) {
      const el = document.createElement("article");
      el.className = "item";
      el.innerHTML = `
        <p><strong>#${o.id}</strong> listing ${o.listing_id} · ${escapeHtml(o.listing_title || "")}</p>
        <p>buyer ${o.buyer_id} → seller ${o.seller_id} · ${(o.amount_cents / 100).toFixed(2)} ${escapeHtml(o.currency || "CNY")} · <b>${escapeHtml(t(`order.status.${o.status}`))}</b></p>
      `;
      adminOrdersNode.appendChild(el);
    }
  } catch (err) {
    adminOrdersNode.innerHTML = `<p>${escapeHtml(err.message)}</p>`;
  }
}

async function loadSummary() {
  const result = await adminFetch("/api/v1/admin/summary");
  summaryNode.innerHTML = `
    <div class="metric"><div>${escapeHtml(t("admin.metric_posts"))}</div><div class="value">${result.posts}</div></div>
    <div class="metric"><div>${escapeHtml(t("admin.metric_comments"))}</div><div class="value">${result.comments}</div></div>
    <div class="metric"><div>${escapeHtml(t("admin.metric_listings"))}</div><div class="value">${result.listings}</div></div>
    <div class="metric"><div>${escapeHtml(t("admin.metric_users"))}</div><div class="value">${result.users ?? "—"}</div></div>
    <div class="metric"><div>${escapeHtml(t("admin.metric_orders"))}</div><div class="value">${result.orders ?? "—"}</div></div>
    <div class="metric"><div>${escapeHtml(t("admin.metric_media"))}</div><div class="value">${result.media ?? "—"}</div></div>
    <div class="metric"><div>${escapeHtml(t("admin.metric_reports"))}</div><div class="value">${result.reports ?? "—"}</div></div>
  `;
}

async function loadPosts() {
  const data = await adminFetch("/api/v1/admin/posts");
  const items = Array.isArray(data.items) ? data.items : [];
  postsNode.innerHTML = items.length ? "" : `<p>${escapeHtml(t("common.no_items"))}</p>`;
  for (const post of items) {
    const el = document.createElement("article");
    el.className = "item";
    el.innerHTML = `
      <p><strong>#${post.id}</strong> ${escapeHtml(post.title)}</p>
      <p>${escapeHtml(post.content)}</p>
      <p>${escapeHtml(t("meta.author"))} ${post.author_id} · ${escapeHtml(post.category)}</p>
      <button class="danger" data-id="${post.id}" data-kind="post">${escapeHtml(t("admin.btn_delete_post"))}</button>
    `;
    postsNode.appendChild(el);
  }
  bindDeleteHandlers(postsNode);
}

async function loadComments() {
  const data = await adminFetch("/api/v1/admin/comments");
  const items = Array.isArray(data.items) ? data.items : [];
  commentsNode.innerHTML = items.length ? "" : `<p>${escapeHtml(t("common.no_items"))}</p>`;
  for (const comment of items) {
    const el = document.createElement("article");
    el.className = "item";
    el.innerHTML = `
      <p><strong>#${comment.id}</strong> post ${comment.post_id} · user ${comment.author_id}</p>
      <p>${escapeHtml(comment.content)}</p>
      <button class="danger" data-id="${comment.id}" data-kind="comment">${escapeHtml(t("admin.btn_delete_comment"))}</button>
    `;
    commentsNode.appendChild(el);
  }
  bindDeleteHandlers(commentsNode);
}

async function loadListings() {
  const data = await adminFetch("/api/v1/admin/listings");
  const items = Array.isArray(data.items) ? data.items : [];
  listingsNode.innerHTML = items.length ? "" : `<p>${escapeHtml(t("common.no_items"))}</p>`;
  for (const listing of items) {
    const el = document.createElement("article");
    el.className = "item";
    el.innerHTML = `
      <p><strong>#${listing.id}</strong> ${escapeHtml(listing.title)}</p>
      <p>${escapeHtml(listing.description || "")}</p>
      <p>${escapeHtml(t("meta.seller"))} ${listing.seller_id} · ${escapeHtml(listing.type)} · ${escapeHtml(t("meta.price"))} ${(listing.price_cents / 100).toFixed(2)} ${escapeHtml(listing.currency)}</p>
      <button class="danger" data-id="${listing.id}" data-kind="listing">${escapeHtml(t("admin.btn_delete_listing"))}</button>
    `;
    listingsNode.appendChild(el);
  }
  bindDeleteHandlers(listingsNode);
}

function bindDeleteHandlers(container) {
  container.querySelectorAll("button.danger").forEach((btn) => {
    btn.addEventListener("click", async () => {
      const id = btn.dataset.id;
      const kind = btn.dataset.kind;
      if (!id || !kind) return;

      if (!confirm(`${t("common.delete")} ${kind} #${id} ?`)) return;

      const pathMap = {
        post: `/api/v1/admin/posts/${id}`,
        comment: `/api/v1/admin/comments/${id}`,
        listing: `/api/v1/admin/listings/${id}`,
      };
      await adminFetch(pathMap[kind], { method: "DELETE" });
      await loadAll();
    });
  });
}

async function adminFetch(url, init = {}) {
  if (!adminKey) {
    throw new Error("请先输入管理员密钥");
  }
  const response = await fetch(url, {
    ...init,
    headers: {
      ...(init.headers || {}),
      "X-Admin-Key": adminKey,
    },
  });
  let payload = null;
  try {
    payload = await response.json();
  } catch (_) {
    payload = null;
  }
  if (!response.ok) {
    const message = (payload && payload.message) || "请求失败";
    throw new Error(message);
  }
  return payload && payload.data !== undefined ? payload.data : payload;
}

function escapeHtml(input) {
  return String(input)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function notify(message, kind) {
  if (window.MeowShared && typeof window.MeowShared.toast === "function") {
    window.MeowShared.toast(message, kind);
    return;
  }
  alert(message);
}

const cachedKey = localStorage.getItem("admin_key");
if (cachedKey) {
  keyInput.value = cachedKey;
  adminKey = cachedKey;
  loadAll().catch((err) => {
    summaryNode.innerHTML = `<p>${escapeHtml(err.message)}</p>`;
  });
}
