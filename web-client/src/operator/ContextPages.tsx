import { Button } from "../components/ui";
import { DataTable, OpsNotice, Panel } from "../operations/ui";
import { label, time, type View } from "./models";
import type { OperatorSession } from "./useOperator";
import { BusinessStatus, SummaryPanel } from "./ui";

export function ReadinessPage({ session: s }: { session: OperatorSession }) {
  const context = s.context;
  const fields = [
    ["运营主体", context?.operator_name],
    ["主体地址", context?.operator_address],
    ["投诉联系方式", context?.operator_contact],
  ];
  const documents = context?.documents ?? [];
  const rows = fields.map(([name, value]) => ({
    key: name!,
    state: value ? "configured" : "missing",
    search: name!,
    cells: [
      name,
      "公开材料配置",
      value || "尚未配置",
      <BusinessStatus value={value ? "已配置，仍须核实" : "未配置"} />,
      "—",
    ],
  }));
  for (const document of documents)
    rows.push({
      key: `document/${document.key}`,
      state: document.status,
      search: `${document.title} ${document.revision}`,
      cells: [
        document.title,
        `${document.type} / ${document.revision}`,
        `${time(document.effective_from)} 至 ${document.effective_until ? time(document.effective_until) : "未设截止"}`,
        <BusinessStatus value={document.status} />,
        <a
          className="ops-link"
          target="_blank"
          rel="noopener noreferrer"
          href={`/api/v2/reading/${encodeURIComponent(document.key)}/download`}
        >
          完整文本
        </a>,
      ],
    });
  return (
    <>
      <OpsNotice title="发布前置核对">
        核对真实经营主体、公开完整文本、联系方式与适用版本。配置由部署方维护；读取材料不产生用户同意，也不直接发布协议。
      </OpsNotice>
      <DataTable
        title="正式发布前置条件"
        columns={["核对项目", "配置来源与版本", "当前说明", "状态", "操作"]}
        rows={rows}
        stateName={label}
        empty="尚未连接正式目录"
        emptyDescription="连接具名会话后读取服务端配置。"
      />
      <Panel title="适用范围仍需核实">
        <p>
          已发布类型：{context?.published_types?.join("、") || "尚无已发布材料"}
          。需结合实际平台性质、业务范围与材料生效时间逐项核对；此页不生成自动准入结论。
        </p>
        <p>材料 SHA-256 由只读目录提供，下载全文后可与发布物校验。</p>
        {documents.map((document) => (
          <p className="ops-caption" key={document.key}>
            {document.title} · {document.sha256 || "未返回摘要"}
          </p>
        ))}
      </Panel>
      <div className="ops-actions">
        <Button disabled={s.busy || !context} onClick={() => void s.refresh()}>
          重新核对目录
        </Button>
        <a className="button secondary" href="/app/ops">
          查看技术运维
        </a>
      </div>
    </>
  );
}
export function AccessPage({
  session: s,
  connect,
  go,
}: {
  session: OperatorSession;
  connect: () => void;
  go: (view: View, id?: string) => void;
}) {
  const roles: Array<[string, string, string]> = [
    ["moderator", "举报／权利事项核实", "原事项与当前版本"],
    ["privacy", "资料请求与保全", "明确事项编号与具体范围"],
    ["identity_verifier", "监护关系核验", "可信关系证据与有效邀请"],
    ["authority_verifier", "处置权核验", "实际授权证明"],
  ];
  return (
    <>
      <OpsNotice title="最小角色与会话">
        凭据仅驻留本页内存。消费者登录、worker及技术运维身份互不自动授予业务处理角色。
      </OpsNotice>
      <div className="ops-columns">
        <SummaryPanel
          title="具名账号"
          items={[
            ["当前账号", s.context?.id ?? "尚未连接"],
            ["服务端返回角色", s.context?.roles.join("、") || "未知"],
          ]}
        />
        <SummaryPanel
          title="会话范围"
          items={[
            ["凭据位置", "仅当前页面内存"],
            ["断开或离开", "取消请求、清空数据并使旧响应失效"],
          ]}
        />
      </div>
      <DataTable
        title="业务角色与职责"
        columns={["角色", "事项范围", "必要条件", "本人授权", "操作"]}
        hideFilters
        rows={roles.map(([role, scope, requirement]) => ({
          key: role,
          state: "role",
          search: role,
          cells: [
            role,
            scope,
            requirement,
            <BusinessStatus
              value={
                s.context
                  ? s.context.roles.includes(role)
                    ? "本人已授权"
                    : "本人未获授权"
                  : "未连接"
              }
            />,
            "由部署方管理",
          ],
        }))}
      />
      <div className="ops-actions">
        {s.context ? (
          <>
            <Button disabled={s.busy} onClick={() => void s.refresh()}>
              重新读取本人角色
            </Button>
            <Button onClick={s.disconnect}>退出并清除会话</Button>
          </>
        ) : (
          <Button onClick={connect}>连接具名会话</Button>
        )}
        <Button onClick={() => go("forbidden")}>查看访问范围</Button>
      </div>
    </>
  );
}
export function SystemPage({
  session: s,
  forbidden,
  connect,
  go,
}: {
  session: OperatorSession;
  forbidden: boolean;
  connect: () => void;
  go: (view: View, id?: string) => void;
}) {
  return (
    <>
      <OpsNotice title={forbidden ? "当前角色无权读取" : "尚未连接业务后台"}>
        {forbidden
          ? "受限用户资料与证据保持隐藏。由管理方按职责授予必要角色后，重新读取本人权限和原事项。"
          : "使用部署方分配的独立具名凭据。连接后逐次校验事项角色、当前版本和目的。"}
      </OpsNotice>
      <div className="ops-columns">
        <SummaryPanel
          title={forbidden ? "访问范围" : "连接前准备"}
          items={[
            ["当前账号", s.context?.id ?? "尚未连接"],
            ["身份来源", "独立具名运营访问"],
            [
              "当前可见",
              forbidden ? "仅本人角色与范围信息" : "公开导航与连接说明",
            ],
            ["私密材料", "不在未连接或受限页展示"],
          ]}
        />
        <SummaryPanel
          title="会话保护"
          items={[
            ["消费者登录", "不会授予后台角色"],
            ["只读访问", "仍须当前角色及目的权限"],
            ["断开或离开", "取消请求并清空会话数据"],
            ["切换账号后", "原响应失效，重新核对事项"],
          ]}
        />
      </div>
      <div className="ops-actions">
        <Button kind="primary" onClick={connect}>
          {forbidden ? "切换具名会话" : "连接业务后台"}
        </Button>
        <Button onClick={() => go("access")}>查看本人角色</Button>
        <Button onClick={() => go("dashboard")}>返回工作台</Button>
      </div>
    </>
  );
}
export function EmergencyPage({
  go,
}: {
  go: (view: View, id?: string) => void;
}) {
  return (
    <>
      <OpsNotice title="安全事件及时分流">
        发现或获知后及时研判、止损、保护必要记录，并联系实际配置的负责人或替补。普通客服首响不代替及时处置。
      </OpsNotice>
      <div className="ops-columns">
        <SummaryPanel
          title="事件响应"
          items={[
            ["报告对象与时限", "按实际事件等级、主体及适用规则确认"],
            ["个人信息事件", "分别核对补救、通知和报告"],
            ["供应商与总结", "核对合同、通报、补报与总结记录"],
          ]}
        />
        <SummaryPanel
          title="原事项留证"
          items={[
            ["必要范围", "按事项编号、具名责任与依据保全"],
            ["复核", "填写未来复核时间，届时重新核对"],
            ["人员与渠道", "通过部署方真实公开配置核实"],
          ]}
        />
      </div>
      <Button onClick={() => go("privacy")}>查看资料与保全事项</Button>
    </>
  );
}
