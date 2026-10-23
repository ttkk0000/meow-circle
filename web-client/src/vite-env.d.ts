/// <reference types="vite/client" />
interface ImportMetaEnv {
  readonly VITE_MND_MAP_EMBED_TEMPLATE?: string;
  readonly VITE_MND_TERMS_URL?: string;
  readonly VITE_MND_PRIVACY_URL?: string;
}
interface ImportMeta {
  readonly env: ImportMetaEnv;
}
