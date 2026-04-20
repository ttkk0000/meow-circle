(() => {
  const TOKEN_KEY = "meow_token";
  const USER_KEY = "meow_user";
  const t = (k) => window.MeowShared.t(k);

  /* If already logged in, skip straight to dashboard. */
  if (localStorage.getItem(TOKEN_KEY)) {
    window.location.replace(returnToTarget());
    return;
  }

  function returnToTarget() {
    const params = new URLSearchParams(location.search);
    const raw = params.get("return_to");
    if (!raw) return "/dashboard";
    try {
      const u = new URL(raw, location.origin);
      if (u.origin === location.origin) {
        return u.pathname + u.search + u.hash;
      }
    } catch (_) {}
    return "/dashboard";
  }

  function setError(nodeId, message) {
    const node = document.getElementById(nodeId);
    if (!node) return;
    node.textContent = message || "";
  }

  async function postJSON(url, body) {
    const res = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    let payload = null;
    try {
      payload = await res.json();
    } catch (_) { /* ignore */ }
    if (!res.ok) {
      const err = new Error((payload && payload.message) || `Request failed (${res.status})`);
      err.status = res.status;
      err.retryAfter = res.headers.get("Retry-After");
      throw err;
    }
    return payload && payload.data !== undefined ? payload.data : payload;
  }

  function saveAuth(token, user) {
    localStorage.setItem(TOKEN_KEY, token);
    if (user) localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  /* ===== Login page ===== */
  const loginForm = document.getElementById("login-form");
  if (loginForm) {
    loginForm.addEventListener("submit", async (event) => {
      event.preventDefault();
      setError("login-error", "");
      const data = new FormData(loginForm);
      const username = String(data.get("username") || "").trim();
      const password = String(data.get("password") || "");
      if (!username || !password) {
        setError("login-error", t("auth.error_required"));
        return;
      }
      const submit = loginForm.querySelector('button[type="submit"]');
      submit.disabled = true;
      try {
        const result = await postJSON("/api/v1/auth/login", { username, password });
        saveAuth(result.token, result.user);
        window.location.replace(returnToTarget());
      } catch (err) {
        let msg = err.message || t("auth.error_login");
        if (err.status === 429) {
          const wait = err.retryAfter ? ` (${err.retryAfter}s)` : "";
          msg = t("auth.error_rate_limit") + wait;
        }
        setError("login-error", msg);
      } finally {
        submit.disabled = false;
      }
    });
  }

  /* ===== Register page ===== */
  const registerForm = document.getElementById("register-form");
  if (registerForm) {
    registerForm.addEventListener("submit", async (event) => {
      event.preventDefault();
      setError("register-error", "");
      const data = new FormData(registerForm);
      const username = String(data.get("username") || "").trim();
      const nickname = String(data.get("nickname") || "").trim();
      const password = String(data.get("password") || "");
      if (username.length < 3 || password.length < 6) {
        setError("register-error", t("auth.error_validation"));
        return;
      }
      const submit = registerForm.querySelector('button[type="submit"]');
      submit.disabled = true;
      try {
        const result = await postJSON("/api/v1/auth/register", {
          username,
          password,
          nickname: nickname || username,
        });
        saveAuth(result.token, result.user);
        window.location.replace(returnToTarget());
      } catch (err) {
        setError("register-error", err.message || t("auth.error_register"));
      } finally {
        submit.disabled = false;
      }
    });
  }
})();
