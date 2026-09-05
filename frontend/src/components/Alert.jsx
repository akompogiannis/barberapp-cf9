export function ErrorAlert({ error }) {
  if (!error) return null;

  return (
    <div className="alert error">
      <div>{error.message}</div>
      {error.fieldErrors && (
        <ul>
          {Object.entries(error.fieldErrors).map(([field, message]) => (
            <li key={field}>
              <strong>{field}</strong>: {message}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export function SuccessAlert({ message }) {
  if (!message) return null;
  return <div className="alert success">{message}</div>;
}
