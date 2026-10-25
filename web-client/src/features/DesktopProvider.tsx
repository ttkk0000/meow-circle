import { useEffect, useRef, useState } from "react";
import { Button, Card, Notice } from "../components/ui";
import { providerConfiguration, providerPopup } from "../core/provider";
import { returnDesktopResult, type DesktopHandoff } from "../core/desktopProvider";

export default function DesktopProvider({ handoff }: { handoff?: DesktopHandoff }) {
  const operation = useRef<ReturnType<typeof providerPopup> | undefined>(undefined);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => () => { operation.current?.cancel(); operation.current = undefined; }, []);
  const config = handoff ? providerConfiguration(handoff.provider) : undefined;

  async function authorize() {
    if (!handoff || !config || operation.current) return;
    const popup = window.open("about:blank", "_blank", "popup,width=560,height=720");
    if (!popup) { setError("浏览器未打开官方窗口，请允许本次弹窗后重试。"); return; }
    popup.opener = null;
    setError(""); setBusy(true);
    const current = providerPopup(handoff.provider, config, handoff.state, popup);
    operation.current = current;
    try {
      const result = await current.result;
      if (operation.current === current) returnDesktopResult(handoff, result);
    } catch (failure) {
      if (operation.current === current) setError(failure instanceof Error ? failure.message : "授权暂不可用，请回到桌面重新发起。");
    } finally {
      if (operation.current === current) { operation.current = undefined; setBusy(false); }
    }
  }

  function cancel() {
    const current = operation.current; operation.current = undefined;
    current?.cancel(); setBusy(false);
    if (handoff) returnDesktopResult(handoff, { type: "mnd-provider-result", state: handoff.state, code: "", cancelled: true });
  }

  return <main className="provider-callback">
    <h1>M&amp;D 桌面官方授权</h1>
    <Card>
      {handoff && config ? <>
        <p>你正在为 M&amp;D 桌面端进行{handoff.provider === "wechat" ? "微信" : "QQ"}授权。完成后请回桌面继续核验和选择账号。</p>
        <p>请保持桌面应用打开。本页不会读取浏览器登录账号，也不会自动创建或合并账号。</p>
        <Button disabled={busy} onClick={() => void authorize()}>{busy ? "正在等待官方授权…" : "前往官方授权"}</Button>
        <Button kind="secondary" onClick={cancel}>取消并返回桌面</Button>
      </> : <Notice title="本次桌面授权不可用">请核对正式 Web 授权配置，并从桌面应用重新发起。</Notice>}
      {error && <Notice title="本次授权未完成">{error}</Notice>}
    </Card>
  </main>;
}
