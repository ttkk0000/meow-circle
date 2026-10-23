import { useEffect, useState } from "react";
import type { PhotoDraft } from "../core/drafts";
import { Button, PrivateImage } from "./ui";

export function PhotoPreview({ photo }: { photo: PhotoDraft }) {
  const [source, setSource] = useState("");
  useEffect(() => {
    if (!photo.blob) {
      setSource("");
      return;
    }
    const url = URL.createObjectURL(photo.blob);
    setSource(url);
    return () => URL.revokeObjectURL(url);
  }, [photo.blob]);
  return source ? (
    <img className="media" src={source} alt={photo.name} />
  ) : photo.scope === "safety" ? (
    <p className="muted">已上传私密材料</p>
  ) : (
    <PrivateImage id={photo.mediaId} alt={photo.name} />
  );
}
export function Attachments({
  photos,
  disabled,
  limit = 9,
  onSelect,
  onUpload,
  onRemove,
  onMove,
  uploading = new Set<string>(),
}: {
  photos: PhotoDraft[];
  disabled: boolean;
  limit?: number;
  onSelect: (files: FileList | null) => void;
  onUpload: (photo: PhotoDraft) => void;
  onRemove: (key: string) => void;
  onMove: (index: number, direction: number) => void;
  uploading?: Set<string>;
}) {
  return (
    <section className="stack">
      <label className="upload-button">
        添加或更换照片
        <input
          aria-label="添加或更换照片"
          type="file"
          accept="image/jpeg,image/png"
          multiple={limit > 1}
          disabled={disabled || photos.length >= limit}
          onChange={(e) => {
            onSelect(e.target.files);
            e.target.value = "";
          }}
        />
      </label>
      <p className="caption muted">
        最多{limit}
        张，JPEG／PNG，10MiB／20MP以内。先保存在当前浏览器，再由你逐张上传；照片顺序与原上传编号会保留。
      </p>
      <div className="photo-queue">
        {photos.map((photo, index) => (
          <figure key={photo.key}>
            <PhotoPreview photo={photo} />
            <figcaption className="caption">{photo.name}</figcaption>
            {!photo.mediaId ? (
              <Button
                disabled={disabled || uploading.has(photo.key)}
                onClick={() => onUpload(photo)}
              >
                {uploading.has(photo.key) ? "正在上传" : "按原编号上传／重试"}
              </Button>
            ) : (
              <small>已上传，尚未提交</small>
            )}
            <div className="actions-row">
              <Button
                kind="quiet"
                disabled={disabled || index === 0}
                onClick={() => onMove(index, -1)}
              >
                前移
              </Button>
              <Button
                kind="quiet"
                disabled={disabled || index === photos.length - 1}
                onClick={() => onMove(index, 1)}
              >
                后移
              </Button>
              <Button
                kind="quiet"
                disabled={disabled}
                onClick={() => onRemove(photo.key)}
              >
                移除
              </Button>
            </div>
          </figure>
        ))}
      </div>
    </section>
  );
}
