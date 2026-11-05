import {Button, Modal, Notice} from "./ui";

export function AccountConflictTask({ busy, hasAccount, onReturn, onOther, onRecovery }: {
  busy: boolean; hasAccount: boolean; onReturn: () => void; onOther: () => void; onRecovery: () => void;
}) {
  return <Modal title="这个登录方式已有关联" task busy={busy} onClose={onReturn}
    actions={<Button kind="primary" disabled={busy} onClick={onOther}>用其他方式登录</Button>}>
    <div className="auth-task-content"><h1>先确认你要使用的账号</h1>
      <Notice title="不会覆盖任何资料" warning>这个方式已关联另一个M&amp;D账号。请使用原方式登录，或通过账号恢复获得帮助。</Notice>
      <Button disabled={busy} onClick={onReturn}>{hasAccount ? "返回当前账号" : "返回刚才的页面"}</Button>
      <Button kind="quiet" disabled={busy} onClick={onRecovery}>账号恢复帮助</Button>
    </div>
  </Modal>;
}

export function LastLoginMethodTask({ busy, onReturn, onAdd }: { busy: boolean; onReturn: () => void; onAdd: () => void }) {
  return <Modal title="先保留一种登录方式" task busy={busy} onClose={onReturn}
    actions={<Button kind="primary" disabled={busy} onClick={onAdd}>添加其他方式</Button>}>
    <div className="auth-task-content"><h1>避免无法再登录</h1>
      <Notice title="这是最后一种可用方式" warning>先关联并验证手机号或另一种方式，再解除当前关联。</Notice>
      <Button kind="quiet" disabled={busy} onClick={onReturn}>返回账号设置</Button>
    </div>
  </Modal>;
}

/** 登录、绑定和官方回跳共用冲突说明，不投影其他账号的任何信息。 */
export function AccountConflict() {
  return (
    <Notice title="保留原绑定">
      <p>
        邮箱、手机号或第三方身份已有绑定时，不覆盖原资料、不迁移聊天，也不透露其他账号信息。
      </p>
      <p>
        请登录或找回原账号，或选择其他登录标识。持有新号码本身不能接管其过去关联的账号。
      </p>
    </Notice>
  );
}
