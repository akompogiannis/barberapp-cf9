import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { register } from '../api/endpoints';
import { ErrorAlert } from '../components/Alert';

const EMPTY = {
  username: '',
  password: '',
  confirmPassword: '',
  firstname: '',
  lastname: '',
  email: '',
  phone: '',
};

export default function Register() {
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();

  const update = (field) => (event) =>
    setForm((current) => ({ ...current, [field]: event.target.value }));

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);

    try {
      await register(form);
      navigate('/login', { replace: true });
    } catch (err) {
      // The backend returns per-field messages for a 400, which ErrorAlert lists.
      setError(err.apiError);
    } finally {
      setSubmitting(false);
    }
  };

  const fieldError = (name) => error?.fieldErrors?.[name];

  return (
    <>
      <h1>Register</h1>
      <p className="page-intro">Create a customer account to book appointments.</p>

      <ErrorAlert error={error} />

      <form className="form card" onSubmit={handleSubmit}>
        <div className="form-row">
          <div className="field">
            <label htmlFor="firstname">First name</label>
            <input id="firstname" value={form.firstname} onChange={update('firstname')} required />
            {fieldError('firstname') && <div className="field-error">{fieldError('firstname')}</div>}
          </div>
          <div className="field">
            <label htmlFor="lastname">Last name</label>
            <input id="lastname" value={form.lastname} onChange={update('lastname')} required />
            {fieldError('lastname') && <div className="field-error">{fieldError('lastname')}</div>}
          </div>
        </div>

        <div className="field">
          <label htmlFor="username">Username</label>
          <input id="username" value={form.username} onChange={update('username')} required />
          {fieldError('username') && <div className="field-error">{fieldError('username')}</div>}
        </div>

        <div className="field">
          <label htmlFor="email">Email</label>
          <input id="email" type="email" value={form.email} onChange={update('email')} required />
          {fieldError('email') && <div className="field-error">{fieldError('email')}</div>}
        </div>

        <div className="field">
          <label htmlFor="phone">Phone</label>
          <input id="phone" value={form.phone} onChange={update('phone')} placeholder="+306941234567" />
          {fieldError('phone') && <div className="field-error">{fieldError('phone')}</div>}
        </div>

        <div className="field">
          <label htmlFor="password">Password</label>
          <input
            id="password"
            type="password"
            value={form.password}
            onChange={update('password')}
            autoComplete="new-password"
            required
          />
          {fieldError('password') && <div className="field-error">{fieldError('password')}</div>}
          <div className="muted" style={{ fontSize: '12px' }}>
            At least 8 characters, with a letter and a digit.
          </div>
        </div>

        <div className="field">
          <label htmlFor="confirmPassword">Confirm password</label>
          <input
            id="confirmPassword"
            type="password"
            value={form.confirmPassword}
            onChange={update('confirmPassword')}
            autoComplete="new-password"
            required
          />
          {fieldError('confirmPassword') && (
            <div className="field-error">{fieldError('confirmPassword')}</div>
          )}
        </div>

        <button type="submit" disabled={submitting}>
          {submitting ? 'Creating…' : 'Create account'}
        </button>
      </form>

      <p className="muted">
        Already registered? <Link to="/login">Log in</Link>.
      </p>
    </>
  );
}
