export type Role = 'ORG_ADMIN' | 'AGENT' | 'CUSTOMER';
export type Status = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export const STATUSES: Status[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
export const PRIORITIES: Priority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

export interface UserSummary { id: number; name: string }

export interface AuthUser {
  userId: number;
  name: string;
  email: string;
  role: Role;
  organizationId: number;
  organizationName: string;
}

export interface UserItem { id: number; name: string; email: string; role: Role; organizationId: number }

export interface Ticket {
  id: number;
  subject: string;
  description: string;
  priority: Priority;
  status: Status;
  requester: UserSummary;
  assignedAgent: UserSummary | null;
  organizationId: number;
  createdAt: string;
  updatedAt: string;
}

export interface TicketComment {
  id: number;
  ticketId: number;
  author: UserSummary;
  content: string;
  internal: boolean;
  createdAt: string;
}

export interface HistoryItem {
  id: number;
  type: 'STATUS_CHANGE' | 'ASSIGNMENT_CHANGE';
  oldValue: string;
  newValue: string;
  changedBy: UserSummary;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface TicketSummary {
  total: number;
  open: number;
  inProgress: number;
  resolved: number;
  closed: number;
}
