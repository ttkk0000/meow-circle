import type { Kind, View } from "./models";

export const businessWorkspaces = [
  {
    role: "moderator",
    label: "内容与权利核实",
    home: "内容与权利工作台",
    views: [
      "dashboard",
      "reviews",
      "reviewDetail",
      "rights",
      "rightsDetail",
      "readiness",
    ],
    queues: ["reviews", "rights-cases"],
  },
  {
    role: "privacy",
    label: "资料保护",
    home: "资料保护工作台",
    views: ["dashboard", "privacy"],
    queues: [],
  },
  {
    role: "identity_verifier",
    label: "身份与监护核验",
    home: "身份与监护工作台",
    views: ["dashboard", "guardian", "ageEvidence"],
    queues: ["guardian-invites"],
  },
  {
    role: "authority_verifier",
    label: "发布与处置权核验",
    home: "发布授权工作台",
    views: ["dashboard", "authority", "readiness"],
    queues: [],
  },
  {
    role: "finance",
    label: "实际收支核对",
    home: "实际收支工作台",
    views: ["dashboard", "revenue", "received", "refund"],
    queues: [],
  },
] as const;
export type BusinessRole = (typeof businessWorkspaces)[number]["role"];
export const assignedWorkspaces = (roles: readonly string[]) =>
  businessWorkspaces.filter((w) => roles.includes(w.role));
export const workspaceFor = (role?: string) =>
  businessWorkspaces.find((w) => w.role === role);
export const workspaceQueues = (role?: string): Kind[] => [
  ...(workspaceFor(role)?.queues ?? []),
];
const common: View[] = ["access", "forbidden", "disconnected", "emergency"];
export function workspaceAllows(
  role: string | undefined,
  view: View,
  preparedKind?: Kind,
): boolean {
  if (common.includes(view) || view === "dashboard") return true;
  const workspace = workspaceFor(role);
  if (!workspace) return false;
  if (view === "confirm" || view === "pending")
    return (
      !!preparedKind &&
      workspaceQueues(role)
        .concat(role === "privacy" ? ["data-requests"] : [])
        .includes(preparedKind)
    );
  return (workspace.views as readonly string[]).includes(view);
}

export const opsWorkspaces = [
  {
    role: "ops_viewer",
    label: "运维观察",
    home: "overview",
    sections: [
      "overview",
      "services",
      "workers",
      "releases",
      "migrations",
      "alerts",
      "audit",
      "access",
    ],
  },
  {
    role: "ops_oncall",
    label: "值班处置",
    home: "alerts",
    sections: [
      "alerts",
      "services",
      "workers",
      "overview",
      "migrations",
      "releases",
      "audit",
      "access",
    ],
  },
  {
    role: "ops_release",
    label: "发布准备",
    home: "releases",
    sections: [
      "releases",
      "migrations",
      "services",
      "overview",
      "workers",
      "alerts",
      "audit",
      "access",
    ],
  },
  {
    role: "ops_admin",
    label: "环境与访问审计",
    home: "access",
    sections: [
      "access",
      "audit",
      "overview",
      "services",
      "workers",
      "migrations",
      "releases",
      "alerts",
    ],
  },
] as const;
