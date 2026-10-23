import { key } from "./wire";
import type { PhotoDraft } from "./drafts";
const text = new TextDecoder();
const allowed = [
  "Label",
  "ContentProducer",
  "ProduceID",
  "ReservedCode1",
  "ContentPropagator",
  "PropagateID",
  "ReservedCode2",
].sort();
function imageDimensions(bytes: Uint8Array, png: boolean) {
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  if (png) {
    if (
      bytes.length < 33 ||
      bytes[0] !== 137 ||
      text.decode(bytes.slice(1, 4)) !== "PNG" ||
      text.decode(bytes.slice(12, 16)) !== "IHDR"
    )
      throw new Error("PNG文件头无效");
    return [view.getUint32(16), view.getUint32(20)];
  }
  if (bytes[0] !== 255 || bytes[1] !== 216) throw new Error("JPEG文件头无效");
  for (let pos = 2; pos + 4 <= bytes.length; ) {
    if (bytes[pos] !== 255) break;
    const marker = bytes[pos + 1];
    if (marker === 255) {
      pos++;
      continue;
    }
    if (marker === 1 || (marker >= 208 && marker <= 215)) {
      pos += 2;
      continue;
    }
    if (marker === 218 || marker === 217) break;
    const length = view.getUint16(pos + 2);
    if (length < 2 || pos + length + 2 > bytes.length) break;
    if (
      [
        192, 193, 194, 195, 197, 198, 199, 201, 202, 203, 205, 206, 207,
      ].includes(marker)
    ) {
      if (length < 7) break;
      return [view.getUint16(pos + 7), view.getUint16(pos + 5)];
    }
    pos += length + 2;
  }
  throw new Error("无法在解码前核对照片尺寸");
}
function jsonLabel(part: string): string | undefined {
  if (!/AIGC/i.test(part)) return;
  const decoded = part
    .replaceAll("&quot;", '"')
    .replaceAll("&apos;", "'")
    .replaceAll("&lt;", "<")
    .replaceAll("&gt;", ">")
    .replaceAll("&amp;", "&");
  const start = /\{\s*"AIGC"\s*:/.exec(decoded)?.index;
  if (start === undefined) throw new Error("来源标识不可读，请保留原文件");
  let depth = 0,
    quoted = false,
    escape = false,
    end = -1;
  for (let i = start; i < Math.min(decoded.length, start + 16384); i++) {
    const c = decoded[i];
    if (quoted) {
      if (c === '"' && !escape) quoted = false;
      if (c === "\\" && !escape) escape = true;
      else escape = false;
    } else if (c === '"') quoted = true;
    else if (c === "{") depth++;
    else if (c === "}" && --depth === 0) {
      end = i + 1;
      break;
    }
  }
  if (end < 0) throw new Error("来源标识不完整，请保留原文件");
  const root = JSON.parse(decoded.slice(start, end)) as Record<string, unknown>;
  if (
    Object.keys(root).join() !== "AIGC" ||
    !root.AIGC ||
    typeof root.AIGC !== "object"
  )
    throw new Error("来源标识无效");
  const original = root.AIGC as Record<string, unknown>;
  if (
    Object.keys(original).some((k) => !allowed.includes(k)) ||
    !["1", "2", "3"].includes(String(original.Label))
  )
    throw new Error("来源标识不符合合同");
  const clean: Record<string, string> = {};
  for (const name of allowed) {
    const value = original[name] ?? "";
    if (
      typeof value !== "string" ||
      value.length > 1024 ||
      /[^\x21-\x7e]|\\/.test(value)
    )
      throw new Error("来源字段无法保留");
    clean[name] = value;
  }
  return JSON.stringify({ AIGC: clean });
}
async function inflate(data: Uint8Array): Promise<Uint8Array> {
  if (typeof DecompressionStream === "undefined")
    throw new Error(
      "当前浏览器无法保留压缩的来源标识，请换用支持的浏览器或保留原文件",
    );
  const stream = new Blob([
    data.buffer.slice(
      data.byteOffset,
      data.byteOffset + data.byteLength,
    ) as ArrayBuffer,
  ])
    .stream()
    .pipeThrough(new DecompressionStream("deflate"));
  const reader = stream.getReader();
  const chunks: Uint8Array[] = [];
  let total = 0;
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      total += value.byteLength;
      if (total > 16384) throw new Error("来源标识过大");
      chunks.push(value);
    }
  } finally {
    await reader.cancel();
  }
  const out = new Uint8Array(total);
  let pos = 0;
  for (const chunk of chunks) {
    out.set(chunk, pos);
    pos += chunk.length;
  }
  return out;
}
async function readOrigin(
  bytes: Uint8Array,
  png: boolean,
): Promise<string | undefined> {
  const parts: string[] = [];
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  if (png) {
    for (let pos = 8; pos + 12 <= bytes.length; ) {
      const length = view.getUint32(pos);
      if (length > bytes.length - pos - 12) throw new Error("PNG元数据不完整");
      const kind = text.decode(bytes.slice(pos + 4, pos + 8));
      if (["tEXt", "iTXt", "zTXt"].includes(kind)) {
        const data = bytes.slice(pos + 8, pos + 8 + length);
        const split = data.indexOf(0);
        if (split < 0) throw new Error("PNG文本格式无效");
        let payload = data.slice(split + 1),
          compressed = false;
        if (kind === "zTXt") {
          if (payload[0] !== 0) throw new Error("压缩标识无效");
          compressed = true;
          payload = payload.slice(1);
        }
        if (kind === "iTXt") {
          if (payload.length < 2 || payload[0] > 1 || payload[1] !== 0)
            throw new Error("来源文本无效");
          compressed = payload[0] === 1;
          payload = payload.slice(2);
          for (let i = 0; i < 2; i++) {
            const end = payload.indexOf(0);
            if (end < 0) throw new Error("来源文本不完整");
            payload = payload.slice(end + 1);
          }
        }
        if (compressed) payload = await inflate(payload);
        if (payload.length > 16384) throw new Error("来源标识过大");
        parts.push(
          text.decode(data.slice(0, split)) + "\0" + text.decode(payload),
        );
      }
      pos += length + 12;
    }
  } else {
    for (let pos = 2; pos + 4 <= bytes.length; ) {
      if (bytes[pos] !== 255) break;
      const marker = bytes[pos + 1];
      if (marker === 0xda || marker === 0xd9) break;
      if (marker === 255) {
        pos++;
        continue;
      }
      if (marker === 1 || (marker >= 0xd0 && marker <= 0xd7)) {
        pos += 2;
        continue;
      }
      const length = view.getUint16(pos + 2);
      if (length < 2 || length > bytes.length - pos - 2)
        throw new Error("JPEG元数据不完整");
      if ((marker >= 0xe0 && marker <= 0xef) || marker === 0xfe)
        parts.push(text.decode(bytes.slice(pos + 4, pos + length + 2)));
      pos += length + 2;
    }
  }
  let label: string | undefined;
  for (const part of parts) {
    const next = jsonLabel(part);
    if (next && label && next !== label)
      throw new Error("来源标识冲突，请保留原文件");
    if (next) label = next;
  }
  return label;
}
function crc32(data: Uint8Array): number {
  let crc = 0xffffffff;
  for (const value of data) {
    crc ^= value;
    for (let bit = 0; bit < 8; bit++)
      crc = (crc >>> 1) ^ (crc & 1 ? 0xedb88320 : 0);
  }
  return (crc ^ 0xffffffff) >>> 0;
}
function inject(bytes: Uint8Array, png: boolean, label: string): Blob {
  const payload = new TextEncoder().encode("AIGC\0" + label);
  if (png) {
    if (bytes.length < 33) throw new Error("PNG输出无效");
    const chunk = new Uint8Array(payload.length + 12);
    new DataView(chunk.buffer).setUint32(0, payload.length);
    chunk.set(new TextEncoder().encode("tEXt"), 4);
    chunk.set(payload, 8);
    new DataView(chunk.buffer).setUint32(
      chunk.length - 4,
      crc32(chunk.slice(4, -4)),
    );
    return new Blob([bytes.slice(0, 33), chunk, bytes.slice(33)], {
      type: "image/png",
    });
  }
  if (bytes.length < 2 || payload.length > 65533)
    throw new Error("JPEG输出无效");
  const header = new Uint8Array([255, 254, 0, 0]);
  new DataView(header.buffer).setUint16(2, payload.length + 2);
  return new Blob([bytes.slice(0, 2), header, payload, bytes.slice(2)], {
    type: "image/jpeg",
  });
}
export async function preparePhoto(
  file: File,
  scope: PhotoDraft["scope"],
): Promise<PhotoDraft> {
  if (
    !["image/jpeg", "image/png"].includes(file.type) ||
    file.size <= 0 ||
    file.size > 10 * 1024 * 1024
  )
    throw new Error("请选择10MiB以内的JPEG或PNG照片");
  const png = file.type === "image/png";
  const bytes = new Uint8Array(await file.arrayBuffer());
  const [width, height] = imageDimensions(bytes, png);
  if (!width || !height || width * height > 20_000_000)
    throw new Error("照片最多20MP");
  const label = await readOrigin(bytes, png);
  const image = await createImageBitmap(file, {
    imageOrientation: "from-image",
  });
  let clean: Blob;
  try {
    if (
      !image.width ||
      !image.height ||
      image.width * image.height > 20_000_000
    )
      throw new Error("照片最多20MP");
    const canvas = document.createElement("canvas");
    canvas.width = image.width;
    canvas.height = image.height;
    const context = canvas.getContext("2d");
    if (!context) throw new Error("照片转换不可用");
    context.drawImage(image, 0, 0);
    clean = await new Promise<Blob>((resolve, reject) =>
      canvas.toBlob(
        (blob) => (blob ? resolve(blob) : reject(new Error("照片转换失败"))),
        file.type,
        0.9,
      ),
    );
  } finally {
    image.close();
  }
  const result = label
    ? inject(new Uint8Array(await clean.arrayBuffer()), png, label)
    : clean;
  if (result.size > 10 * 1024 * 1024) throw new Error("转换后的照片超过10MiB");
  return {
    key: key(),
    name: file.name.slice(0, 200),
    type: file.type,
    blob: result,
    scope,
  };
}
