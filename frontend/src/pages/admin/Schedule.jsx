import { useCallback, useEffect, useState } from 'react';
import {
  addTimeOff,
  deleteTimeOff,
  getTimeOff,
  getWorkingHours,
  replaceWorkingHours,
} from '../../api/endpoints';
import { ErrorAlert, SuccessAlert } from '../../components/Alert';
import { formatDateTime } from '../MyAppointments';

const DAYS = [
  'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY',
];

const EMPTY_TIME_OFF = { startAt: '', endAt: '', reason: '' };

export default function Schedule() {
  const [blocks, setBlocks] = useState([]);
  const [timeOff, setTimeOff] = useState([]);
  const [timeOffForm, setTimeOffForm] = useState(EMPTY_TIME_OFF);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [hoursResponse, timeOffResponse] = await Promise.all([
        getWorkingHours(),
        getTimeOff(),
      ]);
      setBlocks(hoursResponse.data.map((b) => ({
        dayOfWeek: b.dayOfWeek,
        startTime: trim(b.startTime),
        endTime: trim(b.endTime),
      })));
      setTimeOff(timeOffResponse.data);
    } catch (err) {
      setError(err.apiError);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const updateBlock = (index, field) => (event) => {
    const value = event.target.value;
    setBlocks((current) =>
      current.map((block, i) => (i === index ? { ...block, [field]: value } : block)),
    );
  };

  const addBlock = () =>
    setBlocks((current) => [
      ...current,
      { dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '14:00' },
    ]);

  const removeBlock = (index) =>
    setBlocks((current) => current.filter((_, i) => i !== index));

  const saveHours = async () => {
    setError(null);
    setMessage(null);

    try {
      // The whole template is sent at once; the server validates that blocks on
      // the same day do not overlap before replacing anything.
      await replaceWorkingHours(blocks.map((b) => ({ ...b, id: null })));
      setMessage('Working hours saved.');
      load();
    } catch (err) {
      setError(err.apiError);
    }
  };

  const handleAddTimeOff = async (event) => {
    event.preventDefault();
    setError(null);
    setMessage(null);

    try {
      await addTimeOff({
        startAt: timeOffForm.startAt,
        endAt: timeOffForm.endAt,
        reason: timeOffForm.reason || null,
      });
      setMessage('Time off registered.');
      setTimeOffForm(EMPTY_TIME_OFF);
      load();
    } catch (err) {
      setError(err.apiError);
    }
  };

  const handleDeleteTimeOff = async (uuid) => {
    setError(null);
    setMessage(null);

    try {
      await deleteTimeOff(uuid);
      setMessage('Time off removed.');
      load();
    } catch (err) {
      setError(err.apiError);
    }
  };

  return (
    <>
      <h1>Schedule</h1>
      <p className="page-intro">
        The weekly template availability is generated from, plus one-off absences.
      </p>

      <ErrorAlert error={error} />
      <SuccessAlert message={message} />

      {loading && <p className="muted">Loading…</p>}

      {!loading && (
        <>
          <div className="card">
            <h2>Working hours</h2>
            <p className="muted">
              For a lunch break, add two blocks on the same day — the gap between them is the break.
            </p>

            {blocks.map((block, index) => (
              <div className="form-row" key={index}>
                <div className="field">
                  <label>Day</label>
                  <select value={block.dayOfWeek} onChange={updateBlock(index, 'dayOfWeek')}>
                    {DAYS.map((day) => (
                      <option key={day} value={day}>
                        {day.charAt(0) + day.slice(1).toLowerCase()}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="field">
                  <label>From</label>
                  <input type="time" value={block.startTime} onChange={updateBlock(index, 'startTime')} />
                </div>
                <div className="field">
                  <label>To</label>
                  <input type="time" value={block.endTime} onChange={updateBlock(index, 'endTime')} />
                </div>
                <div className="field" style={{ flex: '0 0 auto', alignSelf: 'flex-end' }}>
                  <button type="button" className="danger small" onClick={() => removeBlock(index)}>
                    Remove
                  </button>
                </div>
              </div>
            ))}

            <div className="button-row">
              <button type="button" className="secondary" onClick={addBlock}>
                Add block
              </button>
              <button type="button" onClick={saveHours}>
                Save working hours
              </button>
            </div>
          </div>

          <div className="card">
            <h2>Time off</h2>
            <p className="muted">
              Blocked periods stop being offered. Appointments already booked inside one are left
              alone — cancelling them is your call.
            </p>

            <form className="form-row" onSubmit={handleAddTimeOff}>
              <div className="field">
                <label htmlFor="startAt">From</label>
                <input
                  id="startAt"
                  type="datetime-local"
                  value={timeOffForm.startAt}
                  onChange={(e) => setTimeOffForm((c) => ({ ...c, startAt: e.target.value }))}
                  required
                />
              </div>
              <div className="field">
                <label htmlFor="endAt">To</label>
                <input
                  id="endAt"
                  type="datetime-local"
                  value={timeOffForm.endAt}
                  onChange={(e) => setTimeOffForm((c) => ({ ...c, endAt: e.target.value }))}
                  required
                />
              </div>
              <div className="field">
                <label htmlFor="reason">Reason</label>
                <input
                  id="reason"
                  value={timeOffForm.reason}
                  onChange={(e) => setTimeOffForm((c) => ({ ...c, reason: e.target.value }))}
                />
              </div>
              <div className="field" style={{ flex: '0 0 auto', alignSelf: 'flex-end' }}>
                <button type="submit">Block out</button>
              </div>
            </form>

            {timeOff.length === 0 ? (
              <p className="muted">Nothing blocked out.</p>
            ) : (
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>From</th>
                      <th>To</th>
                      <th>Reason</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    {timeOff.map((item) => (
                      <tr key={item.uuid}>
                        <td>{formatDateTime(item.startAt)}</td>
                        <td>{formatDateTime(item.endAt)}</td>
                        <td>{item.reason || <span className="muted">—</span>}</td>
                        <td>
                          <button
                            type="button"
                            className="danger small"
                            onClick={() => handleDeleteTimeOff(item.uuid)}
                          >
                            Remove
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      )}
    </>
  );
}

function trim(time) {
  return time?.slice(0, 5) ?? time;
}
