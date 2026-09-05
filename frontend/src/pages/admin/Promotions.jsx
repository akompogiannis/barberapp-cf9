import { useCallback, useEffect, useState } from 'react';
import {
  createPromotion,
  deletePromotion,
  getAllPromotions,
  getServices,
} from '../../api/endpoints';
import { ErrorAlert, SuccessAlert } from '../../components/Alert';

const EMPTY = {
  title: '',
  description: '',
  discountPercent: 10,
  validFrom: '',
  validTo: '',
  serviceUuids: [],
};

export default function Promotions() {
  const [promotions, setPromotions] = useState([]);
  const [services, setServices] = useState([]);
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [promoResponse, serviceResponse] = await Promise.all([
        getAllPromotions(),
        getServices(),
      ]);
      setPromotions(promoResponse.data.content ?? []);
      setServices(serviceResponse.data);
    } catch (err) {
      setError(err.apiError);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const update = (field) => (event) =>
    setForm((current) => ({ ...current, [field]: event.target.value }));

  const toggleService = (uuid) =>
    setForm((current) => ({
      ...current,
      serviceUuids: current.serviceUuids.includes(uuid)
        ? current.serviceUuids.filter((id) => id !== uuid)
        : [...current.serviceUuids, uuid],
    }));

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError(null);
    setMessage(null);

    try {
      await createPromotion({
        ...form,
        discountPercent: Number(form.discountPercent),
        description: form.description || null,
      });
      setMessage('Promotion created.');
      setForm(EMPTY);
      load();
    } catch (err) {
      setError(err.apiError);
    }
  };

  const handleDelete = async (promotion) => {
    if (!window.confirm(`Withdraw "${promotion.title}"?`)) return;

    setError(null);
    setMessage(null);

    try {
      await deletePromotion(promotion.uuid);
      setMessage('Promotion withdrawn.');
      load();
    } catch (err) {
      setError(err.apiError);
    }
  };

  return (
    <>
      <h1>Promotions</h1>
      <p className="page-intro">
        Discounts advertised on the shop page. Bookings already made keep the price they were quoted.
      </p>

      <ErrorAlert error={error} />
      <SuccessAlert message={message} />

      <form className="card" onSubmit={handleSubmit}>
        <h2>New promotion</h2>

        <div className="field">
          <label htmlFor="title">Title</label>
          <input id="title" value={form.title} onChange={update('title')} required />
        </div>

        <div className="field">
          <label htmlFor="description">Description</label>
          <textarea id="description" rows={2} value={form.description} onChange={update('description')} />
        </div>

        <div className="form-row">
          <div className="field">
            <label htmlFor="discount">Discount (%)</label>
            <input
              id="discount"
              type="number"
              min="1"
              max="100"
              value={form.discountPercent}
              onChange={update('discountPercent')}
              required
            />
          </div>
          <div className="field">
            <label htmlFor="validFrom">Valid from</label>
            <input
              id="validFrom"
              type="date"
              value={form.validFrom}
              onChange={update('validFrom')}
              required
            />
          </div>
          <div className="field">
            <label htmlFor="validTo">Valid to</label>
            <input
              id="validTo"
              type="date"
              value={form.validTo}
              onChange={update('validTo')}
              required
            />
          </div>
        </div>

        <div className="field">
          <label>Applies to</label>
          {services.map((service) => (
            <label key={service.uuid} style={{ display: 'block', color: 'inherit' }}>
              <input
                type="checkbox"
                style={{ width: 'auto', marginRight: '6px' }}
                checked={form.serviceUuids.includes(service.uuid)}
                onChange={() => toggleService(service.uuid)}
              />
              {service.name}
            </label>
          ))}
        </div>

        <button type="submit" disabled={form.serviceUuids.length === 0}>
          Create promotion
        </button>
      </form>

      {loading && <p className="muted">Loading…</p>}

      {!loading && promotions.length === 0 && <p className="muted">No promotions yet.</p>}

      {!loading && promotions.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Title</th>
                <th>Discount</th>
                <th>Valid</th>
                <th>Services</th>
                <th>Active</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {promotions.map((promotion) => (
                <tr key={promotion.uuid}>
                  <td>{promotion.title}</td>
                  <td>-{promotion.discountPercent}%</td>
                  <td>
                    {promotion.validFrom} → {promotion.validTo}
                  </td>
                  <td>{promotion.serviceNames.join(', ')}</td>
                  <td>
                    <span className="badge">{promotion.active ? 'Active' : 'Off'}</span>
                  </td>
                  <td>
                    <button
                      type="button"
                      className="danger small"
                      onClick={() => handleDelete(promotion)}
                    >
                      Withdraw
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
