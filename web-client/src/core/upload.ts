/** 与原API请求共用Cookie／CSRF／账号代次检查，只为multipart提供浏览器真实传输进度。 */
export function uploadWithProgress(
  url: string,
  method: string,
  headers: Headers,
  body: FormData,
  progress: (percent: number | undefined) => void,
  signal?: AbortSignal,
): Promise<Response> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    let settled = false;
    const cleanup = () => {
      xhr.upload.onprogress = null;
      xhr.onload = xhr.onerror = xhr.ontimeout = xhr.onabort = null;
      signal?.removeEventListener("abort", abort);
    };
    const fail = (error: Error) => {
      if (!settled) {
        settled = true;
        cleanup();
        reject(error);
      }
    };
    const abort = () => {
      xhr.abort();
      fail(new DOMException("上传已取消，原编号保留", "AbortError"));
    };
    if (signal?.aborted) {
      abort();
      return;
    }
    xhr.open(method, url);
    xhr.withCredentials = true;
    xhr.responseType = "blob";
    xhr.timeout = 60_000;
    headers.forEach((value, name) => xhr.setRequestHeader(name, value));
    xhr.upload.onprogress = (event) =>
      progress(
        event.lengthComputable && event.total > 0
          ? Math.min(100, Math.floor((event.loaded / event.total) * 100))
          : undefined,
      );
    xhr.onerror = xhr.ontimeout = () =>
      fail(new TypeError("照片传输连接中断，请按原编号重试"));
    xhr.onabort = () =>
      fail(new DOMException("上传已取消，原编号保留", "AbortError"));
    xhr.onload = () => {
      if (settled) return;
      if (xhr.status < 200) {
        fail(new TypeError("上传未能取得响应"));
        return;
      }
      const responseHeaders = new Headers();
      for (const line of xhr
        .getAllResponseHeaders()
        .trim()
        .split(/[\r\n]+/)) {
        const split = line.indexOf(":");
        if (split > 0)
          responseHeaders.append(
            line.slice(0, split),
            line.slice(split + 1).trim(),
          );
      }
      try {
        const response = new Response(
          [204, 205, 304].includes(xhr.status) ? null : xhr.response,
          {
            status: xhr.status,
            statusText: xhr.statusText,
            headers: responseHeaders,
          },
        );
        settled = true;
        cleanup();
        resolve(response);
      } catch {
        fail(new TypeError("上传响应无效，请核对原上传结果"));
      }
    };
    signal?.addEventListener("abort", abort, { once: true });
    try {
      xhr.send(body);
    } catch {
      fail(new TypeError("照片暂时无法开始传输"));
    }
  });
}
