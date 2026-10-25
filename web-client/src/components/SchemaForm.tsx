import schemasSource from "../core/schemas.json";
import { Card, Field, Choices, Check, Button } from "./ui";
import { integer } from "../core/wire";

export interface Schema {
  type?: string;
  properties?: Record<string, Schema>;
  items?: Schema;
  $ref?: string;
  anyOf?: Schema[];
  enum?: Array<string | number | boolean>;
  additionalProperties?: boolean | Schema;
}
export const schemas = schemasSource as unknown as Record<string, Schema>;
const labels: Record<string, string> = {
  invite_id: "原邀请编号",
  invite_version: "原邀请版本",
  evidence_ref: "已核实证据的最小私有引用",
  discoverable: "在群发现页展示公开简介",
  summary: "公开简介",
  member_count: "成员人数",
  member: "已加入",
  followed_users: "关注的作者",
  watched_cases: "关注的求助",
  name: "名称",
  nickname: "昵称",
  bio: "介绍",
  species: "物种",
  breed: "品种",
  birthday: "生日",
  sex: "性别",
  health: "健康情况与未知事项",
  audience: "可见范围",
  mode: "可见范围",
  user_ids: "好友编号",
  adoption_available: "开放领养介绍",
  missing: "标记为走失",
  entry: "动态内容",
  text: "正文／说明",
  title: "标题",
  space: "发布空间",
  category: "内容分类",
  tags: "标签",
  pet_id: "宠物编号",
  type: "事项类型",
  description: "特征与经过",
  city: "城市",
  area: "公开大致区域",
  latitude: "纬度",
  longitude: "经度",
  occurred_at: "发生时间（带时区）",
  private_location: "私密位置补充",
  case: "求助资料",
  message: "申请与照护计划",
  note: "处理说明",
  supplement: "补充说明",
  trial_until: "试养截止（带时区）",
  trial_terms: "试养照护条款",
  terms: "本次条款",
  context_kind: "关联事项类型",
  context_id: "关联事项编号",
  peer_id: "对方用户编号",
  starts_at: "开始时间（带时区）",
  ends_at: "结束时间（带时区）",
  place: "地点",
  reception: "接待方",
  pet: "宠物与参与情况",
  amount_cents: "基础金额（分）",
  visit_fee_cents: "到访费用（分）",
  required_fees: "全部必付费用说明",
  payee: "收款方／无款项说明",
  price: "报价",
  fees: "费用明细",
  label: "费用名称",
  required: "是否必付",
  responsibilities: "双方责任",
  return_terms: "退养与安全接回条款",
  nature: "转让性质",
  offer_id: "有偿信息编号",
  offer_version: "披露版本",
  responsible_party: "责任方",
  disclosure: "完整披露",
  custodian: "当前照护／经营主体",
  custodian_type: "主体类型",
  relationship: "与宠物的关系",
  origin: "来源说明",
  source: "来源",
  agency: "代理关系",
  agency_authorization: "代理授权情况",
  location: "地点",
  reception_details: "接待信息",
  health_disclosure: "健康披露",
  follow_up: "后续责任",
  valid_until: "有效截止（带时区）",
  total_cents: "合计金额（分）",
  user_id: "对方用户编号",
  target_kind: "关联内容类型",
  target_id: "关联内容编号",
  scope: "授权范围",
  role: "角色",
  reason: "理由与事项经过",
  applicant: "申请人",
  publisher: "发布人",
  action: "处理动作",
  version: "基准版本",
  revision: "提案版本",
  history_version: "历史版本",
  restore_version: "恢复参考版本",
  evidence: "私密证明说明",
  pet_name: "认领宠物名字",
  needs_help: "请求照护协助",
  notice: "群公告",
  members: "成员用户编号",
  muted: "免打扰",
  transfer_to: "接收者用户编号",
  transfer_expires_at: "转让截止（带时区）",
  scopes: "授权用途",
  child_id: "儿童账号编号",
  guardian_id: "监护账号编号",
  consent: "同意指定范围",
  policy_version: "政策版本",
  fields: "指定资料字段",
  selections: "指定记录范围",
  kind: "记录类型",
  id: "记录编号",
  related_case_id: "原事项编号",
  right_type: "具体权利",
  legal_name: "真实姓名／主体名称",
  statement: "本次声明",
  evidence_description: "材料来源与证明事项",
  confirmed: "我已核对本次事项",
  check: "核实项目",
  appeal_of: "原结论编号",
  checks: "逐项到访反馈",
  appointment_id: "预约编号",
  appointment_version: "预约版本",
  latitude_center: "中心纬度",
  longitude_center: "中心经度",
  radius_km: "提醒半径（5–100km）",
  frequency: "提醒频率",
  enabled: "启用",
  direct_messages: "私信提醒",
  groups: "群聊提醒",
  applications: "业务进度提醒",
  interactions: "互动提醒",
  push_permission: "系统权限",
  favorites: "收藏表情",
  recent: "最近使用",
  care_until: "接回前照护安排",
  costs: "费用承担",
  safety: "安全安排",
  phone: "完整手机号",
  email: "联系邮箱",
  identity_id: "登录方式编号",
  background: "个人主页背景",
  theme: "主题偏好",
  avatar_media_id: "头像媒体编号",
  visit_feedback: "到访反馈",
  identity: "主体身份",
  fees_match: "费用一致性",
  aftercare: "后续照护责任",
  authority_summary: "处分与发布授权说明",
  custodian_relationship: "照护方关系",
  deposit_terms: "订金与退还条款",
  known_health: "已知健康情况",
  publisher_type: "发布主体类型",
  price_cents: "基础报价（分）",
  currency: "币种",
  paid_by: "费用承担方",
  when: "费用支付时点",
  pet_version: "宠物档案版本",
  private_address: "私密地址",
  venue_type: "接待场所类型",
  reception_relationship: "接待方关系",
  offer: "完整有偿信息披露",
  items: "逐项反馈",
  preference: "消息提醒偏好",
  subscription: "附近提醒",
  closure_reason: "结束原因",
  matched_claim_id: "匹配认领编号",
};
export const labelFor = (key: string) => labels[key] ?? "补充资料";
export const hidden = new Set([
  "version",
  "client_request_id",
  "idempotency_key",
  "media_ids",
  "reauth_token",
  "query_token",
  "safety_token",
  "password",
  "code",
  "token",
  "device",
  "confirmed",
  "declaration",
  "content_origin",
]);
const formHidden = new Set([
  ...hidden,
  "snapshot",
  "closure_reason",
  "matched_claim_id",
  "push_permission",
  "content",
  "adoption_cycle",
  "adoption_opened",
  "placement_state",
  "latitude",
  "longitude",
  "radius_km",
  "location_precision",
  "coordinate_system",
]);
const enumFields: Record<string, Array<[string, string]>> = {
  species: [
    ["cat", "猫咪"],
    ["dog", "狗狗"],
    ["rabbit", "兔兔"],
    ["other", "其他宠物"],
  ],
  sex: [
    ["", "未知"],
    ["male", "公"],
    ["female", "母"],
  ],
  space: [
    ["community", "社区"],
    ["circle", "宠友圈"],
    ["diary", "宠物日记"],
  ],
  mode: [
    ["private", "仅自己"],
    ["public", "公开"],
    ["friends", "好友"],
    ["selected", "指定好友"],
    ["excluded", "排除指定好友"],
  ],
  nature: [
    ["adoption", "领养"],
    ["sale", "出售"],
    ["paid_transfer", "有偿转让"],
  ],
  frequency: [
    ["instant", "即时"],
    ["digest", "汇总"],
    ["daily", "每日"],
    ["off", "关闭"],
  ],
  scope: [
    ["adoption_editor", "领养介绍协作"],
    ["caretaker", "私密照护"],
    ["group_member", "群成员"],
  ],
  check: [
    ["account", "账号"],
    ["publication_authority", "发布授权"],
    ["pet_recent", "近期情况"],
    ["location", "地点"],
    ["medical_document", "医疗文件"],
    ["medical_source", "医疗来源"],
  ],
  target_kind: [
    ["", "不指定"],
    ["entries", "动态"],
    ["pets", "宠物"],
    ["offers", "有偿信息"],
    ["cases", "走失求助"],
    ["users", "用户资料"],
  ],
  context_kind: [
    ["visit", "宠物到访"],
    ["adoption", "领养事项"],
    ["case", "走失求助"],
    ["offer", "有偿信息"],
  ],
  custodian_type: [
    ["individual", "个人"],
    ["business", "经营主体"],
    ["organization", "组织"],
  ],
  relationship: [
    ["direct", "本人直接照护"],
    ["employee", "雇员"],
    ["agent", "授权代理"],
  ],
  publisher_type: [
    ["person", "个人"],
    ["business", "经营主体"],
    ["rescue", "救助组织"],
  ],
  currency: [["CNY", "人民币"]],
  kind: [
    ["entries", "动态"],
    ["pets", "宠物"],
    ["media", "照片"],
    ["cases", "求助"],
    ["adoptions", "领养事项"],
    ["appointments", "预约"],
  ],
  background: [
    ["picnic", "Picnic"],
    ["desk", "Desk"],
    ["arcade", "Arcade"],
    ["garden", "Garden"],
  ],
};
const types: Record<string, Array<[string, string]>> = {
  Case: [
    ["lost", "走失"],
    ["found", "拾获待认领"],
  ],
  ReviewInput: [
    ["verification", "核实"],
    ["report", "举报"],
    ["appeal", "原结论申诉"],
    ["support", "协助"],
    ["recovery", "恢复协助"],
  ],
  RightsInput: [
    ["safety", "安全求助"],
    ["privacy", "隐私报告"],
    ["rights_notice", "正式权利通知"],
    ["counterstatement", "对原通知声明"],
    ["supplement", "追加原事项"],
    ["data_assistance", "资料事项协助"],
  ],
  DataRequestInput: [
    ["erase_fields", "删除指定范围"],
    ["close_account", "正式注销"],
    ["matters_assistance", "未结事项协助"],
  ],
};
function resolved(schema: Schema): Schema {
  if (schema.$ref) return schemas[schema.$ref.split("/").at(-1)!] ?? {};
  if (schema.anyOf)
    return resolved(schema.anyOf.find((s) => s.type !== "null") ?? {});
  return schema;
}
export function schemaDefault(name: string): Record<string, unknown> {
  return initial(schemas[name]) as Record<string, unknown>;
}
function initial(raw: Schema = {}): unknown {
  if (raw.anyOf?.some((s) => s.type === "null")) return null;
  const schema = resolved(raw);
  if (schema.type === "object")
    return Object.fromEntries(
      Object.entries(schema.properties ?? {})
        .filter(([key]) => !formHidden.has(key))
        .map(([key, value]) => [key, initial(value)]),
    );
  if (schema.type === "array") return [];
  if (schema.type === "boolean") return false;
  if (schema.type === "integer") return 0n;
  if (schema.type === "number") return 0;
  return "";
}
export function SchemaForm({
  name,
  value,
  onChange,
  disabled = false,
  omit = [],
  include,
}: {
  name: string;
  value: Record<string, unknown>;
  onChange: (value: Record<string, unknown>) => void;
  disabled?: boolean;
  omit?: string[];
  include?: readonly string[];
}) {
  const schema = schemas[name];
  if (!schema) return <p role="alert">此表单合同尚不可用</p>;
  return (
    <div className="stack">
      {Object.entries(schema.properties ?? {})
        .filter(
          ([key]) =>
            !formHidden.has(key) &&
            !omit.includes(key) &&
            (!include || include.includes(key)),
        )
        .map(([key, raw]) => (
          <SchemaField
            key={key}
            name={key}
            parent={name}
            raw={raw}
            value={value[key]}
            disabled={
              disabled ||
              (name === "Pet" &&
                key === "adoption_available" &&
                !["cat", "dog"].includes(String(value.species)))
            }
            onChange={(field) => onChange({ ...value, [key]: field })}
          />
        ))}
    </div>
  );
}
function SchemaField({
  name,
  parent,
  raw,
  value,
  onChange,
  disabled,
}: {
  name: string;
  parent: string;
  raw: Schema;
  value: unknown;
  onChange: (value: unknown) => void;
  disabled: boolean;
}) {
  const schema = resolved(raw);
  const nestedName = raw.$ref?.split("/").at(-1) ?? parent;
  const options =
    name === "type"
      ? types[parent]
      : name === "frequency"
        ? parent === "Subscription"
          ? ([
              ["instant", "即时"],
              ["daily", "每日汇总"],
              ["off", "关闭"],
            ] as Array<[string, string]>)
          : ([
              ["instant", "即时"],
              ["digest", "汇总"],
              ["off", "关闭"],
            ] as Array<[string, string]>)
        : name === "species" && parent !== "Pet"
          ? [
              ...(parent === "Subscription"
                ? [["", "全部物种"] as [string, string]]
                : []),
              ...enumFields.species.filter(([value]) =>
                ["cat", "dog"].includes(value),
              ),
            ]
          : enumFields[name];
  if (options)
    return (
      <Choices
        label={labelFor(name)}
        choices={options}
        value={String(value ?? "")}
        disabled={disabled}
        onChange={onChange}
      />
    );
  if (schema.type === "boolean")
    return (
      <Check checked={value === true} onChange={onChange} disabled={disabled}>
        {labelFor(name)}
      </Check>
    );
  if (schema.type === "object") {
    if (schema.additionalProperties && !schema.properties) {
      const object = (value ?? {}) as Record<string, unknown>;
      return (
        <Card title={labelFor(name)}>
          {["identity", "pet", "reception", "place", "fees"].map((key) => (
            <Choices
              key={key}
              label={labelFor(key)}
              value={String(object[key] ?? "unknown")}
              choices={[
                ["unknown", "尚未核实"],
                ["match", "一致"],
                ["mismatch", "不一致"],
              ]}
              disabled={disabled}
              onChange={(v) => onChange({ ...object, [key]: v })}
            />
          ))}
        </Card>
      );
    }
    const object = (value ?? {}) as Record<string, unknown>;
    return (
      <fieldset className="field-section" disabled={disabled}>
        <legend>{labelFor(name)}</legend>
        {Object.entries(schema.properties ?? {})
          .filter(([key]) => !formHidden.has(key))
          .map(([key, child]) => (
            <SchemaField
              key={key}
              name={key}
              parent={nestedName}
              raw={child}
              value={object[key]}
              disabled={
                disabled ||
                (nestedName === "Pet" &&
                  key === "adoption_available" &&
                  !["cat", "dog"].includes(String(object.species)))
              }
              onChange={(v) =>
                onChange({
                  ...object,
                  [key]: v,
                  ...(nestedName === "Pet" &&
                  key === "species" &&
                  !["cat", "dog"].includes(String(v))
                    ? { adoption_available: false }
                    : {}),
                })
              }
            />
          ))}
      </fieldset>
    );
  }
  if (schema.type === "array") {
    const rows = Array.isArray(value) ? value : [];
    const item = resolved(schema.items ?? {});
    if (item.type !== "object")
      return (
        <Field
          label={labelFor(name)}
          value={rows.map(String).join(",")}
          disabled={disabled}
          hint={
            item.type === "integer" ? "多个编号用逗号分隔" : "多项用逗号分隔"
          }
          onChange={(text) => {
            const values = text.split(/[,，\s]+/).filter(Boolean);
            if (item.type === "integer" && values.some((v) => !/^\d+$/.test(v)))
              return;
            onChange(
              item.type === "integer" ? values.map((v) => BigInt(v)) : values,
            );
          }}
        />
      );
    return (
      <fieldset className="field-section" disabled={disabled}>
        <legend>{labelFor(name)}</legend>
        {rows.map((row, index) => (
          <div className="field-array stack" key={index}>
            <SchemaField
              name={name}
              parent={nestedName}
              raw={schema.items!}
              value={row}
              disabled={disabled}
              onChange={(v) =>
                onChange(rows.map((old, i) => (i === index ? v : old)))
              }
            />
            <Button
              onClick={() => onChange(rows.filter((_, i) => i !== index))}
              disabled={disabled}
            >
              移除此项
            </Button>
          </div>
        ))}
        <Button
          disabled={disabled || rows.length >= 30}
          onClick={() => onChange([...rows, initial(schema.items)])}
        >
          添加{labelFor(name)}
        </Button>
      </fieldset>
    );
  }
  if (schema.type === "integer")
    return (
      <Field
        label={labelFor(name)}
        value={value === undefined ? "" : String(value)}
        disabled={disabled}
        onChange={(text) => {
          if (/^-?\d*$/.test(text))
            onChange(text === "" || text === "-" ? undefined : BigInt(text));
        }}
      />
    );
  if (schema.type === "number")
    return (
      <Field
        label={labelFor(name)}
        value={value === undefined ? "" : String(value)}
        type="number"
        disabled={disabled}
        onChange={(text) => onChange(text === "" ? undefined : Number(text))}
      />
    );
  return (
    <Field
      label={labelFor(name)}
      value={String(value ?? "")}
      disabled={disabled}
      multiline={[
        "text",
        "bio",
        "message",
        "note",
        "reason",
        "description",
        "health",
        "statement",
        "evidence",
        "responsibilities",
        "return_terms",
        "safety",
        "care_until",
        "evidence_description",
      ].includes(name)}
      onChange={onChange}
    />
  );
}
export function Facts({ value }: { value: unknown }) {
  if (value === null || value === undefined)
    return <span className="muted">尚未提供</span>;
  if (Array.isArray(value))
    return (
      <div className="stack">
        {value.length ? (
          value.map((item, index) => (
            <div key={index}>
              <Facts value={item} />
            </div>
          ))
        ) : (
          <span className="muted">尚未提供</span>
        )}
      </div>
    );
  if (typeof value !== "object")
    return (
      <span>
        {typeof value === "boolean" ? (value ? "是" : "否") : String(value)}
      </span>
    );
  return (
    <dl className="facts">
      {Object.entries(value)
        .filter(
          ([key]) =>
            !hidden.has(key) &&
            key !== "internal_note" &&
            key !== "token" &&
            labels[key],
        )
        .map(([key, item]) => (
          <div key={key} style={{ display: "contents" }}>
            <dt>{labelFor(key)}</dt>
            <dd>
              <Facts value={item} />
            </dd>
          </div>
        ))}
    </dl>
  );
}
