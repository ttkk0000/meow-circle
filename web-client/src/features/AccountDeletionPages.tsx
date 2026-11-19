import { useEffect, useRef, useState } from "react";
import { api, explain, HttpError, type RecordData } from "../core/api";
import {
  otpExpiryMillis,
  validAuthCode,
  validLoginPhone,
} from "../core/authInput";
import type { DTODataRequestStatus } from "../core/contract";
import {
  clearDraft,
  newDraft,
  readDraft,
  saveDraft,
  type Draft,
} from "../core/drafts";
import { titleOf, usePages } from "../core/pages";
import {
  providerConfiguration,
  providerPopup,
  ProviderPopupError,
} from "../core/provider";
import { invalidateQueries, reload, useQuery } from "../core/query";
import { navigate } from "../core/router";
import { integer, key, parseWire } from "../core/wire";
import { RetrySubmission } from "../components/RetrySubmission";
import {
  Button,
  Card,
  Check,
  Choices,
  Field,
  Notice,
  QueryState,
  RowLink,
  TaskPage,
} from "../components/ui";
import { AuthOtpField } from "./AuthCodePages";
import { stateLabel } from "./workflows";

export type DataRequestPhase =
  | "complete"
  | "retained_copies"
  | "held"
  | "processing"
  | "needs_reconciliation";

export type AccountDeletionStep =
  | "impact"
  | "matters"
  | "verify"
  | "submitting"
  | "failure";

export type AccountDeletionFailureKind =
  | "scope_failed"
  | "rejected"
  | "result_unknown"
  | "capability_invalid";

export interface DataRequestProgressSummary {
  id: bigint;
  version: bigint;
  type: string;
  state: string;
  phase: DataRequestPhase;
  progressVersion: bigint;
  erased: bigint;
  pending: bigint;
  held: bigint;
  physicalPending: bigint;
  providerPending: bigint;
  restrictedCopies: bigint;
  pendingFields: string[];
  externalState: string;
  reviewRequired: boolean;
  nextReviewAt?: string;
}

interface VerificationMethods {
  phone: string;
  password_set: boolean;
  linked: Array<{ id: bigint; provider: string }>;
  available_providers?: string[];
  sms_available: boolean;
}

interface VerificationCapabilities {
  password: boolean;
  email: boolean;
  sms: boolean;
  registration: boolean;
  interaction_contract: bigint;
  scoped_reauth: boolean;
  providers: string[];
}

interface OAuthVerifyResult {
  reauth_token?: string;
  needs_account_choice?: boolean;
}

interface AcceptedDeletionHandoff {
  requestId: string;
  status: DTODataRequestStatus;
  receivedAt: number;
}

let recentAcceptedHandoff: AcceptedDeletionHandoff | undefined;

const fieldLabels: Record<string, string> = {
  bio: "个人介绍",
  avatar: "头像",
  nickname: "昵称",
};

const providerNames: Record<string, string> = {
  wechat: "微信",
  qq: "QQ",
  apple: "Apple",
};

export function handoffDeletionAcceptance(
  requestId: string,
  status: DTODataRequestStatus,
) {
  if (!requestId) return;
  recentAcceptedHandoff = {
    requestId,
    status,
    receivedAt: Date.now(),
  };
}

function takeDeletionAcceptance(
  requestId: string,
): AcceptedDeletionHandoff | undefined {
  if (!recentAcceptedHandoff || recentAcceptedHandoff.requestId !== requestId)
    return undefined;
  const current = recentAcceptedHandoff;
  recentAcceptedHandoff = undefined;
  if (Date.now() - current.receivedAt > 5 * 60 * 1000) return undefined;
  return current;
}

export function statusFromSubmittedRecord(
  record: RecordData | { id?: bigint; version?: bigint; state?: string; data?: Record<string, unknown> } | undefined,
): DTODataRequestStatus {
  const data =
    record && "data" in record && record.data && typeof record.data === "object"
      ? record.data
      : {};
  const items = Array.isArray(data.items) ? data.items : [];
  const fields = Array.isArray(data.fields)
    ? data.fields.map(String)
    : ["bio", "avatar", "nickname"];
  const holds = Array.isArray(data.holds) ? data.holds : [];
  return {
    id: integer(record?.id),
    version: integer(record && "version" in record ? record.version : 1n) || 1n,
    type: String(data.type ?? "close_account"),
    state: String(record && "state" in record && record.state ? record.state : "processing"),
    progress_version: integer(data.progress_version) || 1n,
    erased: 0n,
    pending: BigInt(items.length),
    held: BigInt(holds.length),
    physical_pending: 0n,
    provider_pending: 0n,
    restricted_copies: 0n,
    pending_fields: fields,
    external_state: String(data.external_state ?? "pending_inventory"),
    review_required: false,
    next_review_at: null,
  };
}

export function analyzeDataRequestProgress(
  status: DTODataRequestStatus,
): DataRequestProgressSummary {
  const progressVersion = integer(status.progress_version);
  const erased = integer(status.erased);
  const pending = integer(status.pending);
  const held = integer(status.held);
  const physicalPending = integer(status.physical_pending);
  const providerPending = integer(status.provider_pending);
  const restrictedCopies = integer(status.restricted_copies);
  const pendingFields = Array.isArray(status.pending_fields)
    ? status.pending_fields.map(String).filter(Boolean)
    : [];
  const state = String(status.state ?? "");
  const externalState = String(status.external_state ?? "");
  const reviewRequired = Boolean(status.review_required);
  const nextReviewAt =
    typeof status.next_review_at === "string" && status.next_review_at
      ? status.next_review_at
      : undefined;
  let phase: DataRequestPhase = "needs_reconciliation";
  if (progressVersion === 1n) {
    if (
      state === "completed" &&
      pending === 0n &&
      held === 0n &&
      physicalPending === 0n &&
      providerPending === 0n &&
      pendingFields.length === 0
    ) {
      phase = restrictedCopies > 0n ? "retained_copies" : "complete";
    } else if (held > 0n || state === "partially_held") {
      phase = "held";
    } else {
      phase = "processing";
    }
  }
  return {
    id: integer(status.id),
    version: integer(status.version),
    type: String(status.type ?? ""),
    state,
    phase,
    progressVersion,
    erased,
    pending,
    held,
    physicalPending,
    providerPending,
    restrictedCopies,
    pendingFields,
    externalState,
    reviewRequired,
    nextReviewAt,
  };
}

export function isAccountDeletionDraft(draft: Draft | undefined): boolean {
  if (!draft) return false;
  if (draft.values?.type === "close_account") return true;
  if (draft.original?.path === "data-requests" && draft.original.body) {
    try {
      const parsed = parseWire<Record<string, unknown>>(draft.original.body);
      if (parsed?.type === "close_account") return true;
    } catch {
      return false;
    }
  }
  return false;
}

function formatPendingFields(fields: string[]): string {
  if (!fields.length) return "无待处理资料字段";
  return fields.map((item) => fieldLabels[item] ?? item).join("、");
}

function formatReviewTime(value?: string): string {
  if (!value) return "按服务端复核周期更新";
  const parsed = Date.parse(value);
  return Number.isNaN(parsed)
    ? value
    : new Date(parsed).toLocaleString("zh-CN");
}

export function DataRequestProgressBlock({
  summary,
  requestId,
}: {
  summary: DataRequestProgressSummary;
  requestId: string;
}) {
  return (
    <dl className="auth-facts">
      <div>
        <dt>申请单号</dt>
        <dd>
          {summary.id > 0n
            ? `#${String(summary.id)} · 版本 ${String(summary.version)}`
            : "已受理，等待分配单号"}
        </dd>
      </div>
      <div>
        <dt>原请求号</dt>
        <dd className="break-anywhere">{requestId}</dd>
      </div>
      <div>
        <dt>当前状态</dt>
        <dd>
          {stateLabel(summary.state || "processing")} · 外部处理{" "}
          {stateLabel(summary.externalState || "pending_inventory")}
        </dd>
      </div>
      <div>
        <dt>清除与待办</dt>
        <dd>
          已清除 {String(summary.erased)} 项 · 待处理 {String(summary.pending)}{" "}
          项 · 必要保全 {String(summary.held)} 项
        </dd>
      </div>
      {summary.physicalPending > 0n ||
      summary.providerPending > 0n ||
      summary.restrictedCopies > 0n ? (
        <div>
          <dt>介质与外部</dt>
          <dd>
            物理介质待清除 {String(summary.physicalPending)} 项 · 第三方撤权待确认{" "}
            {String(summary.providerPending)} 项 · 受限副本{" "}
            {String(summary.restrictedCopies)} 份
          </dd>
        </div>
      ) : null}
      <div>
        <dt>资料字段</dt>
        <dd>{formatPendingFields(summary.pendingFields)}</dd>
      </div>
      {summary.held > 0n || summary.nextReviewAt || summary.reviewRequired ? (
        <div>
          <dt>保全复核</dt>
          <dd>
            {summary.reviewRequired ? "已到复核期 · " : ""}
            {formatReviewTime(summary.nextReviewAt)}
          </dd>
        </div>
      ) : null}
    </dl>
  );
}

function AccountDeletionMatters({
  owner,
  disabled,
  onBack,
  onSwitchType,
}: {
  owner: bigint;
  disabled: boolean;
  onBack: () => void;
  onSwitchType: (nextType: "erase_fields" | "matters_assistance") => void;
}) {
  const enabled = owner > 0n;
  const adoptions = usePages("adoptions", {}, enabled);
  const appointments = usePages("appointments", {}, enabled);
  const reviews = usePages("reviews", {}, enabled);
  const cases = usePages("cases", enabled ? { owner_id: owner } : {}, enabled);
  const pets = usePages("pets", enabled ? { owner_id: owner } : {}, enabled);

  const sections = [
    {
      key: "adoptions",
      title: "领养申请与试养交接",
      detail: "进行中的领养、试养与接回协议需先完成交接或撤回，注销不会自动解除现有照护责任。",
      page: adoptions,
      routePrefix: "adoptions",
    },
    {
      key: "appointments",
      title: "预约与照护安排",
      detail: "待确认、已确认或安全暂停中的预约需先核对安排；注销申请不会代替双方取消或确认。",
      page: appointments,
      routePrefix: "appointments",
    },
    {
      key: "reviews",
      title: "审核与异议处理",
      detail: "正在复核的资料或公众安全事项会按规则保留必要核查记录。",
      page: reviews,
      routePrefix: "reviews",
    },
    {
      key: "cases",
      title: "本人发布的求助记录",
      detail: "名下走失或救助求助在正式注销受理后将下线公开入口，涉及线索核实的记录按规则留存。",
      page: cases,
      routePrefix: "cases",
    },
    {
      key: "pets",
      title: "本人名下宠物档案",
      detail: "名下宠物档案与关联日常将纳入注销处理范围；若仍在领养或协作中，请先核对交接。",
      page: pets,
      routePrefix: "pets",
    },
  ];

  return (
    <TaskPage
      title="查看未完成事项"
      subtitle={"M&D｜核对名下领养、预约、审核、求助与宠物档案"}
      scrollScope="task"
      onBack={onBack}
      actions={
        <>
          <Button disabled={disabled} onClick={onBack}>
            返回注销影响说明
          </Button>
          <Button
            kind="primary"
            disabled={disabled}
            onClick={() => onSwitchType("matters_assistance")}
          >
            改用未结事项协助
          </Button>
        </>
      }
    >
      <h1 className="page-headline">先核对名下未结事项，不自动关闭现有安排</h1>
      <Notice title="查看事项不会自动取消或提交注销">
        本页仅读取当前账号名下记录供你逐项核对，不提供「一键关闭全部事项」。如需平台协调未结领养、预约或争议，可改用「未结事项协助」而不必注销账号。
      </Notice>
      {sections.map((section) => (
        <Card key={section.key} title={section.title}>
          <p className="caption muted">{section.detail}</p>
          <QueryState
            loading={section.page.loading}
            error={section.page.error}
            failure={section.page.failure}
            readAt={section.page.readAt}
            onRetry={section.page.retry}
          >
            {section.page.rows.length ? (
              <div className="stack">
                <p className="caption muted">
                  当前已加载 {String(section.page.rows.length)} 条记录
                </p>
                {section.page.rows.slice(0, 10).map((row) => (
                  <RowLink
                    key={String(row.id)}
                    title={titleOf(row)}
                    detail={`#${String(row.id)} · ${stateLabel(row.state)}`}
                    to={`/${section.routePrefix}/${String(row.id)}`}
                  />
                ))}
                {section.page.next > 0n ? (
                  <Button
                    disabled={section.page.loading}
                    onClick={section.page.more}
                  >
                    加载更多{section.title}
                  </Button>
                ) : null}
              </div>
            ) : (
              <p className="caption muted">当前未读到此类记录。</p>
            )}
          </QueryState>
        </Card>
      ))}
    </TaskPage>
  );
}

export function AccountDeletionWorkflow({
  owner,
  draft,
  scope,
  note,
  step,
  busy,
  error,
  errorCode,
  failureKind,
  conflict,
  blocked,
  impactAgreed,
  grant,
  finalConfirmed,
  onStepChange,
  onImpactAgree,
  onGrant,
  onFinalConfirm,
  onBackToForm,
  onSwitchType,
  onRetryPrepare,
  onSubmit,
  onReconciled,
}: {
  owner: bigint;
  draft: Draft;
  scope?: Record<string, unknown>;
  note: string;
  step: AccountDeletionStep;
  busy: boolean;
  error: string;
  errorCode: string;
  failureKind?: AccountDeletionFailureKind;
  conflict: boolean;
  blocked: boolean;
  impactAgreed: boolean;
  grant: string;
  finalConfirmed: boolean;
  onStepChange: (next: AccountDeletionStep) => void;
  onImpactAgree: (value: boolean) => void;
  onGrant: (token: string) => void;
  onFinalConfirm: (value: boolean) => void;
  onBackToForm: () => void;
  onSwitchType: (nextType: "erase_fields" | "matters_assistance") => void;
  onRetryPrepare: () => void;
  onSubmit: () => void;
  onReconciled: (
    status: DTODataRequestStatus,
    outcome: "accepted" | "not_found",
  ) => Promise<void>;
}) {
  const methods = useQuery<VerificationMethods>(
    "auth/methods",
    step === "verify" && owner > 0n,
  );
  const capabilities = useQuery<VerificationCapabilities>(
    "auth/capabilities",
    step === "verify" && owner > 0n,
  );
  const [verifyMethod, setVerifyMethod] = useState<"otp" | "password" | "oauth">(
    "otp",
  );
  const [showMethodSwitcher, setShowMethodSwitcher] = useState(false);
  const [password, setPassword] = useState("");
  const [phone, setPhone] = useState("");
  const [code, setCode] = useState("");
  const [otpUntil, setOtpUntil] = useState(0);
  const [otpExpiresAt, setOtpExpiresAt] = useState(0);
  const [now, setNow] = useState(Date.now());
  const [verifying, setVerifying] = useState(false);
  const [verifyError, setVerifyError] = useState("");
  const [selectedProvider, setSelectedProvider] = useState("");
  const [lookupBusy, setLookupBusy] = useState(false);
  const [lookupError, setLookupError] = useState("");
  const [lookupNotFound, setLookupNotFound] = useState(false);
  const [lookupCapInvalid, setLookupCapInvalid] = useState(false);

  const verifyGeneration = useRef(0);
  const oauthChallenge = useRef("");
  const cancelPopup = useRef<(() => void) | undefined>(undefined);
  const grantCallback = useRef(onGrant);
  const confirmCallback = useRef(onFinalConfirm);
  grantCallback.current = onGrant;
  confirmCallback.current = onFinalConfirm;

  function abortPendingOAuth() {
    const pendingState = oauthChallenge.current;
    oauthChallenge.current = "";
    cancelPopup.current?.();
    cancelPopup.current = undefined;
    if (pendingState) {
      void api
        .request("auth/oauth-finish", {
          method: "POST",
          body: { state: pendingState, credential: {}, cancelled: true },
        })
        .catch(() => {});
    }
  }

  function clearVerificationSecrets(resetGrant = true) {
    verifyGeneration.current++;
    abortPendingOAuth();
    setPassword("");
    setCode("");
    setVerifying(false);
    setVerifyError("");
    if (resetGrant) {
      grantCallback.current("");
      confirmCallback.current(false);
    }
  }

  useEffect(() => {
    clearVerificationSecrets(true);
    return () => {
      verifyGeneration.current++;
      abortPendingOAuth();
    };
  }, [owner, scope]);

  useEffect(() => {
    if (step !== "verify") {
      clearVerificationSecrets(false);
    }
  }, [step]);

  useEffect(() => {
    if (otpUntil <= Date.now() && otpExpiresAt <= Date.now()) return;
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, [otpUntil, otpExpiresAt]);

  const smsReady = Boolean(
    methods.data?.phone &&
      methods.data.sms_available &&
      (capabilities.data?.sms ?? true),
  );
  const passwordReady = Boolean(
    methods.data?.password_set && (capabilities.data?.password ?? true),
  );
  const linkedProviders = (methods.data?.linked ?? []).map((item) =>
    String(item.provider),
  );
  const usableOAuthProviders = linkedProviders.filter(
    (provider) =>
      (provider === "wechat" || provider === "qq") &&
      Boolean(providerConfiguration(provider)) &&
      Boolean(capabilities.data?.scoped_reauth) &&
      Boolean(capabilities.data?.providers?.includes(provider)),
  );

  useEffect(() => {
    if (step !== "verify" || !methods.data) return;
    if (verifyMethod === "otp" && !smsReady) {
      if (passwordReady) setVerifyMethod("password");
      else if (usableOAuthProviders.length) {
        setVerifyMethod("oauth");
        setSelectedProvider(usableOAuthProviders[0]!);
      }
    } else if (verifyMethod === "password" && !passwordReady) {
      if (smsReady) setVerifyMethod("otp");
      else if (usableOAuthProviders.length) {
        setVerifyMethod("oauth");
        setSelectedProvider(usableOAuthProviders[0]!);
      }
    } else if (verifyMethod === "oauth" && !usableOAuthProviders.length) {
      if (smsReady) setVerifyMethod("otp");
      else if (passwordReady) setVerifyMethod("password");
    } else if (
      verifyMethod === "oauth" &&
      usableOAuthProviders.length &&
      !usableOAuthProviders.includes(selectedProvider)
    ) {
      setSelectedProvider(usableOAuthProviders[0]!);
    }
  }, [
    step,
    methods.data,
    smsReady,
    passwordReady,
    usableOAuthProviders.join(","),
    verifyMethod,
    selectedProvider,
  ]);

  function switchVerifyMethod(next: "otp" | "password" | "oauth", provider = "") {
    if (busy || verifying) return;
    clearVerificationSecrets(true);
    setPhone("");
    setOtpExpiresAt(0);
    setVerifyMethod(next);
    if (provider) setSelectedProvider(provider);
    else if (next === "oauth" && usableOAuthProviders.length)
      setSelectedProvider(usableOAuthProviders[0]!);
  }

  async function sendReauthOtp() {
    if (busy || verifying || !smsReady || !validLoginPhone(phone) || now < otpUntil)
      return;
    const current = verifyGeneration.current;
    setVerifying(true);
    setVerifyError("");
    onGrant("");
    onFinalConfirm(false);
    try {
      const result = await api.request<{
        id?: bigint;
        expires_at?: string;
        retry_after_seconds?: bigint;
      }>("auth/otp", {
        method: "POST",
        owner,
        body: { phone: phone.trim(), purpose: "reauth" },
      });
      if (current !== verifyGeneration.current || api.session.user?.id !== owner)
        return;
      const waitSeconds = Math.max(
        1,
        Number(integer(result.retry_after_seconds || 60n)),
      );
      setOtpUntil(Date.now() + waitSeconds * 1000);
      const expiry = otpExpiryMillis(result.expires_at);
      setOtpExpiresAt(
        Number.isFinite(expiry) ? expiry : Date.now() + 5 * 60 * 1000,
      );
    } catch (e) {
      if (current === verifyGeneration.current) setVerifyError(explain(e));
    } finally {
      if (current === verifyGeneration.current) setVerifying(false);
    }
  }

  async function verifyCredential() {
    if (!scope || busy || verifying) return;
    if (verifyMethod === "password" && !password) return;
    if (verifyMethod === "otp" && !validAuthCode(code)) return;
    const current = verifyGeneration.current;
    setVerifying(true);
    setVerifyError("");
    onGrant("");
    onFinalConfirm(false);
    try {
      const body =
        verifyMethod === "password"
          ? { scope, password, code: "" }
          : { scope, password: "", code };
      const result = await api.request<{ reauth_token: string }>(
        "auth/reauth",
        {
          method: "POST",
          owner,
          body,
        },
      );
      if (
        current === verifyGeneration.current &&
        api.session.user?.id === owner &&
        result.reauth_token
      ) {
        grantCallback.current(result.reauth_token);
      }
    } catch (e) {
      if (current === verifyGeneration.current) setVerifyError(explain(e));
    } finally {
      if (current === verifyGeneration.current) {
        setPassword("");
        setCode("");
        setVerifying(false);
      }
    }
  }

  function verifyWithOAuth(provider: string) {
    const configuration = providerConfiguration(provider);
    if (
      !scope ||
      !configuration ||
      !usableOAuthProviders.includes(provider) ||
      typeof BroadcastChannel === "undefined" ||
      busy ||
      verifying
    )
      return;
    const popup = window.open(
      "about:blank",
      "_blank",
      "popup,width=520,height=720",
    );
    if (!popup) {
      setVerifyError("浏览器阻止了官方授权窗口，请允许弹窗或改用手机号／密码验证。");
      return;
    }
    popup.opener = null;
    popup.document.title = "M&D 用途验证授权";
    clearVerificationSecrets(true);
    const attempt = verifyGeneration.current;
    setVerifying(true);
    void (async () => {
      try {
        const ticket = await api.request<{ state: string; nonce: string }>(
          "auth/oauth-start",
          {
            method: "POST",
            owner,
            body: { provider, resume: "close_account", scope },
          },
        );
        if (attempt !== verifyGeneration.current || api.session.user?.id !== owner) {
          popup.close();
          void api
            .request("auth/oauth-finish", {
              method: "POST",
              body: { state: ticket.state, credential: {}, cancelled: true },
            })
            .catch(() => {});
          return;
        }
        oauthChallenge.current = ticket.state;
        const operation = providerPopup(
          provider,
          configuration,
          ticket.state,
          popup,
        );
        cancelPopup.current = operation.cancel;
        const callback = await operation.result;
        if (attempt !== verifyGeneration.current || api.session.user?.id !== owner)
          return;
        cancelPopup.current = undefined;
        const finished = await api.request<OAuthVerifyResult>(
          "auth/oauth-finish",
          {
            method: "POST",
            owner,
            body: {
              state: ticket.state,
              credential: {
                code: callback.code,
                app_id: configuration.appID,
                client: "web",
              },
              cancelled: callback.cancelled,
            },
          },
        );
        oauthChallenge.current = "";
        if (attempt !== verifyGeneration.current || api.session.user?.id !== owner)
          return;
        if (callback.cancelled) {
          setVerifyError("已取消本次第三方用途验证，注销申请尚未提交。");
        } else if (finished.reauth_token) {
          grantCallback.current(finished.reauth_token);
        } else {
          setVerifyError("未能从本次第三方授权取得当前账号的用途验证凭证，请改用手机号或密码验证。");
        }
      } catch (e) {
        if (attempt === verifyGeneration.current) {
          if (e instanceof ProviderPopupError) setVerifyError(e.message);
          else setVerifyError(explain(e));
        }
      } finally {
        if (attempt === verifyGeneration.current) setVerifying(false);
      }
    })();
  }

  async function lookupOriginalRequest() {
    if (lookupBusy || busy) return;
    setLookupBusy(true);
    setLookupError("");
    setLookupNotFound(false);
    setLookupCapInvalid(false);
    try {
      const capDraft = await readDraft(0n, `capability:${draft.requestId}`);
      const access = draft.access ?? capDraft?.access;
      if (!access?.token) {
        setLookupCapInvalid(true);
        setLookupError(
          "本机缺少当前申请的受限查询凭证（X-Request-Access），无法在盲发风险下自动重交，请到恢复中心核对。",
        );
        return;
      }
      const path = access.id
        ? `data-requests/${String(access.id)}`
        : `data-requests/requests/${access.requestId}`;
      const status = await api.request<DTODataRequestStatus>(path, {
        headers: { "X-Request-Access": access.token },
      });
      if (status.type && status.type !== "close_account") {
        setLookupError(
          `原请求编号 #${String(status.id ?? "")} 的事项类型为 ${stateLabel(status.type)}，并非正式注销，不会执行退出或覆盖。`,
        );
        return;
      }
      await onReconciled(status, "accepted");
    } catch (e) {
      if (e instanceof HttpError && e.status === 404) {
        setLookupNotFound(true);
        await onReconciled({}, "not_found");
      } else if (e instanceof HttpError && e.status === 403) {
        setLookupCapInvalid(true);
        setLookupError(
          "当前受限查询凭证已失效或已被撤销（403），不能跳过核对盲目重发。",
        );
      } else {
        setLookupError(explain(e));
      }
    } finally {
      setLookupBusy(false);
    }
  }

  if (step === "matters") {
    return (
      <AccountDeletionMatters
        owner={owner}
        disabled={busy || !!draft.uncertain}
        onBack={() => onStepChange("impact")}
        onSwitchType={onSwitchType}
      />
    );
  }

  if (step === "impact") {
    return (
      <TaskPage
        title="注销账号前"
        subtitle={`M&D｜正式注销申请 · 原编号 ${draft.requestId}`}
        scrollScope="confirmation"
        onBack={onBackToForm}
        actions={
          <>
            <Button
              disabled={busy || !!draft.uncertain}
              onClick={onBackToForm}
            >
              返回修改请求
            </Button>
            <Button
              kind="primary"
              disabled={
                busy ||
                !!draft.uncertain ||
                conflict ||
                blocked ||
                !impactAgreed ||
                !scope
              }
              onClick={() => onStepChange("verify")}
            >
              继续验证身份
            </Button>
          </>
        }
      >
        <h1 className="page-headline">请确认你了解这些影响</h1>
        <Notice title="区分删除指定资料、正式注销与未结事项协助">
          正式注销适用于终止当前 M&amp;D
          账号；若你只想清除个人介绍、头像、昵称或单条内容，请改选「删除部分资料」；若需协调未结领养、预约或争议而不注销账号，请改选「申请未结事项协助」。
          <div className="auth-method-links">
            <Button
              disabled={busy || !!draft.uncertain}
              onClick={() => onSwitchType("erase_fields")}
            >
              改选删除指定资料
            </Button>
            <Button
              disabled={busy || !!draft.uncertain}
              onClick={() => onSwitchType("matters_assistance")}
            >
              改用未结事项协助
            </Button>
          </div>
        </Notice>
        <Card title="正式注销的三项影响">
          <dl className="auth-facts">
            <div>
              <dt>宠物与日记</dt>
              <dd>
                名下公开与私密日记、宠物档案、照护动态、评论、求助、市集服务及本机关联授权将按正式注销范围进入清除队列。
              </dd>
            </div>
            <div>
              <dt>未完成事项</dt>
              <dd>
                进行中的领养试养、预约安排、协作者职责与审核事项不会因提交注销而免除；建议先核对名下未结事项或申请事项协助。
              </dd>
            </div>
            <div>
              <dt>必要历史</dt>
              <dd>
                涉及公众安全、争议处理、履约回执或法定留存的记录会进入「必要保全」并标注复核时间，到期前受限隔离保存。
              </dd>
            </div>
            <div>
              <dt>补充说明</dt>
              <dd>{note.trim() ? note : "未填写补充说明（可选）"}</dd>
            </div>
          </dl>
          <RowLink
            title="查看未完成事项"
            detail="逐项核对名下领养、预约、审核、求助与宠物档案，不自动关闭"
            disabled={busy || !!draft.uncertain}
            onClick={() => onStepChange("matters")}
          />
        </Card>
        <Notice title="受理与清除分开" warning>
          提交正式注销申请并获服务端受理后，当前账号会立即退出所有会话并停止消息推送；但「申请已受理」不代表资料已全部清除，后续可凭本浏览器保存的受限查询能力核对清除、必要保全与受限副本状态。
        </Notice>
        <Check
          checked={impactAgreed}
          disabled={busy || !!draft.uncertain}
          onChange={onImpactAgree}
        >
          我已了解注销影响，并确认提交申请
        </Check>
        {error ? <Notice title={error} error /> : null}
      </TaskPage>
    );
  }

  if (step === "verify" || step === "submitting") {
    const locked = busy || verifying || step === "submitting";
    const otpValidRemaining =
      otpExpiresAt > now ? Math.ceil((otpExpiresAt - now) / 1000) : 0;
    const methodChoices: Array<[string, string]> = [];
    if (smsReady) methodChoices.push(["otp", "绑定手机短信验证码"]);
    if (passwordReady) methodChoices.push(["password", "当前账号密码"]);
    if (usableOAuthProviders.length)
      methodChoices.push(["oauth", "已关联第三方官方授权"]);

    return (
      <TaskPage
        title="确认本人操作"
        subtitle={`M&D｜正式注销用途验证 · 原编号 ${draft.requestId}`}
        scrollScope="confirmation"
        onBack={() => {
          if (!locked) {
            clearVerificationSecrets(true);
            onStepChange("impact");
          }
        }}
        actions={
          <>
            <Button
              disabled={locked || !!draft.uncertain}
              onClick={() => {
                clearVerificationSecrets(true);
                onStepChange("impact");
              }}
            >
              返回影响说明
            </Button>
            <Button
              kind="danger"
              disabled={
                locked ||
                !!draft.uncertain ||
                conflict ||
                blocked ||
                !grant ||
                !finalConfirmed
              }
              onClick={onSubmit}
            >
              {step === "submitting" || busy ? "正在提交注销申请…" : "提交注销申请"}
            </Button>
          </>
        }
      >
        <h1 className="page-headline">请完成本人用途验证，再最终确认注销</h1>
        <Notice title="验证与提交分步确认">
          本次验证仅生成一次性的「正式注销提交」用途凭证（只驻留当前页面内存，不写入草稿、网址或浏览器存储）。通过验证后不会自动提交注销，仍需勾选下方最终确认并点击「提交注销申请」。
        </Notice>
        <QueryState
          loading={methods.loading || capabilities.loading}
          error={methods.error || capabilities.error}
          failure={methods.failure || capabilities.failure}
          onRetry={() => {
            void reload("auth/methods");
            void reload("auth/capabilities");
          }}
        >
          <Card title="当前账号可用验证方式">
            <div className="auth-mode-heading">
              <p className="caption muted">
                当前方式：
                {verifyMethod === "otp"
                  ? `绑定手机短信验证码（${methods.data?.phone || "未绑定"}）`
                  : verifyMethod === "password"
                    ? "当前账号密码"
                    : `已关联第三方授权（${providerNames[selectedProvider] ?? (selectedProvider || "未选择")}）`}
              </p>
              <Button
                kind="quiet"
                disabled={locked}
                onClick={() => setShowMethodSwitcher((old) => !old)}
              >
                {showMethodSwitcher ? "收起其他验证方式" : "换用已关联方式验证"}
              </Button>
            </div>
            {showMethodSwitcher ? (
              <div className="stack">
                {methodChoices.length > 1 ? (
                  <Choices
                    label="切换本次用途验证方式（切换会清空已输入的验证码与已取得的临时凭证）"
                    value={verifyMethod}
                    choices={methodChoices}
                    disabled={locked}
                    onChange={(next) =>
                      switchVerifyMethod(next as "otp" | "password" | "oauth")
                    }
                  />
                ) : null}
                <Notice title="验证方式合同与可用性说明">
                  <ul className="stack">
                    <li>
                      手机短信验证：
                      {smsReady
                        ? `可用（已绑定 ${methods.data?.phone}，需输入完整绑定手机号发送 reauth 验证码）。`
                        : !methods.data?.phone
                          ? "当前账号未绑定手机号，不可用。"
                          : "当前服务端短信通道暂不可用（sms_available=false）。"}
                    </li>
                    <li>
                      当前密码验证：
                      {passwordReady
                        ? "可用（校验当前账号已设置密码）。"
                        : "当前账号尚未设置密码或服务端未启用密码验证。"}
                    </li>
                    <li>
                      第三方关联验证：
                      {usableOAuthProviders.length
                        ? `可用（${usableOAuthProviders.map((p) => providerNames[p] ?? p).join("、")} 已关联且支持用途重验）。`
                        : linkedProviders.some((p) => p === "wechat" || p === "qq")
                          ? "已关联微信／QQ，但当前 Web 环境未配置对应官方 AppID／HTTPS 回跳或服务端未开启 scoped_reauth，请改用手机或密码验证。"
                          : "当前账号未关联可用的微信／QQ Web 授权。"}
                    </li>
                    <li>
                      邮箱验证码与 Apple Web 授权：后端 `auth/reauth` 与邮箱验证码合同未开放 `reauth` 用途，且 Web 端不支持 Apple OAuth 重验；本页不提供虚假入口。
                    </li>
                  </ul>
                </Notice>
              </div>
            ) : null}

            {!smsReady && !passwordReady && !usableOAuthProviders.length ? (
              <Notice title="当前账号暂无可用的本人用途验证方式" error>
                请先前往「账号与安全」绑定手机号或设置密码后再发起正式注销；如需人工协助处理未结事项，可改用「申请未结事项协助」。
                <div className="auth-method-links">
                  <Button
                    disabled={locked}
                    onClick={() => navigate("/security")}
                  >
                    前往账号与安全
                  </Button>
                  <Button
                    disabled={locked}
                    onClick={() => onSwitchType("matters_assistance")}
                  >
                    改用未结事项协助
                  </Button>
                </div>
              </Notice>
            ) : null}

            {verifyMethod === "otp" && smsReady ? (
              <div className="stack">
                <p className="caption muted">
                  已绑定手机号：{methods.data?.phone}。为核对本人持有该号码，请输入完整绑定手机号获取 6 位用途验证码。
                </p>
                <Field
                  label="当前绑定的完整手机号"
                  value={phone}
                  inputMode="tel"
                  autoComplete="tel"
                  placeholder="请输入当前账号绑定的完整手机号"
                  disabled={locked}
                  onChange={(value) => {
                    setPhone(value);
                    setCode("");
                    if (grant) {
                      onGrant("");
                      onFinalConfirm(false);
                    }
                  }}
                />
                <div className="auth-method-links">
                  <Button
                    disabled={
                      locked || !validLoginPhone(phone) || now < otpUntil
                    }
                    onClick={() => void sendReauthOtp()}
                  >
                    {otpUntil > now
                      ? `${Math.ceil((otpUntil - now) / 1000)} 秒后可重发`
                      : "发送注销用途验证码"}
                  </Button>
                  {otpValidRemaining > 0 ? (
                    <span className="caption muted">
                      当前验证码剩余有效期约 {Math.ceil(otpValidRemaining / 60)}{" "}
                      分钟（{otpValidRemaining} 秒）
                    </span>
                  ) : otpExpiresAt > 0 ? (
                    <span className="caption muted">
                      验证码已过期，请重新发送
                    </span>
                  ) : null}
                </div>
                <AuthOtpField
                  value={code}
                  disabled={locked}
                  label="注销用途短信验证码，6位数字"
                  onChange={(value) => {
                    setCode(value);
                    if (grant) {
                      onGrant("");
                      onFinalConfirm(false);
                    }
                  }}
                />
                <Button
                  kind="primary"
                  disabled={locked || !validAuthCode(code)}
                  onClick={() => void verifyCredential()}
                >
                  {verifying ? "正在核验短信验证码…" : "验证本次注销用途"}
                </Button>
              </div>
            ) : null}

            {verifyMethod === "password" && passwordReady ? (
              <div className="stack">
                <Field
                  label="当前账号密码"
                  type="password"
                  autoComplete="current-password"
                  value={password}
                  disabled={locked}
                  onChange={(value) => {
                    setPassword(value);
                    if (grant) {
                      onGrant("");
                      onFinalConfirm(false);
                    }
                  }}
                />
                <Button
                  kind="primary"
                  disabled={locked || !password}
                  onClick={() => void verifyCredential()}
                >
                  {verifying ? "正在核验当前密码…" : "验证本次注销用途"}
                </Button>
              </div>
            ) : null}

            {verifyMethod === "oauth" && usableOAuthProviders.length ? (
              <div className="stack">
                <p className="caption muted">
                  使用当前账号已关联的官方第三方账号完成一次性用途核验（不会新建或切换登录账号）：
                </p>
                <div className="auth-method-links">
                  {usableOAuthProviders.map((provider) => (
                    <Button
                      key={provider}
                      kind="primary"
                      disabled={locked}
                      onClick={() => verifyWithOAuth(provider)}
                    >
                      {verifying && selectedProvider === provider
                        ? `正在核验${providerNames[provider] ?? provider}…`
                        : `使用${providerNames[provider] ?? provider}验证本次用途`}
                    </Button>
                  ))}
                </div>
              </div>
            ) : null}

            {verifyError ? <Notice title={verifyError} error /> : null}
          </Card>
        </QueryState>

        {grant ? (
          <Card title="已通过本人验证 · 待你最终确认提交">
            <Notice title="已取得本次注销用途凭证（尚未提交）" success>
              本人用途验证已通过。若你返回上一步、切换验证方式或关闭页面，本次临时凭证会立即作废。请确认以下最终退出告知后再点击「提交注销申请」。
            </Notice>
            <Check
              checked={finalConfirmed}
              disabled={locked || !!draft.uncertain}
              onChange={onFinalConfirm}
            >
              我已确认注销此账号，并了解提交后会立即退出
            </Check>
          </Card>
        ) : (
          <Notice title="请先完成上方本人操作验证">
            只有通过本人用途验证并勾选最终确认后，「提交注销申请」按钮才会启用。
          </Notice>
        )}
        {error ? <Notice title={error} error /> : null}
      </TaskPage>
    );
  }

  const effectiveFailure: AccountDeletionFailureKind =
    failureKind ??
    (draft.uncertain
      ? "result_unknown"
      : !scope
        ? "scope_failed"
        : "rejected");

  const reauthExpired =
    effectiveFailure === "rejected" &&
    [
      "purpose_required",
      "invalid_reauth",
      "reauth_expired",
      "authentication_required",
      "forbidden",
    ].includes(errorCode);

  return (
    <TaskPage
      title="注销暂未完成"
      subtitle={`M&D｜原请求编号 ${draft.requestId}`}
      scrollScope="confirmation"
      onBack={
        effectiveFailure === "result_unknown" ? undefined : onBackToForm
      }
      actions={
        effectiveFailure === "scope_failed" ? (
          <>
            <Button disabled={busy} onClick={onBackToForm}>
              返回修改请求
            </Button>
            <Button
              disabled={busy}
              onClick={() => onSwitchType("matters_assistance")}
            >
              改用未结事项协助
            </Button>
            <Button kind="primary" disabled={busy} onClick={onRetryPrepare}>
              {busy ? "正在重新加载…" : "重新加载注销影响"}
            </Button>
          </>
        ) : effectiveFailure === "result_unknown" && !lookupNotFound ? (
          <>
            <Button
              disabled={busy || lookupBusy}
              onClick={() =>
                navigate(
                  `/recovery?view=close-account&request=${encodeURIComponent(draft.requestId)}`,
                )
              }
            >
              前往受限查询页核对
            </Button>
            <Button
              kind="primary"
              disabled={busy || lookupBusy}
              onClick={() => void lookupOriginalRequest()}
            >
              {lookupBusy ? "正在查询原请求…" : "查询原请求状态"}
            </Button>
          </>
        ) : (
          <>
            <Button disabled={busy || lookupBusy} onClick={onBackToForm}>
              返回修改请求
            </Button>
            {scope ? (
              <Button
                kind="primary"
                disabled={busy || lookupBusy}
                onClick={() => {
                  setLookupNotFound(false);
                  setLookupError("");
                  onStepChange("verify");
                }}
              >
                {lookupNotFound
                  ? "按原编号重新验证并确认提交"
                  : reauthExpired
                    ? "重新验证本人操作"
                    : "返回验证步骤重试"}
              </Button>
            ) : (
              <Button kind="primary" disabled={busy} onClick={onRetryPrepare}>
                重新加载注销影响
              </Button>
            )}
          </>
        )
      }
    >
      <h1 className="page-headline">
        {effectiveFailure === "result_unknown"
          ? "提交结果暂未确认，你的申请没有丢失"
          : effectiveFailure === "scope_failed"
            ? "未能加载本次注销影响范围"
            : "本次注销申请暂未完成提交"}
      </h1>
      {effectiveFailure === "scope_failed" ? (
        <Notice title="尚未发起注销提交，当前账号保持登录" warning>
          读取受限查询凭证或注销用途范围（`data-requests/scope`）时未成功，服务端尚未创建任何注销请求。你可以重试加载影响范围，或先改用「未结事项协助」。
        </Notice>
      ) : null}
      {effectiveFailure === "result_unknown" ? (
        <Notice title="结果未知时禁止盲目重发" warning>
          注销请求已发出但未收到明确回执（原请求编号 `{draft.requestId}`）。由于正式注销一旦受理会立即递增会话代次并退出账号，在核对原编号回执前已锁定重复提交。
        </Notice>
      ) : null}
      {effectiveFailure === "rejected" ? (
        <Notice title="服务端已明确拒绝本次请求，未受理注销" error>
          本次请求在服务端校验阶段被明确拒绝，账号仍保持当前状态，不会伪造「已受理」或强行退出。
          {reauthExpired
            ? "一次性用途验证凭证已消费或过期，请返回验证步骤重新完成本人验证后再提交。"
            : ""}
        </Notice>
      ) : null}
      <Card title="原申请核对信息">
        <dl className="auth-facts">
          <div>
            <dt>原请求号</dt>
            <dd className="break-anywhere">{draft.requestId}</dd>
          </div>
          <div>
            <dt>申请类型</dt>
            <dd>正式注销账号（close_account）</dd>
          </div>
          <div>
            <dt>当前状态</dt>
            <dd>
              {lookupNotFound
                ? "已核实服务端未收到原请求（404），可按原编号重新验证后提交"
                : effectiveFailure === "result_unknown"
                  ? "提交结果待核对（已锁定盲重发）"
                  : effectiveFailure === "scope_failed"
                    ? "范围准备未完成（未发送注销请求）"
                    : `未受理${errorCode ? `（${errorCode}）` : ""}`}
            </dd>
          </div>
          <div>
            <dt>补充说明</dt>
            <dd>{note.trim() ? note : "未填写补充说明"}</dd>
          </div>
        </dl>
      </Card>
      {error ? <Notice title={error} error /> : null}
      {lookupError ? <Notice title={lookupError} error /> : null}
      {lookupNotFound ? (
        <Notice title="已确认服务端未生成原注销单" success>
          按原请求编号 `{draft.requestId}` 查询返回 404，确认此前请求未被服务端记录。你现在可以点击「按原编号重新验证并确认提交」安全继续。
        </Notice>
      ) : null}
      {lookupCapInvalid ? (
        <Notice title="本机受限查询凭证不可用" warning>
          请前往「本机草稿与待核对事项」核对其他记录，或在确认账号仍处于登录状态时返回重新准备请求。
          <RowLink title="前往本机恢复中心" to="/recovery" />
        </Notice>
      ) : null}
    </TaskPage>
  );
}

export function AccountDeletionResultPage({
  requestId,
  onBackToAll,
}: {
  requestId: string;
  onBackToAll: () => void;
}) {
  const [capabilityDraft, setCapabilityDraft] = useState<Draft>();
  const [ownerDraft, setOwnerDraft] = useState<Draft>();
  const [loadingDraft, setLoadingDraft] = useState(true);
  const [status, setStatus] = useState<DTODataRequestStatus>();
  const [acceptedFromSubmit, setAcceptedFromSubmit] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [httpStatus, setHttpStatus] = useState(0);
  const [confirmRevoke, setConfirmRevoke] = useState(false);
  const [revoking, setRevoking] = useState(false);
  const [revoked, setRevoked] = useState(false);

  async function queryStatus(targetCap?: Draft, silentIfHandoff = false) {
    const activeCap = targetCap ?? capabilityDraft;
    const access = activeCap?.access ?? ownerDraft?.access;
    if (!access?.token) {
      setError("当前浏览器未找到该注销申请的受限查询凭证（X-Request-Access）。");
      return;
    }
    setBusy(true);
    if (!silentIfHandoff) setError("");
    setHttpStatus(0);
    try {
      const path = access.id
        ? `data-requests/${String(access.id)}`
        : `data-requests/requests/${access.requestId}`;
      const result = await api.request<DTODataRequestStatus>(path, {
        headers: { "X-Request-Access": access.token },
      });
      setStatus(result);
      setError("");
      const capRoute = `capability:${access.requestId}`;
      const existing = await readDraft(0n, capRoute);
      const base =
        existing ??
        newDraft(0n, capRoute, {
          type: String(result.type ?? "close_account"),
        });
      const updated = await saveDraft({
        ...base,
        values: {
          ...base.values,
          type: String(result.type ?? base.values?.type ?? "close_account"),
        },
        uncertain: false,
        original: undefined,
        access: { ...access, id: integer(result.id) || access.id },
      });
      setCapabilityDraft(updated);
      if (
        ownerDraft &&
        (ownerDraft.uncertain || ownerDraft.original) &&
        result.type === "close_account"
      ) {
        const cleared = await clearDraft(ownerDraft);
        setOwnerDraft(cleared);
        invalidateQueries();
        if (api.session.authenticated || api.session.user) {
          api.endSession();
        }
      }
    } catch (e) {
      if (e instanceof HttpError) setHttpStatus(e.status);
      setError(explain(e));
    } finally {
      setBusy(false);
    }
  }

  useEffect(() => {
    let active = true;
    setLoadingDraft(true);
    setError("");
    setHttpStatus(0);
    setConfirmRevoke(false);
    setRevoked(false);
    const handoff = takeDeletionAcceptance(requestId);
    if (handoff) {
      setAcceptedFromSubmit(true);
      setStatus(handoff.status);
    }
    const owner = api.session.user?.id ?? 0n;
    void Promise.all([
      readDraft(0n, `capability:${requestId}`),
      owner > 0n ? readDraft(owner, "data::0") : Promise.resolve(undefined),
    ])
      .then(async ([cap, currentOwnerDraft]) => {
        if (!active) return;
        const matchedOwnerDraft =
          currentOwnerDraft &&
          (currentOwnerDraft.requestId === requestId ||
            currentOwnerDraft.access?.requestId === requestId)
            ? currentOwnerDraft
            : undefined;
        setCapabilityDraft(cap);
        setOwnerDraft(matchedOwnerDraft);
        setLoadingDraft(false);
        const sourceDraft = cap?.access ? cap : matchedOwnerDraft;
        if (sourceDraft?.access) {
          await queryStatus(sourceDraft, Boolean(handoff));
        } else if (!handoff) {
          setError(
            "此浏览器未保存原编号对应的受限查询凭证；受限凭证仅保存在发起请求的浏览器中，不会通过网址传输。",
          );
        }
      })
      .catch((e) => {
        if (!active) return;
        setLoadingDraft(false);
        if (!handoff) setError(explain(e));
      });
    return () => {
      active = false;
    };
  }, [requestId]);

  async function revokeLocalCapability() {
    const access = capabilityDraft?.access ?? ownerDraft?.access;
    const id = integer(status?.id) || integer(access?.id);
    if (!access?.token || id <= 0n || revoking) return;
    setRevoking(true);
    setError("");
    try {
      await api.request(`data-requests/${String(id)}/revoke-access`, {
        method: "POST",
        headers: { "X-Request-Access": access.token },
      });
      const capRoute = `capability:${access.requestId}`;
      const existing = await readDraft(0n, capRoute);
      if (existing) {
        const cleared = await clearDraft(existing);
        setCapabilityDraft(cleared);
      }
      setRevoked(true);
      setConfirmRevoke(false);
    } catch (e) {
      setError(explain(e));
    } finally {
      setRevoking(false);
    }
  }

  async function clearInvalidLocalCapability() {
    const capRoute = `capability:${requestId}`;
    const existing = await readDraft(0n, capRoute);
    if (existing) {
      const cleared = await clearDraft(existing);
      setCapabilityDraft(cleared);
    }
    onBackToAll();
  }

  if (loadingDraft && !status) {
    return (
      <TaskPage
        title="注销申请处理中"
        subtitle={`M&D｜原请求编号 ${requestId}`}
        onBack={onBackToAll}
      >
        <p role="status">正在读取本机受限查询凭证并核对注销进度…</p>
      </TaskPage>
    );
  }

  const wrongType =
    status?.type && status.type !== "close_account" ? status.type : "";
  const summary =
    status && !wrongType ? analyzeDataRequestProgress(status) : undefined;

  if (
    summary &&
    (summary.phase === "processing" || summary.phase === "held")
  ) {
    return (
      <TaskPage
        title="注销申请处理中"
        subtitle={`M&D｜受限进度查询 · #${String(summary.id || "待分配")}`}
        scrollScope="task"
        onBack={onBackToAll}
        actions={
          <>
            <Button onClick={onBackToAll}>查看本设备其他受限进度</Button>
            <Button onClick={() => navigate("/")}>浏览公开内容</Button>
            <Button
              kind="primary"
              disabled={busy}
              onClick={() => void queryStatus()}
            >
              {busy ? "正在核对最新进度…" : "重新核对进度"}
            </Button>
          </>
        }
      >
        <h1 className="page-headline">
          {summary.phase === "held"
            ? "注销申请处理中，部分内容处于必要保全"
            : acceptedFromSubmit
              ? "申请已受理，当前账号已安全退出"
              : "注销申请已受理，正在分步清除"}
        </h1>
        <Notice
          title="申请已受理不等于资料已全部清除"
          warning={summary.phase === "held"}
          success={summary.phase !== "held" && acceptedFromSubmit}
        >
          当前设备及所有已登录会话已退出，推送通知已停用。服务端正按注销清单分批处理个人资料字段、关联记录、物理文件与第三方授权撤回；仅当待处理与必要保全全部归零后才会进入「注销已完成」。
        </Notice>
        <Card title="当前受限查询进度">
          <DataRequestProgressBlock summary={summary} requestId={requestId} />
        </Card>
        {summary.phase === "held" ? (
          <Notice title="存在必要保全记录（到期前暂缓清除）" warning>
            当前共有 {String(summary.held)} 项内容因安全核查、未结履约或法定留存要求处于必要保全状态，下次复核时间：
            {formatReviewTime(summary.nextReviewAt)}。
            {summary.reviewRequired
              ? "当前记录已到达复核期限，正在等待后台复核释放。"
              : ""}
            现行受限状态接口（`DataRequestStatus`）仅返回保全数量与复核时间，尚未下发逐项保全依据与责任岗位明细。
          </Notice>
        ) : null}
        <Notice title="受限查询凭证保存在当前浏览器">
          此页面通过 `X-Request-Access`
          受限凭证只读查询本单进度，凭证不写入网址，也不会恢复已退出的账号登录态。
        </Notice>
        {error ? (
          <Notice title={`最新刷新未成功：${error}`} warning />
        ) : null}
      </TaskPage>
    );
  }

  if (
    summary &&
    (summary.phase === "complete" || summary.phase === "retained_copies")
  ) {
    return (
      <TaskPage
        title="注销处理结果"
        subtitle={`M&D｜受限结果回执 · #${String(summary.id)}`}
        scrollScope="task"
        onBack={onBackToAll}
        actions={
          <>
            <Button onClick={onBackToAll}>查看本设备其他受限进度</Button>
            <Button onClick={() => navigate("/")}>浏览公开内容</Button>
            <Button
              kind="primary"
              disabled={busy || revoked}
              onClick={() => void queryStatus()}
            >
              {busy ? "正在重新核对…" : "重新核对结果"}
            </Button>
          </>
        }
      >
        <h1 className="page-headline">
          {summary.phase === "complete"
            ? "账号注销已完成"
            : "注销主体已处理，仍有受限副本保留"}
        </h1>
        {summary.phase === "complete" ? (
          <Notice title="主体资料与关联记录已按注销范围清除" success>
            服务端已确认当前注销单（#{String(summary.id)}）的待处理项、待清除资料字段、物理介质待办、第三方授权撤回与必要保全均已归零（已清除{" "}
            {String(summary.erased)} 项）。
          </Notice>
        ) : (
          <Notice title="主体已注销，但仍保留受限隔离副本" warning>
            账号主体与线上业务记录已完成注销清除（已清除 {String(summary.erased)}{" "}
            项），但外部或备份处理器仍登记有 {String(summary.restrictedCopies)}{" "}
            份受限备份副本（`restricted_copies`）。受限副本仅在灾备周期内封存隔离，不用于日常业务展示，也不会将此状态冒充为「全部副本已物理清零」。
          </Notice>
        )}
        <Card title="最终处理回执">
          <DataRequestProgressBlock summary={summary} requestId={requestId} />
        </Card>
        {revoked ? (
          <Notice title="本机受限查询凭证已撤销" success>
            服务端已注销本次 `X-Request-Access`
            查询哈希，且本浏览器已清除该受限凭证；已完成的注销处理结果不受影响。
          </Notice>
        ) : capabilityDraft?.access?.token && summary.id > 0n ? (
          <Card title="受限查询凭证管理（可选）">
            <p className="caption muted">
              如你已在当前设备核对完注销结果，可主动撤销本机保存的受限查询凭证（调用 `POST data-requests/{String(summary.id)}/revoke-access`）。撤销后任何人均无法再凭此凭证读取该单状态。
            </p>
            {confirmRevoke ? (
              <div className="stack">
                <Notice title="确认撤销本机查询凭证？" warning>
                  撤销后不可恢复，当前浏览器将立即清空该查询凭证，不再显示此单进度。
                </Notice>
                <div className="auth-method-links">
                  <Button
                    disabled={revoking}
                    onClick={() => setConfirmRevoke(false)}
                  >
                    暂不撤销
                  </Button>
                  <Button
                    kind="danger"
                    disabled={revoking}
                    onClick={() => void revokeLocalCapability()}
                  >
                    {revoking ? "正在撤销凭证…" : "确认撤销本机查询凭证"}
                  </Button>
                </div>
              </div>
            ) : (
              <Button
                disabled={busy}
                onClick={() => setConfirmRevoke(true)}
              >
                撤销本机查询凭证
              </Button>
            )}
          </Card>
        ) : null}
        {error ? <Notice title={error} error /> : null}
      </TaskPage>
    );
  }

  const recoveryDraftForRetry =
    httpStatus === 404 &&
    api.session.authenticated &&
    (ownerDraft?.original ? ownerDraft : capabilityDraft?.original ? capabilityDraft : undefined);

  return (
    <TaskPage
      title="注销暂未完成"
      subtitle={`M&D｜受限查询核对 · 原编号 ${requestId}`}
      scrollScope="task"
      onBack={onBackToAll}
      actions={
        <>
          <Button onClick={onBackToAll}>返回本机恢复中心</Button>
          <Button onClick={() => navigate("/")}>浏览公开内容</Button>
          {capabilityDraft?.access?.token || ownerDraft?.access?.token ? (
            <Button
              kind="primary"
              disabled={busy}
              onClick={() => void queryStatus()}
            >
              {busy ? "正在重新查询…" : "重新查询原请求"}
            </Button>
          ) : null}
        </>
      }
    >
      <h1 className="page-headline">
        {wrongType
          ? "该受限凭证并非正式注销申请"
          : summary?.phase === "needs_reconciliation"
            ? "注销回执版本待进一步核对"
            : httpStatus === 404
              ? "尚未查到该编号的注销受理记录"
              : httpStatus === 403
                ? "本机受限查询凭证已失效或已撤销"
                : "暂时无法确认注销进度"}
      </h1>
      {wrongType ? (
        <Notice title="已拦截跨类型状态混用" error>
          原编号 `{requestId}` 在服务端记录的类型为 `{stateLabel(wrongType)}`，并非正式注销（`close_account`）。请返回恢复中心按原事项类型查看。
        </Notice>
      ) : summary?.phase === "needs_reconciliation" ? (
        <>
          <Notice title="服务端返回了更高或未知的进度协议版本" warning>
            当前返回 `progress_version=${String(summary.progressVersion)}`（状态：`{stateLabel(summary.state)}`），客户端不会将未知协议版本猜作「已全部完成」。以下为服务端返回的原始核对统计：
          </Notice>
          <Card title="待人工或新版本核对的回执数据">
            <DataRequestProgressBlock summary={summary} requestId={requestId} />
          </Card>
        </>
      ) : httpStatus === 404 ? (
        <Notice title="服务端未找到原编号的注销记录（404）" warning>
          按原请求编号 `{requestId}` 查询返回 404，说明此前提交可能因网络中断未到达服务端，不能当作已受理或已完成注销。
          {!api.session.authenticated
            ? "如你仍需注销账号，请重新登录后进入「资料删除、正式注销与事项协助」重新验证并提交。"
            : ""}
        </Notice>
      ) : httpStatus === 403 ? (
        <Notice title="受限查询凭证已过期或已被撤销（403）" error>
          当前浏览器保存的 `X-Request-Access` 凭证已无法读取该单（可能已超过 180 天有效期或已主动撤销）。客户端不会猜测注销已完成；你可手动清理此条失效本机记录。
          <Button disabled={busy} onClick={() => void clearInvalidLocalCapability()}>
            清理此条失效本机凭证
          </Button>
        </Notice>
      ) : (
        <Notice title="你的申请核对编号仍保留在本浏览器" warning>
          当前未能完成受限查询，本机保存的请求编号 `{requestId}` 与受限查询凭证未被清除，请待网络或服务恢复后点击「重新查询原请求」。
        </Notice>
      )}
      {error ? <Notice title={error} error /> : null}
      {recoveryDraftForRetry ? (
        <Card title="已确认服务端未收到原请求 · 可按原内容重新验证提交">
          <RetrySubmission
            draft={recoveryDraftForRetry}
            onDone={() => void queryStatus()}
          />
        </Card>
      ) : null}
    </TaskPage>
  );
}
