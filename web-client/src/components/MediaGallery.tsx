import { useState } from "react";
import { Button, Modal, PrivateImage } from "./ui";

export function MediaGallery({
  ids,
  description,
}: {
  ids: bigint[];
  description: string;
}) {
  const [index, setIndex] = useState<number>();
  const [zoom, setZoom] = useState(1);
  return (
    <>
      <div className="gallery">
        {ids.map((id, i) => (
          <button
            type="button"
            className="media-link"
            aria-label={`查看大图${i + 1}`}
            key={String(id)}
            onClick={() => {
              setIndex(i);
              setZoom(1);
            }}
          >
            <PrivateImage id={id} alt={`${description}，照片${i + 1}`} />
          </button>
        ))}
      </div>
      {index !== undefined ? (
        <Modal
          title={`照片 ${index + 1}／${ids.length}`}
          onClose={() => setIndex(undefined)}
          actions={
            <>
              <Button
                disabled={index === 0}
                onClick={() => {
                  setIndex(index - 1);
                  setZoom(1);
                }}
              >
                上一张
              </Button>
              <Button
                disabled={index === ids.length - 1}
                onClick={() => {
                  setIndex(index + 1);
                  setZoom(1);
                }}
              >
                下一张
              </Button>
              <Button onClick={() => setIndex(undefined)}>关闭大图</Button>
            </>
          }
        >
          <label className="field">
            缩放
            <input
              aria-label="缩放照片"
              type="range"
              min="1"
              max="5"
              step=".25"
              value={zoom}
              onChange={(e) => setZoom(Number(e.target.value))}
            />
          </label>
          <div className="gallery-viewport">
            <div style={{ width: `${zoom * 100}%` }}>
              <PrivateImage
                id={ids[index]}
                alt={description}
                className="full-photo"
              />
            </div>
          </div>
        </Modal>
      ) : null}
    </>
  );
}
