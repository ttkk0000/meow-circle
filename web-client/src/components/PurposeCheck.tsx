import { useEffect, useRef, useState } from "react";
import { api, explain } from "../core/api";
import { Button, Field, Notice } from "./ui";
import { integer } from "../core/wire";

export function OTPButton({
  path,
  body,
  disabled,
  onError,
  onSent,
}: {
  path: string;
  body: () => unknown;
  disabled?: boolean;
  onError: (message: string) => void;
  onSent?: () => void;
}) {
  const [working, setWorking] = useState(false);
  const [until, setUntil] = useState(0);
  const [now, setNow] = useState(Date.now());
  useEffect(() => {
    if (until <= Date.now()) return;
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, [until]);
  return (
    <Button
      disabled={disabled || working || now < until}
      onClick={() => {
        setWorking(true);
        void api
          .request<{ retry_after_seconds: bigint }>(path, {
            method: "POST",
            body: body(),
          })
          .then((result) => {
            setUntil(
              Date.now() +
                Math.max(1, Number(integer(result.retry_after_seconds))) * 1000,
            );
            onSent?.();
          })
          .catch((e) => onError(explain(e)))
          .finally(() => setWorking(false));
      }}
    >
      {until > now
        ? `${Math.ceil((until - now) / 1000)}秒后可重发`
        : "发送本次用途验证码"}
    </Button>
  );
}
/** 一次用途验证只留在内存；完成验证不会替调用方提交业务。 */
export function PurposeCheck({
  scope,
  busy,
  onGrant,
}: {
  scope: Record<string, unknown>;
  busy: boolean;
  onGrant: (token: string) => void;
}) {
  const [password, setPassword] = useState("");
  const [code, setCode] = useState("");
  const [phone, setPhone] = useState("");
  const [working, setWorking] = useState(false);
  const [error, setError] = useState("");
  const generation = useRef(0);
  useEffect(() => {
    generation.current++;
    return () => {
      generation.current++;
    };
  }, [scope]);
  const locked = busy || working;
  async function verify() {
    const current = generation.current;
    const owner = api.session.user?.id ?? 0n;
    setWorking(true);
    setError("");
    onGrant("");
    try {
      const result = await api.request<{ reauth_token: string }>(
        "auth/reauth",
        { method: "POST", owner, body: { scope, password, code } },
      );
      if (current === generation.current) onGrant(result.reauth_token);
    } catch (e) {
      if (current === generation.current) setError(explain(e));
    } finally {
      setPassword("");
      setCode("");
      setWorking(false);
    }
  }
  return (
    <section className="stack">
      <Notice title="仅验证当前对象、范围与版本">
        验证后仍需你勾选并主动提交；修改内容会使此次核对失效。
      </Notice>
      <Field
        label="用当前密码验证"
        type="password"
        value={password}
        onChange={setPassword}
        disabled={locked}
      />
      <Field
        label="或输入当前绑定的完整手机号"
        value={phone}
        onChange={(value) => {
          setPhone(value);
          setCode("");
        }}
        disabled={locked}
      />
      <OTPButton
        path="auth/otp"
        body={() => ({ phone, purpose: "reauth" })}
        disabled={locked || !phone}
        onError={setError}
      />
      <Field
        label="本次用途验证码"
        value={code}
        onChange={setCode}
        disabled={locked}
      />
      <Button
        disabled={locked || (!password && !code)}
        onClick={() => void verify()}
      >
        验证本次用途
      </Button>
      {error ? <Notice title={error} error /> : null}
    </section>
  );
}
