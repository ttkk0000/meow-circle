import {Button, Check, Field, Modal, Notice} from "../components/ui";
import {LegalLinks, privacyURL, termsURL} from "../components/AuthPolicies";
import {validAuthCode, validLoginPhone} from "../core/authInput";
import {useEffect, useRef} from "react";

export function AuthOtpField({ value, onChange, disabled, label = "短信验证码，6位数字" }: {
  value: string; onChange: (value: string) => void; disabled: boolean; label?: string;
}) {
  const input = useRef<HTMLInputElement>(null);
  useEffect(() => { if (!disabled) input.current?.focus(); }, [disabled]);
  return <div className="auth-otp">
    <input ref={input} aria-label={label} value={value} onChange={event => onChange(event.target.value.replace(/[^0-9]/g, "").slice(0, 6))}
      onFocus={event => event.currentTarget.setSelectionRange(value.length, value.length)}
      onClick={event => event.currentTarget.setSelectionRange(value.length, value.length)}
      inputMode="numeric" autoComplete="one-time-code" type="text" disabled={disabled} />
    <div className="auth-otp-cells" aria-hidden="true">{Array.from({ length: 6 }, (_, index) =>
      <span key={index} className={index === Math.min(value.length, 5) ? "current" : ""}>{value[index] || "·"}</span>)}</div>
  </div>;
}

export function AuthPhoneErrorTask({ value, onChange, busy, agreed, onAgree, canSend, wait, onSend, onOther, onBack }: {
  value: string; onChange: (value: string) => void; busy: boolean; agreed: boolean; onAgree: (value: boolean) => void;
  canSend: boolean; wait: number; onSend: () => void; onOther: () => void; onBack: () => void;
}) {
  const valid = validLoginPhone(value), policyReady = !!termsURL && !!privacyURL;
  const content = useRef<HTMLDivElement>(null);
  useEffect(() => { content.current?.querySelector("input")?.focus(); }, []);
  return <Modal title="请检查手机号" task busy={busy} onClose={onBack}>
    <div ref={content} className="auth-task-content auth-phone-error"><h1>{value.replace(/[^0-9]/g, "").length < 10 ? "手机号还没有填完整" : "请核对手机号格式"}</h1>
      <Field label="已绑定手机号" value={value} onChange={onChange} type="tel" autoComplete="tel" placeholder="请输入手机号" disabled={busy}
        error={!valid ? "请填写可接收短信的完整手机号。" : undefined} />
      {policyReady ? <><Check checked={agreed} onChange={onAgree} disabled={busy}>我已阅读并同意服务协议与隐私政策</Check><LegalLinks /></> : null}
      <Button kind="primary" disabled={busy || !canSend || !valid || wait > 0 || policyReady && !agreed} onClick={onSend}>{wait ? `${wait}秒后可重发` : "重新获取验证码"}</Button>
      <Button kind="quiet" disabled={busy} onClick={onOther}>换个登录方式</Button>
    </div>
  </Modal>;
}

export function AuthCodeResentTask({ address, value, onChange, busy, canSubmit, onSubmit, onOther, onBack }: {
  address: string; value: string; onChange: (value: string) => void; busy: boolean;
  canSubmit: boolean; onSubmit: () => void; onOther: () => void; onBack: () => void;
}) {
  return <Modal title="验证码已重发" task busy={busy} onClose={onBack}
    actions={<Button kind="primary" disabled={busy || !canSubmit || !validAuthCode(value)} onClick={onSubmit}>验证并继续</Button>}>
    <div className="auth-task-content"><h1>请留意新的短信</h1><strong>{address}</strong>
      <AuthOtpField value={value} onChange={onChange} disabled={busy} />
      <Notice title="使用最新验证码">旧验证码不再使用；不要向他人提供验证码。</Notice>
      <Button kind="quiet" disabled={busy} onClick={onOther}>更换登录方式</Button>
    </div>
  </Modal>;
}

export function AuthCodeExpiredTask({ busy, wait, canSend, onSend, onReturn, onBack }: {
  busy: boolean; wait: number; canSend: boolean; onSend: () => void; onReturn: () => void; onBack: () => void;
}) {
  return <Modal title="验证码已过期" task busy={busy} onClose={onBack}
    actions={<Button kind="primary" disabled={busy || !canSend || wait > 0} onClick={onSend}>{wait ? `${wait}秒后可重发` : "重新获取"}</Button>}>
    <div className="auth-task-content"><h1>验证码已过期</h1>
      <Notice title="可以继续">重新获取后填写最新验证码，无需重新开始刚才的操作。</Notice>
      <Button kind="quiet" disabled={busy} onClick={onReturn}>返回刚才的页面</Button>
    </div>
  </Modal>;
}

export function AuthSmsDelayedTask({ busy, onEdit, onReturn, onRecovery, onBack }: {
  busy: boolean; onEdit: () => void; onReturn: () => void; onRecovery: () => void; onBack: () => void;
}) {
  return <Modal title="暂时没有收到短信" task busy={busy} onClose={onBack}
    actions={<Button kind="primary" disabled={busy} onClick={onEdit}>返回填写</Button>}>
    <div className="auth-task-content"><h1>暂时没有收到短信</h1>
      <Notice title="可以继续">检查号码和短信收件箱，也可以返回登录选择其他已关联的方式。</Notice>
      <Button kind="quiet" disabled={busy} onClick={onReturn}>返回刚才的页面</Button>
      <Button kind="quiet" disabled={busy} onClick={onRecovery}>账号恢复帮助</Button>
    </div>
  </Modal>;
}
