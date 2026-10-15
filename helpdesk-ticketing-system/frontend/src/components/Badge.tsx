import type { Priority, Status } from '../types';

const statusColors: Record<Status, string> = {
  OPEN: 'bg-sky-50 text-sky-700 ring-sky-600/15',
  IN_PROGRESS: 'bg-amber-50 text-amber-700 ring-amber-600/15',
  RESOLVED: 'bg-emerald-50 text-emerald-700 ring-emerald-600/15',
  CLOSED: 'bg-slate-100 text-slate-600 ring-slate-500/15',
};

const priorityColors: Record<Priority, string> = {
  LOW: 'bg-slate-100 text-slate-600 ring-slate-500/15',
  MEDIUM: 'bg-blue-50 text-blue-700 ring-blue-600/15',
  HIGH: 'bg-orange-50 text-orange-700 ring-orange-600/15',
  URGENT: 'bg-rose-50 text-rose-700 ring-rose-600/15',
};

export function StatusBadge({ status }: { status: Status }) {
  return <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ring-inset ${statusColors[status]}`}>{status.replace('_', ' ')}</span>;
}

export function PriorityBadge({ priority }: { priority: Priority }) {
  return <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ring-inset ${priorityColors[priority]}`}>{priority}</span>;
}
