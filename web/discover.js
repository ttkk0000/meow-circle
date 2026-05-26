(() => {
  const t = (key) => (window.MeowShared ? window.MeowShared.t(key) : key);
  const JOIN_KEY = "meow_joined_circles_v1";

  const categories = new Map([
    ["stitch.chip_all", "all"],
    ["stitch.chip_newbie", "newbie"],
    ["stitch.chip_breed", "breed"],
    ["stitch.chip_gear", "gear"],
    ["stitch.chip_health", "health"],
  ]);

  const circles = [
    {
      id: "orange",
      category: "breed",
      titleKey: "stitch.circle_orange",
      descKey: "stitch.circle_orange_desc",
      members: "12.5w",
      mark: "橘",
      badge: "Cat club",
      image: "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&w=900&q=80",
      href: "/?q=%E6%A9%98%E7%8C%AB#home",
    },
    {
      id: "newbie",
      category: "newbie",
      titleKey: "stitch.circle_newbie",
      descKey: "stitch.circle_newbie_desc",
      members: "8.2w",
      mark: "新",
      badge: "Starter",
      image: "https://images.unsplash.com/photo-1592194996308-7b43878e84a6?auto=format&fit=crop&w=900&q=80",
      href: "/?q=%E6%96%B0%E6%89%8B#home",
    },
    {
      id: "black",
      category: "breed",
      titleKey: "stitch.circle_black",
      descKey: "stitch.circle_black_desc",
      members: "5.6w",
      mark: "黑",
      badge: "Mystery",
      image: "https://images.unsplash.com/photo-1518791841217-8f162f1e1131?auto=format&fit=crop&w=900&q=80",
      href: "/?q=%E9%BB%91%E7%8C%AB#home",
    },
    {
      id: "market",
      category: "gear",
      titleKey: "stitch.circle_market",
      descKey: "stitch.circle_market_desc",
      members: "8.9k",
      mark: "市",
      badge: "Market",
      image: "https://images.unsplash.com/photo-1545249390-6bdfa286032f?auto=format&fit=crop&w=900&q=80",
      href: "/market.html",
    },
    {
      id: "health",
      category: "health",
      titleKey: "stitch.chip_health",
      desc: "猫咪医疗、驱虫、绝育和急救经验集中讨论。",
      members: "1.2w",
      mark: "医",
      badge: "Care",
      image: "https://images.unsplash.com/photo-1583337130417-3346a1be7dee?auto=format&fit=crop&w=900&q=80",
      href: "/?q=%E5%8C%BB%E7%96%97#home",
    },
  ];

  const searchInput = document.querySelector('input[type="search"]');
  const chipRow = document.querySelector(".flex.flex-wrap.gap-3.mb-8");
  const grid = document.querySelector("main .grid.grid-cols-1");
  const empty = document.querySelector("#discover-empty");
  if (!chipRow || !grid) return;

  let currentCategory = "all";
  let query = "";

  function escapeHtml(input) {
    return String(input)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
  }

  function readJoined() {
    try {
      const raw = JSON.parse(localStorage.getItem(JOIN_KEY) || "[]");
      return new Set(Array.isArray(raw) ? raw : []);
    } catch (_) {
      return new Set();
    }
  }

  function writeJoined(joined) {
    localStorage.setItem(JOIN_KEY, JSON.stringify(Array.from(joined)));
  }

  function circleDescription(circle) {
    return circle.desc || t(circle.descKey);
  }

  function renderChips() {
    chipRow.querySelectorAll("span").forEach((chip) => {
      const key = chip.getAttribute("data-i18n") || "";
      const category = categories.get(key) || "all";
      const on = category === currentCategory;
      chip.classList.toggle("bg-primary-container", on);
      chip.classList.toggle("text-white", on);
      chip.classList.toggle("bg-surface-container", !on);
      chip.classList.toggle("text-on-surface-variant", !on);
      chip.setAttribute("role", "button");
      chip.setAttribute("tabindex", "0");
    });
  }

  function render() {
    renderChips();
    const joined = readJoined();
    const q = query.trim().toLowerCase();
    const visible = circles.filter((circle) => {
      const categoryMatch = currentCategory === "all" || circle.category === currentCategory;
      const text = `${t(circle.titleKey)} ${circleDescription(circle)} ${circle.members}`.toLowerCase();
      return categoryMatch && (!q || text.includes(q));
    });

    grid.innerHTML = "";
    if (empty) empty.classList.toggle("hidden", visible.length !== 0);

    const frag = document.createDocumentFragment();
    for (const circle of visible) {
      const isJoined = joined.has(circle.id);
      const card = document.createElement("article");
      card.className =
        "group bg-surface-container-lowest rounded-2xl shadow-[0_8px_30px_rgba(0,0,0,0.03)] overflow-hidden border border-surface-container-low hover:-translate-y-1 transition-all flex flex-col";
      card.innerHTML = `
        <div class="relative h-40 w-full overflow-hidden bg-primary-container/10">
          <img class="h-full w-full object-cover transition-transform duration-500 group-hover:scale-[1.04]" alt="" src="${escapeHtml(circle.image)}" />
          <div class="absolute left-3 top-3 rounded-full bg-white/90 px-3 py-1 text-[12px] font-bold text-primary-container shadow-sm">${escapeHtml(circle.badge)}</div>
        </div>
        <div class="p-5 flex flex-col gap-2 flex-1">
          <div class="flex items-center gap-3">
            <div class="h-11 w-11 rounded-full bg-primary-container text-white flex items-center justify-center text-lg font-bold shadow-sm">${escapeHtml(circle.mark)}</div>
            <h3 class="font-headline-lg text-on-surface">${escapeHtml(t(circle.titleKey))}</h3>
          </div>
          <p class="text-body-md text-on-surface-variant flex items-center gap-1"><span class="material-symbols-outlined text-[16px]">group</span> ${escapeHtml(circle.members)}</p>
          <p class="text-body-md text-on-surface-variant line-clamp-2">${escapeHtml(circleDescription(circle))}</p>
          <div class="mt-3 flex flex-wrap gap-2">
            <a href="${escapeHtml(circle.href)}" class="rounded-full bg-surface-container text-on-surface px-4 py-2 text-label-md">${escapeHtml(t("stitch.view_all"))}</a>
            <button type="button" class="join-btn rounded-full ${isJoined ? "bg-surface-container text-on-surface-variant" : "bg-primary-container text-white"} px-4 py-2 text-label-md" data-circle-id="${escapeHtml(circle.id)}">${escapeHtml(t(isJoined ? "stitch.joined" : "stitch.join"))}</button>
          </div>
        </div>`;
      card.querySelector(".join-btn")?.addEventListener("click", () => {
        const next = readJoined();
        if (next.has(circle.id)) next.delete(circle.id);
        else next.add(circle.id);
        writeJoined(next);
        render();
      });
      frag.appendChild(card);
    }
    grid.appendChild(frag);
  }

  chipRow.querySelectorAll("span").forEach((chip) => {
    const activate = () => {
      const key = chip.getAttribute("data-i18n") || "";
      currentCategory = categories.get(key) || "all";
      render();
    };
    chip.addEventListener("click", activate);
    chip.addEventListener("keydown", (ev) => {
      if (ev.key === "Enter" || ev.key === " ") {
        ev.preventDefault();
        activate();
      }
    });
  });

  searchInput?.addEventListener("input", () => {
    query = searchInput.value.trim().toLowerCase();
    render();
  });

  render();
})();
