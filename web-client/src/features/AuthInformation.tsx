import {Button, Card, Modal, Notice, RowLink} from "../components/ui";
import {privacyURL, termsURL} from "../components/AuthPolicies";

export type AuthInformationPage = "entry" | "terms" | "privacy" | "providers";
export function FirstAccountContent({ method }: { method: string }) {
  return <>
    <div className="auth-brand"><strong>M&amp;D</strong><span className="caption muted">meow &amp; doggie</span></div>
    <h1>和毛孩子，记录每一天</h1>
    <Card><dl className="auth-facts"><div><dt>登录方式</dt><dd>{method}</dd></div></dl></Card>
    <Notice title="首次使用">继续将创建一个M&amp;D账号。昵称、头像和宠物资料都可以稍后补充。</Notice>
  </>;
}
export function AuthInformation({ page, onBack, onPage, onManage, onExit }: {
  page: AuthInformationPage; onBack: () => void; onPage: (page: AuthInformationPage) => void; onManage: () => void; onExit: () => void;
}) {
  const title = page === "entry" ? "服务协议与隐私" : page === "terms" ? "M&D 服务协议" : page === "privacy" ? "M&D 隐私政策" : "第三方登录说明";
  const facts: Array<[string, string]> = page === "terms" ? [
    ["账号", "本人使用并保护可用的登录方式"], ["内容与互动", "遵守发布范围，尊重他人资料与权利"], ["退出与申诉", "账号、内容和处理结果有对应入口"],
  ] : page === "privacy" ? [
    ["登录", "用于验证身份和关联同一账号"], ["位置", "附近互助单独说明用途并请求授权"], ["照片", "由你选择后用于对应内容"], ["账号数据", "可在设置申请查询或注销"],
  ] : [
    ["微信 / QQ", "仅处理本次授权返回的账号标识和你允许的资料"], ["Apple", "可在官方页面选择隐藏邮箱，昵称和头像可稍后补充"], ["不会顺带做的事", "不读取通讯录，不默认公开手机号或位置"],
  ];
  const official = page === "terms" ? termsURL : page === "privacy" ? privacyURL : undefined;
  return <Modal title={title} task onClose={onBack}
    actions={<Button kind="primary" onClick={() => page === "entry" || page === "providers" ? onExit() : onPage("entry")}>{page === "entry" || page === "providers" ? "返回登录" : "返回协议入口"}</Button>}>
    <div className="auth-task-content">
      {page === "entry" ? <>
        <RowLink title="服务协议" detail="账号、内容和社区规则" onClick={() => onPage("terms")} />
        <RowLink title="隐私与数据说明" detail="用途、授权和资料控制" onClick={() => onPage("privacy")} />
        <RowLink title="第三方登录说明" detail="微信、QQ与Apple授权范围" onClick={() => onPage("providers")} />
      </> : <>
        {page === "terms" ? <h1>使用前先了解</h1> : page === "privacy" ? <h1>管理你的资料与选择</h1> : null}
        <Card><dl className="auth-facts">{facts.map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}</dl></Card>
        {page === "terms" ? <Notice title="隐私与选择">你可以在设置中管理登录方式、通知和账号数据。</Notice> : null}
        {page === "privacy" ? <RowLink title="第三方登录信息" detail="查看 SDK 与提供方范围" onClick={() => onPage("providers")} /> : null}
        {page === "providers" ? <>
          <RowLink title="管理已关联方式" detail="登录后可在账号设置中调整" onClick={onManage} />
          <a className="button quiet" href={"https://weixin.qq.com/cgi-bin/readtemplate?lang=zh_CN&t=weixin_agreement&s=privacy"} target="_blank" rel="noopener noreferrer">微信官方隐私说明</a>
          <a className="button quiet" href="https://wiki.connect.qq.com/qq互联sdk隐私保护声明" target="_blank" rel="noopener noreferrer">QQ官方隐私说明</a>
          <a className="button quiet" href="https://www.apple.com/legal/privacy/" target="_blank" rel="noopener noreferrer">Apple隐私说明</a>
        </> : <>
          <p className="caption muted">此页为功能与资料用途摘要，正式内容以当前政策为准。</p>
          {official ? <a className="button quiet" href={official} target="_blank" rel="noopener noreferrer">阅读正式全文</a> : null}
        </>}
      </>}
    </div>
  </Modal>;
}
