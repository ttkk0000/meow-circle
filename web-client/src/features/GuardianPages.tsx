import { useEffect, useRef, useState } from "react";
import { api, explain, HttpError, type RecordData } from "../core/api";
import { useSession } from "../core/session";
import { useQuery } from "../core/query";
import { back, navigate } from "../core/router";
import { key } from "../core/wire";
import {
  Button,
  Card,
  Check,
  Field,
  Modal,
  Notice,
  RowLink,
  TaskPage,
} from "../components/ui";
import { PurposeCheck } from "../components/PurposeCheck";
import { AuthDialog } from "./AuthDialog";

interface InviteInfo {
  available: boolean;
  policy_version: string;
  policy_label: string;
  policy_key: string;
}
interface Invite {
  id: bigint;
  version: bigint;
  state: string;
  policy_version: string;
  expires_at: string;
  guardian_record_id: bigint;
  share_url: string;
  mine: boolean;
  can_cancel: boolean;
}
interface InvitePage {
  items: Invite[];
  next_cursor: bigint;
}
interface Eligibility {
  contract_version: bigint;
  adult: boolean;
  phone_verified: boolean;
  age_band: string;
}
interface Guardian {
  child_id: bigint;
  scopes: string[];
  valid_until: string;
  policy_version: string;
}
interface ReadingDocument {
  key: string;
  revision: string;
  status: string;
  sha256: string;
}
type GuardianRecord = RecordData<Guardian>;
interface Decision {
  record: GuardianRecord;
  consent: boolean;
  scope?: Record<string, unknown>;
}
// 只存原操作编号；邀请口令、用途凭据和分享链接均不进入持久化草稿。
let pending:
  | { owner: bigint; key: string; kind: "auth_challenges" | "guardians" }
  | undefined;
const tokenPattern = /^[A-Za-z0-9_-]{43}$/;
const future = (value: string) => Date.parse(value) > Date.now();
const time = (value: string) =>
  Number.isFinite(Date.parse(value))
    ? new Date(value).toLocaleString("zh-CN")
    : "待核对";
const stateLabel = (value: string) =>
  (
    ({
      invite_open: "等待监护人领取",
      relation_pending: "已领取，等待独立关系核对",
      relation_verified: "关系已核实，仍需监护人明确授权",
      invite_cancelled: "原邀请已取消",
      expired: "邀请或请求已过期",
      policy_changed: "指定规则已变化，请取消原邀请后重新核对",
    }) as Record<string, string>
  )[value] ?? "状态待核对";

function useGuardianFlow() {
  const session = useSession();
  const owner = session.user?.id ?? 0n;
  const [info, setInfo] = useState<InviteInfo>();
  const [items, setItems] = useState<Invite[]>([]);
  const [next, setNext] = useState(0n);
  const [guardians, setGuardians] = useState<GuardianRecord[]>([]);
  const [eligibility, setEligibility] = useState<Eligibility>();
  const [busy, setBusy] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [unknown, setUnknown] = useState(pending?.owner === owner);
  const [error, setError] = useState("");
  const epoch = useRef(0);
  const locked = useRef(false);
  useEffect(() => {
    epoch.current++;
    if (pending && pending.owner !== owner) pending = undefined;
    setInfo(undefined);
    setItems([]);
    setGuardians([]);
    setEligibility(undefined);
    setNext(0n);
    setLoaded(false);
    setBusy(false);
    setError("");
    setUnknown(pending?.owner === owner);
    locked.current = false;
    if (owner > 0n) void read();
    return () => {
      epoch.current++;
    };
  }, [owner]);
  async function run(action: (current: () => boolean) => Promise<void>) {
    if (locked.current || owner <= 0n) return;
    const generation = epoch.current;
    const current = () =>
      generation === epoch.current && owner === api.session.user?.id;
    locked.current = true;
    setBusy(true);
    setError("");
    try {
      await action(current);
    } catch (e) {
      if (current()) setError(explain(e));
    } finally {
      if (current()) {
        locked.current = false;
        setBusy(false);
      }
    }
  }
  async function records(current: () => boolean) {
    const [list, eligible] = await Promise.all([
      api.request<GuardianRecord[]>("guardians", { owner }),
      api.request<Eligibility>("eligibility", { owner }),
    ]);
    if (current()) {
      setGuardians(list);
      setEligibility(eligible);
    }
  }
  async function read(more = false) {
    await run(async (current) => {
      const [configuration, page] = await Promise.all([
        api.request<InviteInfo>("guardians/invites/config", { owner }),
        api.request<InvitePage>(
          `guardians/invites?cursor=${more ? next : 0n}`,
          { owner },
        ),
      ]);
      if (!current()) return;
      setInfo(configuration);
      setItems((old) =>
        more
          ? [
              ...old,
              ...page.items.filter(
                (value) => !old.some((prior) => prior.id === value.id),
              ),
            ]
          : page.items,
      );
      setNext(page.next_cursor);
      setLoaded(true);
      await records(current);
    });
  }
  async function mutate(
    path: string,
    body: Record<string, unknown>,
    kind: "auth_challenges" | "guardians" = "auth_challenges",
  ) {
    if (unknown || pending?.owner === owner) return;
    await run(async (current) => {
      const request = key();
      pending = { owner, key: request, kind };
      let result: Invite | GuardianRecord;
      try {
        result = await api.request<Invite | GuardianRecord>(path, {
          method: "POST",
          owner,
          body: { ...body, client_request_id: request },
        });
        if (!current()) return;
        pending = undefined;
        setUnknown(false);
        if (kind === "auth_challenges")
          setItems((old) => [
            result as Invite,
            ...old.filter((value) => value.id !== result.id),
          ]);
      } catch (e) {
        if (current()) {
          const known =
            e instanceof HttpError &&
            [400, 401, 403, 404, 409, 422, 429].includes(e.status);
          if (known) pending = undefined;
          setUnknown(!known);
        }
        throw e;
      }
      await records(current);
    });
  }
  async function reconcile() {
    await run(async (current) => {
      const original = pending;
      if (!original || original.owner !== owner) return;
      const result = await api.request<{
        result_kind: string;
        result_id: bigint;
        command: string;
      }>(`operations/${original.key}`, { owner });
      if (
        result.result_kind !== original.kind ||
        !(original.kind === "guardians"
          ? result.command === "guardian.decision"
          : result.command.startsWith("guardian.invite."))
      )
        throw new Error("回执与原监护操作不一致，请保留原编号");
      if (original.kind === "auth_challenges") {
        const invite = await api.request<Invite>(
          `guardians/invites/${result.result_id}`,
          { owner },
        );
        if (current())
          setItems((old) => [
            invite,
            ...old.filter((value) => value.id !== invite.id),
          ]);
      }
      await records(current);
      if (current()) {
        pending = undefined;
        setUnknown(false);
      }
    });
  }
  return {
    owner,
    info,
    items,
    next,
    guardians,
    eligibility,
    busy,
    loaded,
    unknown,
    error,
    read,
    mutate,
    reconcile,
    run,
  };
}
type Flow = ReturnType<typeof useGuardianFlow>;

function InvitationClaim({
  flow,
  initialCode = "",
  standalone = false,
}: {
  flow: Flow;
  initialCode?: string;
  standalone?: boolean;
}) {
  const [code, setCode] = useState(initialCode);
  const [agreed, setAgreed] = useState(false);
  const previousOwner = useRef(flow.owner);
  useEffect(() => {
    if (previousOwner.current > 0n && previousOwner.current !== flow.owner)
      setCode("");
    previousOwner.current = flow.owner;
    setAgreed(false);
  }, [flow.owner, flow.info?.policy_version]);
  const eligible = flow.eligibility;
  const valid =
    eligible?.contract_version === 1n &&
    eligible.adult &&
    eligible.phone_verified;
  return (
    <Card title="监护人独立办理">
      <p>
        请使用监护人本人的账号。领取邀请后仍需独立核对监护关系，再明确确认指定范围。
      </p>
      {!valid ? (
        <Notice title="先核对本人成年资格与当前手机号">
          一个成年手机号不能单独证明监护关系，不会将儿童账号年龄改成成人。
          {standalone ? (
            <>
              <a
                href="/app/age-check"
                target="_blank"
                rel="noopener noreferrer"
              >
                核对本人成年资格
              </a>{" "}
              ·{" "}
              <a href="/app/security" target="_blank" rel="noopener noreferrer">
                验证本人手机号
              </a>
            </>
          ) : (
            <>
              <RowLink title="核对本人成年资格" to="/age-check" />
              <RowLink title="验证当前手机号" to="/security" />
            </>
          )}
        </Notice>
      ) : null}
      <Field
        label="短时单次邀请口令"
        type="password"
        value={code}
        onChange={setCode}
        disabled={flow.busy || flow.unknown}
      />
      <a
        href={`/app/reading?doc=${encodeURIComponent(flow.info?.policy_key || "")}&type=teen`}
        target="_blank"
        rel="noopener noreferrer"
      >
        阅读本次儿童与监护专门规则
      </a>
      <Check
        checked={agreed}
        onChange={setAgreed}
        disabled={flow.busy || flow.unknown}
      >
        我已阅读当前规则，了解领取、关系核对与必要资料范围
      </Check>
      <Button
        kind="primary"
        disabled={
          !valid ||
          !flow.info?.available ||
          !tokenPattern.test(code) ||
          !agreed ||
          flow.busy ||
          flow.unknown
        }
        onClick={() => {
          const token = code;
          setCode("");
          void flow.mutate("guardians/invites/claim", {
            token,
            accepted: true,
            policy_version: flow.info!.policy_version,
          });
        }}
      >
        领取并等待关系核对
      </Button>
      {standalone ? (
        <Button disabled={flow.busy} onClick={() => void flow.read()}>
          已完成资格核对，刷新
        </Button>
      ) : null}
    </Card>
  );
}
function InvitationStatus({ flow }: { flow: Flow }) {
  const [cancel, setCancel] = useState<Invite>();
  const [copied, setCopied] = useState<bigint>();
  const [copyError, setCopyError] = useState("");
  useEffect(() => {
    setCancel(undefined);
    setCopied(undefined);
    setCopyError("");
  }, [flow.owner]);
  async function copy(invite: Invite) {
    const owner = flow.owner;
    setCopyError("");
    try {
      if (!navigator.clipboard) throw new Error("clipboard unavailable");
      await navigator.clipboard.writeText(invite.share_url);
      if (api.session.user?.id === owner) setCopied(invite.id);
    } catch {
      if (api.session.user?.id === owner)
        setCopyError("未能复制，请手动选取上方链接");
    }
  }
  return (
    <>
      {flow.items.map((invite) => (
        <Card title={`邀请 #${invite.id}`} key={String(invite.id)}>
          <strong>{stateLabel(invite.state)}</strong>
          <p>有效期至 {time(invite.expires_at)}</p>
          {invite.share_url ? (
            <>
              <Notice title="仅向监护人私下分享">
                链接不含儿童资料；领取后仍须核对关系。请勿发到公开社区。
              </Notice>
              <p className="break-anywhere">{invite.share_url}</p>
              <Button disabled={flow.busy} onClick={() => void copy(invite)}>
                {copied === invite.id ? "已复制邀请链接" : "复制邀请链接"}
              </Button>
            </>
          ) : null}
          {invite.guardian_record_id > 0n ? (
            <a href={`/app/guardians/${invite.guardian_record_id}`}>
              查看关系核实后的授权
            </a>
          ) : null}
          {invite.can_cancel ? (
            <Button
              disabled={flow.busy || flow.unknown}
              onClick={() => setCancel(invite)}
            >
              取消原邀请／待核对请求
            </Button>
          ) : null}
        </Card>
      ))}
      {copyError ? <Notice title={copyError} error /> : null}
      {flow.next > 0n ? (
        <Button disabled={flow.busy} onClick={() => void flow.read(true)}>
          继续读取邀请记录
        </Button>
      ) : null}
      {cancel ? (
        <Modal
          title="取消原邀请"
          busy={flow.busy}
          onClose={() => setCancel(undefined)}
          actions={
            <Button
              disabled={flow.busy || flow.unknown}
              onClick={() => {
                const value = cancel;
                setCancel(undefined);
                void flow.mutate(`guardians/invites/${value.id}/cancel`, {
                  version: value.version,
                });
              }}
            >
              确认取消
            </Button>
          }
        >
          <p>
            仅取消邀请 #{String(cancel.id)}{" "}
            的当前版本，保留草稿；不会撤回已生效的监护授权。
          </p>
        </Modal>
      ) : null}
    </>
  );
}
function FlowStatus({ flow }: { flow: Flow }) {
  return (
    <>
      {flow.busy ? <p role="status">正在核对原记录…</p> : null}
      {flow.error ? <Notice title={flow.error} error /> : null}
      {flow.info?.available === false ? (
        <Notice title="邀请服务暂不可用">
          原任务和草稿保留；可以阅读儿童专门规则或提出私密资料事项协助。
        </Notice>
      ) : null}
      {flow.unknown ? (
        <Notice title="原操作结果待核对" warning>
          不会重建邀请、重新领取或重复授权。
          <Button disabled={flow.busy} onClick={() => void flow.reconcile()}>
            按原编号核对
          </Button>
        </Notice>
      ) : null}
      {!flow.loaded && !flow.busy && flow.owner > 0n ? (
        <Button onClick={() => void flow.read()}>重新读取服务与邀请</Button>
      ) : null}
    </>
  );
}

export function Guardians({ id = 0n }: { id?: bigint }) {
  const flow = useGuardianFlow();
  const catalog = useQuery<{ documents: ReadingDocument[] }>("reading");
  const [agreed, setAgreed] = useState(false);
  const [decision, setDecision] = useState<Decision>();
  const [grant, setGrant] = useState("");
  const [confirmed, setConfirmed] = useState(false);
  useEffect(() => {
    setAgreed(false);
    setDecision(undefined);
    setGrant("");
    setConfirmed(false);
  }, [flow.owner, flow.info?.policy_version]);
  const values = flow.guardians.filter((value) => id === 0n || value.id === id);
  async function prepare(record: GuardianRecord, consent: boolean) {
    await flow.run(async (current) => {
      const scope = consent
        ? await api.request<Record<string, unknown>>(
            `guardians/${record.id}/scope`,
            { method: "POST", body: {}, owner: flow.owner },
          )
        : undefined;
      if (current()) {
        setDecision({ record, consent, scope });
        setGrant("");
        setConfirmed(false);
      }
    });
  }
  return (
    <TaskPage
      title="监护授权与管理"
      actions={
        <Button disabled={flow.busy} onClick={back}>
          保留草稿，继续浏览
        </Button>
      }
    >
      <FlowStatus flow={flow} />
      {id === 0n ? (
        <>
          <RowLink
            title="查看儿童与监护专门规则"
            detail="收集事项、用途及撤回方式"
            to="/reading?type=teen"
          />
          <Notice title="监护关系需要核对">
            一个成年手机号不能单独证明监护关系；领取邀请不会生成关系或授权。
          </Notice>
          {flow.info?.available && flow.eligibility?.age_band !== "adult" ? (
            <Card title="请监护人授权">
              <p>
                可以继续浏览适龄公开内容；有效授权完成前不能新增交流，私有草稿保留。
              </p>
              <Check
                checked={agreed}
                onChange={setAgreed}
                disabled={flow.busy || flow.unknown}
              >
                我已阅读当前儿童专门规则，了解邀请与资料范围
              </Check>
              <Button
                kind="primary"
                disabled={!agreed || flow.busy || flow.unknown}
                onClick={() =>
                  void flow.mutate("guardians/invites", {
                    accepted: true,
                    policy_version: flow.info!.policy_version,
                  })
                }
              >
                邀请监护人
              </Button>
            </Card>
          ) : null}
          <InvitationClaim flow={flow} />
          <InvitationStatus flow={flow} />
          {flow.loaded && !flow.busy ? (
            <Button onClick={() => void flow.read()}>刷新邀请与授权状态</Button>
          ) : null}
        </>
      ) : null}
      {id > 0n && flow.loaded && values.length === 0 ? (
        <Notice title="当前授权不可读">
          可能已失效或当前账号无权查看，请重新读取。
        </Notice>
      ) : null}
      {values.map((record) => {
        const mine = record.owner_id === flow.owner;
        const policy = catalog.data?.documents.find(
          (value) =>
            value.sha256 === record.data.policy_version &&
            ["published", "archived"].includes(value.status),
        );
        const active =
          record.state === "active" && future(record.data.valid_until);
        const waiting =
          record.state === "verified_pending_consent" &&
          future(record.data.valid_until);
        return (
          <Card title={`监护授权 #${record.id}`} key={String(record.id)}>
            {record.state === "withdrawn" ||
            !future(record.data.valid_until) ? (
              <Notice title="新增交流已暂停" warning>
                保留合法必要记录与私有草稿；不会自动公开、迁移或删除账号。需有效授权的操作仍由服务端检查。
              </Notice>
            ) : null}
            <strong>
              {active
                ? "授权有效"
                : waiting
                  ? "关系已核实，等待明确授权"
                  : "授权已撤回／失效"}
            </strong>
            <p>
              交流资格仍需当前手机号验证；陌生人私信默认限制，定位不公开精确位置。
            </p>
            <p>
              必要资料处理与受保护的日常交流。不包含商业发布、线下签约、营销与公开定位。有效期至{" "}
              {time(record.data.valid_until)}。
            </p>
            {policy ? (
              <RowLink
                title="查看本次专门规则"
                detail={policy.revision}
                to={`/reading?doc=${policy.key}`}
              />
            ) : (
              <Notice title="本次指定规则暂不可读">
                保留原记录，不提前确认不可读的授权版本。
              </Notice>
            )}
            {mine && waiting ? (
              <Button
                kind="primary"
                disabled={!policy || flow.busy || flow.unknown}
                onClick={() => void prepare(record, true)}
              >
                核对并确认授权
              </Button>
            ) : null}
            {mine &&
            ["active", "verified_pending_consent"].includes(record.state) ? (
              <Button
                disabled={flow.busy || flow.unknown}
                onClick={() => void prepare(record, false)}
              >
                {active ? "撤回授权" : "拒绝授权"}
              </Button>
            ) : null}
            {active && !flow.eligibility?.phone_verified ? (
              <RowLink title="验证当前手机号" to="/security" />
            ) : null}
            <RowLink title="保护设置与陌生人联系" to="/guardian-protection" />
            <RowLink title="数据权利与投诉" to="/edit?flow=data" />
          </Card>
        );
      })}
      <RowLink
        title="需要帮助／对关系有异议"
        detail="私密资料事项协助"
        to="/edit?flow=safety&amp;type=data_assistance"
      />
      {decision ? (
        <Modal
          title={decision.consent ? "核对本次监护授权" : "撤回／拒绝原授权"}
          busy={flow.busy}
          onClose={() => {
            setDecision(undefined);
            setGrant("");
          }}
          actions={
            <Button
              kind="primary"
              disabled={
                flow.busy ||
                flow.unknown ||
                !confirmed ||
                (decision.consent && !grant)
              }
              onClick={() => {
                const value = decision;
                const proof = grant;
                setGrant("");
                setDecision(undefined);
                void flow.mutate(
                  `guardians/${value.record.id}/decision`,
                  {
                    version: value.record.version,
                    consent: value.consent,
                    reauth_token: proof,
                  },
                  "guardians",
                );
              }}
            >
              确认并提交
            </Button>
          }
        >
          <p>
            授权 #{String(decision.record.id)} · 第{" "}
            {String(decision.record.version)} 版
          </p>
          <p>
            {decision.consent
              ? `必要资料处理与受保护日常交流，不包含商业、线下签约、营销或公开定位。有效期至 ${time(decision.record.data.valid_until)}。`
              : "停止此关系的新增授权处理，保留必要历史与独立资料权利。"}
          </p>
          {decision.scope && !grant ? (
            <PurposeCheck
              scope={decision.scope}
              busy={flow.busy}
              onGrant={setGrant}
            />
          ) : null}
          {!decision.consent || grant ? (
            <Check checked={confirmed} onChange={setConfirmed}>
              我已阅读本次专门规则，并核对对象、版本、范围与有效期
            </Check>
          ) : null}
        </Modal>
      ) : null}
    </TaskPage>
  );
}

export function GuardianProtection() {
  return (
    <TaskPage
      title="未成年人保护设置"
      actions={
        <Button kind="primary" onClick={() => navigate("/reading?type=teen")}>
          返回未成年人规则
        </Button>
      }
    >
      <Card>
        <dl>
          <dt>未满14岁</dt>
          <dd>有效监护授权与儿童专门规则</dd>
          <dt>14–17岁</dt>
          <dd>继续适用未成年人保护</dd>
          <dt>陌生人联系</dt>
          <dd>默认限制，支持屏蔽、举报与必要使用管理</dd>
          <dt>商业与线下签约</dt>
          <dd>成年人以本人账号办理，不代点儿童确认</dd>
        </dl>
      </Card>
      <RowLink
        title="查看监护授权与撤回"
        detail="范围、版本及有效状态"
        to="/guardians"
      />
      <RowLink title="屏蔽与好友管理" to="/friendships" />
      <RowLink
        title="资料与侵害求助"
        detail="未成年人私密信息优先保护"
        to="/edit?flow=safety&amp;type=privacy"
      />
      <Notice title="保护与反馈">
        监护授权有范围和有效期；更正年龄、调整授权或请求删除，可通过规则与权利渠道办理。
      </Notice>
    </TaskPage>
  );
}

/** 宿主在渲染前移除 URL 口令，交接对象在首次挂载后清空；全程不写 Storage。 */
export function GuardianClaimPage({ handoff }: { handoff: { code: string } }) {
  const [initialCode, setInitialCode] = useState(handoff.code);
  const [ready, setReady] = useState(false);
  const [error, setError] = useState("");
  const [login, setLogin] = useState(false);
  const session = useSession();
  const flow = useGuardianFlow();
  useEffect(() => {
    handoff.code = "";
    setInitialCode("");
    let live = true;
    void api
      .openSession()
      .then((value) =>
        value.refreshable && !value.authenticated ? api.refresh() : undefined,
      )
      .catch((e) => {
        if (live) setError(explain(e));
      })
      .finally(() => {
        if (live) setReady(true);
      });
    return () => {
      live = false;
    };
  }, [handoff]);
  return (
    <main className="operator-shell">
      <TaskPage
        title="监护人独立领取邀请"
        subtitle="M&amp;D · meow &amp; doggie"
        actions={
          <a className="button" href="/app/guardians">
            进入监护授权与管理
          </a>
        }
      >
        <Notice title="邀请不含儿童资料">
          请使用监护人本人的账号。领取不代表关系已核实或授权已完成。
        </Notice>
        {error ? <Notice title={error} error /> : null}
        {!ready ? (
          <p role="status">正在读取本人会话…</p>
        ) : !session.authenticated ? (
          <Button kind="primary" onClick={() => setLogin(true)}>
            登录监护人本人的账号
          </Button>
        ) : (
          <>
            <p>
              当前本人账号：{session.user?.nickname || session.user?.username}
            </p>
            <p className="caption muted">
              若不是监护人本人，请切换账号；切换会清除口令，之后重新打开原邀请或手动粘贴。
            </p>
            <Button
              disabled={flow.busy || flow.unknown}
              onClick={() =>
                void flow.run(async () => {
                  await api.request("auth/logout", {
                    method: "POST",
                    owner: flow.owner,
                    body: { all: false },
                  });
                  if (!api.session.authenticated) setLogin(true);
                })
              }
            >
              退出此账号，改由监护人登录
            </Button>
            <FlowStatus flow={flow} />
          </>
        )}
        <div hidden={!ready || !session.authenticated}>
          <InvitationClaim flow={flow} initialCode={initialCode} standalone />
        </div>
        {ready && session.authenticated ? (
          <InvitationStatus flow={flow} />
        ) : null}
        {login ? <AuthDialog onClose={() => setLogin(false)} /> : null}
      </TaskPage>
    </main>
  );
}
