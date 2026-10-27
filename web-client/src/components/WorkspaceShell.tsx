import { useEffect, useRef, useState, type ReactNode } from "react";
import { Button, Icon } from "./ui";
import "../operations/operations.css";

export interface WorkspaceItem {
  key: string;
  label: string;
  icon: string;
  href: string;
  onSelect?: () => void;
  collection?: "figma" | "ops";
}
/** 业务管理和技术运维共用纯布局；身份、凭据和业务状态由各自入口持有。 */
export function WorkspaceShell({
  title,
  subtitle,
  breadcrumb,
  heading,
  caption,
  scope,
  identity,
  roles,
  navigation,
  active,
  actions,
  children,
  footer,
  focusKey,
  className = "",
}: {
  title: string;
  subtitle: string;
  breadcrumb: string;
  heading: string;
  caption?: string;
  scope: ReactNode;
  identity: string;
  roles: string;
  navigation: WorkspaceItem[];
  active: string;
  actions: ReactNode;
  children: ReactNode;
  footer: ReactNode;
  focusKey: string;
  className?: string;
}) {
  const [menu, setMenu] = useState(false),
    main = useRef<HTMLElement>(null);
  useEffect(() => {
    setMenu(false);
    main.current?.focus();
  }, [focusKey]);
  return (
    <div className={`ops-workspace ${className}`}>
      <a className="ops-skip" href="#workspace-main">
        跳到页面内容
      </a>
      <aside
        className={`ops-sidebar ${menu ? "open" : ""}`}
        aria-label={`${heading}导航`}
      >
        <a
          className="ops-brand"
          href={navigation[0]?.href}
          onClick={(event) => {
            if (
              event.button === 0 &&
              !event.metaKey &&
              !event.ctrlKey &&
              !event.shiftKey &&
              navigation[0]?.onSelect
            ) {
              event.preventDefault();
              navigation[0].onSelect();
            }
          }}
        >
          <strong>M&amp;D</strong>
          <span>meow &amp; doggie</span>
        </a>
        <p className="ops-nav-caption">{caption ?? heading}</p>
        <nav>
          {navigation.map((item) => (
            <a
              key={item.key}
              className={`ops-nav-item ${active === item.key ? "active" : ""}`}
              aria-current={active === item.key ? "page" : undefined}
              href={item.href}
              onClick={(event) => {
                if (
                  event.button === 0 &&
                  !event.metaKey &&
                  !event.ctrlKey &&
                  !event.shiftKey &&
                  item.onSelect
                ) {
                  event.preventDefault();
                  item.onSelect();
                }
              }}
            >
              <Icon name={item.icon} collection={item.collection ?? "ops"} />
              <span>{item.label}</span>
            </a>
          ))}
        </nav>
        <section className="ops-sidebar-env">
          <p>当前工作空间</p>
          {scope}
        </section>
        <div className="ops-identity">
          <p>{identity}</p>
          <small>{roles}</small>
        </div>
      </aside>
      <header className="ops-topbar">
        <Button kind="quiet" onClick={() => setMenu(!menu)} pressed={menu}>
          <span className="ops-menu-label">导航</span>
        </Button>
        <strong>{heading}</strong>
        <div className="ops-topbar-actions">{actions}</div>
      </header>
      <main id="workspace-main" ref={main} className="ops-main" tabIndex={-1}>
        <header className="ops-page-header">
          <p>{breadcrumb}</p>
          <h1>{title}</h1>
          <p>{subtitle}</p>
        </header>
        <div className="ops-page-body">{children}</div>
        <footer className="ops-footer">{footer}</footer>
      </main>
    </div>
  );
}
