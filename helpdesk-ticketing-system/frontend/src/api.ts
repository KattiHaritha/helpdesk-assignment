import axios from 'axios';
import type { UserItem } from './types';

const api = axios.create({ baseURL: '/api' });

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    // expired/invalid token -> back to login (but not for the login call itself)
    if (err.response?.status === 401 && !window.location.pathname.startsWith('/login')) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  },
);

export function errorMessage(e: unknown): string {
  if (axios.isAxiosError(e)) {
    const data = e.response?.data as { message?: string; errors?: string[] } | undefined;
    if (data?.errors && data.errors.length > 0) return data.errors.join(', ');
    if (data?.message) return data.message;
  }
  return 'Something went wrong';
}

/** Agents + admins of my organization (used for assign / filter dropdowns). */
export async function fetchStaff(): Promise<UserItem[]> {
  const [agents, admins] = await Promise.all([
    api.get<UserItem[]>('/users', { params: { role: 'AGENT' } }),
    api.get<UserItem[]>('/users', { params: { role: 'ORG_ADMIN' } }),
  ]);
  return [...agents.data, ...admins.data];
}

export default api;
