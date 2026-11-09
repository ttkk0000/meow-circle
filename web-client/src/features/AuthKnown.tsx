import type {RememberedIdentity} from "../core/api";
import {Button, Card, Check, Icon, Modal, Notice, PrivateImage} from "../components/ui";
import {LegalLinks, privacyURL, termsURL} from "../components/AuthPolicies";
import {useState} from "react";

export function AuthKnown({ known, busy, onClose, onPhone, onOther, onProvider }: {
  known: RememberedIdentity; busy: boolean; onClose: () => void; onPhone: () => void; onOther: () => void; onProvider: (provider: string) => void;
}) {
  const [agreed, setAgreed] = useState(false);
  return <Modal title="欢迎回来" task busy={busy} onClose={onClose}>
    <div className="auth-task-content auth-known">
      <Card><div className="profile-identity security-identity"><PrivateImage id={undefined} alt="上次账号头像" className="security-avatar" />
        <div><strong>{known.user.nickname || known.user.username}</strong><p className="caption muted">{known.method ? `上次使用${known.method}登录` : "上次在本机登录的账号"}</p></div></div></Card>
      <Notice title="继续使用同一账号">登录原账号后，宠物、日记、收藏和聊天都会保留。请再次验证；返回的其他身份不会按昵称合并。</Notice>
      <Check checked={agreed} onChange={setAgreed} disabled={busy || !termsURL || !privacyURL}>我已阅读并同意服务协议与隐私政策</Check>
      <LegalLinks />
      <div className="provider-methods"><span className="caption muted">快捷登录</span>
        <div>{[["wechat", "微信"], ["qq", "QQ"]].map(([provider, label]) => <Button key={provider} disabled={busy || !agreed || !termsURL || !privacyURL}
          onClick={() => onProvider(provider)}><Icon name={`provider-${provider}`} />{label}</Button>)}</div>
        <div className="apple-provider"><Button disabled={busy || !agreed || !termsURL || !privacyURL} onClick={() => onProvider("apple")}><Icon name="provider-apple" />通过 Apple 登录</Button></div>
      </div>
      <Button kind="quiet" disabled={busy} onClick={onPhone}>换用手机号</Button>
      <Button kind="quiet" disabled={busy} onClick={onOther}>其他登录方式</Button>
    </div>
  </Modal>;
}
