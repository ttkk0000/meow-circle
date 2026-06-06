(function () {
  const root = document.documentElement;
  const validThemes = new Set(["honey", "mint", "night", "neutral"]);
  const validViews = new Set(["feed", "market", "messages", "orders", "profile", "auth", "compose", "safety"]);

  function normalizeTheme(theme) {
    if (theme === "sugar") return "honey";
    if (theme === "system") return "neutral";
    return validThemes.has(theme) ? theme : "honey";
  }

  function setTheme(theme, isManual = false) {
    const next = normalizeTheme(theme);
    root.dataset.theme = next;
    if (isManual) localStorage.removeItem("mnd_theme_override");
    localStorage.setItem("mnd_theme", next);
    document.querySelectorAll("[data-theme-choice]").forEach((button) => {
      const active = button.dataset.themeChoice === next;
      button.classList.toggle("is-active", active);
      button.setAttribute("aria-pressed", String(active));
    });
  }

  function setView(view) {
    const next = validViews.has(view) ? view : "feed";
    document.querySelectorAll("[data-view-panel]").forEach((panel) => {
      panel.classList.toggle("is-active", panel.dataset.viewPanel === next);
    });
    document.querySelectorAll("[data-view]").forEach((button) => {
      button.classList.toggle("is-active", button.dataset.view === next);
    });
    if (location.hash.slice(1) !== next) history.replaceState(null, "", `#${next}`);
    if (matchMedia("(max-width: 1080px)").matches) window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function setProfileBackground(bg) {
    const next = ["picnic", "desk", "arcade", "garden"].includes(bg) ? bg : "picnic";
    root.dataset.profileBg = next;
    localStorage.setItem("mnd_profile_bg", next);
    document.querySelectorAll("[data-bg-choice]").forEach((button) => {
      const active = button.dataset.bgChoice === next;
      button.classList.toggle("is-active", active);
      button.setAttribute("aria-pressed", String(active));
    });
  }

  document.addEventListener("click", (event) => {
    const trigger = event.target.closest(".settings-trigger");
    const dropdown = document.querySelector(".settings-popup");
    if (trigger) {
      const isHidden = dropdown.style.display === "none" || !dropdown.style.display;
      dropdown.style.display = isHidden ? "flex" : "none";
      return;
    }

    if (dropdown && !event.target.closest(".settings-dropdown")) {
      dropdown.style.display = "none";
    }

    const themeButton = event.target.closest("[data-theme-choice]");
    if (themeButton) {
      setTheme(themeButton.dataset.themeChoice, true);
      return;
    }

    const bgButton = event.target.closest("[data-bg-choice]");
    if (bgButton) {
      setProfileBackground(bgButton.dataset.bgChoice);
      return;
    }

    const viewButton = event.target.closest("[data-view], [data-view-jump]");
    if (viewButton) {
      setView(viewButton.dataset.view || viewButton.dataset.viewJump);
    }
  });

  const startView = location.hash.slice(1);
  const initialTheme =
    localStorage.getItem("mnd_theme") ||
    localStorage.getItem("mnd_theme_override") ||
    normalizeTheme(root.dataset.theme);
  setTheme(initialTheme);
  setProfileBackground(localStorage.getItem("mnd_profile_bg") || root.dataset.profileBg);
  setView(validViews.has(startView) ? startView : "feed");
})();
