import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage } from '../api';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError('');
    setMessage('');
    setLoading(true);
    try {
      const response = await api.post<{ message: string }>('/auth/forgot-password', { email });
      setMessage(response.data.message);
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
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-indigo-200">Account recovery</p>
              <h1 className="mt-4 max-w-sm text-4xl font-semibold leading-tight tracking-tight">
                Get back to supporting your customers.
              </h1>
              <p className="mt-5 max-w-sm text-sm leading-6 text-indigo-100/80">
                We’ll send a secure, one-time link to the email address on your account.
              </p>
            </div>
          </div>
          <p className="text-xs text-indigo-200/70">Your account security matters.</p>
        </aside>

        <form onSubmit={submit} className="login-form space-y-5">
          <div className="mb-8">
            <p className="text-sm font-semibold text-indigo-600">Password recovery</p>
            <h2 className="mt-2 text-3xl font-semibold tracking-tight text-slate-900">Forgot your password?</h2>
            <p className="mt-2 text-sm leading-6 text-slate-500">
              Enter the work email you use for Helpdesk and we’ll send reset instructions.
            </p>
          </div>
          <div>
            <label htmlFor="recovery-email" className="mb-1.5 block text-sm font-medium text-slate-700">Work email</label>
            <input
              id="recovery-email"
              className="input"
              type="email"
              placeholder="you@company.com"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
          </div>
          {message && <p role="status" className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2.5 text-sm text-emerald-800">{message}</p>}
          {error && <p role="alert" className="rounded-lg border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-700">{error}</p>}
          <button className="btn w-full !py-3" disabled={loading}>
            {loading ? 'Sending...' : 'Send reset link'}
          </button>
          <p className="pt-2 text-center text-sm text-slate-500">
            Remember your password? <Link to="/login" className="font-semibold text-indigo-600 hover:text-indigo-700 hover:underline">Back to sign in</Link>
          </p>
        </form>
      </div>
    </main>
  );
}
