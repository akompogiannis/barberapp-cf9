import { useCallback, useEffect, useState } from 'react';
import {
  changeAppointmentStatus,
  getAppointments,
} from '../../api/endpoints';
import { ErrorAlert, SuccessAlert } from '../../components/Alert';
import Price from '../../components/Price';
import { Pagination, formatDateTime } from '../MyAppointments';

// Mirrors AppointmentStatus.canTransitionTo on the server.
const NEXT_STATUSES = {
  PENDING: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['COMPLETED', 'CANCELLED', 'NO_SHOW'],
  COMPLETED: [],
  CANCELLED: [],
  NO_SHOW: [],
};

const EMPTY_FILTERS = { status: '', from: '', to: '', customerLastname: '' };

export default function Diary() {
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [page, setPage] = useState(null);
  const [pageNumber, setPageNumber] = useState(0);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      // Blank filter fields are dropped rather than sent as empty strings, so
      // the server sees a genuinely absent parameter.
      const params = { page: pageNumber, size: 10 };
      Object.entries(filters).forEach(([key, value]) => {
        if (value) params[key] = value;
      });

      const { data } = await getAppointments(params);
      setPage(data);
    } catch (err) {
      setError(err.apiError);
    } finally {
      setLoading(false);
    }
  }, [filters, pageNumber]);

  useEffect(() => {
    load();
  }, [load]);

  const updateFilter = (field) => (event) => {
    setPageNumber(0);
    setFilters((current) => ({ ...current, [field]: event.target.value }));
  };

  const handleStatusChange = async (uuid, status) => {
    setError(null);
    setMessage(null);

    try {
      await changeAppointmentStatus(uuid, status);
      setMessage(`Appointment marked ${status.toLowerCase()}.`);
      load();
    } catch (err) {
      setError(err.apiError);
    }
  };

  return (
    <>
      <h1>Diary</h1>
      <p className="page-intro">Every booking in the shop.</p>

      <ErrorAlert error={error} />
      <SuccessAlert message={message} />

      <div className="card">
        <div className="form-row">
          <div className="field">
            <label htmlFor="status">Status</label>
            <select id="status" value={filters.status} onChange={updateFilter('status')}>
              <option value="">Any</option>
              {Object.keys(NEXT_STATUSES).map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label htmlFor="from">From</label>
            <input id="from" type="date" value={filters.from} onChange={updateFilter('from')} />
          </div>
          <div className="field">
            <label htmlFor="to">To</label>
            <input id="to" type="date" value={filters.to} onChange={updateFilter('to')} />
          </div>
          <div className="field">
            <label htmlFor="lastname">Customer surname</label>
            <input
              id="lastname"
              value={filters.customerLastname}
              onChange={updateFilter('customerLastname')}
            />
          </div>
        </div>

        <button
          type="button"
          className="secondary small"
          onClick={() => {
            setFilters(EMPTY_FILTERS);
            setPageNumber(0);
          }}
        >
          Clear filters
        </button>
      </div>

      {loading && <p className="muted">Loading…</p>}

      {!loading && page?.content?.length === 0 && (
        <p className="muted">Nothing matches those filters.</p>
      )}

      {!loading && page?.content?.length > 0 && (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>When</th>
                  <th>Customer</th>
                  <th>Service</th>
                  <th>Price</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {page.content.map((appointment) => (
                  <tr key={appointment.uuid}>
                    <td>{formatDateTime(appointment.startAt)}</td>
                    <td>
                      {appointment.customerName}
                      {appointment.customerPhone && (
                        <div className="muted" style={{ fontSize: '12px' }}>
                          {appointment.customerPhone}
                        </div>
                      )}
                    </td>
                    <td>
                      {appointment.serviceName}
                      <span className="muted"> · {appointment.durationMinutes} min</span>
                    </td>
                    <td>
                      <Price price={appointment.priceCharged} />
                    </td>
                    <td>
                      <span className={`badge ${appointment.status}`}>{appointment.status}</span>
                    </td>
                    <td>
                      <div className="button-row">
                        {NEXT_STATUSES[appointment.status].map((next) => (
                          <button
                            type="button"
                            key={next}
                            className={next === 'CANCELLED' || next === 'NO_SHOW'
                              ? 'danger small'
                              : 'secondary small'}
                            onClick={() => handleStatusChange(appointment.uuid, next)}
                          >
                            {next.toLowerCase().replace('_', ' ')}
                          </button>
                        ))}
                      </div>
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
