import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import axios from 'axios';
import api, { errorMessage, fetchStaff } from '../api';
import { useAuth } from '../auth';
import { PriorityBadge, StatusBadge } from '../components/Badge';
import { PRIORITIES, STATUSES, type HistoryItem, type Priority, type Status, type Ticket, type TicketComment, type UserItem } from '../types';

export default function TicketDetail() {
  const { id } = useParams();
  const { user } = useAuth();
  const isStaff = user?.role !== 'CUSTOMER';

  const [ticket, setTicket] = useState<Ticket | null>(null);
  const [comments, setComments] = useState<TicketComment[]>([]);
  const [history, setHistory] = useState<HistoryItem[]>([]);
  const [staff, setStaff] = useState<UserItem[]>([]);
  const [error, setError] = useState('');
  const [staffError, setStaffError] = useState('');
  const [notFound, setNotFound] = useState(false);

  const [commentText, setCommentText] = useState('');
  const [internal, setInternal] = useState(false);

  const [editing, setEditing] = useState(false);
  const [subject, setSubject] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<Priority>('MEDIUM');

  const load = useCallback(async () => {
    setError('');
    try {
      const t = await api.get<Ticket>(`/tickets/${id}`);
      setTicket(t.data);
      setNotFound(false);
    } catch (ticketError) {
      setNotFound(axios.isAxiosError(ticketError) && ticketError.response?.status === 404);
      setError(errorMessage(ticketError));
      return;
    }

    const [commentsResult, historyResult] = await Promise.allSettled([
      api.get<TicketComment[]>(`/tickets/${id}/comments`),
      api.get<HistoryItem[]>(`/tickets/${id}/history`),
    ]);
    const loadErrors: string[] = [];
    if (commentsResult.status === 'fulfilled') setComments(commentsResult.value.data);
    else loadErrors.push(errorMessage(commentsResult.reason));
    if (historyResult.status === 'fulfilled') setHistory(historyResult.value.data);
    else loadErrors.push(errorMessage(historyResult.reason));
    if (loadErrors.length > 0) setError(loadErrors.join(', '));
  }, [id]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (isStaff) {
      fetchStaff().then(setStaff).catch((e) => setStaffError(errorMessage(e)));
    }
  }, [isStaff]);

  // run an action, then reload everything
  const run = async (fn: () => Promise<unknown>) => {
    setError('');
    try {
      await fn();
      await load();
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  const addComment = (e: FormEvent) => {
    e.preventDefault();
    run(async () => {
      await api.post(`/tickets/${id}/comments`, { content: commentText, internal });
      setCommentText('');
      setInternal(false);
    });
  };

  const startEdit = () => {
    if (!ticket) return;
    setSubject(ticket.subject);
    setDescription(ticket.description);
    setPriority(ticket.priority);
    setEditing(true);
  };

  const saveEdit = (e: FormEvent) => {
    e.preventDefault();
    run(async () => {
      await api.put(`/tickets/${id}`, { subject, description, priority });
      setEditing(false);
    });
  };

  if (notFound) {
    return (
      <div className="card">
        <p className="text-red-700">{error || 'Ticket not found'}</p>
        <Link to="/" className="text-indigo-600 hover:underline">Back to tickets</Link>
      </div>
    );
  }
  if (!ticket) {
    return error
      ? <p role="alert" className="text-red-700">{error}</p>
      : <p className="text-slate-500">Loading...</p>;
  }

  const canCustomerClose = !isStaff && ticket.status !== 'CLOSED';

  return (
    <div className="space-y-6">
      <div>
        <Link to="/" className="text-sm font-medium text-slate-500 transition hover:text-indigo-700">← All tickets</Link>
        <div className="mt-3 flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-sm font-medium text-indigo-600">Ticket #{ticket.id}</p>
            <h1 className="mt-1 text-2xl font-semibold tracking-tight text-slate-900">{ticket.subject}</h1>
          </div>
          <div className="flex gap-2">
            <StatusBadge status={ticket.status} />
            <PriorityBadge priority={ticket.priority} />
          </div>
        </div>
      </div>

      <div className="grid items-start gap-6 xl:grid-cols-3">
      <div className="space-y-6 xl:col-span-2">
        <div className="card space-y-5">
          {editing ? (
            <form onSubmit={saveEdit} className="space-y-3">
              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700">Subject</label>
                <input className="input" value={subject} onChange={(e) => setSubject(e.target.value)} maxLength={200} required />
              </div>
              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700">Description</label>
                <textarea className="input min-h-32 resize-y" value={description} onChange={(e) => setDescription(e.target.value)} maxLength={4000} required />
              </div>
              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700">Priority</label>
                <select className="input sm:max-w-56" value={priority} onChange={(e) => setPriority(e.target.value as Priority)}>
                  {PRIORITIES.map((p) => <option key={p} value={p}>{p}</option>)}
                </select>
              </div>
              <div className="flex gap-2">
                <button className="btn">Save</button>
                <button type="button" className="btn-secondary" onClick={() => setEditing(false)}>Cancel</button>
              </div>
            </form>
          ) : (
            <>
              <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-4">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Request details</p>
                  <p className="mt-1 text-sm text-slate-500">Submitted by <span className="font-medium text-slate-700">{ticket.requester.name}</span></p>
                </div>
                {isStaff && <button className="btn-secondary !py-2" onClick={startEdit}>Edit ticket</button>}
              </div>
              <p className="whitespace-pre-wrap text-sm leading-7 text-slate-700">{ticket.description}</p>
              <p className="text-xs text-slate-400">Created {new Date(ticket.createdAt).toLocaleString()} · Updated {new Date(ticket.updatedAt).toLocaleString()}</p>
            </>
          )}
        </div>

        {error && <p className="rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}

        <div className="card space-y-5">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="font-semibold text-slate-900">Conversation</h2>
              <p className="mt-1 text-sm text-slate-500">Updates and replies for this request.</p>
            </div>
            <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600">{comments.length}</span>
          </div>
          {comments.length === 0 && <p className="rounded-xl bg-slate-50 px-4 py-6 text-center text-sm text-slate-500">No replies yet. Start the conversation below.</p>}
          {comments.map((c) => (
            <div key={c.id} className={`rounded-xl border p-4 text-sm ${c.internal ? 'border-amber-200 bg-amber-50/70' : 'border-slate-200 bg-white'}`}>
              <div className="mb-2 flex flex-wrap items-center gap-2 text-xs text-slate-500">
                <span aria-hidden="true" className="flex h-7 w-7 items-center justify-center rounded-full bg-slate-100 font-semibold text-slate-600">{c.author.name.charAt(0).toUpperCase()}</span>
                <span className="font-semibold text-slate-700">{c.author.name}</span>
                <span>{new Date(c.createdAt).toLocaleString()}</span>
                {c.internal && <span className="rounded-full bg-amber-100 px-2 py-0.5 font-semibold text-amber-800">Internal note</span>}
              </div>
              <p className="whitespace-pre-wrap leading-6 text-slate-700">{c.content}</p>
            </div>
          ))}
          <form onSubmit={addComment} className="space-y-2">
            <label htmlFor="comment-text" className="block text-sm font-medium text-slate-700">Add a reply</label>
            <textarea id="comment-text" className="input min-h-24 resize-y" placeholder="Write a reply..." value={commentText} onChange={(e) => setCommentText(e.target.value)} required maxLength={4000} />
            <div className="flex items-center justify-between">
              {isStaff ? (
                <label className="flex items-center gap-2 text-sm">
                  <input type="checkbox" checked={internal} onChange={(e) => setInternal(e.target.checked)} />
                  Internal note (hidden from customer)
                </label>
              ) : <span />}
              <button className="btn">Add comment</button>
            </div>
          </form>
        </div>
      </div>

      <div className="space-y-6">
        <div className="card space-y-5">
          <div>
            <h2 className="font-semibold text-slate-900">Ticket management</h2>
            <p className="mt-1 text-sm text-slate-500">Status and ownership</p>
          </div>
          {isStaff ? (
            <>
              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700">Status</label>
                <select className="input" value={ticket.status} onChange={(e) => run(() => api.patch(`/tickets/${id}/status`, { status: e.target.value as Status }))}>
                  {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
                </select>
              </div>
              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700">Assigned agent</label>
                {staffError && <p role="alert" className="mb-2 text-sm text-red-700">Could not load staff: {staffError}</p>}
                <select
                  className="input"
                  value={ticket.assignedAgent?.id ?? ''}
                  onChange={(e) => run(() => api.patch(`/tickets/${id}/assign`, { agentId: e.target.value ? Number(e.target.value) : null }))}
                >
                  <option value="">Unassigned</option>
                  {staff.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                </select>
              </div>
              {ticket.status !== 'CLOSED' && (
                <button className="btn-secondary w-full" onClick={() => run(() => api.post(`/tickets/${id}/close`))}>Close ticket</button>
              )}
            </>
          ) : (
            <>
              <p className="text-sm text-slate-600">Assigned to: {ticket.assignedAgent?.name ?? 'Not assigned yet'}</p>
              {canCustomerClose && (
                <button className="btn-secondary w-full" onClick={() => run(() => api.post(`/tickets/${id}/close`))}>Close my ticket</button>
              )}
            </>
          )}
        </div>

        <div className="card space-y-4">
          <div>
            <h2 className="font-semibold text-slate-900">Activity</h2>
            <p className="mt-1 text-sm text-slate-500">Recent changes to this ticket.</p>
          </div>
          {history.length === 0 && <p className="text-sm text-slate-500">No activity recorded yet.</p>}
          <ol className="space-y-4 border-l border-slate-200 pl-4">
            {history.map((h) => (
              <li key={h.id} className="relative text-sm">
                <span aria-hidden="true" className="absolute -left-[1.32rem] top-1.5 h-2 w-2 rounded-full bg-indigo-400 ring-4 ring-white" />
                <div className="font-medium text-slate-800">
                  {h.type === 'STATUS_CHANGE' ? 'Status' : 'Assignment'}: {h.oldValue} → {h.newValue}
                </div>
                <div className="text-xs text-slate-500">{h.changedBy.name} · {new Date(h.createdAt).toLocaleString()}</div>
              </li>
            ))}
          </ol>
        </div>
      </div>
      </div>
    </div>
  );
}
