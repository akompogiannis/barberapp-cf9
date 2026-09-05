import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getBarberProfile } from '../api/endpoints';
import { ErrorAlert } from '../components/Alert';
import Price from '../components/Price';
import { useAuth } from '../auth/AuthContext';

const DAY_ORDER = [
  'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY',
];

export default function Home() {
  const [profile, setProfile] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const { isAuthenticated, can } = useAuth();

  useEffect(() => {
    getBarberProfile()
      .then((response) => setProfile(response.data))
      .catch((err) => setError(err.apiError))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p className="muted">Loading…</p>;
  if (error) return <ErrorAlert error={error} />;
  if (!profile) return null;

  return (
    <>
      <section className="section">
        <h1>{profile.shopName}</h1>
        <p className="page-intro">{profile.bio}</p>
        <p className="muted">
          {profile.address}
          {profile.phone ? ` · ${profile.phone}` : ''}
        </p>

        <div className="button-row">
          {can('BOOK_APPOINTMENT') && <Link to="/book"><button type="button">Book an appointment</button></Link>}
          {!isAuthenticated && (
            <Link to="/login">
              <button type="button">Log in to book</button>
            </Link>
          )}
        </div>
      </section>

      {profile.activePromotions.length > 0 && (
        <section className="section">
          <h2>On offer right now</h2>
          <div className="card-grid">
            {profile.activePromotions.map((promo) => (
              <div className="card" key={promo.uuid}>
                <h3>{promo.title}</h3>
                <p className="muted">{promo.description}</p>
                <p>
                  <span className="badge promo">-{promo.discountPercent}%</span>{' '}
                  <span className="muted">
                    until {new Date(promo.validTo).toLocaleDateString()}
                  </span>
                </p>
                <p className="muted">Applies to: {promo.serviceNames.join(', ')}</p>
              </div>
            ))}
          </div>
        </section>
      )}

      <section className="section">
        <h2>Services</h2>
        <div className="card-grid">
          {profile.services.map((service) => (
            <div className="card" key={service.uuid}>
              <h3>{service.name}</h3>
              <p className="muted">{service.description}</p>
              <p>
                <Price price={service.price} promotionalPrice={service.promotionalPrice} />{' '}
                <span className="muted">· {service.durationMinutes} min</span>
              </p>
              {service.promotionTitle && (
                <span className="badge promo">{service.promotionTitle}</span>
              )}
            </div>
          ))}
        </div>
      </section>

      <section className="section">
        <h2>Opening hours</h2>
        <div className="table-wrap">
          <table>
            <tbody>
              {DAY_ORDER.map((day) => {
                const blocks = profile.workingHours.filter((wh) => wh.dayOfWeek === day);
                return (
                  <tr key={day}>
                    <th style={{ width: '140px' }}>{titleCase(day)}</th>
                    <td>
                      {blocks.length === 0
                        ? <span className="muted">Closed</span>
                        // Two blocks on one day means a break between them.
                        : blocks
                            .map((b) => `${trimSeconds(b.startTime)}–${trimSeconds(b.endTime)}`)
                            .join(', ')}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </section>
    </>
  );
}

function titleCase(day) {
  return day.charAt(0) + day.slice(1).toLowerCase();
}

function trimSeconds(time) {
  return time?.slice(0, 5) ?? time;
}
