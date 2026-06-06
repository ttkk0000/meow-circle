(function () {
  const root = document.documentElement;
  const themeKey = "pawpop_desktop_theme";
  const bgKey = "pawpop_desktop_bg";
  const themes = new Set(["honey", "mint", "night", "neutral"]);

  function normalizeTheme(theme) {
    if (theme === "sugar") return "honey";
    if (theme === "system") return "neutral";
    return themes.has(theme) ? theme : "honey";
  }

  function setTheme(theme) {
    const nextTheme = normalizeTheme(theme);
    root.dataset.theme = nextTheme;
    localStorage.setItem(themeKey, nextTheme);
    document.querySelectorAll("[data-theme-choice]").forEach((button) => {
      const active = button.dataset.themeChoice === nextTheme;
      button.classList.toggle("is-active", active);
      button.setAttribute("aria-pressed", String(active));
    });
  }

  function setBg(bg) {
    root.dataset.profileBg = bg;
    localStorage.setItem(bgKey, bg);
    document.querySelectorAll("[data-bg-choice]").forEach((button) => {
      button.classList.toggle("is-active", button.dataset.bgChoice === bg);
    });
  }

  document.addEventListener("click", (event) => {
    const theme = event.target.closest("[data-theme-choice]");
    if (theme) setTheme(theme.dataset.themeChoice);

    const bg = event.target.closest("[data-bg-choice]");
    if (bg) setBg(bg.dataset.bgChoice);
  });

  setTheme(localStorage.getItem(themeKey) || "honey");
  setBg(localStorage.getItem(bgKey) || "picnic");
})();
