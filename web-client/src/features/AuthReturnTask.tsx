import {Button, Modal, RowLink} from "../components/ui";

/** Uses the current app route, without storing inputs or inventing recoverable tasks. */
export function authReturnTarget(route: string): { title: string; detail: string } {
  const url = new URL(route, "https://mnd.invalid");
  const flow = url.searchParams.get("flow");
  if (url.pathname === "/edit") {
    if (flow === "application") return { title: "继续领养申请", detail: "检查申请内容与共享范围，再确认提交" };
    if (flow === "entry") return { title: "继续圈子草稿", detail: "保留宠物、文字和受众；检查后再发布" };
    if (flow === "case") return { title: "继续走失求助", detail: "状态和公开同意分别确认" };
    return { title: "返回原任务", detail: "检查当前内容与共享范围，不自动提交" };
  }
  if (/^\/conversations\/\d+/.test(url.pathname)) return { title: "继续私信", detail: "回原会话，不自动发送" };
  if (/^\/groups\/\d+/.test(url.pathname)) return { title: "返回群聊", detail: "回原会话，不自动发送" };
  if (url.pathname === "/nearby") return { title: "继续附近提醒", detail: "登录不会自动开启订阅" };
  if (url.pathname === "/security") return { title: "返回账号设置", detail: "继续核对当前账号与登录方式" };
  if (/^\/(pets|adoptions)(\/|$)/.test(url.pathname)) return { title: "返回领养页面", detail: "按当前权限读取，不自动提交申请或确认安排" };
  if (/^\/(cases|claims|leads)(\/|$)/.test(url.pathname)) return { title: "返回走失求助", detail: "按当前权限读取；状态与公开同意分别确认" };
  if (url.pathname === "/messages") return { title: "返回消息", detail: "登录不会自动发送消息或标记全部已读" };
  if (url.pathname === "/publish") return { title: "返回发布选择", detail: "选择类型并检查内容后，再确认发布" };
  return { title: "返回刚才的页面", detail: "继续原页面，不自动发送、发布或开启订阅" };
}

export function AuthReturnTask({ route, busy, onBack, onContinue }: {
  route: string; busy: boolean; onBack: () => void; onContinue: () => void;
}) {
  const target = authReturnTarget(route);
  return <Modal title="登录后回到哪里" task busy={busy} onClose={onBack}
    actions={<Button kind="primary" disabled={busy} onClick={onContinue}>返回刚才的页面</Button>}>
    <div className="auth-task-content auth-return-task">
      <RowLink title={target.title} detail={target.detail} disabled={busy} onClick={onContinue} />
    </div>
  </Modal>;
}
