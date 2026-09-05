import { useState } from 'react';
import { updateMe } from '../api/endpoints';
import { ErrorAlert, SuccessAlert } from '../components/Alert';
import { useAuth } from '../auth/AuthContext';

export default function Profile() {
  const { user, setUser } = useAuth();

  const [form, setForm] = useState({
    firstname: user.firstname ?? '',
    lastname: user.lastname ?? '',
    email: user.email ?? '',
    phone: user.phone ?? '',
  });
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const update = (field) => (event) =>
    setForm((current) => ({ ...current, [field]: event.target.value }));

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError(null);
    setMessage(null);
    setSubmitting(true);

    try {
      const { data } = await updateMe(form);
      setUser(data);
      setMessage('Profile updated.');
    } catch (err) {
      setError(err.apiError);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <h1>My profile</h1>
      <p className="page-intro">
        Signed in as <strong>{user.username}</strong> ({user.role.toLowerCase()}).
      </p>

      <ErrorAlert error={error} />
      <SuccessAlert message={message} />

      <form className="form card" onSubmit={handleSubmit}>
        <div className="form-row">
          <div className="field">
            <label htmlFor="firstname">First name</label>
            <input id="firstname" value={form.firstname} onChange={update('firstname')} required />
          </div>
          <div className="field">
            <label htmlFor="lastname">Last name</label>
            <input id="lastname" value={form.lastname} onChange={update('lastname')} required />
          </div>
        </div>

        <div className="field">
          <label htmlFor="email">Email</label>
          <input id="email" type="email" value={form.email} onChange={update('email')} required />
        </div>

        <div className="field">
          <label htmlFor="phone">Phone</label>
          <input id="phone" value={form.phone} onChange={update('phone')} />
        </div>

        <button type="submit" disabled={submitting}>
          {submitting ? 'Saving…' : 'Save changes'}
        </button>
      </form>
    </>
  );
}
