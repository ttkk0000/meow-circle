import {useEffect, useRef, useState} from "react";
import {api, explain, HttpError, type User,} from "../core/api";
import {AccountConflictTask, LastLoginMethodTask} from "../components/AccountConflict";
import {AuthRecovery} from "./AuthRecovery";
import {AuthBindTask, AuthUnbindTask} from "./AuthBinding";
import {AuthCurrentPhone, AuthNewPhoneContent} from "./AuthPhone";
import {requestSignIn, useSession} from "../core/session";
import {reload, useQuery} from "../core/query";
import {back as goBack, navigate} from "../core/router";
import {integer, key} from "../core/wire";
import {clearDraft, type Draft, listDrafts, newDraft, saveDraft} from "../core/drafts";
import {
  Button,
  Card,
  Check,
  Field,
  Modal,
  Notice,
  PrivateImage,
  QueryState,
  RowLink,
  TaskPage,
} from "../components/ui";
import {OTPButton, PurposeCheck} from "../components/PurposeCheck";
import {workflowURL} from "./workflows";
import {ProviderSignIn} from "./ProviderSignIn";
import type {Capabilities} from "./AuthDialog";

export interface Profile {
  user: User;
  version: bigint;
  theme: string;
  background: string;
}
export function Mine({ login }: { login: () => void }) {
  const session = useSession();
  const profile = useQuery<Profile>(session.authenticated ? "profile" : null);
  const u = profile.data?.user ?? session.user;
  const avatar = integer(u?.avatar_url?.match(/\/media\/(\d+)\/content/)?.[1]);
  return (
    <TaskPage title="我的">
      <Card>
        <div className="profile-identity">
          <PrivateImage
            id={avatar || undefined}
            alt="我的头像"
            className="avatar"
          />
          <div>
            <h1>{u?.nickname || "欢迎来到 M&D"}</h1>
            <p className="muted">meow &amp; doggie</p>
          </div>
        </div>
        <p>{u?.bio || "把普通的一天，也留一份可爱。"}</p>
        {!session.authenticated ? (
          <Button kind="primary" onClick={login}>
            登录，继续我的任务
          </Button>
        ) : (
          <RowLink title="编辑个人资料" to="/profile-edit" />
        )}
      </Card>
      <Card title="我的任务">
        <RowLink title="我的发布" to="/publications" />
        <RowLink title="我的宠物" to={`/pets?owner=${u?.id ?? 0n}`} />
        <RowLink title="领养申请与交接" to="/adoptions" />
        <RowLink title="预约与到访" to="/appointments" />
        <RowLink title="关注作者与求助进展" to="/content" />
        <RowLink title="好友与宠友圈" to="/friendships" />
        <RowLink title="协作与群邀请" to="/invitations" />
      </Card>
      <Card title="设置与协助">
        <RowLink title="本机草稿与待核对操作" to="/recovery" />
        <RowLink title="设置与隐私" to="/settings" />
        <RowLink title="账号安全与交流资格" to="/security" />
        <RowLink title="儿童监护授权" to="/guardians" />
        <RowLink
          title="资料删除、正式注销与事项协助"
          to={workflowURL("data")}
        />
        <RowLink title="核实、举报与申诉" to="/reviews" />
        <RowLink title="安全与隐私求助" to={workflowURL("safety")} />
        <RowLink title="规则与权益" to="/rules" />
      </Card>
    </TaskPage>
  );
}
interface Methods {
  phone: string;
  password_set: boolean;
  linked: Array<{ id: bigint; provider: string }>;
  sms_available: boolean;
}
interface Eligibility {
  interaction: boolean;
  adult: boolean;
  phone_verified: boolean;
  age_band: string;
  reasons: string[];
  contract_version: bigint;
}
const reasons: Record<string, string> = {
  phone_verification_required: "当前手机号尚未验证",
  adult_required: "此事项需要有效成年人资格",
  guardian_consent_required: "尚缺指定范围的有效监护授权",
  age_evidence_required: "年龄情况待有效核对",
  age_unknown: "年龄情况待有效核对",
};
type SecurityView = "accounts" | "phone" | "phone-code" | "phone-saved" | "eligibility" | "methods";
export function Security() {
  const session = useSession();
  const owner = session.user?.id ?? 0n;
  const profile = useQuery<Profile>("profile");
  const methods = useQuery<Methods>("auth/methods");
  const eligibility = useQuery<Eligibility>("eligibility");
  const capabilities = useQuery<Capabilities>("auth/capabilities");
  const [view, setView] = useState<SecurityView>("accounts");
  const [provider, setProvider] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [change, setChange] = useState<string>();
  const [logoutMode, setLogoutMode] = useState<"current" | "all">();
  const [identity, setIdentity] = useState(0n);
  const [guard, setGuard] = useState({ owner: 0n, ready: false, blocked: false, error: "" });
  const pending = useRef(false);
  useEffect(() => {
    let active = true;
    setGuard({ owner, ready: false, blocked: false, error: "" });
    void listDrafts(owner).then(rows => {
      if (active) setGuard({ owner, ready: true, blocked: rows.some(row => row.uncertain && row.route.startsWith("/security?operation=")), error: "" });
    }).catch(e => { if (active) setGuard({ owner, ready: false, blocked: false, error: explain(e) }); });
    return () => { active = false; };
  }, [owner, change]);
  const loading = methods.loading || profile.loading || eligibility.loading;
  const readError = methods.error || profile.error || eligibility.error;
  const ready = !busy && !loading && !readError && !!methods.data && !!profile.data && profile.data.user.id === owner && !!eligibility.data;
  const mutationReady = ready && guard.owner === owner && guard.ready && !guard.blocked;
  function changeView(next: SecurityView) {
    if (pending.current || !ready && next !== "accounts") return;
    setView(next); setError("");
  }
  function openChange(action: string, identity = 0n) {
    if (!mutationReady) return;
    setIdentity(identity); setChange(action);
  }
  async function logout(all: boolean) {
    if (pending.current || !ready) return;
    pending.current = true; setBusy(true); setError("");
    try { await api.request("auth/logout", { method: "POST", body: { all }, owner }); navigate("/mine"); }
    catch (e) { setError(explain(e)); }
    finally { pending.current = false; setBusy(false); }
  }
  const title = view === "phone" ? "交流前验证手机号" : view === "phone-code" ? "验证当前手机号"
    : view === "phone-saved" ? "当前手机号已验证" : view === "eligibility" ? "年龄与交流资格"
      : view === "methods" ? "已关联的登录方式" : "账号与登录方式";
  const labels: Record<string, string> = { wechat: "微信", qq: "QQ", apple: "Apple", email: "邮箱" };
  const linked = methods.data?.linked ?? [];
  const phoneVerified = ready && eligibility.data?.contract_version === 1n && eligibility.data.phone_verified;
  const providers = ["wechat", "qq", ...(linked.some(row => row.provider === "apple") || capabilities.data?.providers.includes("apple") ? ["apple"] : [])];
  if ((view === "phone" || view === "phone-code") && mutationReady && !phoneVerified && methods.data)
    return <AuthCurrentPhone key={String(owner)} owner={owner} maskedPhone={methods.data.phone} smsAvailable={methods.data.sms_available} changing={false}
      onCancel={() => goBack()} onRecovery={() => navigate(workflowURL("safety"))}
      onVerified={() => { setView("phone-saved"); }} />;
  const footer = phoneVerified && (view === "phone" || view === "phone-code") || view === "phone-saved" ? <Button kind="primary" disabled={busy} onClick={goBack}>返回原任务</Button>
        : <Button kind="primary" disabled={busy} onClick={() => view === "accounts" ? navigate("/settings") : changeView("accounts")}>{view === "accounts" ? "返回设置" : "返回账号设置"}</Button>;
  return <TaskPage title={title} actions={footer} onBack={view !== "accounts" ? () => {
    if (busy) return;
    if (view === "phone-saved") goBack();
    else changeView(view === "phone-code" ? "phone" : "accounts");
  } : undefined}>
    <QueryState loading={loading} error={readError} failure={methods.failure || profile.failure || eligibility.failure}
      onRetry={() => { void reload("profile"); void reload("auth/methods"); void reload("eligibility"); }}>
      {view === "accounts" ? <>
        <Card><div className="profile-identity security-identity">
          <PrivateImage id={integer(profile.data?.user.avatar_url?.match(/\/media\/(\d+)\/content/)?.[1]) || undefined} alt="当前账号头像" className="security-avatar" />
          <div><strong>{profile.data?.user.nickname || profile.data?.user.username}</strong><p className="caption muted">同一个 M&amp;D 账号</p></div>
        </div></Card>
        <RowLink title="手机号" detail={methods.data?.phone || "尚未绑定"} disabled={!mutationReady} onClick={() => openChange("phone")} />
        <RowLink title="邮箱" detail="绑定或更换已验证邮箱" disabled={!mutationReady} onClick={() => openChange("email")} />
        {providers.map(name => <RowLink key={name} title={labels[name]!} detail={linked.some(row => row.provider === name) ? "已关联" : "未关联"}
          disabled={!ready} onClick={() => changeView("methods")} />)}
        <RowLink title="密码与恢复" detail={methods.data?.password_set ? "更新密码或恢复登录" : "设置密码与保留恢复方式"} disabled={!mutationReady} onClick={() => openChange("password")} />
        <RowLink title="当前手机号与交流资格" detail="登录、年龄与交流分别核对" disabled={!ready} onClick={() => changeView("eligibility")} />
        <RowLink title="资料删除、注销与协助" detail="先查看影响，再核对用途" to={workflowURL("data")} />
        <RowLink title="退出当前账号" detail="保留公共浏览" disabled={!ready} onClick={() => { setError(""); setLogoutMode("current"); }} />
        <Button kind="quiet" disabled={!ready} onClick={() => { setError(""); setLogoutMode("all"); }}>退出全部会话</Button>
      </> : view === "phone" || view === "phone-code" ? <>
        {phoneVerified ? <Notice title="无需重复收取短信">服务已确认当前号码验证。业务权限、年龄与监护仍需分别核对；返回后不会自动发布。</Notice>
          : <Notice title="当前验证尚未就绪">请先核对账号与本机原操作，再继续验证当前手机号。</Notice>}
      </> : view === "phone-saved" ? <>
        <h1>验证已由服务确认</h1>
        <Notice title="返回原任务继续核对">当前手机号验证不代替年龄、监护、城市准入或业务授权；回到原任务后仍需主动确认，不自动发布或发送。</Notice>
      </> : view === "eligibility" ? <>
        <Notice title="登录与业务资格分别检查">年龄信息来自服务核实；这里不会将自行勾选成年当成验证。手机号、监护授权与具体事项分别核对。</Notice>
        <p>当前手机号：{eligibility.data?.contract_version === 1n && eligibility.data.phone_verified ? "服务已确认验证" : "尚未确认有效"}</p>
        <p>交流资格：{eligibility.data?.contract_version === 1n && eligibility.data.interaction ? "当前可用" : "尚未满足条件"}</p>
        {eligibility.data?.reasons.map((reason, index) => <p className="caption muted" key={index}>{reasons[reason] ?? "请按服务端要求继续核对资格"}</p>)}
        <RowLink title="核对当前手机号" detail="保留原任务，不自动提交" disabled={!ready} onClick={() => changeView("phone")} />
        <RowLink title="年龄与使用范围" detail="年龄、监护与成年业务" to="/age" />
        <RowLink title="监护授权与管理" detail="邀请、独立核对与撤回" to="/guardians" />
        <RowLink title="未成年人保护设置" detail="陌生人联系、屏蔽与资料求助" to="/guardian-protection" />
        <RowLink title="申请核实与事项协助" to={workflowURL("review")} />
      </> : <>
        <RowLink title="关联手机号" detail="核对本次新号码与原账号" disabled={!mutationReady} onClick={() => openChange("phone")} />
        <RowLink title="关联邮箱" detail="核对本次邮箱与原账号" disabled={!mutationReady} onClick={() => openChange("email")} />
        {linked.map(link => <RowLink key={String(link.id)} title={labels[link.provider] ?? "已关联的方式"} detail="已关联 · 解绑前核对当前账号与影响" disabled={!mutationReady} onClick={() => openChange("unlink", link.id)} />)}
        {!linked.length ? <Notice title="尚无已关联的快捷方式">可以选择当前可用的官方授权；相同昵称或头像不会自动合并账号。</Notice> : null}
        {providers.map(name => <RowLink key={name} title={`关联${labels[name]}`} detail="官方授权后核对当前账号与本次关联" disabled={!mutationReady} onClick={() => setProvider(name)} />)}
        <Notice title="保护最后一种登录方式">关联与解绑均需主动确认和本次用途验证，不能移除最后一种可用登录方式。</Notice>
      </>}
      {guard.blocked || guard.error ? <Notice title="先核对本机原操作" error={!!guard.error}>{guard.error || "仍有账号变更结果待核对，不能创建新编号重复提交。"}<RowLink title="查看原操作与草稿" to="/recovery" /></Notice> : null}
      {error ? <Notice title={error} error /> : null}
      {change && profile.data?.user.id === owner && session.user ? <AccountChange key={`${owner}:${change}:${identity}`} action={change} identity={identity} profile={profile.data} methods={methods.data} onClose={() => setChange(undefined)}
        onAddMethod={() => { setChange(undefined); changeView("methods"); }} /> : null}
      {provider && session.user ? <ProviderSignIn provider={provider} capabilities={capabilities.data} onBack={() => setProvider("")}
        onClose={() => { setProvider(""); void reload("auth/methods"); void reload("profile"); }} /> : null}
    </QueryState>
    {logoutMode ? <Modal title={logoutMode === "all" ? "退出全部会话" : "退出当前账号"} task busy={busy}
      onClose={() => { if (!pending.current) setLogoutMode(undefined); }}
      actions={<Button kind="primary" disabled={!ready} onClick={() => void logout(logoutMode === "all")}>{busy ? "正在退出" : "确认退出"}</Button>}>
      <div className="auth-task-content">
        <Notice title="退出后">可继续浏览公开内容。当前账号的私密内容将从本机隐藏，已保存的草稿按账号保留。未保存的输入请先处理后退出。</Notice>
        {logoutMode === "all" ? <Notice title="全部设备">此账号在其他设备的会话也会失效，需要重新登录。</Notice> : null}
        <Button disabled={busy} onClick={() => setLogoutMode(undefined)}>继续使用</Button>
        {error ? <Notice title={error} error /> : null}
      </div>
    </Modal> : null}
  </TaskPage>;
}
function AccountChange({
  action,
  identity,
  profile,
  methods,
  onClose,
  onAddMethod,
}: {
  action: string;
  identity: bigint;
  profile: Profile;
  methods?: Methods;
  onClose: () => void;
  onAddMethod: () => void;
}) {
  const session = useSession();
  const epoch = useRef(api.epoch).current;
  const active = useRef(true);
  useEffect(() => { active.current = true; return () => { active.current = false; }; }, []);
  const [value, setValue] = useState("");
  const [repeated, setRepeated] = useState("");
  const [code, setCode] = useState("");
  const [error, setError] = useState("");
  const [errorCode, setErrorCode] = useState("");
  const [scope, setScope] = useState<Record<string, unknown>>();
  const [grant, setGrant] = useState("");
  const [confirmed, setConfirmed] = useState(false);
  const [busy, setBusy] = useState(false);
  const [request] = useState(key);
  const [uncertain, setUncertain] = useState(false);
  const [recoveryHelp, setRecoveryHelp] = useState(false);
  const [currentPhoneRead, setCurrentPhoneRead] = useState(action !== "phone" || !methods?.phone);
  const [newPhoneReady, setNewPhoneReady] = useState(false);
  const pending = useRef(false);
  const currentOwner = () => active.current && api.epoch === epoch && api.session.user?.id === profile.user.id;
  const fields = () => ({
    version: profile.version,
    client_request_id: request,
    phone: action === "phone" ? value : "",
    email: action === "email" ? value : "",
    password: action === "password" ? value : "",
    identity_id: identity,
    code,
  });
  async function prepare() {
    if (pending.current || uncertain || !currentOwner()) return;
    pending.current = true;
    setBusy(true);
    setError("");
    try {
      const drafts = await listDrafts(profile.user.id);
      if (drafts.some(row => row.uncertain && row.route.startsWith("/security?operation=")))
        throw Error("仍有账号变更结果待核对，请先到恢复中心查询原编号。");
      const bytes = new TextEncoder().encode(value).length;
      if (action === "password" && (bytes < 10 || bytes > 128 || value !== repeated))
        throw Error("请核对10至128字节的新密码，两次填写应一致。");
      if ((action === "phone" || action === "email") && !code.trim()) throw Error("请填写新联系方式的本次验证码。");
      if (action === "phone" && (!currentPhoneRead || !newPhoneReady)) throw Error("请先核对当前号码，再填写新号码的本次有效验证码。");
      if (!currentOwner()) return;
      const result = await api.request<Record<string, unknown>>(
        `auth/account/${action}/scope`,
        { method: "POST", body: fields(), owner: profile.user.id },
      );
      if (!currentOwner()) return;
      setScope(result);
      setGrant("");
      setConfirmed(false);
    } catch (e) {
      setError(explain(e));
      setErrorCode(e instanceof HttpError ? e.code : "");
    } finally {
      pending.current = false;
      setBusy(false);
    }
  }
  async function submit(userConfirmed = confirmed) {
    if (pending.current || !scope || !grant || !userConfirmed || uncertain || !currentOwner()) return;
    pending.current = true;
    setBusy(true);
    let submitted: Draft | undefined;
    try {
      const draft = newDraft(
        profile.user.id,
        `/security?operation=${request}`,
        {
          action,
          phone: action === "phone" ? value : "",
          email: action === "email" ? value : "",
          identity_id: identity,
        },
        profile.version,
      );
      draft.requestId = request;
      draft.uncertain = true;
      draft.original = {
        path: `auth/account/${action}`,
        method: "POST",
        body: "{}",
        replayable: false,
      };
      const saved = await saveDraft(draft);
      submitted = saved;
      if (!currentOwner()) { await clearDraft(saved); return; }
      setUncertain(true);
      await api.request(`auth/account/${action}`, {
        method: "POST",
        body: { ...fields(), reauth_token: grant },
        owner: profile.user.id,
      });
      const completedEpoch = api.epoch;
      await clearDraft(saved);
      if (api.epoch !== completedEpoch || api.session.authenticated && !currentOwner()) return;
      onClose();
      navigate("/mine");
    } catch (e) {
      setError(explain(e));
      setErrorCode(e instanceof HttpError ? e.code : "");
      if (e instanceof HttpError && ["identity_in_use", "last_login_method"].includes(e.code) && submitted) {
        try {
          await clearDraft(submitted);
          setUncertain(false); setScope(undefined); setConfirmed(false);
        } catch { setError("服务已拒绝本次变更，本机原请求记录仍需核对；请在恢复中心处理后再继续。"); }
      }
    } finally {
      setValue("");
      setRepeated("");
      setCode("");
      setGrant("");
      setBusy(false);
      pending.current = false;
    }
  }
  if (session.user?.id !== profile.user.id || api.epoch !== epoch) return null;
  if (recoveryHelp) return <AuthRecovery onBack={() => setRecoveryHelp(false)} onClose={onClose}
    onSupplement={route => { onClose(); navigate(route); }} />;
  if (!uncertain && action === "unlink" && errorCode === "last_login_method") return <LastLoginMethodTask busy={busy} onReturn={onClose} onAdd={onAddMethod} />;
  if (!uncertain && errorCode === "identity_in_use") return <AccountConflictTask busy={busy} hasAccount={api.session.authenticated} onReturn={onClose}
    onOther={() => { onClose(); requestSignIn(); }} onRecovery={() => setRecoveryHelp(true)} />;
  if (action === "phone" && !currentPhoneRead && !uncertain) return <AuthCurrentPhone owner={profile.user.id} maskedPhone={methods?.phone || ""}
    smsAvailable={!!methods?.sms_available} changing onCancel={onClose} onRecovery={() => setRecoveryHelp(true)}
    onVerified={() => { setCurrentPhoneRead(true); setCode(""); }} />;
  if (scope && grant && (!uncertain || busy) && api.session.user?.id === profile.user.id) {
    if (action === "unlink") return <AuthUnbindTask methods={methods} identity={identity} busy={busy} onKeep={onClose} onConfirm={() => void submit(true)} />;
    if (action === "phone" || action === "email") return <AuthBindTask user={profile.user} method={action === "phone" ? "手机号" : "邮箱"} destination={value}
      replacing={action === "phone" ? !!methods?.phone : !!methods?.linked.some(row => row.provider === "email")}
      busy={busy} onCancel={onClose} onConfirm={() => void submit(true)} />;
  }
  return (
    <Modal
      title={scope ? "核对敏感账号变更" : action === "phone" ? "验证新手机号" : action === "email" ? "绑定／更换邮箱" : action === "password" ? "设置新密码" : "解绑登录方式"}
      task
      busy={busy}
      onClose={() => {
        if (busy) return;
        if (scope && !uncertain) { setScope(undefined); setGrant(""); setConfirmed(false); }
        else onClose();
      }}
      actions={
        scope ? (
          <Button
            kind="primary"
            disabled={busy || !confirmed || !grant || uncertain}
            onClick={() => void submit()}
          >
            确认此次变更
          </Button>
        ) : (
          <Button
            kind="primary"
            disabled={busy || uncertain || (action !== "unlink" && !value) || (action === "password" && (new TextEncoder().encode(value).length < 10 || new TextEncoder().encode(value).length > 128 || value !== repeated)) || (["phone", "email"].includes(action) && !code) || (action === "phone" && !newPhoneReady)}
            onClick={() => void prepare()}
          >
            核对本次用途
          </Button>
        )
      }
    >
      {uncertain ? (
        <Notice title="原变更结果待核对">
          请先重新登录后到恢复中心查询原编号 {request}，不能创建新编号重复提交。
          <Button onClick={() => navigate("/recovery")}>核对原操作</Button>
        </Notice>
      ) : null}
      {!scope ? (
        action === "phone" ? <AuthNewPhoneContent owner={profile.user.id} phone={value} code={code} smsAvailable={!!methods?.sms_available} busy={busy || uncertain}
          replacing={!!methods?.phone} onPhone={setValue} onCode={setCode} onReady={setNewPhoneReady} /> :
        <>
          <Card><div className="security-facts"><div><span>原账号</span><p>{profile.user.username}</p></div>
            {action === "phone" ? <div><span>原号码</span><p>{methods?.phone || "尚未绑定"}</p></div> : null}
            <p className="caption muted">保持同一 M&amp;D 账号，不创建新账号。</p>
          </div></Card>
          <Notice title="当前账号安全验证">核对目标后仍需验证本次账号用途；普通登录态或新联系方式不能直接授权接管。</Notice>
          {action !== "unlink" ? (
            <Field
              label={
                action === "password"
                  ? "新密码（10–128字节）"
                  : action === "phone"
                    ? "新的完整手机号"
                    : "新的邮箱"
              }
              type={action === "password" ? "password" : action === "email" ? "email" : "tel"}
              value={value}
              onChange={(value) => {
                setValue(value);
                setCode("");
              }}
              disabled={busy || uncertain}
            />
          ) : (
            <p>
              解绑登录方式 #{String(identity)}
              ；服务端会阻止移除最后一个可登录方式。
            </p>
          )}
          {action === "password" ? <Field label="再次确认" type="password" autoComplete="new-password" value={repeated} onChange={setRepeated} disabled={busy || uncertain}
            error={repeated && repeated !== value ? "两次密码不一致" : undefined} /> : null}
          {["phone", "email"].includes(action) ? (
            <>
              <OTPButton
                path={action === "email" ? "auth/email-otp" : "auth/otp"}
                body={() =>
                  action === "email"
                    ? { email: value, purpose: "email_bind" }
                    : { phone: value, purpose: "bind_phone" }
                }
                disabled={busy || uncertain || !value}
                onError={setError}
              />
              <Field
                label="新方式的验证码"
                value={code}
                onChange={setCode}
                disabled={busy || uncertain}
              />
            </>
          ) : null}
          <RowLink title="原号码已无法使用" detail="独立恢复，不跳过核对" to={workflowURL("safety")} disabled={busy} />
          <Notice title="成功后重新核对资格">只有验证与确认后的真实回执才表示更新；失败保持原绑定与资料。旧会话将失效，请重新登录。</Notice>
        </>
      ) : (
        <>
          <Notice title={`账号 #${profile.user.id} · 版本 ${profile.version}`}>
            目标：
            {action === "password"
              ? "设置新密码"
              : action === "unlink"
                ? `解绑方式 #${identity}`
                : value}
            。成功后所有旧会话将失效。
          </Notice>
          <PurposeCheck scope={scope} busy={busy} onGrant={setGrant} confirmation={["phone", "email", "unlink"].includes(action) ? "explicit" : "checkbox"} />
          <Check checked={confirmed} onChange={setConfirmed} disabled={busy}>
            我已核对当前账号、目标方式与此次影响
          </Check>
          <Button
            kind="quiet"
            disabled={busy || uncertain}
            onClick={() => {
              setScope(undefined);
              setGrant("");
              setConfirmed(false);
            }}
          >
            返回修改，重新核对
          </Button>
        </>
      )}
      {error ? <Notice title={error} error /> : null}
    </Modal>
  );
}
