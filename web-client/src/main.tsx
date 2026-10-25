import { lazy, StrictMode, Suspense } from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import OperatorApp from "./operator/OperatorApp";
import { ProviderCallbackPage } from "./features/ProviderSignIn";
import { GuardianClaimPage } from "./features/GuardianPages";
import { takeProviderCallback } from "./core/provider";
import { takeDesktopHandoff } from "./core/desktopProvider";
import "./styles.css";

const OpsApp = lazy(() => import("./operations/OpsApp"));
const DesktopProvider = lazy(() => import("./features/DesktopProvider"));

const root = document.getElementById("root");
if (!root) throw new Error("缺少 Web 宿主节点");
const callback =
  location.pathname.replace(/\/$/, "") === "/app/provider-callback";
const result = callback ? takeProviderCallback() : undefined;
const desktopProvider = location.pathname.replace(/\/$/, "") === "/app/desktop-provider";
const desktopHandoff = desktopProvider ? takeDesktopHandoff() : undefined;
const guardianClaim =
  location.pathname.replace(/\/$/, "") === "/app/guardian-invite";
const guardianHandoff = { code: "" };
if (guardianClaim) {
  const values = new URLSearchParams(location.hash.slice(1)).getAll("code");
  if (values.length === 1 && /^[A-Za-z0-9_-]{43}$/.test(values[0]))
    guardianHandoff.code = values[0];
  // 在任何 React 路由、滚动记忆或请求读取前移除一次性口令。
  history.replaceState(null, "", "/app/guardian-invite");
}
createRoot(root).render(
  <StrictMode>
    {callback ? (
      <ProviderCallbackPage result={result} />
    ) : desktopProvider ? (
      <Suspense fallback={<p role="status">正在载入桌面授权…</p>}><DesktopProvider handoff={desktopHandoff} /></Suspense>
    ) : guardianClaim ? (
      <GuardianClaimPage handoff={guardianHandoff} />
    ) : location.pathname.replace(/\/$/, "") === "/app/ops" ? (
      <Suspense fallback={<p role="status">正在载入 M&amp;D 技术运维…</p>}>
        <OpsApp />
      </Suspense>
    ) : location.pathname.replace(/\/$/, "") === "/app/operator" ? (
      <OperatorApp />
    ) : (
      <App />
    )}
  </StrictMode>,
);
