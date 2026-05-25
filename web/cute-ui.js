(function () {
  const root = document.documentElement;
  const THEME_KEY = "meow_cute_theme";
  const PROFILE_BG_KEY = "meow_cute_profile_bg";

  const imagePool = [
    "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&w=900&q=80",
    "https://images.unsplash.com/photo-1543852786-1cf6624b9987?auto=format&fit=crop&w=900&q=80",
    "https://images.unsplash.com/photo-1518791841217-8f162f1e1131?auto=format&fit=crop&w=900&q=80",
    "https://images.unsplash.com/photo-1583511655826-05700d52f4d9?auto=format&fit=crop&w=900&q=80",
  ];

  const profileBgUrls = [
    "https://images.unsplash.com/photo-1518791841217-8f162f1e1131?auto=format&fit=crop&w=1400&q=80",
    "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&w=1400&q=80",
    "https://images.unsplash.com/photo-1543852786-1cf6624b9987?auto=format&fit=crop&w=1400&q=80",
    "https://images.unsplash.com/photo-1583511655826-05700d52f4d9?auto=format&fit=crop&w=1400&q=80",
  ];

  const fallbackPosts = [
    {
      title: "猫猫第一次学会开门，家里从此没有秘密",
      content: "给门把手加了保护套，顺便记录一下这个聪明小脑袋。",
      category: "daily_share",
      tags: ["日常", "聪明猫"],
      author: "桃子和拿铁",
      likes: 128,
      comments: 24,
      image: imagePool[0],
    },
    {
      title: "新手求助：幼猫晚上一直叫怎么办？",
      content: "刚到家第三天，白天睡很多，晚上精神特别好。",
      category: "help",
      tags: ["求助", "幼猫"],
      author: "小满",
      likes: 52,
      comments: 31,
      image: imagePool[1],
    },
    {
      title: "周末公园宠物野餐，有没有一起的？",
      content: "准备了饮水点和小零食，欢迎同城喵友来玩。",
      category: "activity",
      tags: ["同城", "活动"],
      author: "泡芙小队",
      likes: 87,
      comments: 16,
      image: imagePool[2],
    },
    {
      title: "闲置猫爬架出一个，适合小户型",
      content: "九成新，拆洗过，附近可自提。",
      category: "trade",
      tags: ["交易", "自提"],
      author: "奶茶",
      likes: 43,
      comments: 9,
      image: imagePool[3],
    },
  ];

  const fallbackListings = [
    {
      type: "product",
      title: "彩色牵引绳套装",
      description: "轻量织带，适合日常散步，附送小挂件。",
      price_cents: 6900,
      currency: "CNY",
      seller: "泡芙小店",
      image: imagePool[3],
    },
    {
      type: "service",
      title: "上门喂猫 30 分钟",
      description: "换水、铲砂、拍照回传，节假日可约。",
      price_cents: 4500,
      currency: "CNY",
      seller: "毛球管家",
      image: imagePool[0],
    },
    {
      type: "adopt",
      title: "三个月橘猫找家",
      description: "已驱虫，亲人活泼，需要稳定家庭。",
      price_cents: 0,
      currency: "CNY",
      seller: "城南救助",
      image: imagePool[1],
    },
  ];

  const topics = [
    { icon: "local_florist", title: "新手养猫", desc: "入门清单、夜叫、猫砂、疫苗。" },
    { icon: "restaurant", title: "科学喂养", desc: "粮食、饮水、体重与挑食问题。" },
    { icon: "volunteer_activism", title: "领养互助", desc: "领养故事、审核流程与回访。" },
    { icon: "festival", title: "同城活动", desc: "野餐、公益日、宠物友好地点。" },
  ];

  const conversations = [
    { name: "泡芙小店", text: "地址发你啦，晚上见。", unread: 2, image: imagePool[3] },
    { name: "城南救助", text: "领养申请表已经收到。", unread: 1, image: imagePool[1] },
    { name: "小满", text: "谢谢！今晚我试一下。", unread: 0, image: imagePool[2] },
  ];

  const safetyItems = [
    { title: "媒体审核", desc: "2 个视频等待人工确认。", status: "pending", label: "待审核" },
    { title: "举报处理", desc: "交易信息被举报描述不完整。", status: "open", label: "开放中" },
    { title: "审计日志", desc: "管理员删除评论操作已记录。", status: "done", label: "已记录" },
    { title: "订单风险", desc: "1 笔订单等待卖家发货超过 48 小时。", status: "pending", label: "关注" },
  ];

  let allPosts = fallbackPosts.slice();
  let allListings = fallbackListings.slice();
  let activeFeedFilter = "all";

  function escapeHtml(value) {
    return String(value ?? "")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
  }

  function categoryLabel(category) {
    const labels = {
      daily_share: "日常",
      help: "求助",
      activity: "活动",
      trade: "交易",
    };
    return labels[category] || category || "动态";
  }

  function listingLabel(type) {
    const labels = {
      product: "商品",
      service: "服务",
      adopt: "领养",
    };
    return labels[type] || type || "好物";
  }

  function formatPrice(cents, currency) {
    if (!cents) return "免费";
    const amount = (Number(cents) / 100).toFixed(2);
    if ((currency || "CNY").toUpperCase() === "CNY") return `¥${amount}`;
    return `${amount} ${currency || ""}`.trim();
  }

  function setTheme(theme) {
    root.setAttribute("data-theme", theme);
    localStorage.setItem(THEME_KEY, theme);
    document.querySelectorAll("[data-theme-choice]").forEach((button) => {
      button.classList.toggle("is-active", button.dataset.themeChoice === theme);
    });
  }

  function setProfileBg(bg) {
    root.setAttribute("data-profile-bg", bg);
    localStorage.setItem(PROFILE_BG_KEY, bg);
    document.querySelectorAll("[data-bg-choice]").forEach((button) => {
      button.classList.toggle("is-active", button.dataset.bgChoice === bg);
    });
  }

  function setView(view) {
    document.querySelectorAll("[data-view-panel]").forEach((panel) => {
      panel.classList.toggle("is-active", panel.dataset.viewPanel === view);
    });
    document.querySelectorAll("[data-view]").forEach((button) => {
      button.classList.toggle("is-active", button.dataset.view === view);
    });
    if (window.innerWidth < 820) window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function postMatches(post, query) {
    const text = `${post.title} ${post.content} ${(post.tags || []).join(" ")} ${post.author || ""}`.toLowerCase();
    return text.includes(query);
  }

  function renderFeed() {
    const mount = document.getElementById("cute-feed");
    const query = (document.getElementById("cute-search")?.value || "").trim().toLowerCase();
    const filtered = allPosts.filter((post) => {
      const categoryOk = activeFeedFilter === "all" || post.category === activeFeedFilter;
      const queryOk = !query || postMatches(post, query);
      return categoryOk && queryOk;
    });

    if (!filtered.length) {
      mount.innerHTML = `
        <article class="feed-card">
          <div class="feed-body">
            <span class="eyebrow">Empty</span>
            <h3>没有找到匹配内容</h3>
            <p>换个关键词或筛选试试看。</p>
          </div>
        </article>
      `;
      return;
    }

    mount.innerHTML = filtered
      .map((post, index) => {
        const tags = (post.tags || []).slice(0, 3).map((tag) => `<span class="tag">${escapeHtml(tag)}</span>`).join("");
        return `
          <article class="feed-card">
            <img src="${escapeHtml(post.image || imagePool[index % imagePool.length])}" alt="${escapeHtml(post.title)}" />
            <div class="feed-body">
              <div class="meta-row"><span class="tag">${escapeHtml(categoryLabel(post.category))}</span>${tags}</div>
              <h3>${escapeHtml(post.title)}</h3>
              <p>${escapeHtml(post.content)}</p>
              <div class="meta-row">
                <span>@${escapeHtml(post.author || "meow")}</span>
                <span>${Number(post.likes || 0)} 喜欢</span>
                <span>${Number(post.comments || 0)} 评论</span>
              </div>
              <div class="card-actions">
                <button class="tiny-action" type="button" data-view-jump="post-detail"><span class="material-symbols-rounded">article</span>详情</button>
                <button class="tiny-action" type="button"><span class="material-symbols-rounded">favorite</span>喜欢</button>
                <button class="tiny-action" type="button" data-view-jump="messages"><span class="material-symbols-rounded">chat</span>私信</button>
              </div>
            </div>
          </article>
        `;
      })
      .join("");
  }

  function renderMarket() {
    const mount = document.getElementById("cute-market");
    mount.innerHTML = allListings
      .map((listing, index) => `
        <article class="market-card">
          <img src="${escapeHtml(listing.image || imagePool[index % imagePool.length])}" alt="${escapeHtml(listing.title)}" />
          <div class="market-body">
            <span class="tag">${escapeHtml(listingLabel(listing.type))}</span>
            <h3>${escapeHtml(listing.title)}</h3>
            <p>${escapeHtml(listing.description)}</p>
            <div class="price-line">
              <strong>${escapeHtml(formatPrice(listing.price_cents, listing.currency))}</strong>
              <span class="trust-pill">${escapeHtml(listing.seller || "可信卖家")}</span>
            </div>
            <div class="card-actions">
              <button class="tiny-action" type="button" data-view-jump="listing-detail"><span class="material-symbols-rounded">open_in_new</span>详情</button>
              <button class="tiny-action" type="button" data-view-jump="messages"><span class="material-symbols-rounded">forum</span>私信</button>
              <button class="tiny-action" type="button"><span class="material-symbols-rounded">shopping_bag</span>下单</button>
            </div>
          </div>
        </article>
      `)
      .join("");
  }

  function renderTopics() {
    document.getElementById("cute-topics").innerHTML = topics
      .map((topic) => `
        <article class="topic-card">
          <span class="material-symbols-rounded">${escapeHtml(topic.icon)}</span>
          <h3>${escapeHtml(topic.title)}</h3>
          <p>${escapeHtml(topic.desc)}</p>
        </article>
      `)
      .join("");
  }

  function renderConversations() {
    document.getElementById("cute-conversations").innerHTML = conversations
      .map((item, index) => `
        <button class="conversation-card ${index === 0 ? "is-active" : ""}" type="button">
          <img class="mini-avatar" src="${escapeHtml(item.image)}" alt="" />
          <span>
            <strong>${escapeHtml(item.name)}</strong>
            <small>${escapeHtml(item.text)}</small>
          </span>
          ${item.unread ? `<b class="unread-dot">${item.unread}</b>` : ""}
        </button>
      `)
      .join("");
  }

  function renderSafety() {
    document.getElementById("cute-safety").innerHTML = safetyItems
      .map((item) => `
        <article class="safety-row">
          <div>
            <h3>${escapeHtml(item.title)}</h3>
            <p>${escapeHtml(item.desc)}</p>
          </div>
          <span class="status ${escapeHtml(item.status)}">${escapeHtml(item.label)}</span>
          <button class="secondary-action" type="button">查看</button>
        </article>
      `)
      .join("");
  }

  async function hydrateFromApi() {
    try {
      const response = await fetch("/api/v1/posts?page_size=9");
      if (response.ok) {
        const payload = await response.json();
        const items = payload?.data?.items || [];
        if (items.length) {
          allPosts = items.map((item, index) => {
            const post = item.post || item;
            return {
              title: post.title,
              content: post.content,
              category: post.category,
              tags: post.tags || [],
              author: item.author?.nickname || item.author?.username || `user${post.author_id || ""}`,
              likes: item.like_count || 0,
              comments: item.comment_count || 0,
              image: imagePool[index % imagePool.length],
            };
          });
        }
      }
    } catch (_) {
      allPosts = fallbackPosts.slice();
    }

    try {
      const response = await fetch("/api/v1/listings?page_size=9");
      if (response.ok) {
        const payload = await response.json();
        const items = payload?.data?.items || [];
        if (items.length) {
          allListings = items.map((listing, index) => ({
            type: listing.type,
            title: listing.title,
            description: listing.description,
            price_cents: listing.price_cents,
            currency: listing.currency,
            seller: `卖家 ${listing.seller_id || ""}`.trim(),
            image: imagePool[(index + 1) % imagePool.length],
          }));
        }
      }
    } catch (_) {
      allListings = fallbackListings.slice();
    }

    renderFeed();
    renderMarket();
  }

  function bindEvents() {
    document.addEventListener("click", (event) => {
      const viewButton = event.target.closest("[data-view]");
      if (viewButton) setView(viewButton.dataset.view);

      const jumpButton = event.target.closest("[data-view-jump]");
      if (jumpButton) setView(jumpButton.dataset.viewJump);

      const themeButton = event.target.closest("[data-theme-choice]");
      if (themeButton) setTheme(themeButton.dataset.themeChoice);

      const bgButton = event.target.closest("[data-bg-choice]");
      if (bgButton) setProfileBg(bgButton.dataset.bgChoice);

      const filterButton = event.target.closest("[data-feed-filter]");
      if (filterButton) {
        activeFeedFilter = filterButton.dataset.feedFilter;
        document.querySelectorAll("[data-feed-filter]").forEach((button) => {
          button.classList.toggle("is-active", button === filterButton);
        });
        renderFeed();
      }

      const studioTab = event.target.closest("[data-studio-tab]");
      if (studioTab) {
        const tab = studioTab.dataset.studioTab;
        document.querySelectorAll("[data-studio-tab]").forEach((button) => {
          button.classList.toggle("is-active", button.dataset.studioTab === tab);
        });
        document.querySelectorAll("[data-studio-panel]").forEach((panel) => {
          panel.classList.toggle("is-active", panel.dataset.studioPanel === tab);
        });
      }
    });

    document.getElementById("cute-search")?.addEventListener("input", renderFeed);
    document.querySelector(".chat-input")?.addEventListener("submit", (event) => event.preventDefault());
  }

  function init() {
    [...imagePool, ...profileBgUrls].forEach((src) => {
      const image = new Image();
      image.src = src;
    });
    setTheme(localStorage.getItem(THEME_KEY) || "sugar");
    setProfileBg(localStorage.getItem(PROFILE_BG_KEY) || "picnic");
    bindEvents();
    renderTopics();
    renderConversations();
    renderSafety();
    renderFeed();
    renderMarket();
    if (new URLSearchParams(window.location.search).has("live")) hydrateFromApi();
    window.addEventListener("test-set-view", (event) => setView(event.detail));
    window.meowCuteSetView = setView;
  }

  init();
})();
