export interface ProviderConfiguration {
  appID: string;
  redirectURI: string;
}
export function providerConfiguration(
  provider: string,
): ProviderConfiguration | undefined {
  const appID =
    provider === "wechat"
      ? import.meta.env.VITE_MND_WECHAT_WEB_APP_ID
      : provider === "qq"
        ? import.meta.env.VITE_MND_QQ_WEB_APP_ID
        : "";
  if (
    !appID ||
    (provider === "wechat"
      ? !/^wx[a-zA-Z0-9]{16}$/.test(appID) || appID === "wx0000000000000000"
      : !/^[1-9][0-9]{4,19}$/.test(appID))
  )
    return;
  try {
    const redirect = new URL(
      import.meta.env.VITE_MND_OAUTH_REDIRECT_URI ?? "/app/provider-callback",
      location.origin,
    );
    if (
      redirect.protocol !== "https:" ||
      redirect.origin !== location.origin ||
      redirect.pathname !== "/app/provider-callback" ||
      redirect.search ||
      redirect.hash ||
      redirect.username ||
      redirect.password
    )
      return;
    return { appID, redirectURI: redirect.href };
  } catch {
    return;
  }
}
export interface ProviderCallback {
  type: "mnd-provider-result";
  state: string;
  code: string;
  cancelled: boolean;
}
const validState = (value: string) => /^[a-zA-Z0-9_-]{32,200}$/.test(value);

/** 只有同源回跳页进入此入口；先移除URL凭据，再加载任何业务页面。 */
export function takeProviderCallback(): ProviderCallback | undefined {
  const params = new URLSearchParams(location.search);
  history.replaceState(null, "", "/app/provider-callback");
  const state = params.get("state") ?? "";
  const code = params.get("code") ?? "";
  if (
    !validState(state) ||
    params.getAll("state").length !== 1 ||
    params.getAll("code").length > 1 ||
    code.length > 4096
  )
    return;
  return {
    type: "mnd-provider-result",
    state,
    code,
    cancelled: !code || params.has("error") || params.has("errcode"),
  };
}

/** 使用同源BroadcastChannel；官方页面没有opener，不能导航原任务或读取其窗口。 */
export function providerPopup(
  provider: string,
  configuration: ProviderConfiguration,
  state: string,
  popup: Window,
) {
  if (!validState(state)) throw new Error("服务返回的授权挑战无效");
  const channel = new BroadcastChannel(`mnd-provider-${state}`);
  let rejectPromise: (reason: Error) => void = () => {};
  let finished = false;
  let timer: ReturnType<typeof setTimeout>;
  function cleanup() {
    channel.close();
    clearTimeout(timer);
    popup.close();
  }
  const result = new Promise<ProviderCallback>((resolve, reject) => {
    rejectPromise = reject;
    channel.onmessage = (event: MessageEvent<ProviderCallback>) => {
      const value = event.data;
      if (
        finished ||
        event.origin !== location.origin ||
        value?.type !== "mnd-provider-result" ||
        value.state !== state ||
        typeof value.code !== "string" ||
        value.code.length > 4096 ||
        typeof value.cancelled !== "boolean"
      )
        return;
      finished = true;
      cleanup();
      resolve(value);
    };
    timer = setTimeout(() => {
      if (!finished) {
        finished = true;
        cleanup();
        reject(new Error("官方授权等待超时，请重新发起；没有自动创建账号"));
      }
    }, 5 * 60_000);
    const url = new URL(
      provider === "wechat"
        ? "https://open.weixin.qq.com/connect/qrconnect"
        : "https://graph.qq.com/oauth2.0/authorize",
    );
    url.searchParams.set(
      provider === "wechat" ? "appid" : "client_id",
      configuration.appID,
    );
    url.searchParams.set("redirect_uri", configuration.redirectURI);
    url.searchParams.set("response_type", "code");
    url.searchParams.set(
      "scope",
      provider === "wechat" ? "snsapi_login" : "get_user_info",
    );
    url.searchParams.set("state", state);
    if (provider === "wechat") url.hash = "wechat_redirect";
    try {
      popup.location.replace(url.href);
    } catch {
      finished = true;
      cleanup();
      reject(new Error("无法打开官方授权页面，请返回重试"));
    }
  });
  return {
    result,
    cancel: () => {
      if (!finished) {
        finished = true;
        cleanup();
        rejectPromise(new DOMException("已取消本次官方授权", "AbortError"));
      }
    },
  };
}
