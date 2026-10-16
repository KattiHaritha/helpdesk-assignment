import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Navigate } from 'react-router-dom';
import api, { errorMessage } from '../api';
import { useAuth } from '../auth';
import type { Role, UserItem } from '../types';

export default function Users() {
  const { user } = useAuth();
  const [users, setUsers] = useState<UserItem[]>([]);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<Role>('AGENT');
  const [error, setError] = useState('');

  const load = useCallback(() => {
    api.get<UserItem[]>('/users').then((r) => setUsers(r.data)).catch((e) => setError(errorMessage(e)));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  if (user?.role !== 'ORG_ADMIN') return <Navigate to="/" replace />;

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      await api.post('/users', { name, email, password, role });
      setName('');
      setEmail('');
      setPassword('');
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <p className="text-sm font-medium text-indigo-600">Organization settings</p>
        <h1 className="mt-1 text-2xl font-semibold tracking-tight text-slate-900">Team members</h1>
        <p className="mt-1 text-sm text-slate-500">Manage access to {user.organizationName}’s support workspace.</p>
      </div>
      <div className="grid items-start gap-6 lg:grid-cols-3">
      <div className="card overflow-x-auto !p-0 lg:col-span-2">
        <table className="w-full text-left text-sm">
          <thead className="border-b border-slate-200 bg-slate-50/80 text-[11px] font-semibold uppercase tracking-wider text-slate-500">
            <tr><th className="px-4 py-3">Name</th><th className="px-4 py-3">Email</th><th className="px-4 py-3">Role</th></tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id} className="border-b border-slate-100 last:border-0 hover:bg-slate-50/80">
                <td className="px-4 py-4 font-medium text-slate-800">{u.name}</td>
                <td className="px-4 py-3">{u.email}</td>
                <td className="px-4 py-3"><span className="rounded-full bg-indigo-50 px-2.5 py-1 text-xs font-semibold text-indigo-700">{u.role.replace('_', ' ')}</span></td>
              </tr>
            ))}
            {users.length === 0 && (
              <tr><td colSpan={3} className="px-4 py-10 text-center text-sm text-slate-500">No team members found.</td></tr>
            )}
          </tbody>
        </table>
      </div>

      <form onSubmit={submit} className="card space-y-3">
        <div className="mb-4">
          <h2 className="font-semibold text-slate-900">Add a team member</h2>
          <p className="mt-1 text-sm text-slate-500">Invite a colleague to your workspace.</p>
        </div>
        <label htmlFor="member-name" className="block text-xs font-semibold text-slate-600">Name
          <input id="member-name" className="input mt-1" placeholder="e.g. Jamie Lee" value={name} onChange={(e) => setName(e.target.value)} required />
        </label>
        <label htmlFor="member-email" className="block text-xs font-semibold text-slate-600">Work email
          <input id="member-email" className="input mt-1" type="email" autoComplete="off" placeholder="name@company.com" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label htmlFor="member-password" className="block text-xs font-semibold text-slate-600">Temporary password
          <input id="member-password" className="input mt-1" type="password" autoComplete="new-password" placeholder="At least 8 characters" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={8} />
        </label>
        <label htmlFor="member-role" className="block text-xs font-semibold text-slate-600">Role
          <select id="member-role" className="input mt-1" value={role} onChange={(e) => setRole(e.target.value as Role)}>
            <option value="AGENT">Support Agent</option>
            <option value="CUSTOMER">Customer</option>
            <option value="ORG_ADMIN">Organization Admin</option>
          </select>
        </label>
        {error && <p className="rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
        <button className="btn w-full">Create user</button>
      </form>
      </div>
    </div>
  );
}
