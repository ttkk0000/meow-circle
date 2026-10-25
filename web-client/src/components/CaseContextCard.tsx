import type { RecordData } from "../core/api";
import { Card, PrivateImage, RowLink } from "./ui";

/** 调用者先读取有权访问的求助；摘要只展示文字区域和原公开照片。 */
export function CaseContextCard({ parent }: { parent: RecordData }) {
  return (
    <Card title="关联求助">
      <div className="lead-case-summary">
        {parent.media_ids?.[0] ? (
          <PrivateImage id={parent.media_ids[0]} alt="关联求助照片" />
        ) : null}
        <div>
          <strong>{String(parent.data.title ?? "本次求助")}</strong>
          <p className="muted">
            {String(parent.data.city ?? "")} · {String(parent.data.area ?? "")}
          </p>
        </div>
      </div>
      <p>本次求助的文字区域，不代表实时位置。</p>
      <RowLink title="查看求助" to={`/cases/${parent.id}`} />
    </Card>
  );
}
