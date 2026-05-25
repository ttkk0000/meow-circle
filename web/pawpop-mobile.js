(function () {
  const root = document.documentElement;
  const themeKey = "pawpop_mobile_theme";
  const bgKey = "pawpop_mobile_bg";

  function setTheme(theme) {
    root.dataset.theme = theme;
    localStorage.setItem(themeKey, theme);
    document.querySelectorAll("[data-theme-choice]").forEach((button) => {
      button.classList.toggle("is-active", button.dataset.themeChoice === theme);
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

  setTheme(localStorage.getItem(themeKey) || "sugar");
  setBg(localStorage.getItem(bgKey) || "picnic");
})();
