import type { ReactNode } from "react";

export type IconName =
  | "activity"
  | "archive"
  | "arrowRight"
  | "bell"
  | "check"
  | "chevronLeft"
  | "chevronRight"
  | "clipboard"
  | "clock"
  | "dashboard"
  | "document"
  | "edit"
  | "folder"
  | "layers"
  | "overview"
  | "plus"
  | "resolution"
  | "users"
  | "warning";

interface IconProps {
  name: IconName;
  size?: number;
}

const paths: Record<IconName, ReactNode> = {
  activity: <><path d="M4 19v-6M10 19V5M16 19v-9M22 19V8" /><path d="M2 19h22" /></>,
  archive: <><path d="M4 7h16" /><path d="M5 7v12h14V7" /><path d="M3 3h18v4H3z" /><path d="M9 11h6" /></>,
  arrowRight: <><path d="M5 12h14" /><path d="m14 7 5 5-5 5" /></>,
  bell: <><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" /><path d="M10 21h4" /></>,
  check: <><circle cx="12" cy="12" r="9" /><path d="m8 12 2.5 2.5L16 9" /></>,
  chevronLeft: <path d="m15 18-6-6 6-6" />,
  chevronRight: <path d="m9 18 6-6-6-6" />,
  clipboard: <><rect x="5" y="4" width="14" height="17" rx="2" /><path d="M9 4.5V3h6v1.5" /><path d="M9 9h6M9 13h6M9 17h4" /></>,
  clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
  dashboard: <><path d="M4 13h7V4H4zM15 20h5V4h-5zM4 20h7v-3H4z" /></>,
  document: <><path d="M6 2h8l4 4v16H6z" /><path d="M14 2v5h5M9 12h6M9 16h6" /></>,
  edit: <><path d="M4 20h4l11-11-4-4L4 16z" /><path d="m13.5 6.5 4 4M4 20h16" /></>,
  folder: <><path d="M3 6h7l2 2h9v11H3z" /></>,
  layers: <><path d="m12 3 9 5-9 5-9-5z" /><path d="m3 12 9 5 9-5M3 16l9 5 9-5" /></>,
  overview: <><rect x="3" y="3" width="7" height="7" rx="1.5" /><rect x="14" y="3" width="7" height="7" rx="1.5" /><rect x="3" y="14" width="7" height="7" rx="1.5" /><rect x="14" y="14" width="7" height="7" rx="1.5" /></>,
  plus: <><path d="M12 5v14" /><path d="M5 12h14" /></>,
  resolution: <><path d="M4 5h11a4 4 0 0 1 4 4v10" /><path d="m15 15 4 4 4-4" /><path d="M9 9 6.5 11.5 5 10" /></>,
  users: <><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" /></>,
  warning: <><path d="M12 3 2.5 20h19z" /><path d="M12 9v5M12 17.5h.01" /></>,
};

export function Icon({ name, size = 18 }: IconProps) {
  return (
    <svg
      className="icon"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {paths[name]}
    </svg>
  );
}
