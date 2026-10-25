import { useEffect, useRef, useState } from "react";
import type { PhotoDraft } from "../core/drafts";
import { Button, PrivateImage, Modal } from "./ui";
import { PhotoChoiceHelpContent } from "../features/PermissionPages";
import { explain } from "../core/api";

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
  progress = {},
  onSaveOrder,
}: {
  photos: PhotoDraft[];
  disabled: boolean;
  limit?: number;
  onSelect: (files: FileList | null) => void;
  onUpload: (photo: PhotoDraft) => void | Promise<void>;
  onRemove: (key: string) => void;
  onMove: (index: number, direction: number) => void;
  uploading?: Set<string>;
  progress?: Record<string, number | undefined>;
  onSaveOrder?: () => Promise<void>;
}) {
  const [help, setHelp] = useState(false);
  const [saving, setSaving] = useState(false);
  const [saveMessage, setSaveMessage] = useState("");
  const orderKey = photos.map((photo) => photo.key).join("|");
  useEffect(() => setSaveMessage(""), [orderKey]);
  const dragged = useRef<string | undefined>(undefined);
  const pointer = useRef<
    { key: string; id: number; startY: number } | undefined
  >(undefined);
  const failed = photos.filter(
    (photo) => !photo.mediaId && photo.error && !uploading.has(photo.key),
  );
  return (
    <section className="stack">
      <Button kind="quiet" onClick={() => setHelp(true)}>
        照片选择说明
      </Button>
      {help ? (
        <Modal
          title="照片选择说明"
          onClose={() => setHelp(false)}
          actions={
            <Button kind="primary" onClick={() => setHelp(false)}>
              返回继续编辑
            </Button>
          }
        >
          <PhotoChoiceHelpContent />
        </Modal>
      ) : null}
      <label className="upload-button">
        添加或更换照片
        <input
          aria-label="添加或更换照片"
          type="file"
          accept="image/jpeg,image/png"
          multiple={limit > 1}
          disabled={disabled || saving || photos.length >= limit}
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
          <figure
            key={photo.key}
            data-photo-key={photo.key}
            draggable={!disabled && !saving}
            onDragStart={(event) => {
              dragged.current = photo.key;
              event.dataTransfer.effectAllowed = "move";
              event.dataTransfer.setData("text/plain", "M&D 照片排序");
            }}
            onDragEnd={() => {
              dragged.current = undefined;
            }}
            onDragOver={(event) => {
              if (dragged.current && !disabled && !saving)
                event.preventDefault();
            }}
            onDrop={(event) => {
              if (disabled || saving || !dragged.current) return;
              event.preventDefault();
              const from = photos.findIndex(
                (item) => item.key === dragged.current,
              );
              if (from >= 0 && from !== index) onMove(from, index - from);
              dragged.current = undefined;
            }}
          >
            <PhotoPreview photo={photo} />
            <figcaption className="caption">
              第 {index + 1} 张{index === 0 ? " · 封面" : ""} · {photo.name}
            </figcaption>
            {!photo.mediaId ? (
              <Button
                disabled={disabled || saving || uploading.has(photo.key)}
                onClick={() => onUpload(photo)}
              >
                {uploading.has(photo.key) ? "正在上传" : "按原编号上传／重试"}
              </Button>
            ) : (
              <small>已上传，尚未提交</small>
            )}
            {uploading.has(photo.key) ? (
              <>
                <label className="caption">
                  {progress[photo.key] === undefined
                    ? "等待上传进度…"
                    : progress[photo.key] === 100
                      ? "已传输 100% · 等待服务器确认"
                      : `上传中 ${progress[photo.key]}%`}
                </label>
                <progress
                  max="100"
                  value={progress[photo.key]}
                  aria-label={`${photo.name}上传进度`}
                />
              </>
            ) : null}
            {photo.error ? (
              <p className="field-error" role="status">
                {photo.error}
              </p>
            ) : null}
            <button
              type="button"
              className="button quiet photo-drag-handle"
              disabled={disabled || saving}
              draggable={false}
              onPointerDown={(event) => {
                if (disabled || saving || event.button !== 0) return;
                event.preventDefault();
                event.currentTarget.focus();
                pointer.current = {
                  key: photo.key,
                  id: event.pointerId,
                  startY: event.clientY,
                };
                event.currentTarget.setPointerCapture(event.pointerId);
              }}
              onPointerMove={(event) => {
                const active = pointer.current;
                if (
                  disabled ||
                  !active ||
                  active.id !== event.pointerId ||
                  Math.abs(event.clientY - active.startY) < 12
                )
                  return;
                const target = document
                  .elementFromPoint(event.clientX, event.clientY)
                  ?.closest<HTMLElement>("[data-photo-key]");
                if (
                  !target ||
                  !event.currentTarget.closest(".photo-queue")?.contains(target)
                )
                  return;
                const from = photos.findIndex(
                    (item) => item.key === active.key,
                  ),
                  to = photos.findIndex(
                    (item) => item.key === target.dataset.photoKey,
                  );
                if (from >= 0 && to >= 0 && from !== to)
                  onMove(from, to - from);
              }}
              onPointerUp={() => {
                pointer.current = undefined;
              }}
              onPointerCancel={() => {
                pointer.current = undefined;
              }}
            >
              拖动排序 · 也可使用前移／后移
            </button>
            <div className="actions-row">
              <Button
                kind="quiet"
                disabled={disabled || saving || index === 0}
                onClick={() => onMove(index, -1)}
              >
                前移
              </Button>
              <Button
                kind="quiet"
                disabled={disabled || saving || index === photos.length - 1}
                onClick={() => onMove(index, 1)}
              >
                后移
              </Button>
              <Button
                kind="quiet"
                disabled={disabled || saving}
                onClick={() => onRemove(photo.key)}
              >
                移除
              </Button>
            </div>
          </figure>
        ))}
      </div>
      {photos.length && onSaveOrder ? (
        <Button
          disabled={disabled || saving}
          onClick={() => {
            void (async () => {
              setSaving(true);
              setSaveMessage("");
              try {
                await onSaveOrder();
                setSaveMessage("照片顺序已保存到本机草稿，尚未发布。");
              } catch (error) {
                setSaveMessage(explain(error));
              } finally {
                setSaving(false);
              }
            })();
          }}
        >
          {saving ? "正在保存顺序…" : "保存顺序到本机草稿"}
        </Button>
      ) : null}
      {saveMessage ? <p role="status">{saveMessage}</p> : null}
      {failed.length ? (
        <>
          <p>
            已完成 {photos.filter((photo) => photo.mediaId).length} 张 · 未完成{" "}
            {failed.length} 张
          </p>
          <Button
            disabled={disabled || saving || uploading.size > 0}
            onClick={() => {
              void (async () => {
                for (const photo of failed) await onUpload(photo);
              })();
            }}
          >
            重试失败照片
          </Button>
          <Button
            disabled={disabled || saving || uploading.size > 0}
            onClick={() => failed.forEach((photo) => onRemove(photo.key))}
          >
            移除失败照片
          </Button>
          <small>保留已完成照片，重试沿用原上传编号。</small>
        </>
      ) : null}
    </section>
  );
}
