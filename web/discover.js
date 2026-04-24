(() => {
  const searchInput = document.querySelector('input[type="search"]');
  const chipRow = document.querySelector(".flex.flex-wrap.gap-3.mb-8");
  const cards = Array.from(document.querySelectorAll("main a.group"));
  if (!chipRow || !cards.length) return;

  const chipCategory = new Map();
  chipCategory.set("stitch.chip_all", "all");
  chipCategory.set("stitch.chip_newbie", "newbie");
  chipCategory.set("stitch.chip_breed", "breed");
  chipCategory.set("stitch.chip_gear", "gear");
  chipCategory.set("stitch.chip_health", "health");

  const cardCategory = new Map();
  cards.forEach((card, idx) => {
    if (idx === 0) cardCategory.set(card, "breed");
    else if (idx === 1) cardCategory.set(card, "newbie");
    else if (idx === 2) cardCategory.set(card, "breed");
    else cardCategory.set(card, "gear");
  });

  let currentCategory = "all";
  let q = "";

  function cardText(card) {
    return card.textContent.toLowerCase();
  }

  function render() {
    let visibleCount = 0;
    for (const card of cards) {
      const matchCategory = currentCategory === "all" || cardCategory.get(card) === currentCategory;
      const matchQuery = !q || cardText(card).includes(q);
      const show = matchCategory && matchQuery;
      card.classList.toggle("hidden", !show);
      if (show) visibleCount += 1;
    }
    const empty = document.querySelector("#discover-empty");
    if (empty) empty.classList.toggle("hidden", visibleCount !== 0);
  }

  chipRow.querySelectorAll("span").forEach((chip) => {
    chip.addEventListener("click", () => {
      const key = chip.getAttribute("data-i18n") || "";
      currentCategory = chipCategory.get(key) || "all";
      chipRow.querySelectorAll("span").forEach((el) => {
        const on = el === chip;
        el.classList.toggle("bg-primary-container", on);
        el.classList.toggle("text-white", on);
        el.classList.toggle("bg-surface-container", !on);
        el.classList.toggle("text-on-surface-variant", !on);
      });
      render();
    });
  });

  if (searchInput) {
    searchInput.addEventListener("input", () => {
      q = searchInput.value.trim().toLowerCase();
      render();
    });
  }

  render();
})();
