(() => {
  const TOKEN_KEY = "meow_token";
  const USER_KEY = "meow_user";
  const t = (k) => window.MeowShared.t(k);
  const toast = (m, k) => window.MeowShared.toast(m, k);

  const guest = document.querySelector("#msg-guest");
  const shell = document.querySelector("#msg-shell");
  const convNode = document.querySelector("#msg-conversations");
  const streamNode = document.querySelector("#msg-stream");
  const peerName = document.querySelector("#msg-peer-name");
  const peerSub = document.querySelector("#msg-peer-sub");
  const form = document.querySelector("#msg-form");
  const refreshBtn = document.querySelector("#msg-refresh");

  let convs = [];
  let filter = "all";
  let activePeerId = 0;

  function getToken() {
    return localStorage.getItem(TOKEN_KEY) || "";
  }

  function me() {
    try {
      return JSON.parse(localStorage.getItem(USER_KEY) || "{}");
    } catch (_) {
      return {};
    }
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
    if (!res.ok) throw new Error((payload && payload.message) || "request failed");
    return payload && payload.data !== undefined ? payload.data : payload;
  }

  function isTradeConv(c) {
    const text = `${c.last_message || ""} ${(c.peer && c.peer.nickname) || ""}`.toLowerCase();
    return ["order", "listing", "交易", "订单", "支付", "发货", "退款", "¥", "$"].some((k) => text.includes(k));
  }

  function filteredConvs() {
    if (filter === "unread") return convs.filter((c) => (c.unread_count || 0) > 0);
    if (filter === "trades") return convs.filter(isTradeConv);
    return convs;
  }

  function fmtTime(v) {
    if (!v) return "";
    const d = new Date(v);
    if (Number.isNaN(d.getTime())) return "";
    return d.toLocaleString();
  }

  function renderConversations() {
    const items = filteredConvs();
    convNode.innerHTML = "";
    if (!items.length) {
      convNode.innerHTML = `<p class="text-sm text-on-surface-variant px-2 py-4">${t("message.empty")}</p>`;
      return;
    }
    for (const c of items) {
      const btn = document.createElement("button");
      btn.type = "button";
      btn.className =
        "w-full text-left rounded-2xl px-3 py-2.5 border transition-colors " +
        (Number(c.peer?.id) === activePeerId
          ? "bg-primary-container/10 border-primary-container/30"
          : "bg-surface-container-lowest border-outline-variant/20 hover:bg-surface-container");
      const name = (c.peer && (c.peer.nickname || c.peer.username)) || `User ${c.peer_id}`;
      const unread = Number(c.unread_count || 0);
      btn.innerHTML = `
        <div class="flex items-center justify-between gap-2">
          <p class="text-body-md font-semibold truncate">${name}</p>
          ${unread > 0 ? `<span class="rounded-full bg-primary-container text-white text-[10px] px-2 py-0.5">${unread > 99 ? "99+" : unread}</span>` : ""}
        </div>
        <p class="text-label-md text-on-surface-variant truncate mt-1">${c.last_message || ""}</p>
      `;
      btn.addEventListener("click", () => openConversation(Number(c.peer.id || c.peer_id || 0), name));
      convNode.appendChild(btn);
    }
  }

  function renderMessages(messages) {
    streamNode.innerHTML = "";
    const meId = Number(me().id || 0);
    if (!messages.length) {
      streamNode.innerHTML = `<p class="text-sm text-on-surface-variant">${t("message.empty")}</p>`;
      return;
    }
    for (const m of messages) {
      const mine = Number(m.sender_id) === meId;
      const wrap = document.createElement("div");
      wrap.className = `flex ${mine ? "justify-end" : "justify-start"}`;
      wrap.innerHTML = `
        <div class="${mine ? "bg-primary-container text-white" : "bg-surface-container-low text-on-surface"} max-w-[80%] rounded-2xl px-3 py-2.5">
          <p class="text-body-md whitespace-pre-wrap break-words">${m.content || ""}</p>
          <p class="text-[11px] opacity-70 mt-1">${fmtTime(m.created_at)}</p>
        </div>
      `;
      streamNode.appendChild(wrap);
    }
    streamNode.scrollTop = streamNode.scrollHeight;
  }

  async function openConversation(peerId, nameHint) {
    if (!peerId) return;
    activePeerId = peerId;
    renderConversations();
    peerName.textContent = nameHint || `User ${peerId}`;
    peerSub.textContent = `ID ${peerId}`;
    form.hidden = false;
    try {
      const data = await apiCall(`/api/v1/me/conversations/${peerId}`, "GET");
      renderMessages(Array.isArray(data.messages) ? data.messages : []);
      await loadConversations();
    } catch (err) {
      toast(err.message, "error");
    }
  }

  async function loadConversations() {
    try {
      const data = await apiCall("/api/v1/me/conversations", "GET");
      convs = Array.isArray(data.items) ? data.items : [];
      if (!activePeerId && convs.length) {
        const first = convs[0];
        const id = Number((first.peer && first.peer.id) || first.peer_id || 0);
        const n = (first.peer && (first.peer.nickname || first.peer.username)) || `User ${id}`;
        await openConversation(id, n);
        return;
      }
      renderConversations();
    } catch (err) {
      convNode.innerHTML = `<p class="text-sm text-error px-2 py-4">${err.message}</p>`;
    }
  }

  async function sendMessage(content) {
    if (!activePeerId) return;
    await apiCall("/api/v1/messages", "POST", {
      recipient_id: activePeerId,
      content,
    });
    await openConversation(activePeerId, peerName.textContent);
  }

  document.querySelectorAll(".msg-filter").forEach((btn) => {
    btn.addEventListener("click", () => {
      filter = btn.dataset.filter || "all";
      document.querySelectorAll(".msg-filter").forEach((b) => {
        const on = b === btn;
        b.classList.toggle("bg-primary-container", on);
        b.classList.toggle("text-white", on);
        b.classList.toggle("bg-surface-container", !on);
        b.classList.toggle("text-on-surface-variant", !on);
      });
      renderConversations();
    });
  });

  refreshBtn?.addEventListener("click", () => {
    loadConversations();
  });

  form?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(form);
    const content = String(fd.get("content") || "").trim();
    if (!content) return;
    try {
      await sendMessage(content);
      form.reset();
    } catch (err) {
      toast(err.message, "error");
    }
  });

  function init() {
    if (!getToken()) {
      guest?.classList.remove("hidden");
      shell?.classList.add("hidden");
      return;
    }
    guest?.classList.add("hidden");
    shell?.classList.remove("hidden");
    loadConversations();
  }

  init();
})();
