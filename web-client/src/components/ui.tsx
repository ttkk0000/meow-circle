import { useEffect, useId, useRef, useState, type ReactNode } from "react";
import { api } from "../core/api";
import { badge, integer } from "../core/wire";
import { navigate } from "../core/router";
import { useSession } from "../core/session";

const scrollMemory = new Map<string, number>();
let scrollEpoch = -1;

export function Icon({ name, label }: { name: string; label?: string }) {
  return (
    <img
      className="icon"
      src={`/app/assets/figma/${name}.svg`}
      width="24"
      height="24"
      alt={label ?? ""}
      aria-hidden={!label}
    />
  );
}
export function Button({
  children,
  onClick,
  disabled,
  kind = "secondary",
  type = "button",
  pressed,
}: {
  children: ReactNode;
  onClick?: () => void;
  disabled?: boolean;
  kind?: "primary" | "secondary" | "quiet" | "danger";
  type?: "button" | "submit";
  pressed?: boolean;
}) {
  return (
    <button
      type={type}
      className={`button ${kind}`}
      onClick={onClick}
      disabled={disabled}
      aria-pressed={pressed}
    >
      {children}
    </button>
  );
}
export function Card({
  children,
  title,
}: {
  children: ReactNode;
  title?: ReactNode;
}) {
  return (
    <section className="card">
      {title ? <h2>{title}</h2> : null}
      {children}
    </section>
  );
}
export function Notice({
  title,
  children,
  error = false,
}: {
  title: string;
  children?: ReactNode;
  error?: boolean;
}) {
  return (
    <aside
      className={`notice ${error ? "error" : ""}`}
      role={error ? "alert" : "status"}
    >
      <strong>{title}</strong>
      {children ? <div>{children}</div> : null}
    </aside>
  );
}
export function Empty({
  title = "暂时没有可显示的内容",
  children,
}: {
  title?: string;
  children?: ReactNode;
}) {
  return (
    <section className="empty">
      <Icon name="paw-print" />
      <h2>{title}</h2>
      {children}
    </section>
  );
}
export function Badge({ count }: { count: unknown }) {
  const text = badge(count);
  return text ? (
    <span className="badge" aria-label={`${integer(count)}条未读`}>
      {text}
    </span>
  ) : null;
}
export function RowLink({
  title,
  detail,
  to,
  onClick,
}: {
  title: string;
  detail?: string;
  to?: string;
  onClick?: () => void;
}) {
  return (
    <button
      type="button"
      className="list-row"
      onClick={onClick ?? (() => to && navigate(to))}
    >
      <span>
        <span className="row-title">{title}</span>
        {detail ? <span className="muted caption">{detail}</span> : null}
      </span>
      <Icon name="chevron-right" />
    </button>
  );
}
export function TaskPage({
  title,
  children,
  actions,
  subtitle,
}: {
  title: string;
  children: ReactNode;
  actions?: ReactNode;
  subtitle?: string;
}) {
  const session = useSession();
  const scroller = useRef<HTMLDivElement>(null);
  const scrollKey = `${session.user?.id ?? 0n}|${location.pathname}${location.search}`;
  useEffect(() => {
    if (scrollEpoch !== api.epoch) {
      scrollMemory.clear();
      scrollEpoch = api.epoch;
    }
    const node = scroller.current;
    if (!node) return;
    let restoring = scrollMemory.get(scrollKey) ?? 0;
    const restore = () => {
      if (restoring && node.scrollHeight - node.clientHeight >= restoring) {
        node.scrollTop = restoring;
        restoring = 0;
      }
    };
    const cancel = () => {
      restoring = 0;
    };
    const remember = () => {
      if (!restoring) {
        scrollMemory.set(scrollKey, node.scrollTop);
        if (scrollMemory.size > 60)
          scrollMemory.delete(scrollMemory.keys().next().value!);
      }
    };
    const observer = new ResizeObserver(restore);
    if (node.firstElementChild) observer.observe(node.firstElementChild);
    restore();
    node.addEventListener("scroll", remember, { passive: true });
    node.addEventListener("wheel", cancel, { passive: true });
    node.addEventListener("touchstart", cancel, { passive: true });
    node.addEventListener("keydown", cancel);
    return () => {
      remember();
      observer.disconnect();
      node.removeEventListener("scroll", remember);
      node.removeEventListener("wheel", cancel);
      node.removeEventListener("touchstart", cancel);
      node.removeEventListener("keydown", cancel);
    };
  }, [scrollKey]);
  return (
    <section className="task-page">
      <div className="task-scroll" ref={scroller}>
        <div className="task-content">
          {title ? <h1>{title}</h1> : null}
          {subtitle ? <p className="muted">{subtitle}</p> : null}
          {children}
        </div>
      </div>
      {actions ? (
        <footer className="task-actions">
          <div>{actions}</div>
        </footer>
      ) : null}
    </section>
  );
}
export function Field({
  label,
  value,
  onChange,
  type = "text",
  multiline = false,
  disabled = false,
  hint,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
  multiline?: boolean;
  disabled?: boolean;
  hint?: string;
}) {
  const id = useId();
  return (
    <label className="field" htmlFor={id}>
      <span>{label}</span>
      {multiline ? (
        <textarea
          id={id}
          aria-describedby={hint ? `${id}-hint` : undefined}
          rows={4}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          disabled={disabled}
        />
      ) : (
        <input
          id={id}
          aria-describedby={hint ? `${id}-hint` : undefined}
          type={type}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          disabled={disabled}
          autoComplete={type === "password" ? "current-password" : "off"}
        />
      )}{" "}
      {hint ? <small id={`${id}-hint`}>{hint}</small> : null}
    </label>
  );
}
export function Choices({
  label,
  value,
  choices,
  onChange,
  disabled,
}: {
  label: string;
  value: string;
  choices: Array<[string, string]>;
  onChange: (value: string) => void;
  disabled?: boolean;
}) {
  const group = useId();
  return (
    <fieldset disabled={disabled} className="choices">
      <legend>{label}</legend>
      <div>
        {choices.map(([key, text]) => (
          <label key={key} className={value === key ? "selected" : ""}>
            <input
              type="radio"
              name={group}
              value={key}
              checked={value === key}
              onChange={() => onChange(key)}
            />
            {text}
          </label>
        ))}
      </div>
    </fieldset>
  );
}
export function Check({
  children,
  checked,
  onChange,
  disabled,
}: {
  children: ReactNode;
  checked: boolean;
  onChange: (value: boolean) => void;
  disabled?: boolean;
}) {
  return (
    <label className="check">
      <input
        type="checkbox"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
        disabled={disabled}
      />
      <span>{children}</span>
    </label>
  );
}
export function Modal({
  title,
  children,
  actions,
  onClose,
  busy = false,
}: {
  title: string;
  children: ReactNode;
  actions: ReactNode;
  onClose: () => void;
  busy?: boolean;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  const titleId = useId();
  useEffect(() => {
    const node = ref.current;
    node?.showModal();
    return () => {
      node?.close();
    };
  }, []);
  return (
    <dialog
      ref={ref}
      aria-labelledby={titleId}
      onCancel={(event) => {
        event.preventDefault();
        if (!busy) onClose();
      }}
    >
      <header>
        <h2 id={titleId}>{title}</h2>
        <Button kind="quiet" onClick={onClose} disabled={busy}>
          关闭
        </Button>
      </header>
      <div className="dialog-content">{children}</div>
      <footer className="dialog-actions">{actions}</footer>
    </dialog>
  );
}
export function PrivateImage({
  id,
  alt,
  className = "media",
}: {
  id: bigint | undefined;
  alt: string;
  className?: string;
}) {
  const [source, setSource] = useState("");
  const [error, setError] = useState(false);
  useEffect(() => {
    setSource("");
    setError(false);
    if (!id) return;
    const abort = new AbortController();
    let url = "";
    void api
      .request<Blob>(`media/${id}/content`, {
        blob: true,
        signal: abort.signal,
      })
      .then((blob) => {
        if (abort.signal.aborted) return;
        url = URL.createObjectURL(blob);
        setSource(url);
      })
      .catch(() => {
        if (!abort.signal.aborted) setError(true);
      });
    return () => {
      abort.abort();
      if (url) URL.revokeObjectURL(url);
    };
  }, [id, api.epoch]);
  return source ? (
    <img className={className} src={source} alt={alt} loading="lazy" />
  ) : (
    <div
      className={`${className} media-empty`}
      role="img"
      aria-label={error ? "照片暂不可读" : alt}
    >
      <Icon name="paw-print" />
      {error ? <span>照片暂不可读</span> : null}
    </div>
  );
}
export function QueryState({
  loading,
  error,
  children,
  onRetry,
  keepContent = false,
}: {
  loading: boolean;
  error?: string;
  children?: ReactNode;
  onRetry?: () => void;
  keepContent?: boolean;
}) {
  return (
    <>
      {loading ? (
        <p className="loading" role="status">
          正在读取…
        </p>
      ) : null}
      {error ? (
        <Notice title={error} error>
          {onRetry ? <Button onClick={onRetry}>重试读取</Button> : null}
        </Notice>
      ) : null}
      {!error || keepContent ? children : null}
    </>
  );
}
