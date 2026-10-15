import { useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage, fetchStaff } from '../api';
import { useAuth } from '../auth';
import { PriorityBadge, StatusBadge } from '../components/Badge';
import { PRIORITIES, STATUSES, type PageResponse, type Ticket, type TicketSummary, type UserItem } from '../types';

const PAGE_SIZE = 10;

export default function Tickets() {
  const { user } = useAuth();
  const [data, setData] = useState<PageResponse<Ticket> | null>(null);
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState('');
  const [priority, setPriority] = useState('');
  const [agentId, setAgentId] = useState('');
  const [search, setSearch] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [staff, setStaff] = useState<UserItem[]>([]);
  const [summary, setSummary] = useState<TicketSummary | null>(null);
  const [summaryError, setSummaryError] = useState('');
  const [staffError, setStaffError] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const isStaff = user?.role !== 'CUSTOMER';

  useEffect(() => {
    if (isStaff) {
      fetchStaff().then(setStaff).catch((e) => setStaffError(errorMessage(e)));
    }
  }, [isStaff]);

  useEffect(() => {
    setSummaryError('');
    api
      .get<TicketSummary>('/tickets/summary')
      .then((res) => setSummary(res.data))
      .catch((e) => setSummaryError(errorMessage(e)));
  }, []);

  useEffect(() => {
    setLoading(true);
    setError('');
    api
      .get<PageResponse<Ticket>>('/tickets', {
        params: {
          status: status || undefined,
          priority: priority || undefined,
          assignedAgentId: agentId || undefined,
          search: search || undefined,
          page,
          size: PAGE_SIZE,
        },
      })
      .then((res) => setData(res.data))
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setLoading(false));
  }, [status, priority, agentId, search, page]);

  const onSearch = (e: FormEvent) => {
    e.preventDefault();
    setPage(0);
    setSearch(searchInput);
  };

  const resetPage = <T,>(setter: (v: T) => void) => (v: T) => {
    setPage(0);
    setter(v);
  };

  const clearFilters = () => {
    setStatus('');
    setPriority('');
    setAgentId('');
    setSearch('');
    setSearchInput('');
    setPage(0);
  };

  const hasFilters = Boolean(status || priority || agentId || search);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm font-medium text-indigo-600">Support overview</p>
          <h1 className="mt-1 text-2xl font-semibold tracking-tight text-slate-900">{isStaff ? 'All tickets' : 'My tickets'}</h1>
          <p className="mt-1 text-sm text-slate-500">{isStaff ? 'Track and manage requests across your organization.' : 'Follow the progress of your support requests.'}</p>
        </div>
        <Link to="/tickets/new" className="btn">Create ticket</Link>
      </div>

      {summaryError && <p className="rounded bg-red-50 px-3 py-2 text-sm text-red-700">{summaryError}</p>}
      {staffError && <p role="alert" className="rounded bg-red-50 px-3 py-2 text-sm text-red-700">Could not load staff filters: {staffError}</p>}
      <section aria-label="Ticket summary" className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
        {[
          { label: 'Total tickets', value: summary?.total, accent: 'bg-indigo-500' },
          { label: 'Open', value: summary?.open, accent: 'bg-blue-500' },
          { label: 'In progress', value: summary?.inProgress, accent: 'bg-amber-500' },
          { label: 'Resolved', value: summary?.resolved, accent: 'bg-violet-500' },
          { label: 'Closed', value: summary?.closed, accent: 'bg-emerald-500' },
        ].map((item) => (
          <button type="button" key={item.label} onClick={() => resetPage(setStatus)(item.label === 'Total tickets' ? '' : item.label === 'In progress' ? 'IN_PROGRESS' : item.label.toUpperCase())} className={`card stat-card flex w-full items-center justify-between text-left ${item.label === 'Open' ? 'stat-open' : item.label === 'In progress' ? 'stat-progress' : item.label === 'Closed' ? 'stat-closed' : 'stat-total'}`} aria-label={`Filter tickets: ${item.label}`}>
            <div>
              <h2 className="text-sm font-medium text-slate-500">{item.label}</h2>
              <p className="mt-2 text-3xl font-semibold tracking-tight text-slate-900" aria-live="polite">
                {item.value ?? '—'}
              </p>
            </div>
            <span aria-hidden="true" className="stat-icon">{item.label.charAt(0)}</span>
          </button>
        ))}
      </section>

      <section aria-label="Find tickets" className="ticket-filters card space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div>
            <h2 className="font-semibold text-slate-900">Find a ticket</h2>
            <p className="mt-1 text-xs text-slate-500">Search by subject or narrow the list with filters.</p>
          </div>
          {hasFilters && <button type="button" className="text-sm font-semibold text-indigo-600 hover:text-indigo-800" onClick={clearFilters}>Clear filters</button>}
        </div>
        <div className="flex flex-wrap items-end gap-3">
        <form onSubmit={onSearch} className="flex min-w-56 flex-1 gap-2">
          <input className="input min-w-0 flex-1" aria-label="Search tickets" placeholder="Search subject..." value={searchInput} onChange={(e) => setSearchInput(e.target.value)} />
          <button className="btn-secondary">Search</button>
        </form>
        <label className="text-xs font-semibold text-slate-500">
          Status
          <select aria-label="Filter by status" className="input mt-1 w-40" value={status} onChange={(e) => resetPage(setStatus)(e.target.value)}>
            <option value="">All statuses</option>
            {STATUSES.map((s) => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
          </select>
        </label>
        <label className="text-xs font-semibold text-slate-500">
          Priority
          <select aria-label="Filter by priority" className="input mt-1 w-40" value={priority} onChange={(e) => resetPage(setPriority)(e.target.value)}>
            <option value="">All priorities</option>
            {PRIORITIES.map((p) => <option key={p} value={p}>{p}</option>)}
          </select>
        </label>
        {isStaff && (
          <label className="text-xs font-semibold text-slate-500">
            Assigned to
            <select aria-label="Filter by assigned agent" className="input mt-1 w-48" value={agentId} onChange={(e) => resetPage(setAgentId)(e.target.value)}>
              <option value="">All agents</option>
              {staff.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </label>
        )}
        </div>
        {hasFilters && <p className="text-xs text-slate-500">Filters are active. Clear filters to see every ticket.</p>}
      </section>

      {error && <p className="rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}

      <div className="hidden overflow-x-auto rounded-2xl border border-slate-200 bg-white shadow-sm md:block">
        <table className="w-full min-w-[760px] text-left text-sm">
          <thead className="border-b border-slate-200 bg-slate-50/80 text-[11px] font-semibold uppercase tracking-wider text-slate-500">
            <tr>
              <th className="px-4 py-3">#</th>
              <th className="px-4 py-3">Subject</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Priority</th>
              <th className="px-4 py-3">Requester</th>
              <th className="px-4 py-3">Assigned to</th>
              <th className="px-4 py-3">Created</th>
            </tr>
          </thead>
          <tbody>
            {data?.content.map((t) => (
              <tr key={t.id} className="ticket-table-row border-b border-slate-100 last:border-0 hover:bg-slate-50/80">
                <td className="px-4 py-4 font-mono text-xs text-slate-400">#{t.id}</td>
                <td className="px-4 py-3">
                  <Link to={`/tickets/${t.id}`} className="font-semibold text-slate-800 transition hover:text-indigo-700">{t.subject}</Link>
                </td>
                <td className="px-4 py-3"><StatusBadge status={t.status} /></td>
                <td className="px-4 py-3"><PriorityBadge priority={t.priority} /></td>
                <td className="px-4 py-3">{t.requester.name}</td>
                <td className="px-4 py-3">{t.assignedAgent?.name ?? <span className="text-slate-400">Unassigned</span>}</td>
                <td className="px-4 py-3 text-slate-500">{new Date(t.createdAt).toLocaleString()}</td>
              </tr>
            ))}
            {data && data.content.length === 0 && (
              <tr><td colSpan={7} className="px-4 py-8 text-center text-slate-500">No tickets found</td></tr>
            )}
            {!data && loading && (
              <tr><td colSpan={7} className="px-4 py-8 text-center text-slate-500">Loading...</td></tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="space-y-3 md:hidden">
        {data?.content.map((t) => (
          <Link key={t.id} to={`/tickets/${t.id}`} className="card block space-y-3 transition hover:border-indigo-200 hover:shadow-md focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-indigo-100">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <p className="text-xs font-medium text-slate-400">Ticket #{t.id}</p>
                <h2 className="mt-1 font-semibold text-slate-900">{t.subject}</h2>
              </div>
              <StatusBadge status={t.status} />
            </div>
            <div className="flex flex-wrap items-center justify-between gap-2 border-t border-slate-100 pt-3 text-xs text-slate-500">
              <span>{t.requester.name}</span>
              <PriorityBadge priority={t.priority} />
              <span>{new Date(t.createdAt).toLocaleDateString()}</span>
            </div>
          </Link>
        ))}
        {data && data.content.length === 0 && <div className="card py-8 text-center text-sm text-slate-500">No tickets match these filters.</div>}
        {!data && loading && <div className="card py-8 text-center text-sm text-slate-500">Loading tickets...</div>}
      </div>

      {data && data.totalPages > 0 && (
        <div className="flex items-center justify-between text-sm">
          <span className="text-slate-500">
            Page {data.page + 1} of {data.totalPages} · {data.totalElements} tickets
          </span>
          <div className="flex gap-2">
            <button className="btn-secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
            <button className="btn-secondary" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>Next</button>
          </div>
        </div>
      )}
    </div>
  );
}
