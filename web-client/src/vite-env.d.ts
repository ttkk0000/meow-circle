/// <reference types="vite/client" />
interface ImportMetaEnv {
  readonly VITE_MND_WECHAT_WEB_APP_ID?: string;
  readonly VITE_MND_QQ_WEB_APP_ID?: string;
  readonly VITE_MND_OAUTH_REDIRECT_URI?: string;
  readonly VITE_MND_AMAP_JS_KEY?: string;
  readonly VITE_MND_TERMS_URL?: string;
  readonly VITE_MND_PRIVACY_URL?: string;
}
interface ImportMeta {
  readonly env: ImportMetaEnv;
}
