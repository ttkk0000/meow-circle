import { useQuery, reload } from "../core/query";
import type { RecordData } from "../core/api";
import {
  ContentSubscription,
  type ContentSubscriptions,
} from "../components/ContentSubscription";
import { Card, Empty, QueryState, RowLink, TaskPage } from "../components/ui";

export function ContentPage() {
  const result = useQuery<RecordData<ContentSubscriptions>>(
    "content-subscriptions",
  );
  return (
    <TaskPage title="关注与求助订阅">
      <QueryState
        loading={result.loading}
        error={result.error}
        onRetry={() => void reload("content-subscriptions")}
      >
        <Card title="关注的作者">
          <RowLink title="查看关注内容流" to="/?space=following" />
          {result.data?.data.followed_users.map((id) => (
            <section className="stack" key={String(id)}>
              <RowLink title={`宠友 #${id}`} to={`/users/${id}`} />
              <ContentSubscription id={id} kind="user" />
            </section>
          ))}
          {result.data && !result.data.data.followed_users.length ? (
            <Empty title="还没有关注的作者" />
          ) : null}
        </Card>
        <Card title="关注的求助">
          <RowLink title="只看我关注的求助" to="/cases?watched=true" />
          {result.data?.data.watched_cases.map((id) => (
            <section className="stack" key={String(id)}>
              <RowLink title={`求助 #${id}`} to={`/cases/${id}`} />
              <ContentSubscription id={id} kind="case" />
            </section>
          ))}
          {result.data && !result.data.data.watched_cases.length ? (
            <Empty title="尚未关注求助" />
          ) : null}
        </Card>
      </QueryState>
    </TaskPage>
  );
}
