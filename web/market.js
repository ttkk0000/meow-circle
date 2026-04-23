const TOKEN_KEY = "meow_token";
const t = (k) => window.MeowShared.t(k);
const listingsContainer = document.querySelector("#listings");

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

async function buyListing(listing) {
  if (!getToken()) {
    notify(t("alert.login_first"), "error");
    location.href = `/login?return_to=${encodeURIComponent(location.pathname)}`;
    return;
  }
  try {
    const order = await apiCall("/api/v1/orders", "POST", { listing_id: listing.id });
    notify(`${t("listing.btn_buy")} #${order.id}`, "success");
  } catch (e) {
    notify(e.message, "error");
  }
}

function card(listing) {
  const el = document.createElement("article");
  el.className =
    "bg-white rounded-[24px] overflow-hidden shadow-[0_8px_30px_rgba(0,0,0,0.04)] border border-surface-container-low hover:-translate-y-1 hover:shadow-[0_12px_40px_rgba(255,90,119,0.08)] transition-all flex flex-col";
  el.id = `l${listing.id}`;
  const price =
    listing.price_cents > 0
      ? `${(listing.price_cents / 100).toFixed(2)} ${listing.currency || "CNY"}`
      : t("stitch.price_negotiate");
  el.innerHTML = `
    <div class="h-40 bg-surface-container-low flex items-center justify-center p-4">
      <span class="material-symbols-outlined text-6xl text-primary-container/30">shopping_bag</span>
    </div>
    <div class="p-5 flex flex-col gap-2 flex-1">
      <div class="flex justify-between gap-2 items-start">
        <h2 class="font-headline-lg text-on-surface line-clamp-2">${escapeHtml(listing.title)}</h2>
        <span class="shrink-0 px-2 py-0.5 rounded-full bg-secondary-container/20 text-secondary text-label-md font-label-md">${escapeHtml(listing.type || "")}</span>
      </div>
      <p class="text-body-md text-on-surface-variant line-clamp-3">${escapeHtml(listing.description || "")}</p>
      <p class="text-label-md text-gray-500 mt-auto">${escapeHtml(t("meta.seller"))} ${listing.seller_id}</p>
      <p class="text-headline-lg font-bold text-primary-container">${escapeHtml(price)}</p>
      <div class="flex gap-2 pt-2">
        <button type="button" class="btn-buy flex-1 py-3 rounded-full bg-gradient-to-r from-primary-container to-[#ff7e95] text-white font-label-md hover:scale-[0.98] transition-transform">${escapeHtml(t("listing.btn_buy"))}</button>
      </div>
    </div>`;
  el.querySelector(".btn-buy").addEventListener("click", () => buyListing(listing));
  return el;
}

async function load() {
  listingsContainer.innerHTML =
    '<div class="col-span-full flex justify-center py-12 text-primary-container"><span class="material-symbols-outlined animate-spin">sync</span></div>';
  try {
    const data = await apiCall("/api/v1/listings?page=1&page_size=30", "GET");
    const items = data.items || [];
    listingsContainer.innerHTML = "";
    if (!items.length) {
      listingsContainer.innerHTML = `<p class="text-gray-500">${escapeHtml(t("common.empty_listings"))}</p>`;
      return;
    }
    for (const it of items) listingsContainer.appendChild(card(it));
    const hash = location.hash.replace(/^#l/, "");
    if (hash) {
      const target = document.getElementById(`l${hash}`);
      if (target) target.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  } catch (e) {
    listingsContainer.innerHTML = `<p class="text-red-600">${escapeHtml(e.message)}</p>`;
  }
}

load();
