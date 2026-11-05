import {useEffect, useRef, useState} from "react";
import {api, explain, HttpError, type SessionEnd, type User} from "../core/api";
import {AccountConflictTask} from "../components/AccountConflict";
import {Button, Card, Check, Field, Icon, Modal, Notice, PrivateImage, RowLink} from "../components/ui";
import {navigate, useRoute} from "../core/router";
import {integer} from "../core/wire";
import {LegalLinks, privacyURL, termsURL} from "../components/AuthPolicies";
import {ProviderSignIn} from "./ProviderSignIn";
import {AuthInformation, type AuthInformationPage, FirstAccountContent} from "./AuthInformation";
import {AuthProfile} from "./AuthProfile";
import {authReturnTarget, AuthReturnTask} from "./AuthReturnTask";
import {useSession} from "../core/session";
import {AuthRecovery} from "./AuthRecovery";
import {AuthKnown} from "./AuthKnown";
import {
  AuthCodeExpiredTask,
  AuthCodeResentTask,
  AuthOtpField,
  AuthPhoneErrorTask,
  AuthSmsDelayedTask
} from "./AuthCodePages";
import {otpExpiryMillis, validAuthCode, validLoginPhone} from "../core/authInput";

export interface Capabilities {
  password: boolean;
  email: boolean;
  sms: boolean;
  registration: boolean;
  interaction_contract: bigint;
  scoped_reauth: boolean;
  providers: string[];
}
type Step = "start" | "code" | "reset" | "help" | "done" | "create-review";
function maskedAddress(value: string) {
  const clean = value.trim();
  if (clean.includes("@")) return `${clean.slice(0, 1)}***@${clean.split("@")[1]}`;
  return clean.length >= 7 ? `${clean.slice(0, 3)} **** ${clean.slice(-4)}` : "****";
}

export function AuthDialog({ onClose, sessionEnd }: { onClose: () => void; sessionEnd?: SessionEnd }) {
  const route = useRoute();
  const session = useSession();
  const [origin] = useState(route);
  const [ended, setEnded] = useState(sessionEnd?.kind !== "account_request" ? sessionEnd : undefined);
  const [returnSettings, setReturnSettings] = useState(false);
  const [information, setInformation] = useState<AuthInformationPage[]>([]);
  const [recoveryHelp, setRecoveryHelp] = useState(false);
  const [completion, setCompletion] = useState<{ owner: bigint; created: boolean; profile: boolean; reviewReturn?: boolean; user?: User }>();
  const [provider, setProvider] = useState("");
  const [mode, setMode] = useState(!api.session.authenticated && api.rememberedIdentity ? "known" : "password");
  const [step, setStep] = useState<Step>("start");
  const [identifier, setIdentifier] = useState("");
  const [sent, setSent] = useState<{ address: string; mode: string; expiresAt: string; expired: boolean }>();
  const [phoneError, setPhoneError] = useState(false);
  const [resent, setResent] = useState(false);
  const [password, setPassword] = useState("");
  const [repeated, setRepeated] = useState("");
  const [username, setUsername] = useState("");
  const [code, setCode] = useState("");
  const [agreed, setAgreed] = useState(false);
  const [cap, setCap] = useState<Capabilities>();
  const [capError, setCapError] = useState("");
  const [error, setError] = useState("");
  const [errorCode, setErrorCode] = useState("");
  const [failureKind, setFailureKind] = useState<"connection" | "rate" | "service">("service");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  const [until, setUntil] = useState(0);
  const [now, setNow] = useState(Date.now());
  const active = useRef(true);
  const pending = useRef(false);
  const capabilitiesRead = useRef(0);
  const observedEpoch = useRef(api.epoch);
  const expectingLogin = useRef(false);
  const generation = useRef(0);
  const content = useRef<HTMLDivElement>(null);
  useEffect(() => { if (route !== origin) onClose(); }, [route, origin, onClose]);
  function clearAccountInputs() {
    generation.current++; pending.current = false;
    setIdentifier(""); setUsername(""); setPassword(""); setRepeated(""); setCode(""); setSent(undefined);
    setAgreed(false); setInformation([]); setRecoveryHelp(false); setCompletion(undefined); setReturnSettings(false);
    setMode("password"); setStep("start"); setUntil(0); setBusy(false); setPhoneError(false); setResent(false); clearFeedback();
    observedEpoch.current = api.epoch;
  }
  useEffect(() => {
    // ProviderSignIn owns its expected account transition and clears its own short-lived proof.
    if (observedEpoch.current !== api.epoch && !expectingLogin.current && !provider) clearAccountInputs();
  }, [session, provider]);
  useEffect(() => {
    if (sessionEnd && sessionEnd.kind !== "account_request") {
      setEnded(sessionEnd); setProvider(""); setPassword(""); setRepeated(""); setCode(""); setSent(undefined);
      setInformation([]);
      setCompletion(undefined);
    }
  }, [sessionEnd]);
  function finishSignIn() {
    onClose();
    if (returnSettings) navigate("/security");
  }
  function authenticated(created: boolean) {
    const user = api.session.user;
    if (!api.session.authenticated || !user) throw Error("尚未确认当前登录状态，请重新核对");
    observedEpoch.current = api.epoch;
    setProvider(""); setCompletion({ owner: user.id, created, profile: created });
  }
  function beginLogin() {
    if (pending.current) return;
    setReturnSettings(ended?.kind === "security_change");
    api.acknowledgeSessionEnd(); setEnded(undefined); changeMode(api.rememberedIdentity ? "known" : "password");
  }
  async function readCapabilities() {
    const sequence = ++capabilitiesRead.current;
    setCap(undefined); setCapError("");
    try {
      const value = await api.request<Capabilities>("auth/capabilities");
      if (active.current && sequence === capabilitiesRead.current) setCap(value);
    } catch (e) {
      if (active.current && sequence === capabilitiesRead.current) setCapError(explain(e));
    }
  }
  useEffect(() => {
    active.current = true;
    void readCapabilities();
    return () => { active.current = false; capabilitiesRead.current++; };
  }, []);
  useEffect(() => {
    const bound = sent;
    const deliveryEnd = bound ? otpExpiryMillis(bound.expiresAt) : 0;
    const deadline = Math.max(until, Number.isFinite(deliveryEnd) ? deliveryEnd : 0);
    if (deadline <= Date.now()) return;
    const timer = setInterval(() => {
      const clock = Date.now(); setNow(clock);
      if (bound && Number.isFinite(deliveryEnd) && clock >= deliveryEnd)
        setSent(current => current && current === bound ? { ...current, expired: true } : current);
      if (clock >= deadline) clearInterval(timer);
    }, 1000);
    return () => clearInterval(timer);
  }, [until, sent?.expiresAt]);
  useEffect(() => {
    if (!busy) content.current?.querySelector<HTMLInputElement>('input:not([type="checkbox"]):not(:disabled)')?.focus();
  }, [step, mode, busy]);
  const emailMode = ["email", "register", "email-recover"].includes(mode);
  const phoneMode = ["phone", "recover"].includes(mode);
  const recovery = mode === "recover" || mode === "email-recover";
  const policyReady = !!termsURL && !!privacyURL;
  const requiresAgreement = mode === "register" || mode === "email";
  const consentReady = !requiresAgreement || policyReady && agreed;
  const available = !!cap && (mode === "password" ? cap.password : emailMode ? cap.email && (mode !== "register" || cap.registration) : cap.sms);
  const sentHere = sent?.mode === mode && sent.address === identifier.trim();
  const receiptExpired = !!sent && sentHere && (sent.expired || now >= otpExpiryMillis(sent.expiresAt));
  const passwordBytes = new TextEncoder().encode(password).length;
  const passwordReady = passwordBytes >= 10 && passwordBytes <= 128 && password === repeated;
  const wait = Math.max(0, Math.ceil((until - now) / 1000));
  function clearFeedback() { setError(""); setErrorCode(""); setNotice(""); }
  function showError(e: unknown) {
    setError(explain(e)); setErrorCode(e instanceof HttpError ? e.code : "");
    setFailureKind(e instanceof HttpError && e.status === 429 ? "rate" : e instanceof TypeError ? "connection" : "service");
  }
  function changeMode(next: string) {
    if (pending.current) return;
    setMode(next); setStep("start"); setSent(undefined);
    setCode(""); setPassword(""); setRepeated(""); setAgreed(false); setPhoneError(false); setResent(false); clearFeedback();
  }
  function back() {
    if (pending.current) return;
    if (step === "reset") { setStep("code"); setPassword(""); setRepeated(""); clearFeedback(); }
    else if (step === "create-review") { setStep("code"); clearFeedback(); }
    else if (step === "code") { setStep("start"); setCode(""); setPassword(""); setRepeated(""); clearFeedback(); }
    else if (step === "help") { setStep(sentHere ? "code" : "start"); clearFeedback(); }
    else if (step === "done") changeMode("password");
    else onClose();
  }
  async function otp() {
    if (observedEpoch.current !== api.epoch) { clearAccountInputs(); return; }
    if (pending.current || !available || !consentReady || !identifier.trim() || Date.now() < until) return;
    if (phoneMode && !validLoginPhone(identifier)) { clearFeedback(); setPhoneError(true); return; }
    pending.current = true; setBusy(true); clearFeedback();
    const current = generation.current;
    const address = identifier.trim();
    const requestedMode = mode;
    const resending = sentHere && phoneMode;
    setSent(undefined);
    try {
      const result = await api.request<{ id: bigint; expires_at: string; retry_after_seconds: bigint }>(emailMode ? "auth/email-otp" : "auth/otp", {
        method: "POST",
        body: emailMode ? { email: address, purpose: mode === "register" ? "email_register" : mode === "email-recover" ? "email_recover" : "email_login" }
          : { phone: address, purpose: mode === "recover" ? "recover" : "login" },
      });
      if (!active.current || current !== generation.current) return;
      const expires = otpExpiryMillis(result.expires_at);
      if (integer(result.id) <= 0n || !Number.isFinite(expires) || expires <= 0) throw Error("本次验证码回执暂不可读，请重新获取");
      const clock = Date.now(); setNow(clock);
      setUntil(clock + Math.max(1, Number(integer(result.retry_after_seconds))) * 1000);
      setSent({ address, mode: requestedMode, expiresAt: result.expires_at, expired: clock >= expires }); setCode(""); setStep("code"); setPhoneError(false); setResent(!!resending);
      setNotice(`验证码已发送，请查看当前${emailMode ? "邮箱" : "手机"}`);
    } catch (e) {
      if (active.current && current === generation.current) showError(e);
    } finally { if (current === generation.current) { pending.current = false; if (active.current) setBusy(false); } }
  }
  const canSubmit = !busy && available && !!identifier.trim() && (mode === "password"
    ? !!password && (!policyReady || agreed)
    : sentHere && !receiptExpired && validAuthCode(code) && consentReady && (step === "reset" || mode === "register" ? passwordReady && (mode !== "register" || !!username.trim()) : true));
  async function submit() {
    if (observedEpoch.current !== api.epoch) { clearAccountInputs(); return; }
    if (pending.current || !canSubmit) return;
    clearFeedback();
    if (recovery && step === "code") { setPassword(""); setRepeated(""); setStep("reset"); return; }
    if (mode === "register" && step === "code") { setStep("create-review"); return; }
    if (mode !== "password" && step !== "code" && step !== "reset" && step !== "create-review") return;
    pending.current = true; setBusy(true);
    const current = generation.current;
    expectingLogin.current = !recovery;
    try {
      const value = identifier.trim();
      const path = mode === "password" ? "password" : mode;
      const body = mode === "password"
        ? { ...(/^\+?\d{6,}$/.test(value) ? { phone: value } : value.includes("@") ? { email: value } : { username: value }), password, device: "web-v2" }
        : emailMode ? { email: value, code, password: recovery || mode === "register" ? password : "", username: mode === "register" ? username.trim() : "", accept_terms: agreed, device: "web-v2" }
          : { phone: value, code, ...(recovery ? { password } : { device: "web-v2" }) };
      const result = await api.request<{ created?: boolean }>(`auth/${path}`, { method: "POST", body });
      if (!active.current || current !== generation.current) return;
      if (recovery) setStep("done");
      else authenticated(result?.created === true);
    } catch (e) {
      if (active.current && current === generation.current) showError(e);
    } finally {
      expectingLogin.current = false;
      if (active.current && observedEpoch.current !== api.epoch) clearAccountInputs();
      else if (current === generation.current) {
        pending.current = false;
        if (active.current) { setPassword(""); setRepeated(""); setCode(""); setBusy(false); }
      }
    }
  }
  if (observedEpoch.current !== api.epoch && !provider) return null;
  if (ended) {
    const signedOut = ended.kind === "signed_out";
    const changed = ended.kind === "security_change";
    const resultTitle = signedOut ? "已退出登录" : !changed ? "登录状态已过期"
      : ended.action === "phone" ? "手机号已更新" : ended.action === "email" ? "邮箱已更新"
        : ended.action === "password" ? "密码已更新" : "登录方式已更新";
    return <Modal title={resultTitle} task busy={busy} onClose={onClose}
      actions={<Button kind="primary" disabled={busy} onClick={signedOut ? onClose : beginLogin}>{signedOut ? "先逛逛" : changed ? "重新登录并返回设置" : "重新登录"}</Button>}>
      <div className="auth-task-content">
        <h1>{signedOut ? "随时回来，继续记录" : changed && ended.maskedAddress ? `已更新为${ended.maskedAddress}` : resultTitle}</h1>
        {signedOut ? <Button disabled={busy} onClick={beginLogin}>重新登录</Button> : <>
          <Notice title={changed ? "账号没有变" : "可以继续"}>{changed
            ? "设置已由服务确认。旧会话已失效，请重新登录后返回账号设置；仍可使用其他已关联的登录方式。"
            : "重新登录后返回当前任务，草稿不会自动提交。"}</Notice>
          {!changed ? <Button kind="quiet" disabled={busy} onClick={onClose}>返回刚才的页面</Button> : null}
        </>}
      </div>
    </Modal>;
  }
  if (completion && api.session.authenticated && api.session.user?.id === completion.owner) {
    if (completion.profile) return <AuthProfile key={String(completion.owner)} owner={completion.owner}
      onDone={profile => setCompletion(current => current && api.session.user?.id === current.owner
        ? { ...current, profile: false, user: profile?.user ?? current.user } : undefined)} />;
    if (completion.reviewReturn) return <AuthReturnTask route={returnSettings ? "/security" : origin} busy={busy}
      onBack={() => setCompletion(current => current ? { ...current, reviewReturn: false } : current)} onContinue={finishSignIn} />;
    const user = completion.user ?? api.session.user;
    return <Modal title="登录成功" task busy={busy} onClose={finishSignIn}
      actions={<Button kind="primary" disabled={busy} onClick={finishSignIn}>返回并继续</Button>}>
      <div className="auth-task-content">
        <Card><div className="profile-identity security-identity"><PrivateImage id={integer(user.avatar_url?.match(/\/media\/(\d+)\/content/)?.[1]) || undefined} alt="当前账号头像" className="security-avatar" />
          <div><strong>{user.nickname || user.username}</strong><p className="caption muted">已登录</p></div></div></Card>
        <h1>继续刚才的事情</h1>
        <Notice title="内容已保留">你可以检查草稿后再发送或发布，不会自动完成上一步。</Notice>
        <div className="auth-return-task"><RowLink title="登录后回到哪里" detail={authReturnTarget(returnSettings ? "/security" : origin).title} disabled={busy}
          onClick={() => setCompletion(current => current ? { ...current, reviewReturn: true } : current)} /></div>
        {completion.created ? <RowLink title="设置头像与昵称" detail="可稍后补充，不影响返回原任务" disabled={busy}
          onClick={() => setCompletion(current => current ? { ...current, profile: true } : current)} /> : null}
      </div>
    </Modal>;
  }
  if (provider) return <ProviderSignIn provider={provider} onBack={() => setProvider("")} onClose={finishSignIn} capabilities={cap} onAuthenticated={authenticated}
    onManageAccount={() => { setProvider(""); changeMode("password"); navigate("/security"); if (api.session.authenticated) onClose(); }} />;
  if (recoveryHelp) return <AuthRecovery key={api.epoch} onBack={() => setRecoveryHelp(false)} onClose={onClose}
    onSupplement={route => { onClose(); navigate(route); }} />;
  if (information.length) return <AuthInformation page={information[information.length - 1]} onBack={() => setInformation(value => value.slice(0, -1))}
    onPage={page => setInformation(value => page === "entry" ? [page] : [...value, page])} onExit={() => setInformation([])}
    onManage={() => { setInformation([]); changeMode("password"); navigate("/security"); if (api.session.authenticated) onClose(); }} />;
  if (mode === "known" && !api.session.authenticated && api.rememberedIdentity) return <AuthKnown known={api.rememberedIdentity} busy={busy} onClose={onClose}
    onPhone={() => changeMode("phone")} onOther={() => changeMode("password")} onProvider={setProvider} />;
  if (errorCode === "identity_in_use") return <AccountConflictTask busy={busy} hasAccount={api.session.authenticated} onReturn={onClose}
    onOther={() => { setIdentifier(""); setUsername(""); changeMode("password"); }} onRecovery={() => { clearFeedback(); setRecoveryHelp(true); }} />;
  const failurePage = !!error && step !== "help" && step !== "done";
  if (!busy && ["code", "reset", "create-review"].includes(step) && (receiptExpired && !failurePage || ["otp_expired", "code_expired"].includes(errorCode)))
    return <AuthCodeExpiredTask busy={busy} wait={wait} canSend={available && consentReady}
      onSend={() => { setPassword(""); setRepeated(""); setCode(""); clearFeedback(); void otp(); }} onReturn={onClose} onBack={back} />;
  if (phoneMode && step === "help") return <AuthSmsDelayedTask busy={busy} onEdit={() => { setStep("start"); setCode(""); clearFeedback(); }}
    onReturn={onClose} onRecovery={() => setRecoveryHelp(true)} onBack={back} />;
  if (!failurePage && phoneMode && step === "start" && phoneError) return <AuthPhoneErrorTask value={identifier}
    onChange={value => { setIdentifier(value); setSent(undefined); setCode(""); clearFeedback(); }} busy={busy} agreed={agreed} onAgree={setAgreed}
    canSend={available && consentReady} wait={wait} onSend={() => void otp()} onOther={() => changeMode("password")} onBack={back} />;
  if (!failurePage && phoneMode && step === "code" && resent) return <AuthCodeResentTask address={maskedAddress(sent?.address ?? "")} value={code} onChange={setCode}
    busy={busy} canSubmit={canSubmit} onSubmit={() => void submit()} onOther={() => changeMode("password")} onBack={back} />;
  const expired = ["otp_expired", "code_expired"].includes(errorCode);
  const failureTitle = expired ? "验证码已过期" : failureKind === "rate" ? "操作有些频繁" : failureKind === "connection" ? "连接暂时中断"
    : step === "code" || step === "reset" ? "验证码暂不可用" : "尚未完成登录";
  const title = failurePage ? failureTitle : step === "help" ? phoneMode ? "暂时没有收到短信" : "验证码暂不可用"
    : step === "create-review" ? "欢迎使用 M&D" : step === "reset" ? "设置新密码" : step === "done" ? "密码已更新"
      : step === "code" ? phoneMode ? "验证手机号" : "验证邮箱"
        : mode === "register" ? "邮箱注册" : recovery ? "找回密码" : mode === "email" ? "邮箱登录 / 注册" : mode === "phone" ? "已绑定手机登录" : "密码登录";
  const actionLabel = failurePage ? failureKind === "rate" ? "换个方式" : expired ? wait ? `${wait}秒后可重发` : "重新获取" : "重新尝试"
    : step === "done" ? "返回登录" : step === "help" ? sentHere ? "重新填写验证码" : "返回填写"
    : step === "create-review" ? "继续并创建账号" : step === "reset" ? "保存新密码" : step === "code" ? recovery ? "继续设置新密码" : mode === "register" ? "核对创建账号" : "验证并继续"
      : mode === "password" ? "登录" : wait ? `${wait}秒后可重发` : phoneMode ? "发送短信验证码" : "发送邮箱验证码";
  const actionDisabled = busy || (failurePage ? expired && (wait > 0 || !available || !consentReady) : step === "help" || step === "done" ? false
    : step === "start" && mode !== "password" ? !available || !consentReady || !identifier.trim() || wait > 0 : !canSubmit);
  function primary() {
    if (failurePage) {
      if (failureKind === "rate") changeMode("password");
      else {
        clearFeedback();
        if (expired || step === "start" && mode !== "password") void otp();
        else if (step === "reset" || step === "create-review") setStep("code");
      }
    } else if (step === "done") changeMode("password");
    else if (step === "help") { setStep(sentHere ? "code" : "start"); clearFeedback(); }
    else if (step === "start" && mode !== "password") void otp();
    else void submit();
  }
  return <Modal title={title} task busy={busy} onClose={back}
    actions={<Button kind="primary" disabled={actionDisabled} onClick={primary}>{actionLabel}</Button>}>
    <div className="auth-task-content" ref={content}>
      {failurePage ? <>
        <h1>{failureTitle}</h1>
        <Notice title="可以继续">{failureKind === "rate" ? "请稍后再试，也可以选择其他登录方式。原来的内容会保留。"
          : expired ? "重新获取后填写最新验证码，无需重新开始刚才的操作。"
            : "尚未确认本次登录或验证，请核对后继续。原账号、绑定与当前任务保持，不会自动提交草稿。"}</Notice>
        <p className="field-error caption" role="alert">{error}</p>
        <Button kind="quiet" disabled={busy} onClick={onClose}>返回刚才的页面</Button>
      </> : step === "create-review" ? <>
        <FirstAccountContent method={`邮箱 · ${maskedAddress(sent?.address ?? "")}`} />
        <p className="caption muted">本次验证码和账号信息将在确认创建时由服务核对。</p>
        <Button disabled={busy} onClick={() => changeMode("password")}>我有账号，去登录</Button>
        <Button kind="quiet" disabled={busy} onClick={() => { setStep("code"); clearFeedback(); }}>返回核对邮箱与验证码</Button>
      </> : step === "done" ? <>
        <h1>原来的资料都在</h1>
        <Notice title="重新登录">使用新密码或已关联的方式，回到刚才的页面；不会自动发布或提交草稿。</Notice>
      </> : step === "help" ? <>
        <h1>未完成验证</h1>
        <Notice title="保留当前任务">验证码错误、过期、发送频繁或网络异常时保持原账号与绑定，不会自动建号、合并账号或发送草稿。</Notice>
        <RowLink title={phoneMode ? "检查已绑定号码" : "检查邮箱地址"} detail={maskedAddress(identifier)} onClick={() => { setStep("start"); setCode(""); clearFeedback(); }} />
        <Button kind="quiet" onClick={() => changeMode("password")}>换个登录方式</Button>
        <Button kind="quiet" onClick={() => setRecoveryHelp(true)}>无法使用原标识，寻求帮助</Button>
      </> : step === "reset" ? <>
        <Field label="新密码" type="password" autoComplete="new-password" value={password} onChange={setPassword} disabled={busy} hint="使用10至128字节的密码" />
        <Field label="再次确认" type="password" autoComplete="new-password" value={repeated} onChange={setRepeated} disabled={busy}
          error={repeated && repeated !== password ? "两次密码不一致" : undefined} />
        <Notice title="下次登录">可以继续使用已关联的快捷登录或新密码。保存时会一并验证刚才的原账号验证码。</Notice>
        {!code || error ? <RowLink title="重新填写验证码" detail="保留原账号和任务" disabled={busy} onClick={() => { setStep("code"); clearFeedback(); }} /> : null}
      </> : <>
        {mode !== "password" ? <h1>{step === "code" ? phoneMode ? "查看手机中的验证码" : "查看邮箱中的验证码"
          : recovery ? "恢复你原来的账号" : phoneMode ? "使用已绑定的手机号" : "使用你已验证的邮箱"}</h1> : null}
        {step === "code" ? <>
          <p className="caption">发送至 {maskedAddress(sent?.address ?? "")}</p>
          <AuthOtpField value={code} onChange={value => { setCode(value); clearFeedback(); }} disabled={busy} label={phoneMode ? "短信验证码，6位数字" : "邮箱验证码，6位数字"} />
          <RowLink title={phoneMode ? "修改手机号" : "修改邮箱"} detail="保留原任务" disabled={busy}
            onClick={() => { setStep("start"); setCode(""); setPassword(""); setRepeated(""); clearFeedback(); }} />
          <RowLink title="验证码错误 / 过期 / 发送频繁" detail="重试不会新建账号" disabled={busy} onClick={() => { setStep("help"); clearFeedback(); }} />
          <Button kind="quiet" disabled={busy || wait > 0} onClick={() => void otp()}>{wait ? `${wait}秒后可重发` : "重新获取验证码"}</Button>
          <Notice title="保护原账号">{recovery ? "验证码仅用于重设原账号密码，不新建账号。下一步设置新密码，保存时由服务端核对验证码。"
            : "验证码仅用于本次登录或明确选择的注册，不授权更换绑定或自动发布。"}</Notice>
          {mode === "register" ? <>
            <Field label="唯一账号名" autoComplete="username" value={username} onChange={setUsername} disabled={busy} />
            <Field label="密码（10至128字节）" type="password" autoComplete="new-password" value={password} onChange={setPassword} disabled={busy} />
            <Field label="再次确认" type="password" autoComplete="new-password" value={repeated} onChange={setRepeated} disabled={busy}
              error={repeated && repeated !== password ? "两次密码不一致" : undefined} />
          </> : null}
        </> : <>
          {(mode === "password" && policyReady || requiresAgreement) ? <>
            <Check checked={agreed} onChange={setAgreed} disabled={busy || !policyReady}>我已阅读并同意服务协议与隐私政策</Check>
            <LegalLinks />
          </> : null}
          <Field label={mode === "password" ? "账号名 / 邮箱 / 手机号" : phoneMode ? "已绑定手机号" : "邮箱"}
            type={emailMode ? "email" : phoneMode ? "tel" : "text"} autoComplete={emailMode ? "email" : phoneMode ? "tel" : "username"}
            value={identifier} onChange={value => { setIdentifier(value); setSent(undefined); setCode(""); clearFeedback(); }} disabled={busy} />
          {mode === "password" ? <>
            <Field label="密码" type="password" autoComplete="current-password" value={password} onChange={value => { setPassword(value); clearFeedback(); }} disabled={busy} error={error || undefined} />
            <RowLink title="忘记密码" detail="验证后设置新密码" disabled={busy} onClick={() => changeMode("email-recover")} />
            <Button kind="quiet" disabled={busy} onClick={() => changeMode("email")}>改用邮箱验证码</Button>
          </> : <>
            <Notice title={requiresAgreement ? "已有账号不会重复创建" : recovery ? "保护原账号" : "仅登录原账号"}>
              {requiresAgreement ? "登录已绑定邮箱会进入原账号；创建新账号需要你主动选择注册，不按同名昵称合并。"
                : recovery ? "使用已验证的标识恢复访问。当前登录态或新号码不能直接授权接管。" : "手机号必须已绑定；验证码不会自动新建账号或代替交流资格。"}
            </Notice>
            <RowLink title={phoneMode ? "短信未到 / 原号码不可用" : "邮件未到 / 地址填写错误"} detail="查看恢复帮助" disabled={busy}
              onClick={() => { setStep("help"); clearFeedback(); }} />
          </>}
          {!policyReady && requiresAgreement ? <Notice title="正式协议地址尚未配置">暂不开放此流程；已有账号仍可使用密码登录。</Notice> : null}
          {!available ? <Notice title={cap ? "当前方式尚未由服务启用" : capError ? "登录方式尚未读到" : "正在读取实际登录能力"} /> : null}
          <nav className="auth-method-links" aria-label="账号登录与恢复">
            <Button kind="quiet" disabled={busy} onClick={() => changeMode(mode === "password" ? "register" : "password")}>{mode === "password" ? "邮箱注册" : "返回密码登录"}</Button>
            {mode !== "phone" ? <Button kind="quiet" disabled={busy} onClick={() => changeMode("phone")}>已绑定手机登录</Button> : null}
            {recovery ? <Button kind="quiet" disabled={busy} onClick={() => changeMode(phoneMode ? "email-recover" : "recover")}>{phoneMode ? "邮箱找回密码" : "已绑定手机恢复"}</Button> : null}
          </nav>
          <div className="provider-methods" aria-label="其他登录方式">
            <span className="muted">其他登录方式</span>
            <div>{["wechat", "qq"].map(name => <Button key={name} disabled={busy} onClick={() => { setPassword(""); setRepeated(""); setCode(""); setProvider(name); }}>
              <Icon name={`provider-${name}`} />{name === "wechat" ? "微信" : "QQ"}
            </Button>)}</div>
          </div>
          <Button kind="quiet" disabled={busy} onClick={() => setInformation(["entry"])}>服务协议与隐私说明</Button>
          <Button kind="quiet" disabled={busy} onClick={() => { onClose(); navigate("/rules"); }}>阅读服务说明与规则草案</Button>
          <Button kind="quiet" disabled={busy} onClick={onClose}>继续以游客身份浏览</Button>
        </>}
      </>}
      {capError ? <Notice title="登录方式尚未读到" error>{capError}<Button disabled={busy} onClick={() => void readCapabilities()}>重新读取登录方式</Button></Notice> : null}
      {notice ? <p role="status" className="caption muted">{notice}</p> : null}
      {busy ? <p role="status">正在核对，请稍候…</p> : null}
    </div>
  </Modal>;
}
