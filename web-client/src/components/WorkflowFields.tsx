import { Card } from "./ui";
import { SchemaForm, schemas } from "./SchemaForm";

type Group = { title: string; fields: readonly string[]; note?: string };
const groups: Record<string, readonly Group[]> = {
  pet: [
    {
      title: "宠物资料",
      fields: ["name", "species", "breed", "sex", "birthday", "bio"],
    },
    {
      title: "状态与健康记录",
      fields: ["health", "missing", "adoption_available", "city"],
      note: "事实与记录分开；修改档案不代表核验通过，已有求助资料需另行确认。",
    },
    {
      title: "资料可见性",
      fields: ["audience"],
      note: "宠物档案、单条日记与公开求助分别设置。",
    },
  ],
  case: [
    {
      title: "发生了什么",
      fields: ["type", "title", "pet_id", "species", "description"],
    },
    { title: "时间与公开区域", fields: ["occurred_at", "city", "area"] },
    {
      title: "私密位置补充",
      fields: ["private_location"],
      note: "精确住址及私密线索不会出现在公开地图。",
    },
  ],
  offer: [
    {
      title: "宠物与发布主体",
      fields: [
        "pet_id",
        "pet_version",
        "nature",
        "publisher_type",
        "relationship",
        "authority_summary",
      ],
    },
    {
      title: "来源与照护情况",
      fields: ["custodian", "custodian_relationship", "origin", "known_health"],
    },
    {
      title: "到访与接待",
      fields: [
        "city",
        "area",
        "venue_type",
        "reception",
        "reception_relationship",
        "private_address",
      ],
    },
    {
      title: "全部费用",
      fields: [
        "price_cents",
        "currency",
        "fees",
        "total_cents",
        "payee",
        "deposit_terms",
      ],
    },
    {
      title: "责任与有效期限",
      fields: ["responsible_party", "aftercare", "valid_until"],
    },
  ],
  appointment: [
    {
      title: "关联事项与双方",
      fields: ["context_kind", "context_id", "peer_id"],
    },
    {
      title: "本次安排",
      fields: ["terms"],
      note: "新提案与已生效安排分别保留，双方确认同一版本后才生效。",
    },
  ],
  contract: [
    {
      title: "本次协议条款",
      fields: ["terms", "revision"],
      note: "核对宠物、费用、照护责任与安全接回安排，再确认本次版本。",
    },
  ],
  return: [
    {
      title: "安全接回安排",
      fields: ["terms"],
      note: "计划、当前照护与实际交接分别记录；未完成双方交接前不会重新开放匹配。",
    },
  ],
  application: [
    {
      title: "介绍你的照护计划",
      fields: ["message"],
      note: "说明居住环境、照护时间及已有宠物；不要在公开介绍中填写精确住址。",
    },
  ],
  review: [
    {
      title: "具体事项",
      fields: ["type", "check", "target_kind", "target_id", "appeal_of"],
    },
    {
      title: "可核实的事实",
      fields: ["reason"],
      note: "不知道的内容标注不确定；证明原件仅用于当前事项。",
    },
  ],
  safety: [
    {
      title: "问题类型与经过",
      fields: [
        "type",
        "reason",
        "description",
        "note",
        "target_kind",
        "target_id",
      ],
    },
    {
      title: "必要联系与本次声明",
      fields: [
        "contact",
        "email",
        "right_type",
        "legal_name",
        "statement",
        "confirmed",
      ],
    },
  ],
  group: [
    { title: "群资料", fields: ["name"] },
    {
      title: "公开发现",
      fields: ["discoverable", "city", "summary"],
      note: "公开简介不包含群内消息；申请经同意后才能入群。",
    },
  ],
  profile: [
    { title: "个人资料", fields: ["nickname", "bio"] },
    { title: "主页背景", fields: ["background"] },
  ],
  data: [
    {
      title: "本次请求",
      fields: ["type", "note"],
      note: "普通删除、注销和未结事项协助分别处理，以真实进度回执为准。",
    },
  ],
};

/** 页面负责信息顺序；字段类型、隐藏凭证和请求合同仍只有SchemaForm一份定义。 */
export function WorkflowFields({
  flow,
  name,
  envelope,
  value,
  onChange,
  disabled,
  omit,
}: {
  flow: string;
  name: string;
  envelope?: string;
  value: Record<string, unknown>;
  onChange: (value: Record<string, unknown>) => void;
  disabled: boolean;
  omit: string[];
}) {
  const plan = groups[flow];
  const bodyName = envelope
    ? schemas[name]?.properties?.[envelope]?.$ref?.split("/").at(-1)
    : name;
  if (!plan || !bodyName || !schemas[bodyName])
    return (
      <SchemaForm
        name={name}
        value={value}
        onChange={onChange}
        disabled={disabled}
        omit={omit}
      />
    );
  const body = (envelope ? (value[envelope] ?? {}) : value) as Record<
    string,
    unknown
  >;
  const edit = (next: Record<string, unknown>) =>
    onChange(envelope ? { ...value, [envelope]: next } : next);
  const keys = new Set(Object.keys(schemas[bodyName].properties ?? {}));
  const used = plan.flatMap((group) => group.fields);
  return (
    <div className="workflow-sections">
      {plan.map((group) =>
        group.fields.some((key) => keys.has(key) && !omit.includes(key)) ? (
          <Card title={group.title} key={group.title}>
            {group.note ? <p className="caption muted">{group.note}</p> : null}
            <SchemaForm
              name={bodyName}
              value={body}
              onChange={edit}
              disabled={disabled}
              include={group.fields}
              omit={omit}
            />
          </Card>
        ) : null,
      )}
      <SchemaForm
        name={bodyName}
        value={body}
        onChange={edit}
        disabled={disabled}
        omit={[...omit, ...used]}
      />
      {envelope ? (
        <SchemaForm
          name={name}
          value={value}
          onChange={onChange}
          disabled={disabled}
          omit={[...omit, envelope]}
        />
      ) : null}
    </div>
  );
}
