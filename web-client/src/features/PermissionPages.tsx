import { Button, Empty, Notice, RowLink, TaskPage } from "../components/ui";
import { back, navigate } from "../core/router";

export function LocationHelp() {
  return (
    <TaskPage
      title="不提供定位也可以"
      actions={
        <Button kind="primary" onClick={() => navigate("/pets")}>
          手动选择城市
        </Button>
      }
    >
      <Empty title="手动选城市，一样能浏览">
        <p>不会因为拒绝定位阻止阅读。目击位置也可以手动说明。</p>
      </Empty>
      {["武汉", "上海"].map((city) => (
        <RowLink
          key={city}
          title={city}
          detail="手动选择城市"
          to={`/pets?city=${encodeURIComponent(city)}`}
        />
      ))}
      <Notice title="按需请求权限">
        只有你选择地图或拍照等动作时再解释原因；拒绝后提供替代。公开信息只填写大致区域，不填写门牌或私人精确地址。
      </Notice>
      <Button kind="quiet" onClick={() => navigate("/cases")}>
        查看求助并提供手动目击线索
      </Button>
    </TaskPage>
  );
}

export function NotificationHelp() {
  return (
    <TaskPage
      title="开启系统通知"
      actions={
        <Button kind="primary" onClick={back}>
          返回原页面
        </Button>
      }
    >
      <Notice title="当前状态">
        手机 App 可在系统设置中找到 M&amp;D 并允许通知，返回 App
        后会重新读取权限状态。当前网页可直接在消息中心查看进度；允许浏览器通知权限不代表已开通设备推送。
      </Notice>
      <RowLink
        title="查看站内消息"
        detail="未授予系统权限也能使用"
        to="/messages"
      />
      <RowLink
        title="通知分类与频率"
        detail="账号偏好和每台设备的系统权限分别设置"
        to="/notification-settings"
      />
    </TaskPage>
  );
}

export function PhotoChoiceHelpContent() {
  return (
    <Empty title="不用开放全部相册">
      <p>
        优先使用系统照片选择器，只分享你选中的照片。取消后仍可编辑文字与保存草稿。首版暂不支持视频。
      </p>
    </Empty>
  );
}
export function PhotoChoiceHelp() {
  return (
    <TaskPage
      title="照片选择说明"
      actions={
        <Button kind="primary" onClick={back}>
          返回上一步
        </Button>
      }
    >
      <PhotoChoiceHelpContent />
      <Button onClick={back}>返回继续编辑</Button>
    </TaskPage>
  );
}
