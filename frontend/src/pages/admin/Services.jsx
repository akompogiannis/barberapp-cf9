import { useCallback, useEffect, useState } from 'react';
import {
  createService,
  deleteService,
  getServices,
  updateService,
} from '../../api/endpoints';
import { ErrorAlert, SuccessAlert } from '../../components/Alert';
import Price from '../../components/Price';

const EMPTY = { name: '', description: '', durationMinutes: 30, price: '' };

export default function Services() {
  const [services, setServices] = useState([]);
  const [form, setForm] = useState(EMPTY);
  const [editingUuid, setEditingUuid] = useState(null);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await getServices();
      setServices(data);
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

  const resetForm = () => {
    setForm(EMPTY);
    setEditingUuid(null);
  };

  const startEditing = (service) => {
    setEditingUuid(service.uuid);
    setForm({
      name: service.name,
      description: service.description ?? '',
      durationMinutes: service.durationMinutes,
      price: service.price,
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError(null);
    setMessage(null);

    const payload = {
      name: form.name,
      description: form.description || null,
      durationMinutes: Number(form.durationMinutes),
      price: Number(form.price),
    };

    try {
      if (editingUuid) {
        // The server checks that the path uuid matches the body, so send both.
        await updateService(editingUuid, { ...payload, uuid: editingUuid, active: true });
        setMessage('Service updated.');
      } else {
        await createService(payload);
        setMessage('Service created.');
      }
      resetForm();
      load();
    } catch (err) {
      setError(err.apiError);
    }
  };

  const handleDelete = async (service) => {
    if (!window.confirm(`Retire "${service.name}"? Existing appointments keep working.`)) return;

    setError(null);
    setMessage(null);

    try {
      await deleteService(service.uuid);
      setMessage('Service retired.');
      load();
    } catch (err) {
      setError(err.apiError);
    }
  };

  return (
    <>
      <h1>Services</h1>
      <p className="page-intro">
        The catalogue customers book from. Duration drives how slots are generated.
      </p>

      <ErrorAlert error={error} />
      <SuccessAlert message={message} />

      <form className="card" onSubmit={handleSubmit}>
        <h2>{editingUuid ? 'Edit service' : 'Add a service'}</h2>

        <div className="form-row">
          <div className="field">
            <label htmlFor="name">Name</label>
            <input id="name" value={form.name} onChange={update('name')} required />
          </div>
          <div className="field">
            <label htmlFor="duration">Duration (minutes)</label>
            <input
              id="duration"
              type="number"
              min="5"
              max="480"
              step="5"
              value={form.durationMinutes}
              onChange={update('durationMinutes')}
              required
            />
          </div>
          <div className="field">
            <label htmlFor="price">Price (€)</label>
            <input
              id="price"
              type="number"
              min="0"
              step="0.50"
              value={form.price}
              onChange={update('price')}
              required
            />
          </div>
        </div>

        <div className="field">
          <label htmlFor="description">Description</label>
          <textarea id="description" rows={2} value={form.description} onChange={update('description')} />
        </div>

        <div className="button-row">
          <button type="submit">{editingUuid ? 'Save changes' : 'Add service'}</button>
          {editingUuid && (
            <button type="button" className="secondary" onClick={resetForm}>
              Cancel
            </button>
          )}
        </div>
      </form>

      {loading && <p className="muted">Loading…</p>}

      {!loading && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Duration</th>
                <th>Price</th>
                <th>Promotion</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {services.map((service) => (
                <tr key={service.uuid}>
                  <td>
                    {service.name}
                    {service.description && (
                      <div className="muted" style={{ fontSize: '12px' }}>
                        {service.description}
                      </div>
                    )}
                  </td>
                  <td>{service.durationMinutes} min</td>
                  <td>
                    <Price price={service.price} promotionalPrice={service.promotionalPrice} />
                  </td>
                  <td>
                    {service.promotionTitle ? (
                      <span className="badge promo">{service.promotionTitle}</span>
                    ) : (
                      <span className="muted">—</span>
                    )}
                  </td>
                  <td>
                    <div className="button-row">
                      <button
                        type="button"
                        className="secondary small"
                        onClick={() => startEditing(service)}
                      >
                        Edit
                      </button>
                      <button
                        type="button"
                        className="danger small"
                        onClick={() => handleDelete(service)}
                      >
                        Retire
                      </button>
                    </div>
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
