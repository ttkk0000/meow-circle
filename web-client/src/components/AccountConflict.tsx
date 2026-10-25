import { Notice } from "./ui";

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
