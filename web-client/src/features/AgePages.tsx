import { useEffect, useRef, useState } from "react";
import { api, explain, HttpError } from "../core/api";
import { useSession } from "../core/session";
import { back, navigate, returnTo } from "../core/router";
import { key } from "../core/wire";
import {
  Button,
  Card,
  Check,
  Notice,
  RowLink,
  TaskPage,
} from "../components/ui";

interface Provider {
  available: boolean;
  name: string;
  privacy_url: string;
}
interface Status {
  id: bigint;
  state: string;
  task_key: string;
  task_label: string;
  provider: string;
  launch_url: string;
  expires_at: string;
  valid_until: string | null;
}
interface Eligibility {
  adult: boolean;
  interaction: boolean;
  reasons: string[];
  age_band: string;
  phone_verified: boolean;
  contract_version: bigint;
}
type Stage =
  | "loading"
  | "overview"
  | "notice"
  | "ready"
  | "pending"
  | "passed"
  | "cancelled"
  | "failed"
  | "unsupported"
  | "not_eligible"
  | "expired"
  | "review"
  | "unknown";
const ageLabel = (value?: string) =>
  (
    ({ under14: "未满14岁", "14_17": "14–17岁", adult: "成年" }) as Record<
      string,
      string
    >
  )[value ?? ""] ?? "待有效核对";
const active = (value?: string) =>
  ["pending", "preparing", "unknown"].includes(value ?? "");
function internalTask(value: string | null): string {
  return value &&
    value.length <= 120 &&
    /^\/(?:[a-z][a-z0-9-]*\/?|[a-z][a-z0-9_-]*\/\d+(?:\/[a-z-]+)?\/?)?(?:\?[^#]*)?$/.test(
      value,
    ) &&
    !value.includes("..") &&
    !value.includes("://")
    ? value
    : "/security";
}

export function AgeRange() {
  return (
    <TaskPage
      title="年龄与使用范围"
      actions={<Button onClick={back}>先继续浏览</Button>}
    >
      <h1 className="page-headline">为你提供适合的保护</h1>
      <p>
        只保留必要年龄区间；普通浏览不需要上传身份证。选择下列说明不会更改已核实的年龄结果。
      </p>
      <RowLink
        title="未满14岁"
        detail="请监护人完成指定范围授权"
        to="/guardians"
      />
      <RowLink
        title="14–17岁"
        detail="交流需满足当前手机与年龄资格，保护陌生人联系"
        to="/reading?type=teen"
      />
      <RowLink
        title="年满18岁"
        detail="商业与线下业务仍需有效成年资格"
        to="/age-check"
      />
      <Notice title="登录成功不等于所有权限">
        年龄、监护、手机号与业务角色分别检查。
      </Notice>
      <RowLink
        title="年龄或监护信息有误"
        detail="通过私密资料事项协助提出疑问"
        to="/edit?flow=safety&amp;type=data_assistance"
      />
    </TaskPage>
  );
}

/** 回到应用不视为成功；只查询同一请求，重新确认后再返回原任务。 */
export function AgeAssurance({ params }: { params: URLSearchParams }) {
  const session = useSession();
  const owner = session.user?.id ?? 0n;
  const from = internalTask(params.get("from"));
  const taskLabel = params.get("label")?.slice(0, 80) || "账号成年资格";
  const [stage, setStage] = useState<Stage>("loading");
  const [provider, setProvider] = useState<Provider>();
  const [current, setCurrent] = useState<Status>();
  const [eligibility, setEligibility] = useState<Eligibility>();
  const [accepted, setAccepted] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const request = useRef("");
  const preparedTask = useRef<{ key: string; label: string } | undefined>(
    undefined,
  );
  const mounted = useRef(true);
  const operation = useRef(false);
  const operationOwner = useRef(0);
  const generation = useRef(0);
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
      generation.current++;
    };
  }, []);
  useEffect(() => {
    const revision = ++generation.current;
    operation.current = false;
    operationOwner.current = revision;
    setBusy(false);
    request.current = key();
    preparedTask.current = undefined;
    setAccepted(false);
    setCurrent(undefined);
    setStage("loading");
    setError("");
    void Promise.all([
      api.request<Provider>("auth/age-assurance/config", { owner }),
      api.request<Status | null>("auth/age-assurance/current", { owner }),
    ])
      .then(([info, value]) => {
        if (!mounted.current || revision !== generation.current) return;
        setProvider(info);
        if (value) receive(value);
        else setStage("overview");
      })
      .catch((failure) => {
        if (mounted.current && revision === generation.current)
          setError(explain(failure));
      });
  }, [owner, from, taskLabel]);
  function receive(value: Status) {
    setCurrent(value);
    setStage(
      value.state === "preparing"
        ? "pending"
        : value.state === "superseded"
          ? "expired"
          : [
                "pending",
                "passed",
                "cancelled",
                "failed",
                "not_eligible",
                "expired",
              ].includes(value.state)
            ? (value.state as Stage)
            : "unknown",
    );
  }
  async function run(action: () => Promise<void>) {
    if (operation.current) return;
    const revision = generation.current;
    operation.current = true;
    operationOwner.current = revision;
    setBusy(true);
    setError("");
    try {
      await action();
    } catch (failure) {
      if (mounted.current && revision === generation.current)
        setError(explain(failure));
    } finally {
      if (operationOwner.current === revision) operation.current = false;
      if (mounted.current && revision === generation.current) setBusy(false);
    }
  }
  function launch(url: string, popup?: Window | null) {
    try {
      const target = new URL(url);
      if (
        target.protocol !== "https:" ||
        target.username ||
        target.password ||
        target.hash
      )
        throw new Error("核验地址不可用，请查询原请求");
      if (popup) popup.location.replace(target.href);
      else window.open(target.href, "_blank", "noopener,noreferrer");
    } catch (failure) {
      popup?.close();
      setError(explain(failure));
    }
  }
  function begin() {
    if (
      stage !== "ready" ||
      !provider?.available ||
      !accepted ||
      operation.current
    )
      return;
    const task = preparedTask.current;
    if (!task) return;
    const popup = window.open(
      "about:blank",
      "_blank",
      "popup,width=520,height=760",
    );
    if (popup) {
      popup.opener = null;
      popup.document.title = "M&D 本次成年资格核对";
    }
    const revision = generation.current;
    void run(async () => {
      try {
        const value = await api.request<Status>("auth/age-assurance", {
          method: "POST",
          owner,
          body: {
            client_request_id: request.current,
            task_key: task.key,
            task_label: task.label,
            accepted: true,
          },
        });
        if (!mounted.current || revision !== generation.current) {
          popup?.close();
          return;
        }
        receive(value);
        if (value.state === "pending" && value.launch_url && popup)
          launch(value.launch_url, popup);
        else popup?.close();
      } catch (failure) {
        popup?.close();
        if (mounted.current && revision === generation.current)
          setStage(
            failure instanceof HttpError &&
              [400, 401, 403, 409, 422, 429, 503].includes(failure.status)
              ? "failed"
              : "unknown",
          );
        throw failure;
      }
    });
  }
  async function query() {
    const revision = generation.current;
    const original =
      current ??
      (await api.request<Status | null>("auth/age-assurance/current", {
        owner,
      }));
    if (!original)
      throw new Error("暂未查到本次请求，请保留原任务；不会自动创建新核验");
    const value = await api.request<Status>(
      `auth/age-assurance/${original.id}/query`,
      { method: "POST", owner, body: {} },
    );
    if (mounted.current && revision === generation.current) receive(value);
  }
  function restart() {
    request.current = key();
    preparedTask.current = undefined;
    setCurrent(undefined);
    setAccepted(false);
    setStage("notice");
    setError("");
  }
  const terminal = ["cancelled", "failed", "not_eligible", "expired"].includes(
    stage,
  );
  const actions = (
    <>
      {stage === "notice" ? (
        <Button
          kind="primary"
          disabled={busy || !accepted}
          onClick={() => {
            preparedTask.current = { key: from, label: taskLabel };
            setStage(provider?.available ? "ready" : "unsupported");
          }}
        >
          核对服务并继续
        </Button>
      ) : null}
      {stage === "ready" ? (
        <Button kind="primary" disabled={busy || !accepted} onClick={begin}>
          打开本次核验
        </Button>
      ) : null}
      {stage === "pending" || stage === "unknown" ? (
        <Button disabled={busy} onClick={() => void run(query)}>
          查询本次结果
        </Button>
      ) : null}
      {stage === "passed" ? (
        <Button
          kind="primary"
          disabled={busy}
          onClick={() =>
            void run(async () => {
              const revision = generation.current;
              const value = await api.request<Eligibility>("eligibility", {
                owner,
              });
              if (mounted.current && revision === generation.current) {
                setEligibility(value);
                setStage("review");
              }
            })
          }
        >
          复核当前任务条件
        </Button>
      ) : null}
      {stage === "review" ? (
        <Button disabled={busy} onClick={() => returnTo(from)}>
          刷新原任务条件
        </Button>
      ) : null}
      {stage === "expired" ? (
        <Button kind="primary" disabled={busy} onClick={restart}>
          重新核对本次用途
        </Button>
      ) : null}
      <Button
        kind={
          terminal || stage === "unsupported" || stage === "overview"
            ? "secondary"
            : "quiet"
        }
        disabled={busy}
        onClick={() => returnTo(from)}
      >
        {stage === "pending" || stage === "unknown"
          ? "退出等待，保留原请求"
          : "取消并返回原任务"}
      </Button>
    </>
  );
  return (
    <TaskPage
      title={
        (
          {
            overview: "成年业务资格",
            notice: "成年核对前说明",
            ready: "准备打开核验服务",
            pending: "成年资格核对中",
            passed: "成年资格结果已返回",
            cancelled: "核对尚未完成",
            failed: "成年核对未通过",
            unsupported: "核对服务暂不可用",
            not_eligible: "当前不符合成年业务范围",
            expired: "成年资格需要重新核对",
            review: "继续原任务核对",
            unknown: "本次结果待核对",
          } as Record<string, string>
        )[stage] ?? "成年资格核对"
      }
      actions={actions}
    >
      {busy ? <p role="status">正在核对本次请求…</p> : null}
      {stage === "loading" ? (
        <Notice title="正在读取核验通道与原请求">
          {error ? (
            <Button onClick={() => navigate("/security")}>
              返回账号安全后重新核对
            </Button>
          ) : null}
        </Notice>
      ) : null}
      {stage === "overview" ? (
        <>
          <Notice title="年满18岁">
            商业发布、预约与线下签约需有效成年资格，并继续检查角色和资料。
          </Notice>
          <p>年龄核对完成后，才能办理需要成年人身份的事项。</p>
          <RowLink
            title="开始年龄核对"
            detail="仅返回必要结果"
            onClick={() => setStage("notice")}
          />
          <RowLink
            title="暂不核对"
            detail="继续日常浏览与交流流程"
            onClick={() => returnTo(from)}
          />
          <RowLink
            title="核对有误或无法完成"
            detail="申请私密资料事项协助"
            to="/edit?flow=safety&amp;type=data_assistance"
          />
        </>
      ) : null}
      {stage === "notice" ? (
        <>
          <Notice title="只为本次成年业务核对">
            当前任务：{taskLabel}。普通浏览无需完成此核对。
          </Notice>
          <Card title="核对范围">
            <p>成年资格结果、有效期与必要来源。</p>
            <p>原始材料在已确认通道处理，不进入公开资料。</p>
            <p>未完成时保持成年业务受限，原任务与草稿保留。</p>
          </Card>
          <Check checked={accepted} onChange={setAccepted} disabled={busy}>
            我已了解本次核对用途与资料范围
          </Check>
        </>
      ) : null}
      {stage === "ready" ? (
        <>
          <Notice title="核对服务可用后才继续">
            返回应用不代表核验成功，结果由服务端查询。
          </Notice>
          <Card title="本次核验">
            <p>{preparedTask.current?.label ?? taskLabel}</p>
            <p>{session.user?.username}</p>
            <p>{provider?.name}</p>
            <p>只回传结果、有效期与必要来源。</p>
          </Card>
          <a
            href={provider?.privacy_url}
            target="_blank"
            rel="noopener noreferrer"
          >
            阅读核验通道隐私说明
          </a>
        </>
      ) : null}
      {stage === "pending" || stage === "unknown" ? (
        <>
          <Card title="成年资格核对中">
            <p>
              请求 #{String(current?.id ?? "待确认")} ·{" "}
              {current?.task_label ?? taskLabel}
            </p>
            <p>
              {stage === "unknown" ? "本次结果待核对" : "等待核验服务返回结果"}
            </p>
          </Card>
          <Notice title="收到结果后才更新">
            查询或退出等待不会确认成功；结果不明时保留原请求，不创建新核验。
          </Notice>
          {current?.launch_url ? (
            <Button disabled={busy} onClick={() => launch(current.launch_url)}>
              打开原核验页面
            </Button>
          ) : null}
        </>
      ) : null}
      {stage === "passed" ? (
        <>
          <Notice title="成年资格结果已返回">
            有效期至 {current?.valid_until}
            。手机号、角色、当前版本和其他条件仍须分别核对。
          </Notice>
          <Card title="核对结果">
            <p>
              {session.user?.username} · {current?.task_label ?? taskLabel}
            </p>
            <p>{current?.provider}</p>
            <p>有效期至 {current?.valid_until}</p>
          </Card>
        </>
      ) : null}
      {stage === "review" ? (
        <>
          <Card title="回到原任务前再核对">
            <p>{taskLabel}</p>
            <p>
              当前成年资格：
              {eligibility?.contract_version === 1n && eligibility.adult
                ? "有效"
                : "待核对"}{" "}
              · {ageLabel(eligibility?.age_band)}
            </p>
            {eligibility?.reasons.map((reason) => (
              <p key={reason}>{explain(new HttpError(403, reason))}</p>
            ))}
          </Card>
          {!eligibility?.phone_verified ? (
            <RowLink
              title="当前手机号仍未验证"
              detail="保留同一任务，进入交流验证"
              to="/security"
            />
          ) : null}
          <Notice title="仍需主动确认">
            核验成功不会自动签约、发布、确认预约或提交原草稿。
          </Notice>
        </>
      ) : null}
      {stage === "unsupported" ? (
        <Notice title="成年业务暂不可办理" warning>
          可以继续浏览，原任务和输入保留；成年业务仍以当前有效资格为准。
        </Notice>
      ) : null}
      {terminal ? (
        <>
          <Notice
            title={
              (
                {
                  cancelled: "已取消本次核对",
                  failed: "本次核对未完成",
                  not_eligible: "当前未取得有效成年资格",
                  expired: "本次结果已失效或资格已变化",
                } as Record<string, string>
              )[stage] ?? "核对本次结果"
            }
            warning={stage === "failed" || stage === "expired"}
          >
            {stage === "not_eligible"
              ? "可以浏览与保留草稿；商业发布和线下签约由成年人用自己的账号办理。"
              : "原任务和输入保留。成年业务仍以当前服务端资格为准。"}
          </Notice>
          {stage === "not_eligible" ? null : current &&
            !active(current.state) ? (
            <Button disabled={busy} onClick={restart}>
              重新阅读用途并核对
            </Button>
          ) : (
            <Button disabled={busy} onClick={() => void run(query)}>
              查询原请求
            </Button>
          )}
          {stage === "not_eligible" ? (
            <RowLink
              title="查看成年人办理范围"
              detail="不把本账号临时改成成人"
              to="/reading?type=seller"
            />
          ) : null}
          <RowLink
            title="对结果有疑问"
            detail="私密提出资料事项协助"
            to="/edit?flow=safety&amp;type=data_assistance"
          />
        </>
      ) : null}
      {error ? <Notice title={error} error /> : null}
    </TaskPage>
  );
}
