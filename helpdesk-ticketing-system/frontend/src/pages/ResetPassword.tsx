import { useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import api, { errorMessage } from '../api';

export default function ResetPassword() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') ?? '';
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError('');
    setMessage('');
    if (password !== confirmPassword) {
      setError('The passwords do not match.');
      return;
    }
    setLoading(true);
    try {
      const response = await api.post<{ message: string }>('/auth/reset-password', { token, password });
      setMessage(response.data.message);
      setPassword('');
      setConfirmPassword('');
    } catch (requestError) {
      setError(errorMessage(requestError));
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
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-indigo-200">Account security</p>
              <h1 className="mt-4 max-w-sm text-4xl font-semibold leading-tight tracking-tight">
                Choose a new password.
              </h1>
              <p className="mt-5 max-w-sm text-sm leading-6 text-indigo-100/80">
                Use a strong password that you don’t use for other accounts.
              </p>
            </div>
          </div>
          <p className="text-xs text-indigo-200/70">The reset link can only be used once.</p>
        </aside>

        <form onSubmit={submit} className="login-form space-y-5">
          <div className="mb-8">
            <p className="text-sm font-semibold text-indigo-600">Password recovery</p>
            <h2 className="mt-2 text-3xl font-semibold tracking-tight text-slate-900">Set a new password</h2>
            <p className="mt-2 text-sm leading-6 text-slate-500">Your new password must be at least 8 characters.</p>
          </div>
          {!token && <p role="alert" className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2.5 text-sm text-amber-800">This reset link is missing its token. Request a new link to continue.</p>}
          <div>
            <label htmlFor="new-password" className="mb-1.5 block text-sm font-medium text-slate-700">New password</label>
            <input
              id="new-password"
              className="input"
              type="password"
              autoComplete="new-password"
              minLength={8}
              maxLength={100}
              placeholder="At least 8 characters"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
              disabled={!token || Boolean(message)}
            />
          </div>
          <div>
            <label htmlFor="confirm-password" className="mb-1.5 block text-sm font-medium text-slate-700">Confirm password</label>
            <input
              id="confirm-password"
              className="input"
              type="password"
              autoComplete="new-password"
              minLength={8}
              maxLength={100}
              placeholder="Enter the password again"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              required
              disabled={!token || Boolean(message)}
            />
          </div>
          {message && <p role="status" className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2.5 text-sm text-emerald-800">{message}</p>}
          {error && <p role="alert" className="rounded-lg border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-700">{error}</p>}
          {!message && (
            <button className="btn w-full !py-3" disabled={loading || !token}>
              {loading ? 'Updating...' : 'Reset password'}
            </button>
          )}
          <p className="pt-2 text-center text-sm text-slate-500">
            <Link to="/login" className="font-semibold text-indigo-600 hover:text-indigo-700 hover:underline">Back to sign in</Link>
            {' · '}
            <Link to="/forgot-password" className="font-semibold text-indigo-600 hover:text-indigo-700 hover:underline">Request a new link</Link>
          </p>
        </form>
      </div>
    </main>
  );
}
