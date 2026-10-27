import { useState } from "react";
import { api, explain } from "../core/api";
import { reload, useQuery } from "../core/query";
import { back, navigate, returnTo } from "../core/router";
import {
  Button,
  Card,
  Field,
  Notice,
  QueryState,
  RowLink,
  TaskPage,
} from "../components/ui";
import { LegalLinks } from "../components/AuthPolicies";
import { workflowURL } from "./workflows";
import type {
  DTOReadingFact,
  DTOReadingDocument,
  DTOReadingCatalog,
} from "../core/contract";

type ReadingFact = Required<DTOReadingFact>;
type ReadingDocument = Required<Omit<DTOReadingDocument, "facts">> & {
  facts: ReadingFact[];
};
type ReadingCatalog = Required<Omit<DTOReadingCatalog, "documents">> & {
  documents: ReadingDocument[];
};
const dataTypes = new Set([
  "privacy",
  "inventory",
  "sdk",
  "providers",
  "retention",
]);
const knownTypes = new Set([
  ...dataTypes,
  "service",
  "adoption",
  "teen",
  "seller",
]);
function selectDocument(catalog: ReadingCatalog | undefined, type: string) {
  const rows =
    catalog?.documents.filter((document) => document.type === type) ?? [];
  return (
    rows.find((document) => document.status === "published") ??
    rows.find((document) => document.status !== "archived") ??
    rows[0]
  );
}
const labels: Record<string, string> = {
  published: "已发布文本",
  archived: "历史只读文本",
  proposed: "正在公示，未默认同意",
  draft: "草案预览，尚未正式生效",
  reference: "内容说明",
};
const documentURL = (key: string) => `/reading?doc=${encodeURIComponent(key)}`;

function DocumentFacts({ facts }: { facts: ReadingFact[] }) {
  return facts.length ? (
    <Card>
      {facts.map((fact, index) => (
        <div className="reading-fact" key={index}>
          <span className="muted reading-label">{fact.label}</span>
          <p>{fact.text}</p>
        </div>
      ))}
    </Card>
  ) : null;
}

export function ReadingPage({
  documentKey = "",
  documentType = "",
  login,
}: {
  documentKey?: string;
  documentType?: string;
  login: () => void;
}) {
  const byType = knownTypes.has(documentType);
  const valid = byType || /^[a-z][a-z0-9-]{0,63}$/.test(documentKey);
  const path = valid && !byType ? `reading/${documentKey}` : null;
  const single = useQuery<ReadingDocument>(path);
  const catalog = useQuery<ReadingCatalog>(byType ? "reading" : null);
  const result = byType ? catalog : single;
  const [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const document = byType
    ? selectDocument(catalog.data, documentType)
    : single.data;
  async function download() {
    if (!document || busy) return;
    setBusy(true);
    setError("");
    try {
      const blob = await api.request<Blob>(`reading/${document.key}/download`, {
        blob: true,
      });
      const url = URL.createObjectURL(blob),
        link = window.document.createElement("a");
      link.href = url;
      link.download = `mnd-${document.key}.txt`;
      link.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (e) {
      setError(explain(e));
    } finally {
      setBusy(false);
    }
  }
  if (!valid)
    return (
      <TaskPage title="阅读入口无效">
        <Notice title="没有找到这份材料" />
        <RowLink title="返回规则中心" to="/rules" />
      </TaskPage>
    );
  return (
    <TaskPage
      title={
        document?.title ??
        (documentKey === "care-first-cat" ? "养宠指南" : "阅读说明")
      }
      actions={
        <Button
          kind="primary"
          onClick={() =>
            document?.type === "service"
              ? login()
              : returnTo(
                  document?.type === "guide"
                    ? "/"
                    : document?.type === "privacy"
                      ? "/settings"
                      : dataTypes.has(document?.type ?? "")
                        ? "/privacy"
                        : "/rules",
                )
          }
        >
          {document?.type === "guide"
            ? "看看大家的日常"
            : document?.type === "service"
              ? "返回登录"
              : document?.type === "privacy"
                ? "返回设置"
                : dataTypes.has(document?.type ?? "")
                  ? "返回隐私与数据"
                  : "返回规则中心"}
        </Button>
      }
    >
      <QueryState
        loading={result.loading}
        error={result.error}
        failure={result.failure}
        readAt={result.readAt}
        onRetry={() => void reload(byType ? "reading" : path!)}
      >
        {!result.loading && !result.error && !document ? (
          <Notice title="尚未发布这类材料">
            可返回规则中心查看已配置文本，或通过隐私与安全报告联系处理。
          </Notice>
        ) : null}
        {document ? (
          <>
            {document.cover_asset === "first-cat" ? (
              <img
                className="reading-cover cat-guide"
                src="/app/assets/cats/golden-portrait.jpg"
                alt="内容配图，非用户档案"
              />
            ) : null}
            {document.type === "guide" ? (
              <p className="muted caption">{document.notice}</p>
            ) : (
              <Notice title={labels[document.status] ?? "当前文本"}>
                {document.notice ||
                  "请核对适用版本和日期；阅读不会自动同意新规则。"}
              </Notice>
            )}
            {document.heading ? (
              <h2
                className={
                  document.type === "guide" ? "reading-heading" : "reading-lead"
                }
              >
                {document.heading}
              </h2>
            ) : null}
            <DocumentFacts facts={document.facts} />
            {document.paragraphs.map((text, index) => (
              <p className="prose" key={index}>
                {text}
              </p>
            ))}
            {document.changes.length ? (
              <Card title="重要变更">
                {document.changes.map((text, index) => (
                  <p key={index}>{text}</p>
                ))}
              </Card>
            ) : null}
            {document.type === "guide" ? (
              <RowLink title="回到社区交流" to="/" />
            ) : (
              <>
                <p className="caption muted">
                  版本：{document.revision}
                  {document.effective_from
                    ? ` · 生效时间 ${document.effective_from}`
                    : ""}
                  {document.effective_until
                    ? ` · 适用截止 ${document.effective_until}`
                    : ""}
                </p>
                <Button onClick={() => void download()} disabled={busy}>
                  下载完整文本 · {document.revision}
                </Button>
                {error ? <Notice title={error} error /> : null}
                {document.type === "teen" ? (
                  <>
                    <RowLink title="未满14岁的监护授权" to="/guardians" />
                    <RowLink
                      title="年龄／监护有误，申请复核"
                      to={workflowURL("safety")}
                    />
                    <RowLink title="个人资料权利" to={workflowURL("data")} />
                    <RowLink title="继续前检查交流资格" to="/security" />
                  </>
                ) : null}
                {document.type === "seller" ? (
                  <>
                    <RowLink title="查看合同与售后" to="/appointments" />
                    <RowLink
                      title="查看隐私与权利渠道"
                      to={workflowURL("safety")}
                    />
                  </>
                ) : null}
                {document.type === "privacy" ? (
                  <>
                    <RowLink
                      title="个人信息处理清单"
                      detail="用途、处理方与保存期限"
                      to="/reading?type=inventory"
                    />
                    <RowLink
                      title="访问／更正／撤回／删除"
                      detail="查看真实请求流程"
                      to={workflowURL("data")}
                    />
                    <RowLink
                      title="未成年人专门规则"
                      detail="监护授权与保护设置"
                      to="/reading?type=teen"
                    />
                    <RowLink
                      title="第三方与数据去向"
                      detail="逐项查看用途与处理范围"
                      to="/reading?type=providers"
                    />
                  </>
                ) : null}
                {dataTypes.has(document.type) ? (
                  <>
                    <RowLink
                      title="查看与撤回授权"
                      detail="必要用途及影响分项说明"
                      to="/security"
                    />
                    {document.type !== "sdk" ? (
                      <RowLink
                        title="第三方与公开联系"
                        to="/reading?type=sdk"
                      />
                    ) : null}
                    {document.type !== "retention" ? (
                      <RowLink
                        title="必要留存与复核"
                        to="/reading?type=retention"
                      />
                    ) : null}
                    <RowLink
                      title="申请删除、解释或更正范围"
                      to={workflowURL("data")}
                    />
                    <RowLink
                      title="隐私投诉，无需注册"
                      to={workflowURL("safety", "", 0n, { type: "privacy" })}
                    />
                  </>
                ) : null}
                <RowLink title="完整规则与历史版本" to="/rules?view=history" />
                <RowLink title="表达意见或申诉" to="/rules?view=appeal" />
              </>
            )}
          </>
        ) : null}
      </QueryState>
    </TaskPage>
  );
}

export function Rules({
  params = new URLSearchParams(),
}: {
  params?: URLSearchParams;
}) {
  const view = params.get("view") ?? "";
  const result = useQuery<ReadingCatalog>("reading");
  const [query, setQuery] = useState("");
  const catalog = result.data;
  const documents = (catalog?.documents ?? []).filter(
    (document) => document.type !== "guide",
  );
  const titles: Record<string, string> = {
    history: "规则历史与下载",
    changes: "规则变更与退出",
    appeal: "处理依据与人工申诉",
  };
  return (
    <TaskPage
      title={titles[view] ?? "平台规则中心"}
      actions={
        <Button
          kind="primary"
          onClick={() => (view ? returnTo("/rules") : back())}
        >
          {view ? "返回规则中心" : "返回原页面"}
        </Button>
      }
    >
      <QueryState
        loading={result.loading}
        error={result.error}
        failure={result.failure}
        readAt={result.readAt}
        onRetry={() => void reload("reading")}
      >
        {view === "history" ? (
          <>
            {documents.map((document) => (
              <RowLink
                key={document.key}
                title={document.title}
                detail={`${document.revision} · ${labels[document.status] ?? "当前文本"}`}
                to={documentURL(document.key)}
              />
            ))}
            {catalog &&
            !documents.some((document) =>
              ["published", "archived"].includes(document.status),
            ) ? (
              <Notice title="尚未配置正式版本和历史版本">
                草案预览不替代正式协议。
              </Notice>
            ) : null}
            <Notice title="版本保存">
              适用期间的完整文本保留为只读，不用新规则倒改既有记录。
            </Notice>
            <RowLink
              title="查看正在公示的变更"
              detail="比较差异与退出影响"
              to="/rules?view=changes"
            />
          </>
        ) : view === "changes" ? (
          <>
            <Notice title="重大变化单独提示">
              不默认同意，不追溯剥夺已产生权益；实际公示和过渡以已发布文本为准。
            </Notice>
            {documents
              .filter((document) => document.status === "proposed")
              .map((document) => (
                <RowLink
                  key={document.key}
                  title={document.title}
                  detail={`${document.revision} · ${document.effective_from || "拟生效日期待明确"}`}
                  to={documentURL(document.key)}
                />
              ))}
            {catalog &&
            !documents.some((document) => document.status === "proposed") ? (
              <p>当前没有已配置的公示变更。</p>
            ) : null}
            <RowLink title="查看当前与历史规则" to="/rules?view=history" />
            <RowLink title="表达意见或申诉" to="/rules?view=appeal" />
            <RowLink
              title="退出或注销的影响"
              detail="未完事项分别处理"
              to={workflowURL("data")}
            />
          </>
        ) : view === "appeal" ? (
          <>
            <Notice title="保留原记录">
              从原事项查看处理范围、时间、事实摘要及结论；保护举报者和他人私密材料，不因申诉自动扩大处理。
            </Notice>
            <RowLink
              title="选择本账号的原核实／举报事项"
              detail="查看实际结论后补充或申请人工复核"
              to="/reviews"
            />
            <RowLink
              title="安全、隐私或权利意见"
              detail="保留原事项编号与版本"
              to={workflowURL("safety")}
            />
          </>
        ) : (
          <>
            <Field label="检索规则" value={query} onChange={setQuery} />
            <RowLink
              title="权益、服务与安全"
              detail="主体、数据、投诉与合同"
              to={workflowURL("safety")}
            />
            {documents
              .filter((document) =>
                [
                  document.title,
                  document.heading,
                  ...document.facts.map((fact) => fact.text),
                  ...document.paragraphs,
                ]
                  .join(" ")
                  .toLowerCase()
                  .includes(query.toLowerCase()),
              )
              .map((document) => (
                <RowLink
                  key={document.key}
                  title={document.title}
                  detail={`${document.revision} · ${labels[document.status] ?? "当前文本"}`}
                  to={documentURL(document.key)}
                />
              ))}
            <LegalLinks />
            {catalog ? (
              <Card title="平台主体">
                <p>{catalog.operator_name || "真实主体名称尚未配置"}</p>
                {catalog.operator_address ? (
                  <p>{catalog.operator_address}</p>
                ) : null}
                {catalog.operator_contact ? (
                  <p>{catalog.operator_contact}</p>
                ) : null}
              </Card>
            ) : null}
            <RowLink
              title="历史版本与下载"
              detail="保留适用期间完整内容"
              to="/rules?view=history"
            />
            <RowLink
              title="规则变更与意见"
              detail="公示期间可反馈"
              to="/rules?view=changes"
            />
            <RowLink
              title="处理依据与人工申诉"
              detail="保留原结论"
              to="/rules?view=appeal"
            />
          </>
        )}
      </QueryState>
    </TaskPage>
  );
}

export function SettingsHome() {
  return (
    <TaskPage
      title="设置"
      actions={
        <Button kind="primary" onClick={() => navigate("/mine")}>
          返回我的
        </Button>
      }
    >
      <RowLink
        title="账号与登录"
        detail="关联、换绑、恢复与退出"
        to="/security"
      />
      <RowLink
        title="通知偏好"
        detail="私信、群聊、申请与附近提醒"
        to="/notification-settings"
      />
      <RowLink
        title="隐私与可见范围"
        detail="个人资料和圈子内容"
        to="/privacy"
      />
      <RowLink
        title="账号数据与注销"
        detail="查看影响，再验证身份"
        to={workflowURL("data")}
      />
      <RowLink
        title="表情与贴纸"
        detail="常用、收藏与输入方式"
        to="/expressions"
      />
      <RowLink
        title="未成年人保护"
        detail="年龄、监护与权限"
        to="/reading?type=teen"
      />
      <RowLink title="平台规则" detail="规则、历史与申诉" to="/rules" />
      <RowLink
        title="隐私与安全报告"
        detail="游客也可报告与查询"
        to={workflowURL("safety")}
      />
    </TaskPage>
  );
}
