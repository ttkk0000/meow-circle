import {type User} from "../core/api";
import {integer} from "../core/wire";
import {Button, Card, Modal, Notice, PrivateImage} from "../components/ui";

export interface BindingMethods {
  phone: string;
  password_set: boolean;
  linked: Array<{ id: bigint; provider: string }>;
}
function methodLabel(provider: string) {
  return ({ wechat: "微信", qq: "QQ", apple: "Apple", email: "邮箱" } as Record<string, string>)[provider] || "其他已关联方式";
}
export function AuthUnbindTask({ methods, identity, busy, onKeep, onConfirm }: {
  methods?: BindingMethods; identity: bigint; busy: boolean; onKeep: () => void; onConfirm: () => void;
}) {
  const selected = methods?.linked.find(row => row.id === identity);
  const remaining = methods ? [methods.password_set ? "账号密码" : "", methods.phone ? `手机号 ${methods.phone}` : "",
    ...methods.linked.filter(row => row.id !== identity).map(row => methodLabel(row.provider))].filter(Boolean) : [];
  return <Modal title="解除登录关联" task busy={busy} onClose={onKeep}
    actions={<Button disabled={busy || !selected} onClick={onConfirm}>确认解除</Button>}>
    <div className="auth-task-content">
      <Card><dl className="auth-facts"><div><dt>登录方式</dt><dd>{selected ? methodLabel(selected.provider) : "本次方式待核对"}</dd></div>
        <div><dt>其他可用方式</dt><dd>{methods ? remaining.join("、") || "尚未确认其他方式" : "尚未读取"}</dd></div></dl></Card>
      <Notice title="解除后会怎样">此方式将不能登录当前账号，资料和其他已关联方式保留。成功后旧会话会失效，请重新登录；最终是否允许解除由服务核对。</Notice>
      <Button disabled={busy} onClick={onKeep}>保留关联</Button>
    </div>
  </Modal>;
}
export function AuthBindTask({ user, method, destination, replacing, busy, onCancel, onConfirm }: {
  user: User; method: string; destination: string; replacing: boolean; busy: boolean; onCancel: () => void; onConfirm: () => void;
}) {
  return <Modal title={replacing ? "更新登录方式" : "关联登录方式"} task busy={busy} onClose={onCancel}
    actions={<Button kind="primary" disabled={busy} onClick={onConfirm}>{replacing ? "确认更新" : "确认关联"}</Button>}>
    <div className="auth-task-content">
      <Card><div className="profile-identity security-identity">
        <PrivateImage id={integer(user.avatar_url?.match(/\/media\/(\d+)\/content/)?.[1]) || undefined} alt="当前账号头像" className="security-avatar" />
        <div><strong>{user.nickname || user.username}</strong><p className="caption muted">当前账号</p></div>
      </div></Card>
      <Card><dl className="auth-facts"><div><dt>{replacing ? "将更新的方式" : "将关联的方式"}</dt><dd>{method}</dd></div>
        <div><dt>本次联系方式</dt><dd>{destination}</dd></div></dl></Card>
      <Notice title="确认是你本人的账号">下次可用此方式登录同一个M&amp;D账号。此次确认不创建新账号，也不改变交流资格；成功后旧会话会失效，请重新登录。</Notice>
      <Button kind="quiet" disabled={busy} onClick={onCancel}>{replacing ? "暂不更新" : "暂不关联"}</Button>
    </div>
  </Modal>;
}
