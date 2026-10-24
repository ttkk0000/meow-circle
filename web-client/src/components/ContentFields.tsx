import { useSession } from "../core/session";
import { useState } from "react";
import { useQuery } from "../core/query";
import { usePages } from "../core/pages";
import { integer } from "../core/wire";
import type { RecordData, User } from "../core/api";
import {
  Button,
  Card,
  Check,
  Choices,
  Field,
  PrivateImage,
  QueryState,
} from "./ui";

function PersonChoice({
  id,
  checked,
  onChange,
  disabled,
  filter = "",
}: {
  id: bigint;
  checked: boolean;
  onChange: (checked: boolean) => void;
  disabled: boolean;
  filter?: string;
}) {
  const person = useQuery<User>(`users/${id}`);
  const name = person.data?.nickname || person.data?.username || `宠友 #${id}`;
  if (filter && !name.toLowerCase().includes(filter.toLowerCase()) && !checked)
    return null;
  return (
    <Check
      checked={checked}
      onChange={onChange}
      disabled={disabled || !person.data}
    >
      <span className="author">
        <PrivateImage
          id={
            integer(
              person.data?.avatar_url?.match(/\/media\/(\d+)\/content/)?.[1],
            ) || undefined
          }
          alt="宠友头像"
          className="mini-avatar"
        />
        {name}
        {person.error ? " · 资料暂不可读" : person.loading ? " · 正在读取" : ""}
      </span>
    </Check>
  );
}

export function PeopleSelector({
  value,
  onChange,
  disabled,
  ids,
}: {
  value: bigint;
  onChange: (value: bigint) => void;
  disabled: boolean;
  ids?: bigint[];
}) {
  const owner = useSession().user?.id;
  const friends = usePages(
    "friendships",
    { state: "accepted" },
    ids === undefined && !!owner,
  );
  const [filter, setFilter] = useState("");
  const candidates = (
    ids ??
    friends.rows.map((friend) =>
      integer(friend.data.requester_id) === owner
        ? integer(friend.data.recipient_id)
        : integer(friend.data.requester_id),
    )
  ).filter((id) => id > 0n && id !== owner);
  return (
    <Card title={ids ? "选择当前群成员" : "选择当前好友"}>
      <Field
        label="筛选已加载的昵称"
        value={filter}
        onChange={setFilter}
        disabled={disabled}
      />
      <QueryState
        loading={ids ? false : friends.loading}
        error={ids ? undefined : friends.error}
        onRetry={friends.retry}
        keepContent={!!friends.rows.length && !friends.denied}
      >
        {candidates.map((id) => (
          <PersonChoice
            key={String(id)}
            id={id}
            checked={value === id}
            onChange={(checked) => onChange(checked ? id : 0n)}
            disabled={disabled}
            filter={filter}
          />
        ))}
        {value > 0n && !candidates.includes(value) ? (
          <p className="disclosure">
            原选择尚未出现在当前可用范围，请继续加载或重新选择；不会自动更换对象。
          </p>
        ) : null}
        {!ids && friends.next ? (
          <Button disabled={disabled || friends.loading} onClick={friends.more}>
            继续读取好友
          </Button>
        ) : null}
      </QueryState>
    </Card>
  );
}

export function PetSelector({
  value,
  onChange,
  disabled,
}: {
  value: bigint;
  onChange: (pet: RecordData | undefined) => void;
  disabled: boolean;
}) {
  const owner = useSession().user?.id;
  const pets = usePages("pets", { owner_id: owner }, !!owner);
  return (
    <Card title="关联我的宠物">
      <QueryState
        loading={pets.loading}
        error={pets.error}
        onRetry={pets.retry}
        keepContent={!!pets.rows.length && !pets.denied}
      >
        {pets.rows.map((pet) => (
          <button
            type="button"
            className={`pet-row ${pet.id === value ? "selected-record" : ""}`}
            aria-pressed={pet.id === value}
            disabled={disabled}
            key={String(pet.id)}
            onClick={() => onChange(pet)}
          >
            <PrivateImage id={pet.media_ids[0]} alt={String(pet.data.name)} />
            <span>
              <strong>{String(pet.data.name)}</strong>
              <small>档案版本 {String(pet.version)}</small>
            </span>
          </button>
        ))}
        {pets.next ? (
          <Button disabled={disabled || pets.loading} onClick={pets.more}>
            继续读取我的宠物
          </Button>
        ) : null}
        <Button
          kind="quiet"
          disabled={disabled}
          onClick={() => onChange(undefined)}
        >
          暂不关联宠物
        </Button>
      </QueryState>
    </Card>
  );
}
export function AudienceFields({
  value,
  onChange,
  disabled,
  space,
}: {
  value: { mode?: string; user_ids?: bigint[] };
  onChange: (value: { mode: string; user_ids: bigint[] }) => void;
  disabled: boolean;
  space?: string;
}) {
  const owner = useSession().user?.id;
  const friends = usePages(
    "friendships",
    { state: "accepted" },
    !!owner && ["selected", "excluded"].includes(value.mode ?? ""),
  );
  const choices: Array<[string, string]> =
    space === "community"
      ? [["public", "公开到社区"]]
      : [
          ["private", "仅自己"],
          ["friends", "好友"],
          ["selected", "部分好友"],
          ["excluded", "排除部分好友"],
          ...(space === "circle"
            ? []
            : [["public", "公开"] as [string, string]]),
        ];
  const selected = value.user_ids ?? [];
  return (
    <Card title="谁可以看到">
      <Choices
        label="可见范围"
        value={value.mode ?? ""}
        choices={choices}
        disabled={disabled}
        onChange={(mode) =>
          onChange({
            mode,
            user_ids:
              mode === "selected" || mode === "excluded" ? selected : [],
          })
        }
      />
      {["selected", "excluded"].includes(value.mode ?? "") ? (
        <>
          <p className="caption muted">
            仅使用当前有效好友；关系或权限变化后，服务会重新校验。
          </p>
          <QueryState
            loading={friends.loading}
            error={friends.error}
            onRetry={friends.retry}
            keepContent={!!friends.rows.length && !friends.denied}
          >
            {friends.rows.map((friend) => {
              const id =
                integer(friend.data.requester_id) === owner
                  ? integer(friend.data.recipient_id)
                  : integer(friend.data.requester_id);
              return (
                <PersonChoice
                  key={String(id)}
                  id={id}
                  disabled={disabled}
                  checked={selected.includes(id)}
                  onChange={(checked) =>
                    onChange({
                      mode: value.mode!,
                      user_ids: checked
                        ? [...selected, id]
                        : selected.filter((v) => v !== id),
                    })
                  }
                />
              );
            })}
            {friends.next ? (
              <Button
                onClick={friends.more}
                disabled={disabled || friends.loading}
              >
                继续读取好友
              </Button>
            ) : null}
          </QueryState>
        </>
      ) : null}
    </Card>
  );
}
export function EntryFields({
  value,
  onChange,
  disabled,
}: {
  value: Record<string, unknown>;
  onChange: (value: Record<string, unknown>) => void;
  disabled: boolean;
}) {
  return (
    <section className="stack">
      <Field
        label="写一句标题"
        value={String(value.title ?? "")}
        onChange={(title) => onChange({ ...value, title })}
        disabled={disabled}
      />
      <Field
        label="记录今天的小可爱"
        value={String(value.text ?? "")}
        onChange={(text) => onChange({ ...value, text })}
        multiline
        disabled={disabled}
      />
      <Field
        label="城市（选填，不含精确定位）"
        value={String(value.city ?? "")}
        onChange={(city) => onChange({ ...value, city })}
        disabled={disabled}
      />
      <Field
        label="城市（选填，不含精确位置）"
        value={String(value.city ?? "")}
        onChange={(city) => onChange({ ...value, city })}
        disabled={disabled}
      />
      <Choices
        label="发布空间"
        value={String(value.space ?? "community")}
        choices={[
          ["community", "社区"],
          ["circle", "宠友圈"],
          ["diary", "宠物日记"],
        ]}
        onChange={(space) => onChange({ ...value, space })}
        disabled={disabled}
      />
      <AudienceFields
        space={String(value.space)}
        value={(value.audience ?? {}) as { mode: string; user_ids: bigint[] }}
        disabled={disabled}
        onChange={(audience) => onChange({ ...value, audience })}
      />
      <Field
        label="分类"
        value={String(value.category ?? "daily_share")}
        onChange={(category) => onChange({ ...value, category })}
        disabled={disabled}
      />
      <Field
        label="标签（逗号分隔）"
        value={Array.isArray(value.tags) ? value.tags.join(",") : ""}
        onChange={(text) =>
          onChange({
            ...value,
            tags: text
              .split(/[,，]/)
              .map((t) => t.trim())
              .filter(Boolean),
          })
        }
        disabled={disabled}
      />
      <PetSelector
        value={integer(value.pet_id)}
        disabled={disabled}
        onChange={(pet) => onChange({ ...value, pet_id: pet?.id ?? 0n })}
      />
    </section>
  );
}
