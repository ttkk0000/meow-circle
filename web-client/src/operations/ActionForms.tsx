import { useEffect, useRef, useState, type ReactNode } from "react";
import { Button, Field, Modal } from "../components/ui";
import { OpsError, message } from "./client";
import { useOps } from "./context";
import { jobNames, type Document, type Incident, type Release } from "./models";
import { OpsNotice } from "./ui";

function CommandModal({
  title,
  path,
  valid,
  buildInput,
  render,
  onClose,
  onSaved,
}: {
  title: string;
  path: string;
  valid: boolean;
  buildInput: () => Record<string, unknown>;
  render: (disabled: boolean) => ReactNode;
  onClose: () => void;
  onSaved: (record: Document<unknown>) => void;
}) {
  const { session, demo } = useOps(),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const [pending, setPending] = useState<Record<string, unknown>>(),
    active = useRef(true);
  useEffect(() => {
    active.current = true;
    return () => {
      active.current = false;
    };
  }, []);
  async function submit() {
    if (demo || busy) return;
    const body = pending ?? {
      ...buildInput(),
      request_id: crypto.randomUUID(),
    };
    setPending(body);
    setBusy(true);
    setError("");
    const epoch = session.client.epoch;
    try {
      const result = await session.client.request<Document<unknown>>(
        path,
        body,
      );
      if (!active.current || epoch !== session.client.epoch) return;
      setPending(undefined);
      onSaved(result);
      void session.refresh();
    } catch (e) {
      if (!active.current || epoch !== session.client.epoch) return;
      setError(message(e));
      if (e instanceof OpsError && e.status >= 400 && e.status < 500) {
        setPending(undefined);
        if (e.status === 401 || e.status === 403)
          session.report(
            e,
            `${session.context?.environment ?? "当前环境"} / ${title}`,
          );
      } else if (
        e instanceof OpsError &&
        e.code === "environment_unconfigured"
      ) {
        setPending(undefined);
        session.report(e);
      }
    } finally {
      if (active.current && epoch === session.client.epoch) setBusy(false);
    }
  }
  return (
    <Modal
      title={title}
      onClose={() => {
        if (!pending) onClose();
      }}
      busy={busy || !!pending}
      actions={
        <>
          <Button disabled={busy || !!pending} onClick={onClose}>
            取消
          </Button>
          <Button
            kind="primary"
            disabled={demo || busy || (!pending && !valid)}
            onClick={() => void submit()}
          >
            {busy ? "正在记录…" : pending ? "按原请求核对" : "确认并记录"}
          </Button>
        </>
      }
    >
      <p className="ops-caption">
        记录将绑定当前环境、具名账号与原请求编号；不要输入密码、Token或用户私密资料。
      </p>
      {render(busy || !!pending)}
      {pending ? (
        <OpsNotice title={busy ? "正在等待本次操作结果" : "结果尚未确认"}>
          本次请求号：
          <code className="ops-wrap">{String(pending.request_id)}</code>
          。结果未确认时保留原内容，按相同编号核对，避免重复提交。
        </OpsNotice>
      ) : null}
      {error ? (
        <OpsNotice title="操作未完成" error>
          {error}
        </OpsNotice>
      ) : null}
    </Modal>
  );
}
const sha256Pattern = /^[a-f0-9]{64}$/;
const evidencePattern = /^[a-zA-Z0-9][a-zA-Z0-9_./:#-]{2,159}$/;
const runeCount = (value: string) => Array.from(value).length;
const hasControlChar = (value: string) =>
  value.includes("\0") || value.includes("\r");

export function CreateRelease({
  onClose,
  record,
  onSaved,
}: {
  onClose: () => void;
  record?: Document<Release>;
  onSaved?: (row: Document<Release>) => void;
}) {
  const { go } = useOps(),
    [version, setVersion] = useState(record?.data.version ?? ""),
    [hash, setHash] = useState(record?.data.artifact_sha256 ?? ""),
    [backup, setBackup] = useState(record?.data.backup_ref ?? ""),
    [approval, setApproval] = useState(record?.data.approval_ref ?? ""),
    [reason, setReason] = useState(record?.data.reason ?? "");
  const trimmedVersion = version.trim(),
    versionError =
      record && !trimmedVersion
        ? "请填写候选版本"
        : trimmedVersion && (runeCount(trimmedVersion) > 80 || hasControlChar(version))
          ? "候选版本最多 80 字，且不能包含控制字符"
          : "",
    hashError =
      hash && !sha256Pattern.test(hash)
        ? "构件摘要须为 64 位小写十六进制 SHA-256"
        : "",
    backupError =
      backup && !evidencePattern.test(backup)
        ? "备份证据引用须为 3–160 位字母或数字开头的编号（支持 _ . / : # -，不含空格）"
        : "",
    approvalError =
      approval && !evidencePattern.test(approval)
        ? "审批引用须为 3–160 位字母或数字开头的编号（支持 _ . / : # -，不含空格）"
        : "",
    reasonError =
      record && !reason.trim()
        ? "请填写变更原因"
        : reason && (runeCount(reason) > 500 || hasControlChar(reason))
          ? "变更原因最多 500 字，且不能包含回车控制符"
          : "",
    valid =
      !!trimmedVersion &&
      !versionError &&
      !!reason.trim() &&
      !reasonError &&
      !hashError &&
      !backupError &&
      !approvalError;
  return (
    <CommandModal
      title={record ? "补齐发布资料" : "登记发布准备"}
      path={record ? `releases/${record.id}/actions` : "releases"}
      valid={valid}
      onClose={onClose}
      onSaved={(row) => {
        onClose();
        onSaved?.(row as Document<Release>);
        go("release", row.id);
      }}
      buildInput={() => {
        const release = {
          version: trimmedVersion,
          artifact_sha256: hash,
          backup_ref: backup,
          approval_ref: approval,
          reason,
        };
        return record ? { record_version: record.version, release } : release;
      }}
      render={(disabled) => (
        <div className="ops-form">
          <Field
            label="候选版本（必填，最多80字）"
            value={version}
            onChange={setVersion}
            error={versionError}
            disabled={disabled}
          />
          <Field
            label="不可变构件 SHA-256（可稍后补齐）"
            value={hash}
            onChange={setHash}
            error={hashError}
            disabled={disabled}
            hint="64位小写十六进制，仅记录摘要，不代表构件已验证。"
          />
          <Field
            label="备份与恢复证据引用"
            value={backup}
            onChange={setBackup}
            error={backupError}
            disabled={disabled}
            hint="使用工单或证据编号，例如 BACKUP-001，不填写签名下载链接。"
          />
          <Field
            label="外部审批引用"
            value={approval}
            onChange={setApproval}
            error={approvalError}
            disabled={disabled}
            hint="使用审批编号，例如 APPROVAL-001（3–160位字母/数字及 _./:#-）。"
          />
          <Field
            label="变更原因（必填，最多500字）"
            value={reason}
            onChange={setReason}
            error={reasonError}
            multiline
            disabled={disabled}
          />
          <OpsNotice title="不会执行部署">
            只登记准备记录；发布执行器和外部证据验证未接入，不能用填写的引用绕过核对。
          </OpsNotice>
        </div>
      )}
    />
  );
}
export function CreateIncident({ onClose }: { onClose: () => void }) {
  const { go } = useOps(),
    [title, setTitle] = useState(""),
    [service, setService] = useState("api"),
    [severity, setSeverity] = useState("P2"),
    [evidence, setEvidence] = useState("");
  return (
    <CommandModal
      title="登记运维事件"
      path="incidents"
      valid={!!title.trim() && title.length <= 160}
      onClose={onClose}
      onSaved={(row) => {
        onClose();
        go("incident", row.id);
      }}
      buildInput={() => ({ title, service, severity, evidence_ref: evidence })}
      render={(disabled) => (
        <div className="ops-form">
          <Field
            label="事件标题（必填，最多160字）"
            value={title}
            onChange={setTitle}
            disabled={disabled}
          />
          <label className="field">
            <span>影响服务</span>
            <select
              value={service}
              onChange={(e) => setService(e.target.value)}
              disabled={disabled}
            >
              {[
                "api",
                "postgres",
                "redis",
                "caddy",
                ...Object.keys(jobNames),
              ].map((name) => (
                <option key={name}>{name}</option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>事件等级</span>
            <select
              value={severity}
              onChange={(e) => setSeverity(e.target.value)}
              disabled={disabled}
            >
              {["P1", "P2", "P3"].map((level) => (
                <option key={level}>{level}</option>
              ))}
            </select>
          </label>
          <Field
            label="证据引用（编号，不含凭据）"
            value={evidence}
            onChange={setEvidence}
            disabled={disabled}
          />
        </div>
      )}
    />
  );
}
export function IncidentAction({
  record,
  action,
  onClose,
  onSaved,
}: {
  record: Document<Incident>;
  action: string;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [note, setNote] = useState(""),
    [evidence, setEvidence] = useState("");
  const names: Record<string, string> = {
    claim: "认领事件",
    note: "补充处置记录",
    observe: "记录缓解并进入观察",
    resolve: "验证恢复并结案",
  };
  const requiresEvidence = action === "observe" || action === "resolve";
  return (
    <CommandModal
      title={names[action] ?? "记录处置"}
      path={`incidents/${record.id}/actions`}
      valid={
        !!note.trim() &&
        note.length <= 500 &&
        (!requiresEvidence || !!evidence.trim())
      }
      onClose={onClose}
      onSaved={() => {
        onClose();
        onSaved();
      }}
      buildInput={() => ({
        version: record.version,
        action,
        note,
        evidence_ref: evidence,
      })}
      render={(disabled) => (
        <div className="ops-form">
          <p>
            事件 {record.id} / 版本 {record.version}
            。服务端会重新检查负责人、状态和版本。
          </p>
          <Field
            label="处置说明（必填，最多500字）"
            value={note}
            onChange={setNote}
            multiline
            disabled={disabled}
          />
          <Field
            label={`恢复或处置证据引用${requiresEvidence ? "（必填）" : ""}`}
            value={evidence}
            onChange={setEvidence}
            disabled={disabled}
          />
          {action === "resolve" ? (
            <OpsNotice title="结案需要恢复验证">
              worker事件必须存在事件发生后的最新成功结果；只填写“已恢复”文字不能关闭事件。
            </OpsNotice>
          ) : null}
        </div>
      )}
    />
  );
}
