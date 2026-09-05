import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { bookAppointment, getAvailability, getServices } from '../api/endpoints';
import { ErrorAlert } from '../components/Alert';
import Price from '../components/Price';

// Today as yyyy-MM-dd in the *browser's* timezone.
const todayISO = () => {
  const now = new Date();
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
};

export default function Book() {
  const [services, setServices] = useState([]);
  const [serviceUuid, setServiceUuid] = useState('');
  const [date, setDate] = useState(todayISO());
  const [availability, setAvailability] = useState(null);
  const [selectedSlot, setSelectedSlot] = useState(null);
  const [notes, setNotes] = useState('');

  const [loadingSlots, setLoadingSlots] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const navigate = useNavigate();

  useEffect(() => {
    getServices()
      .then((response) => {
        setServices(response.data);
        if (response.data.length > 0) setServiceUuid(response.data[0].uuid);
      })
      .catch((err) => setError(err.apiError));
  }, []);

  const loadAvailability = useCallback(async () => {
    if (!serviceUuid || !date) return;

    setLoadingSlots(true);
    setError(null);
    setSelectedSlot(null);

    try {
      const { data } = await getAvailability(serviceUuid, date);
      setAvailability(data);
    } catch (err) {
      setError(err.apiError);
      setAvailability(null);
    } finally {
      setLoadingSlots(false);
    }
  }, [serviceUuid, date]);

  useEffect(() => {
    loadAvailability();
  }, [loadAvailability]);

  const handleBook = async () => {
    if (!selectedSlot) return;

    setSubmitting(true);
    setError(null);

    try {
      await bookAppointment({
        serviceUuid,
        startAt: `${date}T${selectedSlot.startTime}`,
        notes: notes || null,
      });
      navigate('/my-appointments', { replace: true });
    } catch (err) {
      setError(err.apiError);

      // A 409 means somebody took the slot in the meantime. Reloading is the
      // useful response: the slot list is stale, not the request wrong.
      if (err.apiError?.status === 409) {
        loadAvailability();
      }
    } finally {
      setSubmitting(false);
    }
  };

  const selectedService = services.find((s) => s.uuid === serviceUuid);

  return (
    <>
      <h1>Book an appointment</h1>
      <p className="page-intro">Choose a service and a day to see the free times.</p>

      <ErrorAlert error={error} />

      <div className="card">
        <div className="form-row">
          <div className="field">
            <label htmlFor="service">Service</label>
            <select
              id="service"
              value={serviceUuid}
              onChange={(e) => setServiceUuid(e.target.value)}
            >
              {services.map((service) => (
                <option key={service.uuid} value={service.uuid}>
                  {service.name} — {service.durationMinutes} min
                </option>
              ))}
            </select>
          </div>

          <div className="field">
            <label htmlFor="date">Date</label>
            <input
              id="date"
              type="date"
              value={date}
              min={todayISO()}
              onChange={(e) => setDate(e.target.value)}
            />
          </div>
        </div>

        {selectedService && (
          <p className="muted">
            <Price
              price={selectedService.price}
              promotionalPrice={selectedService.promotionalPrice}
            />
            {selectedService.promotionTitle && (
              <> · <span className="badge promo">{selectedService.promotionTitle}</span></>
            )}
          </p>
        )}
      </div>

      <div className="card">
        <h2>Available times</h2>

        {loadingSlots && <p className="muted">Checking availability…</p>}

        {/* "Closed" and "fully booked" are genuinely different situations. */}
        {!loadingSlots && availability?.closed && (
          <p className="muted">The shop is closed on this day.</p>
        )}

        {!loadingSlots && availability && !availability.closed && availability.slots.length === 0 && (
          <p className="muted">No free times left on this day. Try another date.</p>
        )}

        {!loadingSlots && availability && availability.slots.length > 0 && (
          <div className="slot-grid">
            {availability.slots.map((slot) => {
              const isSelected = selectedSlot?.startTime === slot.startTime;
              return (
                <button
                  type="button"
                  key={slot.startTime}
                  className={`slot${isSelected ? ' selected' : ''}`}
                  onClick={() => setSelectedSlot(slot)}
                >
                  {slot.startTime.slice(0, 5)}
                </button>
              );
            })}
          </div>
        )}
      </div>

      {selectedSlot && (
        <div className="card">
          <h2>Confirm</h2>
          <p>
            {selectedService?.name} on {date} at {selectedSlot.startTime.slice(0, 5)}, finishing at{' '}
            {selectedSlot.endTime.slice(0, 5)}.
          </p>

          <div className="field">
            <label htmlFor="notes">Anything the barber should know? (optional)</label>
            <textarea
              id="notes"
              rows={3}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </div>

          <button type="button" onClick={handleBook} disabled={submitting}>
            {submitting ? 'Booking…' : 'Confirm booking'}
          </button>
        </div>
      )}
    </>
  );
}
