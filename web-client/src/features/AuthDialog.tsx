import { useEffect, useState } from "react";
import { api, explain } from "../core/api";
import { Button, Check, Choices, Field, Modal, Notice } from "../components/ui";
import { navigate } from "../core/router";
import { integer } from "../core/wire";

export interface Capabilities {
  password: boolean;
  email: boolean;
  sms: boolean;
  registration: boolean;
  interaction_contract: bigint;
  scoped_reauth: boolean;
}
export const configuredURL = (value: unknown) => {
  try {
    const u = new URL(String(value));
    return u.protocol === "https:" ? u.href : undefined;
  } catch {
    return undefined;
  }
};
export const termsURL = configuredURL(import.meta.env.VITE_MND_TERMS_URL);
export const privacyURL = configuredURL(import.meta.env.VITE_MND_PRIVACY_URL);
export function LegalLinks() {
  return (
    <div className="legal-links">
      {termsURL ? (
        <a href={termsURL} target="_blank" rel="noopener noreferrer">
          用户协议
        </a>
      ) : (
        <span>用户协议尚未配置</span>
      )}
      {privacyURL ? (
        <a href={privacyURL} target="_blank" rel="noopener noreferrer">
          隐私政策
        </a>
      ) : (
        <span>隐私政策尚未配置</span>
      )}
    </div>
  );
}
export function AuthDialog({ onClose }: { onClose: () => void }) {
  const [mode, setMode] = useState("password");
  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [username, setUsername] = useState("");
  const [code, setCode] = useState("");
  const [agreed, setAgreed] = useState(false);
  const [cap, setCap] = useState<Capabilities>();
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  const [until, setUntil] = useState(0);
  const [now, setNow] = useState(Date.now());
  useEffect(() => {
    let active = true;
    void api
      .request<Capabilities>("auth/capabilities")
      .then((value) => {
        if (active) setCap(value);
      })
      .catch((e) => {
        if (active) setError(explain(e));
      });
    return () => {
      active = false;
    };
  }, []);
  useEffect(() => {
    if (until <= Date.now()) return;
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, [until]);
  const emailMode = ["email", "register", "email-recover"].includes(mode);
  const phoneMode = ["phone", "recover"].includes(mode);
  const needsCode = mode !== "password";
  const needsPassword = [
    "password",
    "register",
    "recover",
    "email-recover",
  ].includes(mode);
  const available =
    !!cap &&
    (mode === "password"
      ? cap.password
      : emailMode
        ? cap.email && (mode !== "register" || cap.registration)
        : cap.sms);
  async function otp() {
    setBusy(true);
    setError("");
    try {
      const result = await api.request<{ retry_after_seconds: bigint }>(
        emailMode ? "auth/email-otp" : "auth/otp",
        {
          method: "POST",
          body: emailMode
            ? {
                email: identifier.trim(),
                purpose:
                  mode === "register"
                    ? "email_register"
                    : mode === "email-recover"
                      ? "email_recover"
                      : "email_login",
              }
            : {
                phone: identifier.trim(),
                purpose: mode === "recover" ? "recover" : "login",
              },
        },
      );
      setUntil(
        Date.now() +
          Math.max(1, Number(integer(result.retry_after_seconds))) * 1000,
      );
      setNotice("验证码已发送，请查看当前邮箱或手机");
    } catch (e) {
      setError(explain(e));
    } finally {
      setBusy(false);
    }
  }
  async function submit() {
    if (!available || busy) return;
    setBusy(true);
    setError("");
    try {
      if (mode === "register" && (!agreed || !termsURL || !privacyURL))
        throw new Error("请阅读当前协议并明确同意后注册");
      const value = identifier.trim();
      const path = mode === "password" ? "password" : mode;
      const body =
        mode === "password"
          ? {
              ...(/^\+?\d{6,}$/.test(value)
                ? { phone: value }
                : value.includes("@")
                  ? { email: value }
                  : { username: value }),
              password,
              device: "web-v2",
            }
          : emailMode
            ? {
                email: value,
                code,
                password: needsPassword ? password : "",
                username: mode === "register" ? username : "",
                accept_terms: agreed,
                device: "web-v2",
              }
            : {
                phone: value,
                code,
                ...(mode === "recover" ? { password } : { device: "web-v2" }),
              };
      await api.request(`auth/${path}`, { method: "POST", body });
      if (mode === "recover" || mode === "email-recover") {
        setNotice("密码恢复完成，请主动登录");
        setMode("password");
      } else onClose();
    } catch (e) {
      setError(explain(e));
    } finally {
      setPassword("");
      setCode("");
      setBusy(false);
    }
  }
  return (
    <Modal
      title="登录 M&amp;D"
      busy={busy}
      onClose={onClose}
      actions={
        <Button
          kind="primary"
          onClick={() => void submit()}
          disabled={
            busy ||
            !available ||
            !identifier.trim() ||
            (needsCode && !code) ||
            (needsPassword && !password) ||
            (mode === "register" && (!agreed || !termsURL || !privacyURL))
          }
        >
          {" "}
          {mode === "register"
            ? "注册并继续"
            : mode.includes("recover")
              ? "恢复密码"
              : "登录并回到当前任务"}
        </Button>
      }
    >
      <div>
        <span className="brand">M&amp;D</span>
        <p className="muted">meow &amp; doggie · 和小可爱一起，记录每一天</p>
      </div>
      <Choices
        label="登录方式"
        value={mode}
        disabled={busy}
        choices={[
          ["password", "账号密码"],
          ["email", "邮箱验证码"],
          ["phone", "手机号验证码"],
          ["register", "邮箱注册"],
          ["email-recover", "邮箱恢复"],
          ["recover", "手机恢复"],
        ]}
        onChange={(next) => {
          setMode(next);
          setCode("");
          setPassword("");
          setError("");
        }}
      />
      {!available ? (
        <Notice
          title={cap ? "当前方式尚未由服务启用" : "正在读取实际登录能力"}
        />
      ) : null}
      <Field
        label={
          emailMode
            ? "邮箱"
            : phoneMode
              ? "已绑定的手机号"
              : "账号名、邮箱或手机号"
        }
        value={identifier}
        onChange={(value) => {
          setIdentifier(value);
          setCode("");
        }}
        disabled={busy}
      />
      {needsCode ? (
        <>
          <Button
            onClick={() => void otp()}
            disabled={busy || !available || !identifier.trim() || until > now}
          >
            {until > now
              ? `${Math.ceil((until - now) / 1000)}秒后可重发`
              : "发送当前用途验证码"}
          </Button>
          <Field
            label="验证码"
            value={code}
            onChange={setCode}
            disabled={busy}
          />
        </>
      ) : null}
      {mode === "register" ? (
        <Field
          label="账号名"
          value={username}
          onChange={setUsername}
          disabled={busy}
        />
      ) : null}
      {needsPassword ? (
        <Field
          label={mode === "password" ? "密码" : "设置新密码"}
          type="password"
          value={password}
          onChange={setPassword}
          disabled={busy}
        />
      ) : null}
      {mode === "register" ? (
        <Check checked={agreed} onChange={setAgreed} disabled={busy}>
          我已阅读并同意用户协议和隐私政策
        </Check>
      ) : null}
      <LegalLinks />
      <Button
        kind="quiet"
        disabled={busy}
        onClick={() => {
          onClose();
          navigate("/rules");
        }}
      >
        阅读服务说明与规则草案
      </Button>
      <Notice title="微信／QQ 登录暂不可用">
        Web 尚未接入真实供应商回调。可使用服务已启用的账号登录方式。
      </Notice>
      {notice ? <Notice title={notice} /> : null}
      {error ? <Notice title={error} error /> : null}
    </Modal>
  );
}
