import {useEffect, useRef, useState} from "react";
import {api, explain} from "../core/api";
import {integer} from "../core/wire";
import {useSession} from "../core/session";
import {replaceQueryData} from "../core/query";
import {otpExpiryMillis, validAuthCode, validLoginPhone} from "../core/authInput";
import {Button, Card, Field, Modal, Notice} from "../components/ui";

/** Delivery metadata is scoped to one owner, address and purpose; it grants no mutation. */
function usePhoneCode(owner: bigint, address: string, purpose: string) {
  const session = useSession();
  const epoch = useRef(api.epoch).current;
  const [receipt, setReceipt] = useState<{ address: string; expires: number; expired: boolean }>();
  const [until, setUntil] = useState(0);
  const [now, setNow] = useState(Date.now());
  const [sending, setSending] = useState(false);
  const [error, setError] = useState("");
  const active = useRef(true);
  const pending = useRef(false);
  useEffect(() => { active.current = true; return () => { active.current = false; }; }, []);
  useEffect(() => {
    const bound = receipt;
    const deadline = Math.max(until, bound?.expires || 0);
    if (deadline <= Date.now()) return;
    const timer = setInterval(() => {
      const clock = Date.now(); setNow(clock);
      if (bound && clock >= bound.expires) setReceipt(current => current === bound ? { ...bound, expired: true } : current);
      if (clock >= deadline) clearInterval(timer);
    }, 1000);
    return () => clearInterval(timer);
  }, [until, receipt?.expires]);
  const owned = session.user?.id === owner && api.epoch === epoch;
  const sentHere = owned && !!receipt && receipt.address === address.trim();
  const expired = sentHere && (receipt.expired || now >= receipt.expires);
  async function send() {
    if (pending.current || api.epoch !== epoch || api.session.user?.id !== owner || !validLoginPhone(address) || Date.now() < until) return;
    pending.current = true; setSending(true); setError(""); setReceipt(undefined);
    const target = address.trim();
    try {
      const result = await api.request<{ id: bigint; expires_at: string; retry_after_seconds: bigint }>("auth/otp", {
        method: "POST", owner, body: { phone: target, purpose },
      });
      if (!active.current || api.epoch !== epoch || api.session.user?.id !== owner) return;
      const expires = otpExpiryMillis(result.expires_at);
      if (integer(result.id) <= 0n || !Number.isFinite(expires) || expires <= 0) throw Error("本次验证码回执暂不可读，请重新获取");
      const clock = Date.now(); setNow(clock);
      setReceipt({ address: target, expires, expired: clock >= expires });
      setUntil(clock + Math.max(1, Number(integer(result.retry_after_seconds))) * 1000);
    } catch (e) { if (active.current && api.epoch === epoch) setError(explain(e)); }
    finally { pending.current = false; if (active.current) setSending(false); }
  }
  return { owned, sending, error, sentHere, expired, wait: Math.max(0, Math.ceil((until - now) / 1000)), send };
}

export function AuthCurrentPhone({ owner, maskedPhone, smsAvailable, changing, onVerified, onCancel, onRecovery }: {
  owner: bigint; maskedPhone: string; smsAvailable: boolean; changing: boolean;
  onVerified: () => void; onCancel: () => void; onRecovery: () => void;
}) {
  const [phone, setPhone] = useState("");
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const active = useRef(true);
  const pending = useRef(false);
  const epoch = useRef(api.epoch).current;
  const otp = usePhoneCode(owner, phone, "verify_phone");
  useEffect(() => { active.current = true; return () => { active.current = false; }; }, []);
  const locked = busy || otp.sending;
  async function verify() {
    if (pending.current || locked || !otp.owned || !otp.sentHere || otp.expired || !validAuthCode(code) || api.epoch !== epoch) return;
    pending.current = true; setBusy(true); setError("");
    const proof = code; setCode("");
    try {
      await api.request("auth/verify-phone", { method: "POST", owner, body: { code: proof } });
      const actual = await api.request<{ contract_version: bigint; phone_verified: boolean }>("eligibility", { owner });
      if (!active.current || api.epoch !== epoch || api.session.user?.id !== owner) return;
      if (actual.contract_version !== 1n || !actual.phone_verified) throw Error("当前手机号验证状态尚未确认，请重新读取资格");
      replaceQueryData("eligibility", actual);
      setPhone(""); onVerified();
    } catch (e) { if (active.current && api.epoch === epoch) setError(explain(e)); }
    finally { pending.current = false; if (active.current) setBusy(false); }
  }
  if (!otp.owned) return null;
  return <Modal title={changing ? "更换手机号" : "验证手机号"} task busy={locked} onClose={onCancel}
    actions={<Button kind="primary" disabled={locked || !otp.sentHere || otp.expired || !validAuthCode(code)} onClick={() => void verify()}>{changing ? "验证并填写新号码" : "验证并回到任务"}</Button>}>
    <div className="auth-task-content">
      {changing ? <Card><dl className="auth-facts"><div><dt>当前号码</dt><dd>{maskedPhone}</dd></div></dl></Card> : <>
        <h1>继续这次操作前验证</h1><Notice title="用于本次功能">验证不会公开你的手机号，也不会改变内容受众或开启通知。手机号、年龄与具体业务资格仍由服务分别核对。</Notice>
      </>}
      <Field label="当前完整手机号" type="tel" autoComplete="tel" value={phone} disabled={locked} onChange={value => { setPhone(value); setCode(""); setError(""); }} />
      <Field label={changing ? "当前号码验证码" : "验证码"} value={code} autoComplete="one-time-code" inputMode="numeric" disabled={locked || !otp.sentHere || otp.expired}
        onChange={value => { setCode(value.replace(/[^0-9]/g, "").slice(0, 6)); setError(""); }} />
      <Button kind="quiet" disabled={locked || !smsAvailable || !validLoginPhone(phone) || otp.wait > 0} onClick={() => { setCode(""); void otp.send(); }}>{otp.wait ? `${otp.wait}秒后可重发` : "获取当前号码验证码"}</Button>
      {otp.expired ? <Notice title="验证码已过期">请重新获取当前号码验证码，填写最新验证码后再验证。</Notice> : null}
      {!smsAvailable ? <Notice title="当前短信渠道尚未启用">保留公共浏览；没有实际验证码不会继续验证或换号。</Notice> : null}
      {changing ? <><Notice title="只更新登录号码">宠物、日记和聊天仍属于这个M&amp;D账号。当前号验证不代替后续针对新号码的变更用途验证。</Notice>
        <Button kind="quiet" disabled={locked} onClick={onRecovery}>旧号码已不能使用</Button></> : <Button kind="quiet" disabled={locked} onClick={onCancel}>暂不验证</Button>}
      {error || otp.error ? <Notice title={error || otp.error} error /> : null}
    </div>
  </Modal>;
}

export function AuthNewPhoneContent({ owner, phone, code, smsAvailable, busy, replacing, onPhone, onCode, onReady }: {
  owner: bigint; phone: string; code: string; smsAvailable: boolean; busy: boolean; replacing: boolean;
  onPhone: (value: string) => void; onCode: (value: string) => void; onReady: (ready: boolean) => void;
}) {
  const otp = usePhoneCode(owner, phone, "bind_phone");
  const ready = otp.owned && otp.sentHere && !otp.expired && !otp.sending && validAuthCode(code);
  const callback = useRef(onReady); callback.current = onReady;
  useEffect(() => { callback.current(ready); return () => callback.current(false); }, [ready]);
  const locked = busy || otp.sending;
  return <div className="auth-task-content">
    <Field label="新的手机号" type="tel" autoComplete="tel" value={phone} disabled={locked} onChange={value => { onPhone(value); onCode(""); }} />
    <Field label="验证码" autoComplete="one-time-code" inputMode="numeric" value={code} disabled={locked || !otp.sentHere || otp.expired} onChange={value => onCode(value.replace(/[^0-9]/g, "").slice(0, 6))} />
    <Button kind="quiet" disabled={locked || !smsAvailable || !validLoginPhone(phone) || otp.wait > 0} onClick={() => { onCode(""); void otp.send(); }}>{otp.wait ? `${otp.wait}秒后可重发` : "获取新号码验证码"}</Button>
    <Notice title="确认号码可用">{replacing ? "验证新号码后，旧号码不再用于当前账号登录。" : "验证新号码后关联到同一账号。"}接下来仍需核对本次变更用途并主动确认。</Notice>
    {otp.expired ? <Notice title="验证码已过期">请重新获取新号码的验证码。</Notice> : null}
    {otp.error ? <Notice title={otp.error} error /> : null}
  </div>;
}
