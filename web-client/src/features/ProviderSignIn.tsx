import {useEffect, useRef, useState} from "react";
import {api, explain, HttpError, type User} from "../core/api";
import {useSession} from "../core/session";
import {type ProviderCallback, providerConfiguration, providerPopup, ProviderPopupError,} from "../core/provider";
import {Button, Card, Check, Field, Icon, Modal, Notice, PrivateImage, RowLink,} from "../components/ui";
import {LegalLinks, privacyURL, termsURL} from "../components/AuthPolicies";
import {integer} from "../core/wire";
import {AccountConflictTask} from "../components/AccountConflict";
import {AuthRecovery} from "./AuthRecovery";
import type {Capabilities} from "./AuthDialog";
import {AuthInformation, FirstAccountContent} from "./AuthInformation";
import {navigate} from "../core/router";

type Stage =
  | "consent"
  | "authorizing"
  | "verifying"
  | "choice"
  | "create-review"
  | "cancelled"
  | "expired"
  | "unavailable"
  | "conflict"
  | "existing"
  | "link-review"
  | "link-ready"
  | "unknown"
  | "failed";
interface ProviderResult {
  login?: { user: User; created?: boolean };
  needs_account_choice: boolean;
  resolution_token: string;
}

export function ProviderCallbackPage({
  result,
}: {
  result?: ProviderCallback;
}) {
  useEffect(() => {
    if (!result || typeof BroadcastChannel === "undefined") return;
    const channel = new BroadcastChannel(`mnd-provider-${result.state}`);
    channel.postMessage(result);
    const timer = setTimeout(() => {
      channel.close();
      window.close();
    }, 300);
    return () => {
      clearTimeout(timer);
      channel.close();
    };
  }, [result]);
  return (
    <main className="provider-callback">
      <h1>M&amp;D 官方授权回跳</h1>
      <p>
        {result
          ? "本次官方回跳已发送，请回原窗口继续由服务端核验。"
          : "没有可核对的当前授权，请返回原任务重新发起。"}
      </p>
    </main>
  );
}

/** 账号选择、关联用途验证与最终确认是三个主动步骤，不以返回邮箱自动合并。 */
export function ProviderSignIn({
  provider,
  onBack,
  onClose,
  capabilities,
  onManageAccount,
  onAuthenticated,
}: {
  provider: string;
  onBack: () => void;
  onClose: () => void;
  capabilities?: Capabilities;
  onManageAccount?: () => void;
  onAuthenticated?: (created: boolean) => void;
}) {
  const configuration = providerConfiguration(provider);
  const session = useSession();
  const [stage, setStage] = useState<Stage>("consent");
  const [information, setInformation] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [agreed, setAgreed] = useState(false);
  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [code, setCode] = useState("");
  const [sent, setSent] = useState<{ address: string; purpose: string }>();
  const [phone, setPhone] = useState("");
  const [channel, setChannel] = useState("phone");
  const [recoveryHelp, setRecoveryHelp] = useState(false);
  const [until, setUntil] = useState(0);
  const [now, setNow] = useState(Date.now());
  const mounted = useRef(true);
  const working = useRef(false);
  const owner = useRef(session.user?.id ?? 0n);
  const expectingLogin = useRef(false);
  const resolution = useRef("");
  const proof = useRef("");
  const challenge = useRef("");
  const generation = useRef(0);
  const cancelPopup = useRef<(() => void) | undefined>(undefined);
  const name = provider === "wechat" ? "微信" : provider === "apple" ? "Apple" : "QQ";
  function clear() {
    generation.current++;
    resolution.current = "";
    proof.current = "";
    challenge.current = "";
    cancelPopup.current?.();
    cancelPopup.current = undefined;
  }
  function completed(created: boolean) {
    clear();
    if (onAuthenticated) onAuthenticated(created); else onClose();
  }
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
      clear();
    };
  }, []);
  useEffect(() => {
    const next = session.user?.id ?? 0n;
    if (next !== owner.current && !expectingLogin.current) {
      clear();
      onBack();
    }
  }, [session.user?.id, onBack]);
  useEffect(() => {
    if (until <= Date.now()) return;
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, [until]);
  async function ownLogin<T>(action: () => Promise<T>): Promise<T> {
    expectingLogin.current = true;
    try {
      const value = await action();
      owner.current = api.session.user?.id ?? 0n;
      return value;
    } finally {
      expectingLogin.current = false;
      if (owner.current !== (api.session.user?.id ?? 0n)) {
        clear();
        onBack();
      }
    }
  }
  async function run(action: () => Promise<void>, uncertain = false) {
    if (working.current) return;
    working.current = true;
    setBusy(true);
    setError("");
    try {
      await action();
    } catch (failure) {
      if (
        mounted.current &&
        !(failure instanceof DOMException && failure.name === "AbortError")
      ) {
        setError(explain(failure));
        if (
          failure instanceof HttpError &&
          failure.code === "identity_in_use"
        ) {
          clear();
          setStage("conflict");
        }
        if (
          uncertain &&
          !(
            failure instanceof HttpError &&
            [400, 401, 403, 404, 409, 422, 429].includes(failure.status)
          )
        ) {
          proof.current = "";
          setStage("unknown");
        }
      }
    } finally {
      working.current = false;
      if (mounted.current) setBusy(false);
    }
  }
  function leave(after: () => void) {
    if (working.current && stage !== "authorizing") return;
    const original = challenge.current;
    clear();
    setPassword("");
    setCode("");
    after();
    if (original)
      void api
        .request("auth/oauth-finish", {
          method: "POST",
          body: { state: original, credential: {}, cancelled: true },
        })
        .catch(() => {});
  }
  function close() { leave(onBack); }
  function accountChoice() {
    if (working.current) return;
    proof.current = ""; setPassword(""); setCode(""); setPhone(""); setSent(undefined); setError(""); setStage("choice");
  }
  function start() {
    if (
      !configuration ||
      !capabilities?.providers?.includes(provider) ||
      typeof BroadcastChannel === "undefined" ||
      working.current
    )
      return;
    // 在用户手势内先取得窗口；其opener立即清空，通信仅走同源随机挑战通道。
    const popup = window.open(
      "about:blank",
      "_blank",
      "popup,width=520,height=720",
    );
    if (!popup) {
      setStage("unavailable");
      setError("浏览器阻止了授权窗口，请允许本次弹窗后重试");
      return;
    }
    popup.opener = null;
    popup.document.title = "M&D 官方授权";
    clear();
    const attempt = generation.current;
    setStage("authorizing");
    void run(async () => {
      try {
        const ticket = await api.request<{ state: string; nonce: string }>(
          "auth/oauth-start",
          { method: "POST", body: { provider } },
        );
        if (!mounted.current || attempt !== generation.current) {
          popup.close();
          void api
            .request("auth/oauth-finish", {
              method: "POST",
              body: { state: ticket.state, credential: {}, cancelled: true },
            })
            .catch(() => {});
          return;
        }
        challenge.current = ticket.state;
        const operation = providerPopup(
          provider,
          configuration,
          ticket.state,
          popup,
        );
        cancelPopup.current = operation.cancel;
        const result = await operation.result;
        if (!mounted.current || attempt !== generation.current) return;
        cancelPopup.current = undefined;
        setStage("verifying");
        const value = await ownLogin(() =>
          api.request<ProviderResult>("auth/oauth-finish", {
            method: "POST",
            body: {
              state: ticket.state,
              credential: {
                code: result.code,
                app_id: configuration.appID,
                client: "web",
              },
              cancelled: result.cancelled,
            },
          }),
        );
        challenge.current = "";
        if (!mounted.current) return;
        if (result.cancelled) {
          setStage("cancelled");
          setError("已取消本次官方授权，没有创建或关联账号");
        } else if (value.login) {
          completed(value.login.created === true);
        } else if (value.needs_account_choice && value.resolution_token) {
          resolution.current = value.resolution_token;
          setStage("choice");
        } else {
          setStage("failed");
          setError("未取得可用的授权结果，请重新发起");
        }
      } catch (failure) {
        popup.close();
        if (mounted.current && attempt === generation.current)
          setStage(
            failure instanceof ProviderPopupError ? failure.kind : failure instanceof HttpError && failure.status === 401
              ? "expired"
              : "failed",
          );
        throw failure;
      }
    });
  }
  async function sendCode(reauth = false) {
    const email = !reauth && channel === "email";
    const address = (reauth ? phone : identifier).trim();
    const purpose = reauth ? "reauth" : email ? "email_login" : "login";
    const result = await api.request<{ retry_after_seconds: bigint }>(
      email ? "auth/email-otp" : "auth/otp",
      {
        method: "POST",
        body: email
          ? { email: address, purpose }
          : {
              phone: address,
              purpose,
            },
      },
    );
    if (!mounted.current) return;
    setSent({ address, purpose }); setCode("");
    setUntil(
      Date.now() + Math.max(1, Number(result.retry_after_seconds)) * 1000,
    );
  }
  const waiting = Math.max(0, Math.ceil((until - now) / 1000));
  const existingOtpReady = !!code.trim() && sent?.address === identifier.trim() && sent.purpose === (channel === "email" ? "email_login" : "login");
  const linkOtpReady = !!code.trim() && sent?.address === phone.trim() && sent.purpose === "reauth";
  const available =
    !!configuration &&
    !!capabilities?.providers?.includes(provider) &&
    typeof BroadcastChannel !== "undefined";
  if (information) return <AuthInformation page="providers" onBack={() => setInformation(false)} onPage={() => {}}
    onExit={close} onManage={() => {
      clear();
      if (onManageAccount) onManageAccount();
      else { if (api.session.authenticated) onClose(); else onBack(); navigate("/security"); }
    }} />;
  const unavailable = stage === "unavailable" || stage === "consent" &&
    (!configuration || typeof BroadcastChannel === "undefined" || !!capabilities && !capabilities.providers.includes(provider));
  if ((session.user?.id ?? 0n) !== owner.current) return null;
  if (recoveryHelp) return <AuthRecovery key={api.epoch} onBack={() => setRecoveryHelp(false)} onClose={() => leave(onClose)}
    onSupplement={route => { leave(onClose); navigate(route); }} />;
  if (stage === "conflict") return <AccountConflictTask busy={busy} hasAccount={session.authenticated} onReturn={() => leave(onClose)}
    onOther={close} onRecovery={() => { clear(); setRecoveryHelp(true); }} />;
  if (unavailable || stage === "cancelled" || stage === "expired") {
    const title = unavailable ? "暂时无法使用这种方式" : stage === "cancelled" ? "已取消授权" : "授权已失效";
    return <Modal title={title} task busy={busy} onClose={close}
      actions={<Button kind="primary" disabled={busy} onClick={close}>{unavailable ? "选择其他方式" : stage === "cancelled" ? "返回登录" : "重新登录"}</Button>}>
      <div className="auth-task-content">
        <h1>{title}</h1>
        <Notice title="可以继续">{unavailable ? "检查对应应用是否可用，也可以改用已绑定手机号或其他方式。"
          : stage === "cancelled" ? "本次授权已取消，没有创建或关联账号。刚才正在看的页面和可保留的草稿仍在。"
            : "请重新发起授权。这次不会自动创建账号，原任务和可保留的草稿仍在。"}</Notice>
        <Button kind="quiet" disabled={busy} onClick={() => leave(onClose)}>返回刚才的页面</Button>
      </div>
    </Modal>;
  }
  const createAction = (
    <Button
      kind="primary"
      disabled={!agreed || !termsURL || !privacyURL || busy}
      onClick={() =>
        void run(async () => {
          const result = await ownLogin(() =>
            api.request<{ created?: boolean }>("auth/oauth-resolve", {
              method: "POST",
              body: {
                token: resolution.current,
                create: true,
                accept_terms: true,
              },
            }),
          );
          completed(result?.created === true);
        }, true)
      }
    >
      确认创建新账号
    </Button>
  );
  const existingAction = (
    <Button
      kind="primary"
      disabled={
        busy ||
        !identifier.trim() ||
        (channel === "password" ? !password || !capabilities?.password : !existingOtpReady || !(channel === "email" ? capabilities?.email : capabilities?.sms))
      }
      onClick={() =>
        void run(async () => {
          const value = identifier.trim();
          const secret = password;
          const otp = code;
          setPassword("");
          setCode("");
          const body =
            channel === "password"
              ? {
                  ...(/^\+?\d{6,}$/.test(value)
                    ? { phone: value }
                    : value.includes("@")
                      ? { email: value }
                      : { username: value }),
                  password: secret,
                }
              : channel === "email"
                ? { email: value, code: otp }
                : { phone: value, code: otp };
          await ownLogin(() =>
            api.request(
              `auth/${channel === "password" ? "password" : channel}`,
              { method: "POST", body },
            ),
          );
          setStage("link-review");
        })
      }
    >
      验证原账号
    </Button>
  );
  const verifyAction = (
    <Button
      kind="primary"
      disabled={busy || (!password && !linkOtpReady)}
      onClick={() =>
        void run(async () => {
          const secret = password;
          const otp = linkOtpReady ? code : "";
          setPassword("");
          setCode("");
          const scope = await api.request("auth/oauth-link-scope", {
            method: "POST",
            body: { token: resolution.current },
          });
          const result = await api.request<{ reauth_token: string }>(
            "auth/reauth",
            { method: "POST", body: { scope, password: secret, code: otp } },
          );
          setPassword("");
          setCode("");
          proof.current = result.reauth_token;
          setStage("link-ready");
        })
      }
    >
      验证本次关联用途
    </Button>
  );
  const linkAction = (
    <Button
      kind="primary"
      disabled={busy || !proof.current || !session.authenticated || !session.user}
      onClick={() =>
        void run(async () => {
          const reauth = proof.current;
          if (!reauth || owner.current !== (api.session.user?.id ?? 0n)) return;
          proof.current = "";
          const result = await ownLogin(() =>
            api.request<{ created?: boolean }>("auth/oauth-resolve", {
              method: "POST",
              body: {
                token: resolution.current,
                create: false,
                reauth_token: reauth,
              },
            }),
          );
          completed(result?.created === true);
        }, true)
      }
    >
      确认关联
    </Button>
  );
  const authorizeAction = (
    <Button kind="primary" disabled={!available || busy} onClick={start}>
      {stage === "consent" ? "同意并进行本次官方授权" : "重新官方授权核对账号"}
    </Button>
  );
  const primary =
    stage === "create-review"
      ? createAction
      : stage === "existing"
        ? existingAction
        : stage === "link-review"
          ? verifyAction
          : stage === "link-ready"
            ? linkAction
            : ["consent", "failed", "unknown"].includes(
                  stage,
                )
              ? authorizeAction
              : null;
  return (
    <Modal
      task
      title={
        stage === "choice"
          ? "继续使用原账号"
          : stage === "create-review"
            ? "欢迎使用 M&D"
            : stage === "existing"
              ? "关联已有账号"
              : stage === "link-ready"
                ? "确认账号关联"
              : stage === "link-review"
                ? "验证本次关联用途"
                : `${name}官方授权`
      }
      busy={busy && stage !== "authorizing"}
      onClose={["existing", "link-review", "link-ready"].includes(stage) ? accountChoice : close}
      actions={
        <>
          {primary}
          {!["existing", "link-ready"].includes(stage) ? <Button
            kind={stage === "choice" ? "secondary" : "quiet"}
            disabled={busy && stage !== "authorizing"}
            onClick={close}
          >
            {stage === "authorizing" ? "取消本次授权" : "返回登录方式"}
          </Button> : null}
        </>
      }
    >
      <div className="auth-task-content">
      {["consent", "failed", "unknown"].includes(
        stage,
      ) ? (
        <>
          <Notice
            title={
              stage === "unknown"
                ? "本次处理结果待核对"
                : available
                      ? "仅本次官方授权"
                      : "暂时无法使用这种方式"
            }
          >
            {stage === "unknown"
              ? "不会自动重发创建或关联。重新官方授权用于核对已关联账号；新身份仍需主动选择。"
              : "将在官方站点完成授权，回到本应用后再由服务端核验；不会依据昵称、头像或返回邮箱自动合并账号。"}
          </Notice>
          <Icon name={`provider-${provider}`} />
          {stage === "consent" ? (
            <>
            <RowLink title="第三方登录信息" detail="查看 SDK 与提供方范围" disabled={busy} onClick={() => setInformation(true)} />
            <p className="caption">
              <a
                href={
                  provider === "wechat"
                    ? "https://weixin.qq.com/cgi-bin/readtemplate?lang=zh_CN&t=weixin_agreement&s=privacy"
                    : "https://wiki.connect.qq.com/qq互联sdk隐私保护声明"
                }
                target="_blank"
                rel="noreferrer noopener"
              >
                阅读{name}隐私说明
              </a>
            </p>
            </>
          ) : null}
          {stage === "failed" ? (
            <Button
              kind="quiet"
              onClick={() => {
                clear();
                onClose();
              }}
            >
              返回刚才的页面
            </Button>
          ) : null}
        </>
      ) : null}
      {stage === "verifying" ? (
        <Notice title="正在核验官方结果">
          请保留本次任务，核验成功后再选择账号，不会自动创建或关联。
        </Notice>
      ) : null}
      {stage === "authorizing" ? (
        <Notice title="等待官方授权回跳">
          请在官方窗口完成或取消。本窗口保留原任务；关闭官方窗口后可主动返回登录方式。
        </Notice>
      ) : null}
      {stage === "choice" ? (
        <>
          <Notice title="新身份需要主动选择">
            相同昵称、头像和第三方返回邮箱不作为合并依据。
          </Notice>
          <RowLink
            title={"已有 M&D 账号，去关联"}
            detail="先验证原账号，再确认本次用途"
            onClick={() =>
              setStage(api.session.authenticated ? "link-review" : "existing")
            }
          />
          {!session.authenticated ? (
            <RowLink
              title="继续创建新账号"
              detail="确认协议与年龄范围后建立"
              onClick={() => setStage("create-review")}
            />
          ) : null}
          <Notice title="不会自动合并">
            相同昵称、头像或第三方返回邮箱不作为合并依据。
          </Notice>
        </>
      ) : null}
      {stage === "create-review" ? (
        <>
          <FirstAccountContent method={name} />
          <LegalLinks />
          <Check
            checked={agreed}
            onChange={setAgreed}
            disabled={!termsURL || !privacyURL || busy}
          >
            我已阅读当前协议，主动创建新账号
          </Check>
          <p className="caption muted">新账号不会自动取得交流或成年人资格。</p>
          <Button disabled={busy} onClick={() => setStage(api.session.authenticated ? "link-review" : "existing")}>我有账号，去关联</Button>
        </>
      ) : null}
      {stage === "existing" ? (
        <>
          <h1>把资料留在原来的账号</h1>
          <Notice title={"验证你已有的M&D账号"}>关联后继续使用原有宠物、日记和聊天，不按昵称或头像自动合并。</Notice>
          <Field
            label={
              channel === "phone"
                ? "原账号手机号"
                : channel === "email"
                  ? "原账号邮箱"
                  : "账号名、邮箱或手机号"
            }
            value={identifier}
            type={channel === "phone" ? "tel" : channel === "email" ? "email" : "text"}
            autoComplete={channel === "phone" ? "tel" : channel === "email" ? "email" : "username"}
            onChange={value => { setIdentifier(value); setCode(""); setSent(undefined); }}
            disabled={busy}
          />
          {channel === "password" ? (
            <Field
              label="原账号密码"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={setPassword}
              disabled={busy}
            />
          ) : (
            <>
              <Field
                label="验证码"
                autoComplete="one-time-code"
                value={code}
                onChange={setCode}
                disabled={busy}
              />
              <Button kind="quiet"
                disabled={
                  busy ||
                  !identifier ||
                  waiting > 0 ||
                  !(channel === "email"
                    ? capabilities?.email
                    : capabilities?.sms)
                }
                onClick={() => void run(() => sendCode())}
              >
                {waiting ? `${waiting}秒后可重发` : "发送验证码"}
              </Button>
            </>
          )}
          {[["password", "使用原账号密码验证"], ["phone", "使用已绑定手机验证"], ["email", "使用原账号邮箱验证"]].filter(([method]) => method !== channel).map(([method, label]) =>
            <Button key={method} kind="quiet" disabled={busy || !(method === "password" ? capabilities?.password : method === "phone" ? capabilities?.sms : capabilities?.email)}
              onClick={() => { setChannel(method); setPassword(""); setCode(""); setSent(undefined); setError(""); }}>{label}</Button>)}
          {!(channel === "password" ? capabilities?.password : channel === "phone" ? capabilities?.sms : capabilities?.email)
            ? <Notice title="当前方式尚未由服务启用">请选择其他已关联的登录方式，不会自动创建或接管账号。</Notice> : null}
        </>
      ) : null}
      {stage === "link-review" ? (
        <>
          <Notice title="只关联到当前已验证账号">
            {session.user?.nickname || session.user?.username}
            。继续完成本次用途验证，再主动确认。
          </Notice>
          <Field
            label="当前账号密码（也可使用短信验证）"
            type="password"
            value={password}
            onChange={setPassword}
            disabled={busy}
          />
          <Field
            label="当前完整手机号"
            value={phone}
            onChange={value => { setPhone(value); setCode(""); setSent(undefined); }}
            disabled={busy}
          />
          <Field
            label="本次用途验证码"
            value={code}
            onChange={setCode}
            disabled={busy}
          />
          <Button
            disabled={busy || !phone || waiting > 0 || !capabilities?.sms}
            onClick={() => void run(() => sendCode(true))}
          >
            {waiting ? `${waiting}秒后可重发` : "发送本次用途验证码"}
          </Button>
        </>
      ) : null}
      {stage === "link-ready" ? (
        <>
          <Card>
            <div className="profile-identity security-identity">
              <PrivateImage
                id={
                  integer(
                    session.user?.avatar_url?.match(
                      /\/media\/(\d+)\/content/,
                    )?.[1],
                  ) || undefined
                }
                alt="当前账号头像"
                className="security-avatar"
              />
              <div>
                <strong>{session.user?.nickname || session.user?.username}</strong>
                <p className="caption muted">{session.user?.username}</p>
              </div>
            </div>
          </Card>
          <Card><dl className="auth-facts"><div><dt>将关联的方式</dt><dd>{name}</dd></div></dl></Card>
          <Notice title="只关联登录方式">
            不会覆盖原有宠物、日记、群和历史消息，也不改变交流资格。
          </Notice>
          <Button kind="quiet" disabled={busy} onClick={accountChoice}>取消关联</Button>

          {!proof.current && !busy ? (
            <Button
              onClick={() => {
                setStage("link-review");
                setError("");
              }}
            >
              重新核对本次关联用途
            </Button>
          ) : null}
        </>
      ) : null}
      {busy ? <p role="status">正在核对本次授权，请保留当前任务…</p> : null}
      {error ? <Notice title={error} error /> : null}
      </div>
    </Modal>
  );
}
