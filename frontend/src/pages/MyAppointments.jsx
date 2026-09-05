import { useCallback, useEffect, useState } from 'react';
import { cancelAppointment, getOwnAppointments } from '../api/endpoints';
import { ErrorAlert, SuccessAlert } from '../components/Alert';
import Price from '../components/Price';

const CANCELLABLE = ['PENDING', 'CONFIRMED'];

export default function MyAppointments() {
  const [page, setPage] = useState(null);
  const [pageNumber, setPageNumber] = useState(0);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await getOwnAppointments(pageNumber);
      setPage(data);
    } catch (err) {
      setError(err.apiError);
    } finally {
      setLoading(false);
    }
  }, [pageNumber]);

  useEffect(() => {
    load();
  }, [load]);

  const handleCancel = async (uuid) => {
    if (!window.confirm('Cancel this appointment?')) return;

    setError(null);
    setMessage(null);

    try {
      await cancelAppointment(uuid);
      setMessage('Appointment cancelled.');
      load();
    } catch (err) {
      setError(err.apiError);
    }
  };

  return (
    <>
      <h1>My appointments</h1>
      <p className="page-intro">Everything you have booked, newest bookings by date.</p>

      <ErrorAlert error={error} />
      <SuccessAlert message={message} />

      {loading && <p className="muted">Loading…</p>}

      {!loading && page?.content?.length === 0 && (
        <p className="muted">You have not booked anything yet.</p>
      )}

      {!loading && page?.content?.length > 0 && (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>When</th>
                  <th>Service</th>
                  <th>Price</th>
                  <th>Status</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {page.content.map((appointment) => (
                  <tr key={appointment.uuid}>
                    <td>{formatDateTime(appointment.startAt)}</td>
                    <td>
                      {appointment.serviceName}
                      <span className="muted"> · {appointment.durationMinutes} min</span>
                      {appointment.promotionTitle && (
                        <>
                          {' '}
                          <span className="badge promo">{appointment.promotionTitle}</span>
                        </>
                      )}
                    </td>
                    <td>
                      <Price price={appointment.priceCharged} />
                    </td>
                    <td>
                      <span className={`badge ${appointment.status}`}>{appointment.status}</span>
                    </td>
                    <td>
                      {CANCELLABLE.includes(appointment.status) && (
                        <button
                          type="button"
                          className="danger small"
                          onClick={() => handleCancel(appointment.uuid)}
                        >
                          Cancel
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <Pagination page={page} onChange={setPageNumber} />
        </>
      )}
    </>
  );
}

export function Pagination({ page, onChange }) {
  if (!page || page.totalPages <= 1) return null;

  return (
    <div className="pagination">
      <button
        type="button"
        className="secondary small"
        disabled={page.first}
        onClick={() => onChange(page.number - 1)}
      >
        Previous
      </button>
      <span className="muted">
        Page {page.number + 1} of {page.totalPages}
      </span>
      <button
        type="button"
        className="secondary small"
        disabled={page.last}
        onClick={() => onChange(page.number + 1)}
      >
        Next
      </button>
    </div>
  );
}

export function formatDateTime(value) {
  if (!value) return '-';
  const date = new Date(value);
  return date.toLocaleString(undefined, {
    weekday: 'short',
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  });
}
