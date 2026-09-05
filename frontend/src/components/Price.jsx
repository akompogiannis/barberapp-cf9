export default function Price({ price, promotionalPrice }) {
  const hasPromo = promotionalPrice != null && promotionalPrice !== price;

  return (
    <span className="price">
      {hasPromo && <span className="was">{format(price)}</span>}
      {format(hasPromo ? promotionalPrice : price)}
    </span>
  );
}

function format(value) {
  if (value == null) return '-';
  return `${Number(value).toFixed(2)} €`;
}
