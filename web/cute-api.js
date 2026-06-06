/**
 * cute-api.js — M&D Web Client API integration
 * Connects cute.html SPA shell to the Go backend REST API.
 * Depends on cute-ui.js (loaded first) for theme/view switching.
 */
(function () {
  'use strict';

  /* ───────── constants ───────── */
  const TOKEN_KEY = 'meow_token';
  const USER_KEY  = 'meow_user';
  const API_BASE  = '';                         // same origin
  const ADMIN_KEY_STORAGE = 'meow_admin_key';

  /* ───────── state ───────── */
  let currentUser   = null;
  let feedFilter    = 'rec';
  let feedPage      = 1;
  let marketFilter  = '';
  let orderRole     = 'buyer';
  let activePeerId  = null;
  let pollTimer     = null;

  /* ───────── helpers ───────── */
  function getToken()  { return localStorage.getItem(TOKEN_KEY); }
  function setToken(t) { localStorage.setItem(TOKEN_KEY, t); }
  function setUser(u)  { currentUser = u; localStorage.setItem(USER_KEY, JSON.stringify(u)); }
  function clearAuth() { localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(USER_KEY); currentUser = null; }

  function escHtml(s) {
    const el = document.createElement('span');
    el.textContent = s ?? '';
    return el.innerHTML;
  }

  function toast(msg, kind) {
    let box = document.getElementById('mnd-toast');
    if (!box) {
      box = document.createElement('div');
      box.id = 'mnd-toast';
      box.className = 'mnd-toast';
      document.body.appendChild(box);
    }
    const item = document.createElement('div');
    item.className = 'mnd-toast-item ' + (kind === 'error' ? 'toast-error' : kind === 'success' ? 'toast-success' : 'toast-info');
    item.textContent = msg;
    box.appendChild(item);
    requestAnimationFrame(() => item.classList.add('show'));
    setTimeout(() => { item.classList.remove('show'); setTimeout(() => item.remove(), 320); }, 3200);
  }

  async function api(path, method, body, opts) {
    const headers = { 'Content-Type': 'application/json' };
    const token = getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;
    if (opts?.adminKey) headers['X-Admin-Key'] = opts.adminKey;
    const init = { method: method || 'GET', headers };
    if (body !== undefined) init.body = JSON.stringify(body);
    const res = await fetch(API_BASE + path, init);
    let json = null;
    try { json = await res.json(); } catch (_) {}
    if (!res.ok) {
      if (res.status === 401 && token) { clearAuth(); updateAuthUI(); }
      throw new Error((json && json.message) || 'Request failed');
    }
    return json && json.data !== undefined ? json.data : json;
  }

  function formatPrice(cents, currency) {
    return (cents / 100).toFixed(2) + ' ' + (currency || 'CNY');
  }
  function timeAgo(iso) {
    const d = new Date(iso);
    const s = Math.floor((Date.now() - d.getTime()) / 1000);
    if (s < 60) return 'just now';
    if (s < 3600) return Math.floor(s / 60) + 'm ago';
    if (s < 86400) return Math.floor(s / 3600) + 'h ago';
    return Math.floor(s / 86400) + 'd ago';
  }
  function skeleton(n) {
    return Array.from({ length: n }, () =>
      '<article class="feed-card skeleton-card" aria-label="loading"><header><i></i><div><em></em><em></em></div></header><p></p><div class="skeleton-media"></div></article>'
    ).join('');
  }
  function emptyCard(icon, title, desc) {
    return `<article class="empty-card"><span class="material-symbols-rounded">${escHtml(icon)}</span><b>${escHtml(title)}</b><p>${escHtml(desc)}</p></article>`;
  }

  /* ───────── AUTH UI ───────── */
  function updateAuthUI() {
    const navAuth = document.querySelector('[data-view="auth"] b');
    if (navAuth) navAuth.textContent = currentUser ? 'Logout' : 'Login';
    // Sidebar badge
    const msgBadge = document.querySelector('.nav-item[data-view="messages"] i');
    if (msgBadge) refreshUnreadCount(msgBadge);
  }

  async function refreshUnreadCount(badge) {
    if (!getToken() || !badge) return;
    try {
      const d = await api('/api/v1/notifications?unread=true');
      const c = d.unread_count || 0;
      badge.textContent = c > 0 ? String(c) : '';
      badge.style.display = c > 0 ? '' : 'none';
    } catch (_) {}
  }

  /* ───────── AUTH panel ───────── */
  function bindAuthPanel() {
    const loginBtn = document.querySelector('.auth-form-card .primary-button');
    const regBtn   = document.querySelector('.auth-register-card .primary-button');
    if (loginBtn && !loginBtn._bound) {
      loginBtn._bound = true;
      loginBtn.addEventListener('click', async () => {
        const form = loginBtn.closest('form');
        const inputs = form.querySelectorAll('.auth-field input');
        const username = inputs[0]?.value?.trim();
        const password = inputs[1]?.value?.trim();
        if (!username || !password) { toast('Please enter username and password', 'error'); return; }
        try {
          loginBtn.disabled = true;
          const d = await api('/api/v1/auth/login', 'POST', { username, password });
          setToken(d.token);
          setUser(d.user);
          toast('Welcome back, ' + (d.user.nickname || d.user.username) + '!', 'success');
          updateAuthUI();
          document.querySelector('[data-view="feed"]')?.click();
        } catch (e) { toast(e.message, 'error'); }
        finally { loginBtn.disabled = false; }
      });
    }
    if (regBtn && !regBtn._bound) {
      regBtn._bound = true;
      regBtn.addEventListener('click', async () => {
        const form = regBtn.closest('form');
        const inputs = form.querySelectorAll('.auth-field input');
        const nickname = inputs[0]?.value?.trim();
        const phone    = inputs[1]?.value?.trim();
        const code     = inputs[2]?.value?.trim();
        const username = nickname?.toLowerCase().replace(/\s+/g, '_');
        if (!nickname || nickname.length < 3) { toast('Display name must be at least 3 characters', 'error'); return; }
        try {
          regBtn.disabled = true;
          const d = await api('/api/v1/auth/register', 'POST', {
            username, password: 'catcircle', nickname, phone, sms_code: code
          });
          setToken(d.token);
          setUser(d.user);
          toast('Account created! Welcome to M&D.', 'success');
          updateAuthUI();
          document.querySelector('[data-view="feed"]')?.click();
        } catch (e) { toast(e.message, 'error'); }
        finally { regBtn.disabled = false; }
      });
    }
    // Logout via nav
    const authNavBtn = document.querySelector('.nav-item[data-view="auth"]');
    if (authNavBtn && !authNavBtn._logoutBound) {
      authNavBtn._logoutBound = true;
      authNavBtn.addEventListener('click', () => {
        if (currentUser) { clearAuth(); toast('Signed out', 'info'); updateAuthUI(); }
      });
    }
  }

  /* ───────── FEED panel ───────── */
  async function loadFeed() {
    const stream = document.querySelector('.web-feed-stream');
    if (!stream) return;
    stream.innerHTML = skeleton(3);
    try {
      const params = new URLSearchParams({ page: feedPage, page_size: 20, filter: feedFilter });
      const d = await api('/api/v1/posts?' + params);
      const items = Array.isArray(d.items) ? d.items : [];
      if (items.length === 0) {
        stream.innerHTML = emptyCard('rss_feed', 'No posts yet', 'Be the first to share a cat moment!');
        return;
      }
      stream.innerHTML = '';
      // Hero card
      const hero = document.createElement('div');
      hero.className = 'feed-hero-card';
      hero.innerHTML = `<div><span class="eyebrow">Feed · ${escHtml(feedFilter)}</span><h1>Peach Circle</h1><p>Morning cats, collar finds, and nearby meetups from trusted local people.</p></div><img src="assets/stitch-remote/screens/5e8d58afba4142068ea51010b1e17e3a.png" alt="cat">`;
      stream.appendChild(hero);
      // Composer
      const comp = document.createElement('article');
      comp.className = 'web-composer-card';
      comp.innerHTML = `<img src="assets/stitch-remote/screens/5e8d58afba4142068ea51010b1e17e3a.png" alt="avatar"><button type="button" data-view-jump="compose">Share a cat moment or local pet update</button><span class="material-symbols-rounded">add_photo_alternate</span>`;
      stream.appendChild(comp);
      // Post cards
      items.forEach(item => stream.appendChild(buildWebPostCard(item)));
    } catch (e) { stream.innerHTML = emptyCard('error', 'Failed to load', e.message); }
  }

  function buildWebPostCard(item) {
    const post = item.post || item;
    const author = item.author || {};
    const likeCount = item.like_count ?? 0;
    let liked = item.liked ?? false;
    const fm = item.first_media;
    const card = document.createElement('article');
    card.className = 'web-post-card';
    const avatar = author.avatar_url || 'assets/stitch-remote/screens/5e8d58afba4142068ea51010b1e17e3a.png';
    const mediaHtml = fm && fm.url
      ? `<div class="web-media-grid"><img src="${escHtml(fm.url)}" alt="post media"></div>`
      : (post.media_ids?.length ? `<div class="web-media-grid"><img src="assets/stitch-remote/screens/5e8d58afba4142068ea51010b1e17e3a.png" alt=""></div>` : '');
    card.innerHTML = `
      <header>
        <img src="${escHtml(avatar)}" alt="avatar">
        <div><b>@${escHtml(author.username || 'user')}</b><span>${escHtml(author.nickname || '')} · ${timeAgo(post.created_at)}</span></div>
        <button class="ghost-button follow-btn" type="button" data-author-id="${post.author_id}">Follow</button>
      </header>
      <p><strong>${escHtml(post.title)}</strong><br>${escHtml(post.content || '')}</p>
      ${mediaHtml}
      <footer>
        <button type="button" class="like-btn" data-post-id="${post.id}"><span class="material-symbols-rounded">${liked ? 'favorite' : 'favorite_border'}</span><span class="lc">${likeCount}</span></button>
        <button type="button" class="comment-btn"><span class="material-symbols-rounded">chat_bubble</span></button>
        <button type="button"><span class="material-symbols-rounded">share</span>Share</button>
        <button type="button"><span class="material-symbols-rounded">bookmark</span>Save</button>
      </footer>
      <div class="comments-section" style="display:none;padding:16px;border-top:1px solid var(--border-color);flex-direction:column;gap:12px;margin-top:8px;">
        <div class="comments-list" style="display:flex;flex-direction:column;gap:8px;font-size:12px;"></div>
        <form class="comment-form" style="display:flex;gap:8px;margin:0;">
          <input type="text" placeholder="Write a comment..." class="comment-input" style="flex:1;padding:8px;border-radius:8px;border:1px solid var(--border-color);font-size:12px;background:var(--bg-secondary);color:var(--text-primary);" />
          <button class="primary-button" type="submit" style="padding:6px 14px;font-size:12px;border-radius:999px;">Post</button>
        </form>
      </div>`;
    // Like
    const likeBtn = card.querySelector('.like-btn');
    likeBtn?.addEventListener('click', async () => {
      if (!getToken()) { toast('Please sign in to like', 'info'); return; }
      try {
        const r = await api(`/api/v1/posts/${post.id}/like`, 'POST');
        liked = r.liked;
        likeBtn.querySelector('.material-symbols-rounded').textContent = r.liked ? 'favorite' : 'favorite_border';
        likeBtn.querySelector('.lc').textContent = r.like_count;
      } catch (e) { toast(e.message, 'error'); }
    });
    // Follow
    const followBtn = card.querySelector('.follow-btn');
    followBtn?.addEventListener('click', async (ev) => {
      ev.stopPropagation();
      if (!getToken()) { toast('Please sign in first', 'info'); return; }
      try {
        const r = await api(`/api/v1/me/follow/${post.author_id}`, 'POST');
        followBtn.textContent = r.following ? 'Following' : 'Follow';
      } catch (e) { toast(e.message, 'error'); }
    });
    // Comments toggler & list loader
    const commentBtn = card.querySelector('.comment-btn');
    const commentsSec = card.querySelector('.comments-section');
    const commentsList = card.querySelector('.comments-list');
    const commentForm = card.querySelector('.comment-form');
    async function loadPostComments() {
      commentsList.innerHTML = '<div style="color:var(--text-secondary)">Loading comments...</div>';
      try {
        const detail = await api(`/api/v1/posts/${post.id}`);
        const list = detail.comments || [];
        if (list.length === 0) {
          commentsList.innerHTML = '<div style="color:var(--text-secondary)">No comments yet.</div>';
          return;
        }
        commentsList.innerHTML = list.map(c => `
          <div style="display:flex;gap:8px;align-items:flex-start;padding:4px 0;">
            <div style="width:22px;height:22px;border-radius:50%;background:var(--primary-muted);display:flex;align-items:center;justify-content:center;font-size:10px;color:var(--primary)">👤</div>
            <div style="flex:1;">
              <div style="display:flex;gap:6px;align-items:baseline;">
                <b style="color:var(--text-primary);font-size:12px;">${escHtml(c.author?.nickname || c.author?.username || 'User')}</b>
                <span style="color:var(--text-secondary);font-size:10px;">@${escHtml(c.author?.username || '')}</span>
              </div>
              <p style="margin:2px 0 0 0;font-size:12px;color:var(--text-primary);line-height:1.4;">${escHtml(c.content)}</p>
            </div>
          </div>
        `).join('');
      } catch (e) {
        commentsList.innerHTML = `<div style="color:var(--error-color)">${escHtml(e.message)}</div>`;
      }
    }
    commentBtn?.addEventListener('click', () => {
      const isHidden = commentsSec.style.display === 'none';
      commentsSec.style.display = isHidden ? 'flex' : 'none';
      if (isHidden) loadPostComments();
    });
    commentForm?.addEventListener('submit', async (ev) => {
      ev.preventDefault();
      const input = commentForm.querySelector('.comment-input');
      const content = input.value.trim();
      if (!content) return;
      try {
        await api(`/api/v1/posts/${post.id}/comments`, 'POST', { content });
        input.value = '';
        loadPostComments();
      } catch (e) {
        toast(e.message, 'error');
      }
    });
    return card;
  }

  function bindFeedTabs() {
    document.querySelectorAll('.feed-tabs button').forEach(btn => {
      if (btn._bound) return;
      btn._bound = true;
      btn.addEventListener('click', () => {
        document.querySelectorAll('.feed-tabs button').forEach(b => b.classList.remove('is-active'));
        btn.classList.add('is-active');
        const text = btn.textContent.toLowerCase();
        feedFilter = text === 'following' ? 'follow' : text === 'nearby' ? 'new' : 'rec';
        feedPage = 1;
        loadFeed();
      });
    });
  }

  /* ───────── MARKET panel ───────── */
  async function loadMarket() {
    const grid = document.querySelector('.market-grid');
    if (!grid) return;
    grid.innerHTML = skeleton(3);
    try {
      const d = await api('/api/v1/listings');
      let items = Array.isArray(d.items) ? d.items : [];
      
      const activeBtn = document.querySelector('.market-toolbar button.is-active');
      if (activeBtn) {
        const typeChoice = activeBtn.textContent.trim().toLowerCase();
        if (typeChoice === 'sell' || typeChoice === 'trade') {
          items = items.filter(item => item.type === 'product');
        } else if (typeChoice === 'looking for') {
          items = items.filter(item => item.type === 'service');
        } else if (typeChoice === 'free / donation') {
          items = items.filter(item => item.type === 'adopt');
        }
      }

      if (items.length === 0) {
        grid.innerHTML = emptyCard('storefront', 'No listings yet', 'Publish your first item or service!');
        return;
      }
      grid.innerHTML = '';
      items.forEach(item => {
        const card = document.createElement('article');
        card.className = 'product-card';
        const type = (item.type || 'product').replace('_', ' ');
        const typeLabel = type.charAt(0).toUpperCase() + type.slice(1);
        card.innerHTML = `
          <span class="trade-chip">${escHtml(typeLabel)}</span>
          <img src="assets/stitch-remote/screens/e57b8cd42f6f45c08949322cbcc3b89f.png" alt="${escHtml(item.title)}">
          <div><b>${escHtml(item.title)}</b><strong>${formatPrice(item.price_cents, item.currency)}</strong></div>
          <footer><span>Seller #${item.seller_id}</span><button type="button" data-listing-id="${item.id}" class="buy-btn">Buy Now</button></footer>`;
        const buyBtn = card.querySelector('.buy-btn');
        buyBtn?.addEventListener('click', async () => {
          if (!getToken()) { toast('Please sign in to buy', 'info'); return; }
          try {
            await api('/api/v1/orders', 'POST', { listing_id: item.id });
            toast('Order created! Go to Orders to pay.', 'success');
          } catch (e) { toast(e.message, 'error'); }
        });
        grid.appendChild(card);
      });
    } catch (e) { grid.innerHTML = emptyCard('error', 'Failed to load', e.message); }
  }

  function bindMarketToolbar() {
    document.querySelectorAll('.market-toolbar button').forEach(btn => {
      if (btn._bound) return;
      btn._bound = true;
      btn.addEventListener('click', () => {
        document.querySelectorAll('.market-toolbar button').forEach(b => b.classList.remove('is-active'));
        btn.classList.add('is-active');
        loadMarket();
      });
    });
  }

  /* ───────── MESSAGES panel ───────── */
  async function loadConversations() {
    const list = document.querySelector('.conversation-list');
    if (!list || !getToken()) return;
    try {
      const d = await api('/api/v1/me/conversations');
      const items = Array.isArray(d.items) ? d.items : [];
      list.innerHTML = '';
      if (items.length === 0) {
        list.innerHTML = '<div style="padding:16px;color:var(--text-secondary)">No conversations yet</div>';
        return;
      }
      items.forEach((c, i) => {
        const peer = c.peer || {};
        const el = document.createElement('article');
        el.className = (activePeerId === peer.id) ? 'is-active' : (activePeerId === null && i === 0) ? 'is-active' : '';
        el.innerHTML = `<img src="${escHtml(peer.avatar_url || 'assets/stitch-remote/screens/5e8d58afba4142068ea51010b1e17e3a.png')}" alt=""><div><b>${escHtml(peer.nickname || peer.username || 'User')}</b><span>${escHtml(c.last_message || '')}</span></div>${c.unread_count > 0 ? `<i>${c.unread_count}</i>` : ''}`;
        el.addEventListener('click', () => {
          list.querySelectorAll('article').forEach(a => a.classList.remove('is-active'));
          el.classList.add('is-active');
          loadChat(peer.id, peer);
        });
        list.appendChild(el);
      });
      if (activePeerId === null && items.length > 0) {
        loadChat(items[0].peer.id, items[0].peer);
      }
    } catch (e) { toast(e.message, 'error'); }
  }

  async function loadChat(peerId, peer, isSilent = false) {
    activePeerId = peerId;
    const panel = document.querySelector('.chat-panel');
    if (!panel) return;
    
    const input = document.getElementById('msg-input');
    const currentTyped = input ? input.value : '';

    try {
      const d = await api(`/api/v1/me/conversations/${peerId}`);
      const msgs = d.messages || [];
      const peerInfo = d.peer || peer || {};
      
      if (isSilent) {
        const currentBubbles = panel.querySelectorAll('.bubble');
        if (currentBubbles.length === msgs.length) {
          return;
        }
      }

      let html = `<header><div><b>${escHtml(peerInfo.nickname || peerInfo.username || 'User')}</b><span>ID: ${peerId}</span></div><button class="ghost-button" type="button">View Details</button></header>`;
      html += `<div class="chat-bubbles-container" style="flex:1;overflow-y:auto;padding:8px 0;display:flex;flex-direction:column;gap:8px;">`;
      msgs.forEach(m => {
        const isMine = m.sender_id !== peerId;
        html += `<p class="bubble ${isMine ? 'out' : 'in'}" style="margin:0;">${escHtml(m.content)}</p>`;
      });
      html += `</div>`;
      html += `<form class="message-input"><button type="button" aria-label="Attach"><span class="material-symbols-rounded">attach_file</span></button><input placeholder="Write a message..." id="msg-input" value="${escHtml(currentTyped)}" autocomplete="off" /><button type="submit" aria-label="Send"><span class="material-symbols-rounded">send</span></button></form>`;
      panel.innerHTML = html;
      
      const container = panel.querySelector('.chat-bubbles-container');
      if (container) container.scrollTop = container.scrollHeight;

      const form = panel.querySelector('.message-input');
      form?.addEventListener('submit', async (ev) => {
        ev.preventDefault();
        const inputEl = document.getElementById('msg-input');
        const content = inputEl?.value?.trim();
        if (!content) return;
        try {
          await api('/api/v1/messages', 'POST', { recipient_id: peerId, content });
          inputEl.value = '';
          loadChat(peerId, peerInfo);
        } catch (e) { toast(e.message, 'error'); }
      });
      
      if (pollTimer) clearInterval(pollTimer);
      pollTimer = setInterval(() => {
        if (document.querySelector('.view.is-active')?.id === 'messages' && activePeerId === peerId) {
          loadChat(peerId, peer, true);
        } else {
          clearInterval(pollTimer);
        }
      }, 3000);
    } catch (e) { 
      if (!isSilent) {
        panel.innerHTML = `<div style="padding:24px;color:var(--text-secondary)">${escHtml(e.message)}</div>`; 
      }
    }
  }

  /* ───────── ORDERS panel ───────── */
  function bindOrdersRoleToggle() {
    document.querySelectorAll('.order-role-toggle .role-btn').forEach(btn => {
      if (btn._bound) return;
      btn._bound = true;
      btn.addEventListener('click', () => {
        document.querySelectorAll('.order-role-toggle .role-btn').forEach(b => {
          b.classList.remove('is-active');
          b.style.background = 'transparent';
          b.style.color = 'var(--text-secondary)';
        });
        btn.classList.add('is-active');
        btn.style.background = 'var(--primary)';
        btn.style.color = '#fff';
        
        orderRole = btn.dataset.role;
        loadOrders();
      });
    });
  }

  async function loadOrders() {
    bindOrdersRoleToggle();
    const grid = document.querySelector('.orders-grid');
    if (!grid || !getToken()) return;
    grid.innerHTML = skeleton(3);
    try {
      const d = await api(`/api/v1/me/orders?role=${orderRole}`);
      const items = Array.isArray(d.items) ? d.items : [];
      if (items.length === 0) {
        grid.innerHTML = emptyCard('orders', 'No orders yet', 'Your orders will appear here after purchasing from the market.');
        return;
      }
      grid.innerHTML = '';
      items.forEach(o => grid.appendChild(buildOrderCard(o)));
    } catch (e) { grid.innerHTML = emptyCard('error', 'Failed to load', e.message); }
  }

  function buildOrderCard(o) {
    const card = document.createElement('article');
    card.className = 'order-card';
    const statusClass = o.status === 'completed' ? 'success' : o.status === 'shipped' ? 'info' : o.status === 'pending_payment' ? 'warning' : '';
    let actions = '';
    if (o.status === 'pending_payment') {
      if (orderRole === 'buyer') {
        actions = '<button class="act-pay">Pay Now</button><button class="act-cancel">Cancel Order</button>';
      } else {
        actions = '<button class="act-cancel">Cancel Order</button>';
      }
    } else if (o.status === 'paid') {
      if (orderRole === 'seller') {
        actions = '<button class="act-ship">Ship Item</button><button class="act-refund">Refund</button>';
      } else {
        actions = '<span>Awaiting shipment</span>';
      }
    } else if (o.status === 'shipped') {
      if (orderRole === 'buyer') {
        actions = '<button class="act-complete">Confirm Receipt</button>';
      } else {
        actions = '<span>Shipped (Pending receipt)</span>';
      }
    } else {
      actions = '<button class="act-detail">View Details</button>';
    }
    card.innerHTML = `
      <span class="status ${statusClass}">${escHtml(o.status)}</span>
      <h2>${escHtml(o.listing_title || 'Order #' + o.id)}</h2>
      <p>${formatPrice(o.amount_cents, o.currency)} · ${timeAgo(o.created_at)}</p>
      <footer>${actions}</footer>`;
    const bind = (sel, action) => {
      const btn = card.querySelector(sel);
      btn?.addEventListener('click', async () => {
        try {
          btn.disabled = true;
          if (action === 'pay') await api(`/api/v1/orders/${o.id}/pay`, 'POST', { method: 'mock' });
          else if (action === 'cancel') await api(`/api/v1/orders/${o.id}/cancel`, 'POST');
          else if (action === 'ship') await api(`/api/v1/orders/${o.id}/ship`, 'POST');
          else if (action === 'complete') await api(`/api/v1/orders/${o.id}/complete`, 'POST');
          else if (action === 'refund') await api(`/api/v1/orders/${o.id}/refund`, 'POST');
          toast(`Order #${o.id} ${action} done`, 'success');
          loadOrders();
        } catch (e) { toast(e.message, 'error'); btn.disabled = false; }
      });
    };
    bind('.act-pay', 'pay');
    bind('.act-cancel', 'cancel');
    bind('.act-ship', 'ship');
    bind('.act-complete', 'complete');
    bind('.act-refund', 'refund');
    return card;
  }

  /* ───────── PROFILE panel ───────── */
  async function loadProfile() {
    const card = document.querySelector('.profile-card');
    if (!card || !getToken()) return;

    const startEditBtn = document.getElementById('profile-start-edit-btn');
    const editSec = document.querySelector('.profile-edit-section');
    const nicknameInput = document.getElementById('profile-edit-nickname');
    const bioTextarea = document.getElementById('profile-edit-bio');
    const saveBtn = document.getElementById('profile-save-btn');
    const cancelBtn = document.getElementById('profile-cancel-btn');

    if (startEditBtn) startEditBtn.style.display = 'block';

    try {
      const [me, postsData] = await Promise.all([
        api('/api/v1/auth/me'),
        api('/api/v1/me/posts'),
      ]);
      
      if (nicknameInput) nicknameInput.value = me.nickname || me.username || '';
      if (bioTextarea) bioTextarea.value = me.bio || '';

      const postCount = Array.isArray(postsData.items) ? postsData.items.length : 0;
      card.querySelector('h1').textContent = me.nickname || me.username;
      card.querySelector('p').textContent = `@${me.username} · ${me.bio || 'A cat lover on M&D'}`;
      const stats = card.querySelectorAll('.profile-stats b');
      if (stats[0]) stats[0].innerHTML = `${postCount}<span>Posts</span>`;
      if (stats[1]) stats[1].innerHTML = `—<span>Followers</span>`;
      if (stats[2]) stats[2].innerHTML = `—<span>Pets</span>`;
      // Try stats API
      try {
        const s = await api(`/api/v1/users/${me.id}/stats`);
        if (stats[1] && s.followers !== undefined) stats[1].innerHTML = `${s.followers}<span>Followers</span>`;
        if (stats[2] && s.following !== undefined) stats[2].innerHTML = `${s.following}<span>Following</span>`;
      } catch (_) { /* stats endpoint may not exist yet */ }
      const avatar = card.querySelector('.profile-cover img');
      if (avatar && me.avatar_url) avatar.src = me.avatar_url;

      // Bind edit handlers
      if (startEditBtn && !startEditBtn._bound) {
        startEditBtn._bound = true;
        startEditBtn.addEventListener('click', () => {
          editSec.style.display = 'flex';
          startEditBtn.style.display = 'none';
        });
      }
      if (cancelBtn && !cancelBtn._bound) {
        cancelBtn._bound = true;
        cancelBtn.addEventListener('click', () => {
          editSec.style.display = 'none';
          startEditBtn.style.display = 'block';
          if (nicknameInput) nicknameInput.value = me.nickname || me.username || '';
          if (bioTextarea) bioTextarea.value = me.bio || '';
        });
      }
      if (saveBtn && !saveBtn._bound) {
        saveBtn._bound = true;
        saveBtn.addEventListener('click', async () => {
          const nickname = nicknameInput?.value?.trim();
          const bio = bioTextarea?.value?.trim();
          if (!nickname) { toast('Nickname is required', 'error'); return; }
          try {
            saveBtn.disabled = true;
            await api('/api/v1/me', 'PATCH', { nickname, bio, avatar_url: me.avatar_url || '' });
            toast('Profile saved!', 'success');
            editSec.style.display = 'none';
            startEditBtn.style.display = 'block';
            loadProfile();
          } catch (e) { toast(e.message, 'error'); }
          finally { saveBtn.disabled = false; }
        });
      }
    } catch (e) { toast(e.message, 'error'); }
  }

  /* ───────── COMPOSE panel ───────── */
  function bindComposeForms() {
    // Post composer
    const postBtn = document.querySelector('.form-card:not(.mint-card) .primary-button');
    if (postBtn && !postBtn._bound) {
      postBtn._bound = true;
      postBtn.addEventListener('click', async () => {
        if (!getToken()) { toast('Please sign in first', 'info'); return; }
        const form = postBtn.closest('form');
        const category = form.querySelector('select')?.value?.toLowerCase().replace(/\s+/g, '_') || 'daily_share';
        const textarea = form.querySelector('textarea');
        const content = textarea?.value?.trim();
        if (!content) { toast('Please write something', 'error'); return; }
        try {
          postBtn.disabled = true;
          await api('/api/v1/posts', 'POST', { title: content.slice(0, 60), content, category });
          toast('Post published!', 'success');
          textarea.value = '';
          loadFeed();
          document.querySelector('[data-view="feed"]')?.click();
        } catch (e) { toast(e.message, 'error'); }
        finally { postBtn.disabled = false; }
      });
    }
    // Listing publisher
    const listBtn = document.querySelector('.mint-card .primary-button');
    if (listBtn && !listBtn._bound) {
      listBtn._bound = true;
      listBtn.addEventListener('click', async () => {
        if (!getToken()) { toast('Please sign in first', 'info'); return; }
        const form = listBtn.closest('form');
        const typeSelect = form.querySelector('select');
        const priceInput = form.querySelector('input');
        const tradeType = typeSelect?.value?.toLowerCase().replace(/\s+\/?\s*/g, '_') || 'product';
        const typeMap = { sell: 'product', trade: 'product', 'looking_for': 'service', 'free___donation': 'adopt', 'free_/_donation': 'adopt' };
        const type = typeMap[tradeType] || 'product';
        const priceStr = priceInput?.value?.replace(/[^0-9.]/g, '') || '0';
        const priceCents = Math.round(parseFloat(priceStr) * 100);
        try {
          listBtn.disabled = true;
          await api('/api/v1/listings', 'POST', {
            type, title: `${typeSelect?.value || 'Product'} listing`, description: 'Published from M&D web client',
            price_cents: priceCents, currency: 'USD'
          });
          toast('Listing published!', 'success');
          loadMarket();
          document.querySelector('[data-view="market"]')?.click();
        } catch (e) { toast(e.message, 'error'); }
        finally { listBtn.disabled = false; }
      });
    }
  }

  /* ───────── SAFETY / NEUTRAL panel ───────── */
  async function loadSafetyReview() {
    const tbody = document.querySelector('.admin-table tbody');
    if (!tbody) return;
    const adminKey = localStorage.getItem(ADMIN_KEY_STORAGE) || 'admin123';
    try {
      const d = await api('/api/v1/admin/audit-logs', 'GET', undefined, { adminKey });
      const items = Array.isArray(d.items) ? d.items : [];
      if (items.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;color:var(--text-secondary)">No audit logs</td></tr>';
        return;
      }
      tbody.innerHTML = '';
      items.slice(0, 20).forEach(log => {
        const tr = document.createElement('tr');
        tr.innerHTML = `<td>#${log.id}</td><td>${escHtml(log.action)}</td><td><span class="status success">OK</span></td><td>${escHtml(log.actor || '—')}</td>`;
        tbody.appendChild(tr);
      });
    } catch (_) { /* silent — admin key may be wrong */ }
  }

  /* ───────── SEARCH ───────── */
  function bindSearch() {
    const input = document.querySelector('.search input');
    if (!input || input._bound) return;
    input._bound = true;
    let timer = null;
    input.addEventListener('input', () => {
      clearTimeout(timer);
      const q = input.value.trim();
      if (!q) return;
      timer = setTimeout(async () => {
        try {
          const d = await api(`/api/v1/search?q=${encodeURIComponent(q)}&type=all`);
          const posts = d.posts || [];
          const listings = d.listings || [];
          const total = posts.length + listings.length;
          toast(`Found ${total} result${total !== 1 ? 's' : ''} for "${q}"`, 'info');
        } catch (_) {}
      }, 500);
    });
  }

  /* ───────── VIEW CHANGE HOOKS ───────── */
  // Observe view changes and load data for the active panel
  const observer = new MutationObserver(() => {
    const active = document.querySelector('.view.is-active');
    if (!active) return;
    const panel = active.dataset.viewPanel;
    if (panel === 'feed') { loadFeed(); bindFeedTabs(); }
    else if (panel === 'market') { loadMarket(); bindMarketToolbar(); }
    else if (panel === 'messages') loadConversations();
    else if (panel === 'orders') loadOrders();
    else if (panel === 'profile') loadProfile();
    else if (panel === 'compose') bindComposeForms();
    else if (panel === 'safety') loadSafetyReview();
    else if (panel === 'auth') bindAuthPanel();
  });

  // Observe all view panels for class changes
  document.querySelectorAll('.view').forEach(v => {
    observer.observe(v, { attributes: true, attributeFilter: ['class'] });
  });

  /* ───────── NOTIFICATIONS POLL ───────── */
  function startNotifPoll() {
    if (pollTimer) clearInterval(pollTimer);
    pollTimer = setInterval(() => {
      const badge = document.querySelector('.nav-item[data-view="messages"] i');
      refreshUnreadCount(badge);
    }, 30000);
  }

  /* ───────── INIT ───────── */
  function init() {
    // Restore profile bg choice
    const cachedBg = localStorage.getItem('meow_profile_bg') || 'picnic';
    document.documentElement.setAttribute('data-profile-bg', cachedBg);
    document.querySelectorAll('[data-bg-choice]').forEach(btn => {
      btn.classList.toggle('is-active', btn.dataset.bgChoice === cachedBg);
      if (!btn._bgBound) {
        btn._bgBound = true;
        btn.addEventListener('click', () => {
          const bg = btn.dataset.bgChoice;
          localStorage.setItem('meow_profile_bg', bg);
        });
      }
    });

    const raw = localStorage.getItem(USER_KEY);
    if (raw) { try { currentUser = JSON.parse(raw); } catch (_) {} }
    updateAuthUI();
    bindSearch();
    bindAuthPanel();
    bindComposeForms();
    startNotifPoll();
    setTimeout(() => { loadFeed(); bindFeedTabs(); }, 100);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
