import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext';

export default function ProtectedRoute({ children, capability }) {
  const { isAuthenticated, loading, can } = useAuth();
  const location = useLocation();

  if (loading) {
    return <p className="muted">Loading…</p>;
  }

  if (!isAuthenticated) {
    // Remember where they were headed so login can send them back there.
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }

  if (capability && !can(capability)) {
    return (
      <div className="card">
        <h2>Not allowed</h2>
        <p className="muted">Your account does not have permission to view this page.</p>
      </div>
    );
  }

  return children;
}
