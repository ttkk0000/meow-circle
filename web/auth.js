(() => {
  const TOKEN_KEY = "meow_token";
  const USER_KEY = "meow_user";
  const t = (k) => window.MeowShared.t(k);
  const toast = (msg, kind) => window.MeowShared.toast(msg, kind);

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

  function wirePasswordToggle(button, input, iconSelector) {
    if (!button || !input) return;
    button.addEventListener("click", () => {
      const isHidden = input.getAttribute("type") === "password";
      input.setAttribute("type", isHidden ? "text" : "password");
      const icon = iconSelector ? button.querySelector(iconSelector) : null;
      if (icon) icon.textContent = isHidden ? "visibility" : "visibility_off";
    });
  }

  function wireAuthChrome() {
    const forgot = document.getElementById("forgot-password-link");
    if (forgot) {
      forgot.addEventListener("click", (e) => {
        e.preventDefault();
        toast(t("auth.forgot_coming"), "info");
      });
    }

    document.querySelectorAll(".social-login").forEach((btn) => {
      btn.addEventListener("click", () => toast(t("auth.social_coming"), "info"));
    });

    wirePasswordToggle(
      document.getElementById("login-password-toggle"),
      document.getElementById("login-password"),
      ".login-pw-icon",
    );
    wirePasswordToggle(
      document.getElementById("register-password-toggle"),
      document.getElementById("reg-password"),
      ".reg-pw-icon",
    );

    const sendBtn = document.getElementById("send-verification-code");
    if (sendBtn) {
      let cooldown = 0;
      let timer = null;
      const baseText = () => t("auth.send_code");
      const phoneInput = document.getElementById("reg-phone");
      sendBtn.addEventListener("click", async () => {
        if (cooldown > 0) return;
        const raw = (phoneInput && phoneInput.value.trim()) || "";
        const digits = raw.replace(/\D/g, "");
        if (digits.length < 10 || digits.length > 15) {
          toast(t("auth.error_phone_invalid"), "error");
          return;
        }
        try {
          await postJSON("/api/v1/auth/send-verification-code", { phone: raw });
          toast(t("auth.send_code_ok"), "info");
        } catch (err) {
          toast(err.message || t("auth.error_register"), "error");
          return;
        }
        cooldown = 60;
        sendBtn.disabled = true;
        const tick = () => {
          if (cooldown <= 0) {
            if (timer) clearInterval(timer);
            timer = null;
            sendBtn.disabled = false;
            sendBtn.textContent = baseText();
            return;
          }
          sendBtn.textContent = `${cooldown}${t("auth.send_code_wait")}`;
          cooldown -= 1;
        };
        tick();
        timer = setInterval(tick, 1000);
      });
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", wireAuthChrome);
  } else {
    wireAuthChrome();
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
      const password = String(data.get("password") || "");
      const phoneRaw = String(data.get("phone") || "").trim();
      const phoneDigits = phoneRaw.replace(/\D/g, "");
      const smsCode = String(data.get("code") || "").trim();
      const agreed = registerForm.querySelector("#agreement")?.checked;
      if (!agreed) {
        setError("register-error", t("auth.error_agreement"));
        return;
      }
      if (username.length < 3 || password.length < 6) {
        setError("register-error", t("auth.error_validation"));
        return;
      }
      if (phoneDigits.length > 0) {
        if (phoneDigits.length < 10 || phoneDigits.length > 15) {
          setError("register-error", t("auth.error_phone_invalid"));
          return;
        }
        if (!smsCode) {
          setError("register-error", t("auth.error_sms_required"));
          return;
        }
      }
      const submit = registerForm.querySelector('button[type="submit"]');
      submit.disabled = true;
      try {
        const body = { username, password, nickname: username };
        if (phoneDigits.length > 0) {
          body.phone = phoneRaw;
          body.sms_code = smsCode;
        }
        const result = await postJSON("/api/v1/auth/register", body);
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
