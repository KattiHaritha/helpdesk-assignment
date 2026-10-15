import { Link, Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth';

const roleLabel = { ORG_ADMIN: 'Admin', AGENT: 'Agent', CUSTOMER: 'Customer' } as const;

/** Protected layout: redirects to /login when there is no logged-in user. */
export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  if (!user) return <Navigate to="/login" replace />;

  const navClass = (path: string) =>
    `nav-link ${location.pathname === path ? 'nav-link-active' : ''}`;

  return (
    <div className="min-h-screen">
      <header className="app-header">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-3 sm:px-6">
          <div className="flex min-w-0 items-center gap-5">
            <Link to="/" className="flex shrink-0 items-center gap-2.5 text-lg font-bold tracking-tight text-slate-900">
              <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-br from-indigo-500 to-violet-700 text-xs font-bold text-white shadow-lg shadow-indigo-200">HD</span>
              <span className="hidden sm:inline">Helpdesk<span className="text-indigo-600">.</span><span className="ml-2 rounded-full bg-slate-100 px-2 py-0.5 align-middle text-[10px] font-semibold uppercase tracking-wider text-slate-500">Workspace</span></span>
            </Link>
            <nav aria-label="Main navigation" className="app-nav flex items-center gap-1">
              <Link to="/" className={navClass('/')}>Tickets</Link>
              <Link to="/tickets/new" className={navClass('/tickets/new')}>New ticket</Link>
              {user.role === 'ORG_ADMIN' && (
                <Link to="/users" className={navClass('/users')}>Users</Link>
              )}
            </nav>
          </div>
          <div className="flex shrink-0 items-center gap-3 text-sm">
            <span className="app-user-label max-w-52 truncate text-right">
              <span className="block font-semibold text-slate-800">{user.name}</span>
              <span className="block text-xs text-slate-500">{roleLabel[user.role]} · {user.organizationName}</span>
            </span>
            <span aria-hidden="true" className="flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-indigo-100 to-violet-100 text-sm font-bold text-indigo-700 ring-2 ring-white shadow-sm">
              {user.name.charAt(0).toUpperCase()}
            </span>
            <button
              className="btn-secondary !rounded-xl !px-3 !py-2"
              onClick={() => {
                logout();
                navigate('/login');
              }}
            >
              Logout
            </button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 sm:py-8">
        <Outlet />
      </main>
    </div>
  );
}
