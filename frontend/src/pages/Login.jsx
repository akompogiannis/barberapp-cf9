import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export default function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);

    try {
      const user = await login(username, password);

      // Back to wherever they were headed before the redirect; otherwise the
      // barber lands on the diary and a customer on the booking page.
      const fallback = user.role === 'ADMIN' ? '/admin/diary' : '/book';
      navigate(location.state?.from ?? fallback, { replace: true });
    } catch (err) {
      setError(err.apiError?.message ?? 'Login failed');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <h1>Log in</h1>
      <p className="page-intro">Sign in to book and manage appointments.</p>

      {error && <div className="alert error">{error}</div>}

      <form className="form card" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="username">Username</label>
          <input
            id="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            required
          />
        </div>

        <div className="field">
          <label htmlFor="password">Password</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
          />
        </div>

        <button type="submit" disabled={submitting}>
          {submitting ? 'Signing in…' : 'Log in'}
        </button>
      </form>

      <p className="muted">
        No account yet? <Link to="/register">Register</Link>.
      </p>
    </>
  );
}
