export function PublishButton({
  size = "default",
  onClick,
  disabled = false,
  current = false,
}: {
  size?: "default" | "compact" | "icon";
  onClick: () => void;
  disabled?: boolean;
  current?: boolean;
}) {
  return (
    <button
      type="button"
      className={`publish-button publish-button--${size}`}
      aria-label={size === "icon" ? "发布" : undefined}
      aria-current={current ? "page" : undefined}
      disabled={disabled}
      onClick={onClick}
    >
      <span className="publish-button__face">
        <span className="publish-button__icon" aria-hidden="true" />
        {size !== "icon" ? <span>发布</span> : null}
      </span>
    </button>
  );
}
