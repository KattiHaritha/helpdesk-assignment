import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import api, { errorMessage } from '../api';
import { PRIORITIES, type Priority, type Ticket } from '../types';

export default function NewTicket() {
  const navigate = useNavigate();
  const [subject, setSubject] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<Priority>('MEDIUM');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setSaving(true);
    try {
      const res = await api.post<Ticket>('/tickets', { subject, description, priority });
      navigate(`/tickets/${res.data.id}`);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <form onSubmit={submit} className="card mx-auto max-w-2xl space-y-5">
      <div className="mb-2">
        <p className="text-sm font-medium text-indigo-600">Support request</p>
        <h1 className="mt-1 text-2xl font-semibold tracking-tight text-slate-900">Create a ticket</h1>
        <p className="mt-1 text-sm text-slate-500">Tell us what you need help with and our team will take it from there.</p>
      </div>
      <div>
        <label className="mb-1 block text-sm font-medium">Subject</label>
        <input className="input" value={subject} onChange={(e) => setSubject(e.target.value)} maxLength={200} required />
      </div>
      <div>
        <label className="mb-1 block text-sm font-medium">Description</label>
        <textarea className="input min-h-36 resize-y" value={description} onChange={(e) => setDescription(e.target.value)} maxLength={4000} required />
      </div>
      <div>
        <label className="mb-1 block text-sm font-medium">Priority</label>
        <select className="input w-48" value={priority} onChange={(e) => setPriority(e.target.value as Priority)}>
          {PRIORITIES.map((p) => <option key={p} value={p}>{p}</option>)}
        </select>
      </div>
      {error && <p className="rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
      <div className="flex gap-3">
        <button className="btn" disabled={saving}>{saving ? 'Creating...' : 'Create ticket'}</button>
        <button type="button" className="btn-secondary" onClick={() => navigate('/')}>Cancel</button>
      </div>
    </form>
  );
}
