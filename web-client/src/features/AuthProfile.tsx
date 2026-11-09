import {useEffect, useRef, useState} from "react";
import {api, explain, HttpError, type RecordData} from "../core/api";
import type {PhotoDraft} from "../core/drafts";
import {preparePhoto} from "../core/photos";
import {replaceQueryData} from "../core/query";
import {integer} from "../core/wire";
import {PhotoPreview} from "../components/Attachments";
import {Button, Card, Field, Modal, Notice, PrivateImage, RowLink} from "../components/ui";
import type {Profile} from "./Account";

interface ExpectedProfile {
  version: bigint; nickname: string; bio: string; theme: string; background: string;
  avatar_media_id?: bigint; originalAvatar: string;
}
function matches(actual: Profile, expected: ExpectedProfile, owner: bigint) {
  return actual.user.id === owner && actual.version > expected.version &&
    actual.user.nickname === expected.nickname && (actual.user.bio ?? "") === expected.bio &&
    actual.theme === expected.theme && actual.background === expected.background &&
    (expected.avatar_media_id !== undefined
      ? integer(actual.user.avatar_url?.match(/\/media\/(\d+)\/content/)?.[1]) === expected.avatar_media_id
      : (actual.user.avatar_url ?? "") === expected.originalAvatar);
}

/** Current-owner memory only. The original route stays mounted until explicit continuation. */
export function AuthProfile({ owner, onDone }: { owner: bigint; onDone: (profile?: Profile) => void }) {
  const [profile, setProfile] = useState<Profile>();
  const [nickname, setNickname] = useState("");
  const [photo, setPhoto] = useState<PhotoDraft>();
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [unknown, setUnknown] = useState(false);
  const [needsReload, setNeedsReload] = useState(false);
  const [error, setError] = useState("");
  const active = useRef(true);
  const pending = useRef(false);
  const reading = useRef(false);
  const loaded = useRef(false);
  const expected = useRef<ExpectedProfile>();
  const picker = useRef<HTMLInputElement>(null);
  const current = () => active.current && api.session.authenticated && api.session.user?.id === owner;
  function completed(actual: Profile) {
    replaceQueryData("profile", actual);
    expected.current = undefined;
    onDone(actual);
  }
  async function readCurrent() {
    if (pending.current || reading.current || !current()) return;
    reading.current = true; setLoading(true); setError("");
    try {
      const actual = await api.request<Profile>("profile", { owner });
      if (!current()) return;
      if (actual.user.id !== owner) throw Error("资料与当前账号不一致，请重新登录后核对");
      const sent = expected.current;
      if (sent) {
        if (matches(actual, sent, owner)) completed(actual);
        else setError("尚未确认本次修改结果，请先核对当前资料；不会自动再次保存。");
      } else {
        setProfile(actual); replaceQueryData("profile", actual); setNeedsReload(false);
        if (!loaded.current) setNickname(actual.user.nickname);
        loaded.current = true;
      }
    } catch (e) { if (current()) setError(explain(e)); }
    finally { reading.current = false; if (current()) setLoading(false); }
  }
  useEffect(() => {
    active.current = true;
    void readCurrent();
    return () => { active.current = false; };
  }, [owner]);
  async function select(file?: File) {
    if (!file || pending.current || reading.current || unknown || !current()) return;
    pending.current = true; setBusy(true); setError("");
    try {
      const selected = await preparePhoto(file, "public");
      if (current()) setPhoto(selected);
    } catch (e) { if (current()) setError(explain(e)); }
    finally { pending.current = false; if (current()) setBusy(false); }
  }
  async function save() {
    if (pending.current || reading.current || !profile || unknown || needsReload || !current()) return;
    const value = nickname.trim();
    if (Array.from(value).length < 1 || Array.from(value).length > 40) { setError("昵称需为1至40字"); return; }
    if (value === profile.user.nickname && !photo) { completed(profile); return; }
    pending.current = true; setBusy(true); setError("");
    let sent = false;
    try {
      let avatar = photo?.mediaId;
      if (photo && !avatar) {
        if (!photo.blob) throw Error("原照片暂不可读，请重新选择");
        const form = new FormData(); form.append("scope", "public"); form.append("file", photo.blob, photo.name);
        const media = await api.request<RecordData>("media", { method: "POST", body: form, owner,
          headers: { "Idempotency-Key": photo.key } });
        if (!current()) return;
        if (media.id <= 0n || media.owner_id !== owner || media.data.scope !== "public") throw Error("头像上传回执尚未确认");
        avatar = media.id; setPhoto({ ...photo, mediaId: avatar });
      }
      if (!current()) return;
      const body = { version: profile.version, nickname: value, bio: profile.user.bio ?? "", theme: profile.theme,
        background: profile.background, ...(avatar !== undefined ? { avatar_media_id: avatar } : {}) };
      expected.current = { ...body, originalAvatar: profile.user.avatar_url ?? "" }; sent = true;
      const actual = await api.request<Profile>("profile", { method: "PATCH", body, owner });
      if (!current()) return;
      if (!matches(actual, expected.current!, owner)) throw Error("资料回执与本次修改不一致，请先核对当前资料");
      completed(actual);
    } catch (e) {
      if (current()) {
        const known = e instanceof HttpError && [400, 401, 403, 404, 409, 422, 429].includes(e.status);
        const uncertain = sent && !known;
        if (!uncertain) expected.current = undefined;
        setUnknown(uncertain); setNeedsReload(e instanceof HttpError && e.status === 409);
        setError(uncertain ? "保存结果待核对，输入已保留；请读取当前资料，不会自动重发。" : explain(e));
      }
    } finally { pending.current = false; if (current()) setBusy(false); }
  }
  function skip() { if (!pending.current) onDone(); }
  const user = profile?.user ?? api.session.user;
  const ready = !!profile && !loading && !busy && !unknown;
  return <Modal title="设置你的资料" task busy={busy} onClose={skip}
    actions={<Button kind="primary" disabled={!ready || needsReload || !nickname.trim()} onClick={() => void save()}>{busy ? "正在保存" : "保存并继续"}</Button>}>
    <div className="auth-task-content auth-profile">
      <Card><div className="profile-identity security-identity">
        {photo ? <PhotoPreview key={photo.key} photo={photo} className="security-avatar" />
          : <PrivateImage id={integer(user?.avatar_url?.match(/\/media\/(\d+)\/content/)?.[1]) || undefined} alt="当前账号头像" className="security-avatar" />}
        <div><strong>{nickname || user?.nickname || user?.username}</strong><p className="caption muted">头像与昵称可随时修改</p></div>
      </div></Card>
      <Field label="昵称" value={nickname} onChange={value => { if (!pending.current && !reading.current && !unknown) setNickname(value); }} disabled={!ready} autoComplete="nickname" />
      <input ref={picker} hidden type="file" accept="image/jpeg,image/png" aria-label="选择头像" tabIndex={-1}
        disabled={!ready} onChange={event => { const file = event.target.files?.[0]; event.target.value = ""; void select(file); }} />
      <RowLink title="选择头像" detail="只读取你主动选择的照片" disabled={!ready} onClick={() => picker.current?.click()} />
      {photo ? <p className="caption muted">所选头像将在你保存时上传。</p> : null}
      <Button kind="quiet" disabled={busy} onClick={skip}>稍后再说</Button>
      {error ? <Notice title={unknown ? "保存结果待核对" : "资料尚未确认"} error>{error}</Notice> : null}
      {unknown ? <p className="caption muted">稍后再说只退出设置，不撤销已经发出的保存；之后可在账号设置核对。</p> : null}
      {!profile || needsReload || unknown ? <Button disabled={loading || busy} onClick={() => void readCurrent()}>{loading ? "正在读取资料" : unknown ? "核对刚才的保存结果" : "重新读取当前资料"}</Button> : null}
      {busy ? <p className="caption" role="status">正在保存，请保留当前任务…</p> : null}
    </div>
  </Modal>;
}
