import { useEffect, useRef, useState } from "react";
import { api, explain, HttpError, type User } from "../core/api";
import { useSession } from "../core/session";
import {
  providerConfiguration,
  providerPopup,
  type ProviderCallback,
} from "../core/provider";
import {
  Button,
  Card,
  Check,
  Choices,
  Field,
  Icon,
  Modal,
  Notice,
  RowLink,
  PrivateImage,
} from "../components/ui";
import { LegalLinks, termsURL, privacyURL } from "../components/AuthPolicies";
import { integer } from "../core/wire";
import { AccountConflict } from "../components/AccountConflict";
import type { Capabilities } from "./AuthDialog";

type Stage =
  | "consent"
  | "authorizing"
  | "verifying"
  | "choice"
  | "create-review"
  | "cancelled"
  | "expired"
  | "conflict"
  | "existing"
  | "link-review"
  | "link-ready"
  | "unknown"
  | "failed";
interface ProviderResult {
  login?: { user: User };
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
}: {
  provider: string;
  onBack: () => void;
  onClose: () => void;
  capabilities?: Capabilities;
}) {
  const configuration = providerConfiguration(provider);
  const session = useSession();
  const [stage, setStage] = useState<Stage>("consent");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [agreed, setAgreed] = useState(false);
  const [confirmed, setConfirmed] = useState(false);
  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [code, setCode] = useState("");
  const [phone, setPhone] = useState("");
  const [channel, setChannel] = useState("password");
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
  const name = provider === "wechat" ? "微信" : "QQ";
  function clear() {
    generation.current++;
    resolution.current = "";
    proof.current = "";
    challenge.current = "";
    cancelPopup.current?.();
    cancelPopup.current = undefined;
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
  function close() {
    if (busy && stage !== "authorizing") return;
    const original = challenge.current;
    clear();
    setPassword("");
    setCode("");
    onBack();
    if (original)
      void api
        .request("auth/oauth-finish", {
          method: "POST",
          body: { state: original, credential: {}, cancelled: true },
        })
        .catch(() => {});
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
          clear();
          onClose();
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
            failure instanceof HttpError && failure.status === 401
              ? "expired"
              : "failed",
          );
        throw failure;
      }
    });
  }
  async function sendCode(reauth = false) {
    const email = !reauth && channel === "email";
    const result = await api.request<{ retry_after_seconds: bigint }>(
      email ? "auth/email-otp" : "auth/otp",
      {
        method: "POST",
        body: email
          ? { email: identifier.trim(), purpose: "email_login" }
          : {
              phone: reauth ? phone.trim() : identifier.trim(),
              purpose: reauth ? "reauth" : "login",
            },
      },
    );
    setUntil(
      Date.now() + Math.max(1, Number(result.retry_after_seconds)) * 1000,
    );
  }
  const waiting = Math.max(0, Math.ceil((until - now) / 1000));
  const available =
    !!configuration &&
    !!capabilities?.providers?.includes(provider) &&
    typeof BroadcastChannel !== "undefined";
  const createAction = (
    <Button
      kind="primary"
      disabled={!agreed || !termsURL || !privacyURL || busy}
      onClick={() =>
        void run(async () => {
          await ownLogin(() =>
            api.request("auth/oauth-resolve", {
              method: "POST",
              body: {
                token: resolution.current,
                create: true,
                accept_terms: true,
              },
            }),
          );
          clear();
          onClose();
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
        (channel === "password" ? !password || !capabilities?.password : !code)
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
      disabled={busy || (!password && !code)}
      onClick={() =>
        void run(async () => {
          const secret = password;
          const otp = code;
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
          setConfirmed(false);
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
      disabled={busy || !confirmed || !proof.current}
      onClick={() =>
        void run(async () => {
          const reauth = proof.current;
          proof.current = "";
          await ownLogin(() =>
            api.request("auth/oauth-resolve", {
              method: "POST",
              body: {
                token: resolution.current,
                create: false,
                reauth_token: reauth,
              },
            }),
          );
          clear();
          onClose();
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
            : ["consent", "failed", "unknown", "cancelled", "expired"].includes(
                  stage,
                )
              ? authorizeAction
              : null;
  return (
    <Modal
      title={
        stage === "choice"
          ? "继续使用原账号"
          : stage === "create-review"
            ? "继续创建新账号"
            : stage === "existing"
              ? "验证原账号"
              : stage.startsWith("link")
                ? `确认关联${name}`
                : `${name}官方授权`
      }
      busy={busy && stage !== "authorizing"}
      onClose={close}
      actions={
        <>
          {primary}
          <Button
            kind="quiet"
            disabled={busy && stage !== "authorizing"}
            onClick={close}
          >
            {stage === "authorizing" ? "取消本次授权" : "返回登录方式"}
          </Button>
        </>
      }
    >
      {["consent", "failed", "unknown", "cancelled", "expired"].includes(
        stage,
      ) ? (
        <>
          <Notice
            title={
              stage === "unknown"
                ? "本次处理结果待核对"
                : stage === "cancelled"
                  ? "已取消授权"
                  : stage === "expired"
                    ? "本次授权已过期"
                    : available
                      ? "仅本次官方授权"
                      : "暂时无法使用这种方式"
            }
          >
            {stage === "unknown"
              ? "不会自动重发创建或关联。重新官方授权用于核对已关联账号；新身份仍需主动选择。"
              : stage === "cancelled" || stage === "expired"
                ? "原页面和可保留的草稿仍在，可以重新授权或选择其他登录方式。"
                : "将在官方站点完成授权，回到本应用后再由服务端核验；不会依据昵称、头像或返回邮箱自动合并账号。"}
          </Notice>
          <Icon name={`provider-${provider}`} />
          {stage === "consent" ? (
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
          ) : null}
          {["cancelled", "expired", "failed"].includes(stage) ? (
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
            title="已有 M&amp;D 账号，去关联"
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
        <Card title="继续创建新账号">
          <LegalLinks />
          <Check
            checked={agreed}
            onChange={setAgreed}
            disabled={!termsURL || !privacyURL || busy}
          >
            我已阅读当前协议，主动创建新账号
          </Check>
          <p className="caption muted">新账号不会自动取得交流或成年人资格。</p>
        </Card>
      ) : null}
      {stage === "existing" ? (
        <>
          <Choices
            label="验证原账号"
            value={channel}
            disabled={busy}
            onChange={(value) => {
              setChannel(value);
              setPassword("");
              setCode("");
            }}
            choices={[
              ["password", "密码"],
              ["email", "邮箱验证码"],
              ["phone", "已绑定手机"],
            ]}
          />
          <Field
            label={
              channel === "phone"
                ? "已绑定手机号"
                : channel === "email"
                  ? "邮箱"
                  : "账号名、邮箱或手机号"
            }
            value={identifier}
            onChange={setIdentifier}
            disabled={busy}
          />
          {channel === "password" ? (
            <Field
              label="原账号密码"
              type="password"
              value={password}
              onChange={setPassword}
              disabled={busy}
            />
          ) : (
            <>
              <Field
                label="本次验证码"
                value={code}
                onChange={setCode}
                disabled={busy}
              />
              <Button
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
            onChange={setPhone}
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
            <div className="profile-identity">
              <PrivateImage
                id={
                  integer(
                    session.user?.avatar_url?.match(
                      /\/media\/(\d+)\/content/,
                    )?.[1],
                  ) || undefined
                }
                alt="当前账号头像"
                className="avatar"
              />
              <div>
                <h2>{session.user?.nickname || session.user?.username}</h2>
                <p className="caption muted">{session.user?.username}</p>
              </div>
            </div>
          </Card>
          <Card title="将关联的方式">{name}</Card>
          <Notice title="只关联登录方式">
            {name} → {session.user?.username}
            。不会合并其他账号的数据，也不改变交流资格。
          </Notice>
          <Check checked={confirmed} onChange={setConfirmed} disabled={busy}>
            我确认本次关联对象
          </Check>

          {!proof.current && !busy ? (
            <Button
              onClick={() => {
                setConfirmed(false);
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
      {stage === "conflict" ? <AccountConflict /> : null}
      {error ? <Notice title={error} error /> : null}
    </Modal>
  );
}
