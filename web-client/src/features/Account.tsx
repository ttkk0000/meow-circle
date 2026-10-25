import { useState } from "react";
import {
  api,
  explain,
  HttpError,
  type RecordData,
  type User,
} from "../core/api";
import { AccountConflict } from "../components/AccountConflict";
import { useSession } from "../core/session";
import { reload, useQuery } from "../core/query";
import { navigate } from "../core/router";
import { integer, key } from "../core/wire";
import { newDraft, saveDraft } from "../core/drafts";
import {
  Button,
  Card,
  Check,
  Choices,
  Field,
  Modal,
  Notice,
  PrivateImage,
  QueryState,
  RowLink,
  TaskPage,
} from "../components/ui";
import { OTPButton, PurposeCheck } from "../components/PurposeCheck";
import { workflowURL } from "./workflows";
import { ProviderSignIn } from "./ProviderSignIn";
import type { Capabilities } from "./AuthDialog";

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
export function Security() {
  const session = useSession();
  const profile = useQuery<Profile>("profile");
  const methods = useQuery<Methods>("auth/methods");
  const eligibility = useQuery<Eligibility>("eligibility");
  const capabilities = useQuery<Capabilities>("auth/capabilities");
  const [provider, setProvider] = useState("");
  const [phone, setPhone] = useState("");
  const [code, setCode] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  const [change, setChange] = useState<string>();
  const [identity, setIdentity] = useState(0n);
  async function verifyPhone() {
    setBusy(true);
    try {
      await api.request("auth/verify-phone", {
        method: "POST",
        body: { code },
      });
      setNotice("服务已接受此次验证，请以重新读取的资格状态为准");
      void reload("eligibility");
    } catch (e) {
      setError(explain(e));
    } finally {
      setBusy(false);
      setCode("");
    }
  }
  async function logout(all: boolean) {
    if (!confirm(all ? "退出此账号的所有登录设备？" : "退出当前浏览器账号？"))
      return;
    setBusy(true);
    try {
      await api.request("auth/logout", { method: "POST", body: { all } });
      navigate("/mine");
    } catch (e) {
      setError(explain(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <TaskPage title="账号安全与交流资格">
      <QueryState
        loading={methods.loading || profile.loading}
        error={methods.error || profile.error}
      >
        <Card title="服务端资格">
          <RowLink
            title="年龄与使用范围"
            detail="年龄、监护与成年业务分别核对"
            to="/age"
          />
          <p>
            交流：
            {eligibility.data?.contract_version === 1n &&
            eligibility.data.interaction
              ? "当前可用"
              : "尚未确认可用"}{" "}
            · 线下／签约：
            {eligibility.data?.adult ? "具备成年人资格" : "待核对"}
          </p>
          <p>当前手机号：{methods.data?.phone || "尚未绑定"}</p>
          <p>
            当前手机号验证：
            {eligibility.data?.phone_verified ? "已验证" : "未验证或已失效"}
          </p>
          {eligibility.data?.reasons.map((reason, index) => (
            <p className="caption muted" key={index}>
              {reasons[reason] ?? "请按服务端要求继续核对资格"}
            </p>
          ))}
          <Notice title="年龄与监护不能靠本地开关确认">
            儿童交流须具备对应有效监护范围；线下、商业和签约仍按服务端成年人及城市准入规则校验。
          </Notice>
          <RowLink title="核对监护邀请" to="/guardians" />
          <RowLink title="未成年人保护设置" to="/guardian-protection" />
          <RowLink title="申请核实与事项协助" to={workflowURL("review")} />
        </Card>
        <Card title="验证当前绑定的手机号">
          <Field
            label="当前完整手机号"
            value={phone}
            onChange={(value) => {
              setPhone(value);
              setCode("");
            }}
            disabled={busy}
          />
          <OTPButton
            path="auth/otp"
            body={() => ({ phone, purpose: "verify_phone" })}
            disabled={busy || !methods.data?.sms_available || !phone}
            onError={setError}
          />
          <Field
            label="本次验证码"
            value={code}
            onChange={setCode}
            disabled={busy}
          />
          <Button disabled={busy || !code} onClick={() => void verifyPhone()}>
            验证当前手机号
          </Button>
        </Card>
        <Card title="登录方式与敏感变更">
          {["phone", "email", "password"].map((action) => (
            <RowLink
              key={action}
              title={
                (
                  {
                    phone: "绑定／更换手机号",
                    email: "绑定／更换邮箱",
                    password: "设置／更改密码",
                  } as Record<string, string>
                )[action]!
              }
              onClick={() => {
                setIdentity(0n);
                setChange(action);
              }}
            />
          ))}
          {methods.data?.linked.map((link) => (
            <RowLink
              key={String(link.id)}
              title={`解绑${({ wechat: "微信", qq: "QQ", apple: "Apple", email: "邮箱" } as Record<string, string>)[link.provider] ?? "当前登录方式"}`}
              onClick={() => {
                setIdentity(link.id);
                setChange("unlink");
              }}
            />
          ))}
          {[
            ["wechat", "关联微信"],
            ["qq", "关联QQ"],
          ].map(([id, title]) => (
            <RowLink
              key={id}
              title={title!}
              detail="官方授权后，核对当前账号并确认本次关联"
              onClick={() => setProvider(id!)}
            />
          ))}
        </Card>
        {notice ? <Notice title={notice} /> : null}
        {error ? <Notice title={error} error /> : null}
        <div className="actions-row">
          <Button disabled={busy} onClick={() => void logout(false)}>
            退出当前账号
          </Button>
          <Button disabled={busy} onClick={() => void logout(true)}>
            退出所有设备
          </Button>
        </div>
        {change && profile.data && session.user ? (
          <AccountChange
            action={change}
            identity={identity}
            profile={profile.data}
            onClose={() => setChange(undefined)}
          />
        ) : null}
        {provider && session.user ? (
          <ProviderSignIn
            provider={provider}
            capabilities={capabilities.data}
            onBack={() => setProvider("")}
            onClose={() => {
              setProvider("");
              void reload("auth/methods");
              void reload("profile");
            }}
          />
        ) : null}
      </QueryState>
    </TaskPage>
  );
}
function AccountChange({
  action,
  identity,
  profile,
  onClose,
}: {
  action: string;
  identity: bigint;
  profile: Profile;
  onClose: () => void;
}) {
  const [value, setValue] = useState("");
  const [code, setCode] = useState("");
  const [error, setError] = useState("");
  const [errorCode, setErrorCode] = useState("");
  const [scope, setScope] = useState<Record<string, unknown>>();
  const [grant, setGrant] = useState("");
  const [confirmed, setConfirmed] = useState(false);
  const [busy, setBusy] = useState(false);
  const [request] = useState(key);
  const [uncertain, setUncertain] = useState(false);
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
    setBusy(true);
    setError("");
    try {
      const result = await api.request<Record<string, unknown>>(
        `auth/account/${action}/scope`,
        { method: "POST", body: fields() },
      );
      setScope(result);
      setGrant("");
      setConfirmed(false);
    } catch (e) {
      setError(explain(e));
    } finally {
      setBusy(false);
    }
  }
  async function submit() {
    if (!scope || !grant || !confirmed || uncertain) return;
    setBusy(true);
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
      await saveDraft(draft);
      setUncertain(true);
      await api.request(`auth/account/${action}`, {
        method: "POST",
        body: { ...fields(), reauth_token: grant },
      });
      onClose();
      navigate("/mine");
    } catch (e) {
      setError(explain(e));
      setErrorCode(e instanceof HttpError ? e.code : "");
    } finally {
      setValue("");
      setCode("");
      setGrant("");
      setBusy(false);
    }
  }
  return (
    <Modal
      title="核对敏感账号变更"
      busy={busy}
      onClose={onClose}
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
            disabled={busy || (action !== "unlink" && !value)}
            onClick={() => void prepare()}
          >
            核对本次用途
          </Button>
        )
      }
    >
      {errorCode === "identity_in_use" ? <AccountConflict /> : null}
      {uncertain ? (
        <Notice title="原变更结果待核对">
          请先重新登录后到恢复中心查询原编号 {request}，不能创建新编号重复提交。
          <Button onClick={() => navigate("/recovery")}>核对原操作</Button>
        </Notice>
      ) : null}
      {!scope ? (
        <>
          {action !== "unlink" ? (
            <Field
              label={
                action === "password"
                  ? "新密码（10–128字节）"
                  : action === "phone"
                    ? "新的完整手机号"
                    : "新的邮箱"
              }
              type={action === "password" ? "password" : "text"}
              value={value}
              onChange={(value) => {
                setValue(value);
                setCode("");
              }}
              disabled={busy}
            />
          ) : (
            <p>
              解绑登录方式 #{String(identity)}
              ；服务端会阻止移除最后一个可登录方式。
            </p>
          )}
          {["phone", "email"].includes(action) ? (
            <>
              <OTPButton
                path={action === "email" ? "auth/email-otp" : "auth/otp"}
                body={() =>
                  action === "email"
                    ? { email: value, purpose: "email_bind" }
                    : { phone: value, purpose: "bind_phone" }
                }
                disabled={busy || !value}
                onError={setError}
              />
              <Field
                label="新方式的验证码"
                value={code}
                onChange={setCode}
                disabled={busy}
              />
            </>
          ) : null}
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
          <PurposeCheck scope={scope} busy={busy} onGrant={setGrant} />
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
