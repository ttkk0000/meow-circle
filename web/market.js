const TOKEN_KEY = "meow_token";
const t = (k) => window.MeowShared.t(k);
const listingsContainer = document.querySelector("#listings");
const marketSearch = document.querySelector("#market-search");
let activeType = "all";
let listings = [];

const FALLBACK_LISTINGS = [
  {
    id: 9101,
    type: "product",
    title: "猫猫彩色牵引绳套装",
    description: "轻量织带，适合日常散步，附送 M&D 小挂件。doggie 也可用。",
    price_cents: 6900,
    currency: "CNY",
    seller_id: "泡芙小店",
    image: "https://images.unsplash.com/photo-1583511655826-05700d52f4d9?auto=format&fit=crop&w=900&q=80",
  },
  {
    id: 9102,
    type: "service",
    title: "上门喂猫 30 分钟",
    description: "换水、铲砂、拍照回传，节假日可约。",
    price_cents: 4500,
    currency: "CNY",
    seller_id: "毛球管家",
    image: "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&w=900&q=80",
  },
  {
    id: 9103,
    type: "adopt",
    title: "三个月橘猫找家",
    description: "已驱虫，亲人活泼，需要稳定家庭。",
    price_cents: 0,
    currency: "CNY",
    seller_id: "城南救助",
    image: "https://images.unsplash.com/photo-1543852786-1cf6624b9987?auto=format&fit=crop&w=900&q=80",
  },
];

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

function typeLabel(type) {
  const labels = { product: "商品", service: "服务", adopt: "领养" };
  return labels[type] || type || "好物";
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
    <div class="h-48 bg-surface-container-low overflow-hidden">
      ${
        listing.image
          ? `<img class="h-full w-full object-cover" src="${escapeHtml(listing.image)}" alt="${escapeHtml(listing.title)}" />`
          : `<div class="flex h-full items-center justify-center"><span class="material-symbols-outlined text-6xl text-primary-container/30">shopping_bag</span></div>`
      }
    </div>
    <div class="p-5 flex flex-col gap-2 flex-1">
      <div class="flex justify-between gap-2 items-start">
        <h2 class="font-headline-lg text-on-surface line-clamp-2">${escapeHtml(listing.title)}</h2>
        <span class="shrink-0 px-2 py-0.5 rounded-full bg-secondary-container/20 text-secondary text-label-md font-label-md">${escapeHtml(typeLabel(listing.type))}</span>
      </div>
      <p class="text-body-md text-on-surface-variant line-clamp-3">${escapeHtml(listing.description || "")}</p>
      <p class="text-label-md text-gray-500 mt-auto">${escapeHtml(t("meta.seller"))} ${escapeHtml(listing.seller_id || "M&D 卖家")}</p>
      <p class="text-headline-lg font-bold text-primary-container">${escapeHtml(price)}</p>
      <div class="flex gap-2 pt-2">
        <button type="button" class="btn-buy flex-1 py-3 rounded-full bg-gradient-to-r from-primary-container to-[#ff7e95] text-white font-label-md hover:scale-[0.98] transition-transform">${escapeHtml(t("listing.btn_buy"))}</button>
      </div>
    </div>`;
  el.querySelector(".btn-buy").addEventListener("click", () => buyListing(listing));
  return el;
}

function filteredListings() {
  const query = (marketSearch?.value || "").trim().toLowerCase();
  return listings.filter((listing) => {
    const typeOk = activeType === "all" || listing.type === activeType;
    const text = `${listing.title} ${listing.description} ${listing.seller_id || ""}`.toLowerCase();
    return typeOk && (!query || text.includes(query));
  });
}

function renderListings() {
  const items = filteredListings();
  listingsContainer.innerHTML = "";
  if (!items.length) {
    listingsContainer.innerHTML = `<article class="col-span-full rounded-2xl border border-outline-variant/20 bg-white p-8 text-center text-gray-500">没有找到匹配的 M&D 好物。</article>`;
    return;
  }
  for (const item of items) listingsContainer.appendChild(card(item));
}

async function load() {
  listingsContainer.innerHTML =
    '<div class="col-span-full flex justify-center py-12 text-primary-container"><span class="material-symbols-outlined animate-spin">sync</span></div>';
  if (!new URLSearchParams(location.search).has("live")) {
    listings = FALLBACK_LISTINGS;
    renderListings();
    return;
  }
  try {
    const data = await apiCall("/api/v1/listings?page=1&page_size=30", "GET");
    const items = data.items || [];
    listings = items.length ? items : FALLBACK_LISTINGS;
    renderListings();
    const hash = location.hash.replace(/^#l/, "");
    if (hash) {
      const target = document.getElementById(`l${hash}`);
      if (target) target.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  } catch (e) {
    listings = FALLBACK_LISTINGS;
    renderListings();
  }
}

document.querySelectorAll("[data-market-type]").forEach((button) => {
  button.addEventListener("click", () => {
    activeType = button.dataset.marketType || "all";
    document.querySelectorAll("[data-market-type]").forEach((item) => {
      const active = item === button;
      item.classList.toggle("bg-primary-container", active);
      item.classList.toggle("text-white", active);
      item.classList.toggle("bg-surface-container", !active);
      item.classList.toggle("text-on-surface-variant", !active);
    });
    renderListings();
  });
});

marketSearch?.addEventListener("input", renderListings);

load();
