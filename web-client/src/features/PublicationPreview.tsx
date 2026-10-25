import type { User, RecordData } from "../core/api";
import type { DTOSourceDeclarationBody } from "../core/contract";
import type { PhotoDraft } from "../core/drafts";
import { integer } from "../core/wire";
import {
  Button,
  Card,
  Check,
  Choices,
  Notice,
  RowLink,
  TaskPage,
} from "../components/ui";
import { PhotoPreview } from "../components/Attachments";
import { workflowURL } from "./workflows";

const origins: Record<string, string> = {
  original: "原创／实拍",
  ai_generated: "AI 生成",
  ai_modified: "AI 修改",
  unknown: "来源尚不确定",
};
const audiences: Record<string, string> = {
  public: "公开",
  private: "仅自己",
  friends: "好友可见",
  selected: "指定好友",
  excluded: "排除指定好友",
};

/** 只展示已核对快照；编辑来源必须返回原稿并重新取得服务端清单。 */
export function PublicationPreview({
  body,
  photos,
  user,
  busy,
  agreed,
  error,
  onAgree,
  onSubmit,
  onBack,
  onSource,
}: {
  body: Record<string, unknown>;
  photos: PhotoDraft[];
  user: User | null;
  busy: boolean;
  agreed: boolean;
  error: string;
  onAgree: (value: boolean) => void;
  onSubmit: () => void;
  onBack: () => void;
  onSource: () => void;
}) {
  const entry = body.entry as Record<string, unknown>;
  const audience = entry.audience as
    | { mode?: string; user_ids?: bigint[] }
    | undefined;
  const declaration = body.declaration as { origin?: string } | undefined;
  return (
    <TaskPage
      title="发布前预览"
      scrollScope="publication-preview"
      actions={
        <Button kind="primary" disabled={busy || !agreed} onClick={onSubmit}>
          确认发布
        </Button>
      }
    >
      <div className="publication-author">
        <strong>{user?.nickname || user?.username || "当前账号"}</strong>
        <span className="muted">
          {entry.space === "circle"
            ? "宠友圈"
            : entry.space === "diary"
              ? "宠物日记"
              : "社区日常"}
          {integer(entry.pet_id) > 0n ? ` · 关联宠物 #${entry.pet_id}` : ""}
        </span>
      </div>
      {photos.map((photo, index) => (
        <figure
          className={index === 0 ? "publication-cover" : "publication-photo"}
          key={photo.key}
        >
          <PhotoPreview photo={photo} />
          <figcaption className="muted">
            {index === 0 ? "封面" : `照片 ${index + 1}`}
          </figcaption>
        </figure>
      ))}
      <Button disabled={busy} onClick={onSource}>
        内容来源与 AI 声明 ·{" "}
        {audience?.mode === "private"
          ? "仅自己，本次无需公开声明"
          : (origins[declaration?.origin ?? "unknown"] ?? "来源尚不确定")}
      </Button>
      {entry.title ? <h2>{String(entry.title)}</h2> : null}
      <p className="publication-text">{String(entry.text ?? "")}</p>
      <p className="muted">
        {audiences[audience?.mode ?? "public"] ?? "受众待核对"} ·{" "}
        {photos.length} 张照片
      </p>
      {audience?.user_ids?.length ? (
        <p>受众名单：{audience.user_ids.map((id) => `#${id}`).join("、")}</p>
      ) : null}
      {entry.city ? <p className="muted">城市：{String(entry.city)}</p> : null}
      <Check checked={agreed} onChange={onAgree} disabled={busy}>
        我已核对当前内容、照片、受众和来源声明
      </Check>
      <p className="muted">
        来源改变后须重新获取清单并再次确认。来源声明不代表素材权利或真实性已通过核验。
      </p>
      <Button disabled={busy} onClick={onBack}>
        返回修改，保留草稿
      </Button>
      {error ? <Notice title={error} error /> : null}
    </TaskPage>
  );
}

export function PublicationSourceReview({
  entry,
  origin,
  busy,
  error,
  onOrigin,
  onConfirm,
  onBack,
}: {
  entry: Record<string, unknown>;
  origin: string;
  busy: boolean;
  error: string;
  onOrigin: (origin: string) => void;
  onConfirm: () => void;
  onBack: () => void;
}) {
  const [reviewing, setReviewing] = useState(false);
  const [rights, setRights] = useState(false);
  const ai = origin === "ai_generated" || origin === "ai_modified";
  function choose(value: string) {
    onOrigin(value);
    setRights(false);
    setReviewing(true);
  }
  return (
    <TaskPage
      title={
        !reviewing
          ? "声明内容来源"
          : ai
            ? "AI 内容声明确认"
            : "普通内容来源确认"
      }
      scrollScope="publication-source"
      actions={
        reviewing ? (
          <Button kind="primary" disabled={busy || !rights} onClick={onConfirm}>
            提交本次声明
          </Button>
        ) : (
          <Button disabled={busy} onClick={onBack}>
            暂不选择，返回草稿
          </Button>
        )
      }
    >
      {!reviewing ? (
        <>
          <Notice title="帮助大家理解图片来源">
            选择与你的文字、图片实际情况一致的声明；普通裁切调色不自动视为 AI
            生成。
          </Notice>
          <Button disabled={busy} onClick={() => choose("ai_generated")}>
            包含 AI 生成或合成内容
          </Button>
          <Button disabled={busy} onClick={() => choose("original")}>
            未使用 AI 生成或合成内容
          </Button>
          <Notice title="隐私与标识分别处理">
            照片中的 GPS 等隐私信息与适用的 AI
            来源标识分别处理；保留原文件，不清除已有来源依据。
          </Notice>
        </>
      ) : (
        <>
          <Card title="本次内容">
            <strong>{String(entry.title || "当前发布草稿")}</strong>
            <p className="publication-text">{String(entry.text ?? "")}</p>
            <p>来源：{origins[origin] ?? origins.unknown}</p>
            {ai ? (
              <>
                <Choices
                  label="AI 来源类型"
                  value={origin}
                  choices={[
                    ["ai_generated", "AI 生成"],
                    ["ai_modified", "AI 修改"],
                  ]}
                  disabled={busy}
                  onChange={choose}
                />
                <p>公开展示：内容周边显示相应 AI 提示</p>
              </>
            ) : null}
          </Card>
          <Notice title={ai ? "真实宠物材料另有要求" : "识别存在差异时"}>
            {ai
              ? "明确标注的 AI 示意内容不能代替待领养宠物的近期实拍、来源或医疗证明。"
              : "发现已有生成标识时会提示核对与求助，不直接认定故意造假。"}
          </Notice>
          <Check checked={rights} onChange={setRights} disabled={busy}>
            我确认声明与本次内容相符，并拥有必要素材权利
          </Check>
          <Button
            disabled={busy}
            onClick={() => {
              setReviewing(false);
              setRights(false);
            }}
          >
            修改来源选择
          </Button>
          <p className="muted">
            保存声明后仍需在预览页核对受众并主动确认发布。
          </p>
        </>
      )}
      {error ? <Notice title={error} error /> : null}
      {reviewing ? (
        <Button disabled={busy} onClick={onBack}>
          返回草稿
        </Button>
      ) : null}
    </TaskPage>
  );
}

export function PublicationSourceRestriction({
  onBack,
}: {
  onBack: () => void;
}) {
  return (
    <TaskPage
      title="素材使用范围待核对"
      scrollScope="publication-restriction"
      actions={
        <Button kind="primary" onClick={onBack}>
          返回草稿
        </Button>
      }
    >
      <Notice title="保留草稿，重新核对来源">
        材料已有 AI
        来源标识，不能声明为原创。请保留原文件并重新说明来源；本次拒绝不代表内容已发布。
      </Notice>
      <Button onClick={onBack}>重新选择并说明</Button>
      <RowLink
        title="提出异议或求助"
        detail="通过安全报告补充必要说明，不自动清除来源标识"
        to={workflowURL("safety")}
      />
    </TaskPage>
  );
}

export type SourceDeclarationRecord = RecordData<DTOSourceDeclarationBody>;
export function PublicationSourceResult({
  record,
  busy,
  error,
  onContinue,
  onCorrect,
}: {
  record: SourceDeclarationRecord;
  busy: boolean;
  error: string;
  onContinue: () => void;
  onCorrect: () => void;
}) {
  return (
    <TaskPage
      title="来源声明已记录"
      scrollScope="publication-source-result"
      actions={
        <Button kind="primary" disabled={busy} onClick={onContinue}>
          回到发布预览
        </Button>
      }
    >
      <Notice title="实际记录已返回">
        来源声明已保存，内容尚未发布；这不代表素材权利或真实性已通过平台核验。
      </Notice>
      <Card title="本次记录">
        <p>声明 #{record.id.toString()}</p>
        <p>来源：{origins[record.data.origin] ?? origins.unknown}</p>
        <p className="muted">记录时间：{record.created_at}</p>
        <p>下一步：检查受众、隐私和发布资格</p>
      </Card>
      <Button disabled={busy} onClick={onCorrect}>
        更正来源声明，产生新记录
      </Button>
      {error ? <Notice title={error} error /> : null}
    </TaskPage>
  );
}
export function PublicationSourcePending({
  requestId,
  busy,
  error,
  onQuery,
  onBack,
}: {
  requestId: string;
  busy: boolean;
  error: string;
  onQuery: () => void;
  onBack: () => void;
}) {
  return (
    <TaskPage
      title="来源声明结果待确认"
      scrollScope="publication-source-pending"
      actions={
        <Button kind="primary" disabled={busy} onClick={onQuery}>
          查询本次结果
        </Button>
      }
    >
      <Notice title="先不要重复提交">
        保留原草稿与本次请求标识；未取得实际回执前，不显示声明已保存或内容已发布。
      </Notice>
      <p className="muted">本次请求：{requestId}</p>
      <Button disabled={busy} onClick={onBack}>
        返回草稿，保留原请求
      </Button>
      {error ? <Notice title={error} error /> : null}
    </TaskPage>
  );
}
import { useState } from "react";
