(function () {
  const root = document.documentElement;
  const themeKey = "pawpop_desktop_theme";
  const viewKey = "pawpop_desktop_view";
  const validThemes = new Set(["honey", "mint", "night", "neutral"]);
  const validViews = new Set(["feed", "market", "messages", "orders", "profile", "auth", "neutral"]);

  function normalizeTheme(theme) {
    if (theme === "sugar") return "honey";
    if (theme === "system") return "neutral";
    return validThemes.has(theme) ? theme : "honey";
  }

  function setTheme(theme, isManual = false) {
    const nextTheme = normalizeTheme(theme);
    root.dataset.theme = nextTheme;
    if (isManual) localStorage.setItem(themeKey, nextTheme);
    document.querySelectorAll("[data-theme-choice]").forEach((button) => {
      const active = button.dataset.themeChoice === nextTheme;
      button.classList.toggle("is-active", active);
      button.setAttribute("aria-pressed", String(active));
    });
  }

  function setView(view, updateHash = true) {
    const nextView = validViews.has(view) ? view : "feed";

    document.querySelectorAll("[data-view-panel]").forEach((panel) => {
      const active = panel.dataset.viewPanel === nextView;
      panel.classList.toggle("is-active", active);
      panel.setAttribute("aria-hidden", String(!active));
    });

    document.querySelectorAll("[data-view]").forEach((button) => {
      const active = button.dataset.view === nextView;
      button.classList.toggle("is-active", active);
      button.setAttribute("aria-pressed", String(active));
    });

    localStorage.setItem(viewKey, nextView);

    if (updateHash && location.hash.slice(1) !== nextView) {
      history.replaceState(null, "", `#${nextView}`);
    }
    if (matchMedia("(max-width: 1080px)").matches) {
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  }

  document.addEventListener("click", (event) => {
    const theme = event.target.closest("[data-theme-choice]");
    if (theme) {
      setTheme(theme.dataset.themeChoice, true);
      return;
    }

    const view = event.target.closest("[data-view]");
    if (view) setView(view.dataset.view);
  });

  window.addEventListener("hashchange", () => {
    setView(location.hash.slice(1), false);
  });

  const startView = location.hash.slice(1) || localStorage.getItem(viewKey) || "feed";
  setTheme(localStorage.getItem(themeKey) || root.dataset.theme);
  setView(startView, false);
})();
