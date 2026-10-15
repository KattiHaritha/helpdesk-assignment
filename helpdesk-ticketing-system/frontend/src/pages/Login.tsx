import { useState, type FormEvent } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { Link } from 'react-router-dom';
import api, { errorMessage } from '../api';
import { useAuth } from '../auth';
import type { AuthUser } from '../types';

type AuthResponse = AuthUser & { token: string };

export default function Login() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [orgName, setOrgName] = useState('');
  const [adminName, setAdminName] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  if (user) return <Navigate to="/" replace />;

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res =
        mode === 'login'
          ? await api.post<AuthResponse>('/auth/login', { email, password })
          : await api.post<AuthResponse>('/auth/register-org', { orgName, adminName, email, password });
      const { token, ...u } = res.data;
      login(token, u);
      navigate('/');
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="login-page">
      <div className="login-shell">
        <aside className="login-promo">
          <div>
            <div className="flex items-center gap-3 text-lg font-bold tracking-tight">
              <span className="flex h-10 w-10 items-center justify-center rounded-xl border border-white/20 bg-white/10 text-xs">HD</span>
              Helpdesk<span className="text-indigo-200">.</span>
            </div>
            <div className="mt-20">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-indigo-200">Customer support, organized</p>
              <h2 className="mt-4 max-w-sm text-4xl font-semibold leading-tight tracking-tight">
                Great service starts with a clear view.
              </h2>
              <p className="mt-5 max-w-sm text-sm leading-6 text-indigo-100/80">
                Manage requests, work together, and keep every customer in the loop.
              </p>
            </div>
          </div>
          <p className="text-xs text-indigo-200/70">A simpler way to support your customers.</p>
        </aside>

        <form onSubmit={submit} className="login-form space-y-5">
          <div className="mb-8">
            <p className="text-sm font-semibold text-indigo-600">{mode === 'login' ? 'Welcome back' : 'Get started'}</p>
            <h1 className="mt-2 text-3xl font-semibold tracking-tight text-slate-900">
              {mode === 'login' ? 'Sign in to Helpdesk' : 'Create your workspace'}
            </h1>
            <p className="mt-2 text-sm leading-6 text-slate-500">
              {mode === 'login' ? 'Enter your details to access your support workspace.' : 'Set up your organization and admin account.'}
            </p>
          </div>

          {mode === 'register' && (
            <>
              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700" htmlFor="organization-name">Organization name</label>
                <input id="organization-name" className="input" placeholder="Acme Inc." value={orgName} onChange={(e) => setOrgName(e.target.value)} required autoComplete="organization" />
              </div>
              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700" htmlFor="admin-name">Your name</label>
                <input id="admin-name" className="input" placeholder="Jane Smith" value={adminName} onChange={(e) => setAdminName(e.target.value)} required autoComplete="name" />
              </div>
            </>
          )}
          <div>
            <label className="mb-1.5 block text-sm font-medium text-slate-700" htmlFor="email">Work email</label>
            <input id="email" className="input" type="email" placeholder="you@company.com" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" />
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-slate-700" htmlFor="password">Password</label>
          <input id="password" className="input" type="password" placeholder="Enter your password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={mode === 'login' ? undefined : 8} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} />
            {mode === 'login' && (
              <div className="mt-2 text-right">
                <Link to="/forgot-password" className="text-sm font-medium text-indigo-600 hover:text-indigo-700 hover:underline">
                  Forgot password?
                </Link>
              </div>
            )}
          </div>

          {error && <p role="alert" className="rounded-lg border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-700">{error}</p>}

          <button className="btn w-full !py-3" disabled={loading}>
            {loading ? 'Please wait...' : mode === 'login' ? 'Sign in' : 'Create workspace'}
          </button>

          <p className="pt-2 text-center text-sm text-slate-500">
            {mode === 'login' ? 'New organization?' : 'Already have an account?'}{' '}
            <button
              type="button"
              className="font-semibold text-indigo-600 hover:text-indigo-700 hover:underline"
              onClick={() => {
                setMode(mode === 'login' ? 'register' : 'login');
                setError('');
              }}
            >
              {mode === 'login' ? 'Create an account' : 'Sign in'}
            </button>
          </p>
        </form>
      </div>
    </main>
  );
}
