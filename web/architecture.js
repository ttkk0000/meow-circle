async function loadArchitectureDoc() {
  const response = await fetch("/architecture-data.json");
  if (!response.ok) {
    throw new Error("加载架构数据失败");
  }
  const data = await response.json();
  renderProject(data.project);
  renderCapabilities(data.business_capabilities || []);
  renderLayers(data.architecture_layers || []);
  renderApiModules(data.api_modules || []);
  renderEnvelope(data.response_envelope || {});
  renderList("#security-list", data.security || []);
  renderMobile(data.mobile_integration || {});
  renderRoadmap(data.roadmap || []);
}

function renderEnvelope(envelope) {
  document.querySelector("#envelope-desc").textContent = envelope.description || "";
  document.querySelector("#envelope-success").textContent = envelope.example_success || "";
  document.querySelector("#envelope-error").textContent = envelope.example_error || "";
}

function renderProject(project) {
  document.querySelector("#project-positioning").textContent = project.positioning || "";
  document.querySelector("#project-version").textContent = `v${project.version || "-"}`;
  document.querySelector("#project-updated").textContent = `${project.last_updated || "-"}`;
}

function renderCapabilities(items) {
  const container = document.querySelector("#capabilities");
  container.innerHTML = "";
  for (const item of items) {
    const card = document.createElement("article");
    card.className = "card";
    card.innerHTML = `
      <h3>${escapeHtml(item.name)}</h3>
      <p>${escapeHtml(item.description)}</p>
      <span class="status status-${escapeHtml(item.status)}">${escapeHtml(item.status)}</span>
    `;
    container.appendChild(card);
  }
}

function renderLayers(items) {
  const container = document.querySelector("#layers");
  container.innerHTML = "";
  for (const item of items) {
    const card = document.createElement("article");
    card.className = "card";
    card.innerHTML = `
      <h3>${escapeHtml(item.layer)}</h3>
      <p><strong>目录:</strong> <code>${escapeHtml(item.path)}</code></p>
      <p>${escapeHtml(item.purpose)}</p>
    `;
    container.appendChild(card);
  }
}

function renderApiModules(modules) {
  const container = document.querySelector("#api-modules");
  container.innerHTML = "";

  for (const group of modules) {
    const block = document.createElement("section");
    block.className = "api-group";
    block.innerHTML = `<h3>${escapeHtml(group.module)}</h3>`;
    for (const endpoint of group.endpoints || []) {
      const item = document.createElement("div");
      item.className = "api-item";
      item.innerHTML = `
        <span class="method">${escapeHtml(endpoint.method)}</span>
        <span class="path">${escapeHtml(endpoint.path)}</span>
        <span>${escapeHtml(endpoint.purpose)}</span>
      `;
      block.appendChild(item);
    }
    container.appendChild(block);
  }
}

function renderMobile(mobile) {
  renderList("#mobile-strategy", mobile.recommended_strategy || []);
  renderList("#android-stack", mobile.android_stack_example || []);
}

function renderRoadmap(items) {
  const container = document.querySelector("#roadmap");
  container.innerHTML = "";
  for (const phase of items) {
    const card = document.createElement("article");
    card.className = "card";
    const list = (phase.items || []).map((item) => `<li>${escapeHtml(item)}</li>`).join("");
    card.innerHTML = `
      <h3>${escapeHtml(phase.phase)}</h3>
      <ul>${list}</ul>
    `;
    container.appendChild(card);
  }
}

function renderList(selector, items) {
  const node = document.querySelector(selector);
  node.innerHTML = items.map((item) => `<li>${escapeHtml(item)}</li>`).join("");
}

function escapeHtml(input) {
  return String(input)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

loadArchitectureDoc().catch((error) => {
  const body = document.querySelector(".layout");
  body.innerHTML = `<section class="panel"><h2>文档加载失败</h2><p>${escapeHtml(error.message)}</p></section>`;
});
