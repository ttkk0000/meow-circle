import type { ReactNode } from "react";
import type { RecordData } from "../core/api";
import { Button, Notice, PrivateImage, TaskPage } from "../components/ui";

export function LeadReviewTask({
  parent,
  text,
  count,
  disabled,
  busy,
  error,
  onBack,
  onSubmit,
}: {
  parent?: RecordData;
  text: string;
  count: number;
  disabled: boolean;
  busy: boolean;
  error: string;
  onBack: () => void;
  onSubmit: () => void;
}) {
  return (
    <TaskPage
      title="检查私密线索"
      actions={
        <Button kind="primary" disabled={disabled} onClick={onSubmit}>
          确认私密提交
        </Button>
      }
    >
      <strong>
        提交给：{String(parent?.data.title ?? "当前求助")} #
        {parent?.id.toString()}
      </strong>
      <Notice title="本次共享">
        <p className="publication-text">{text}</p>
        <p>
          已选照片 {count}{" "}
          张；仅本案当前发布人与本人可见。手动地点仅以你填写的文字为准。
        </p>
      </Notice>
      <Notice title="收到不等于匹配">
        不移动公共标记，不修改宠物状态；后续仍需核对。
      </Notice>
      {error ? <Notice title={error} error /> : null}
      <Button kind="quiet" disabled={busy} onClick={onBack}>
        返回修改
      </Button>
    </TaskPage>
  );
}

export function LeadPhotoTask({
  photo,
  busy,
  locked,
  canEdit,
  error,
  onBack,
  onSkip,
  children,
}: {
  photo?: bigint;
  busy: boolean;
  locked: boolean;
  canEdit: boolean;
  error: string;
  onBack: () => void;
  onSkip: () => void;
  children: ReactNode;
}) {
  return (
    <TaskPage
      title="线索照片说明"
      actions={
        <Button kind="primary" disabled={locked} onClick={onBack}>
          使用照片并返回
        </Button>
      }
    >
      {photo ? (
        <div className="lead-submission-photo">
          <PrivateImage id={photo} alt="本次目击照片" />
        </div>
      ) : null}
      <Notice title="私密材料">
        不表示识别成功；仅提供你有权使用的照片，不公开精确位置。
      </Notice>
      {error ? <Notice title={error} error /> : null}
      {children}
      <Button kind="quiet" disabled={busy || !canEdit} onClick={onSkip}>
        不附照片继续
      </Button>
      <Button disabled={busy} onClick={onBack}>
        返回表单，保留已选照片
      </Button>
    </TaskPage>
  );
}

export function LeadFailureTask({
  error,
  uncertain,
  conflict,
  busy,
  onBack,
  onRetry,
}: {
  error: string;
  uncertain: boolean;
  conflict: boolean;
  busy: boolean;
  onBack: () => void;
  onRetry: () => void;
}) {
  return (
    <TaskPage
      title={uncertain ? "线索结果待核对" : "线索尚未发送"}
      actions={
        <Button kind="primary" disabled={busy} onClick={onRetry}>
          {uncertain
            ? "核对原操作结果"
            : conflict
              ? "核对最新求助"
              : "重试检查摘要"}
        </Button>
      }
    >
      <Notice title="输入未清空" warning>
        未知结果先核对本次提交；保留原编号与原文，不另发副本。
      </Notice>
      {error ? <Notice title={error} error /> : null}
      <Button kind="quiet" disabled={busy} onClick={onBack}>
        回到表单
      </Button>
    </TaskPage>
  );
}
