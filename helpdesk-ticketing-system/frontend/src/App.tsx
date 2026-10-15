import { Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import Login from './pages/Login';
import Tickets from './pages/Tickets';
import NewTicket from './pages/NewTicket';
import TicketDetail from './pages/TicketDetail';
import Users from './pages/Users';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />
      <Route element={<Layout />}>
        <Route path="/" element={<Tickets />} />
        <Route path="/tickets/new" element={<NewTicket />} />
        <Route path="/tickets/:id" element={<TicketDetail />} />
        <Route path="/users" element={<Users />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
