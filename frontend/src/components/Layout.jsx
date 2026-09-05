import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export default function Layout() {
  const { user, isAuthenticated, can, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <>
      <header className="site-header">
        <div className="inner">
          <Link to="/" className="brand">
            Nikos Barber Studio
          </Link>

          {/*
            Links are shown per capability, using the same names the backend
            enforces. This is only about what the menu offers - the server
            re-checks every request regardless.
          */}
          <nav className="nav">
            <NavLink to="/">Home</NavLink>

            {can('BOOK_APPOINTMENT') && <NavLink to="/book">Book</NavLink>}
            {can('VIEW_OWN_APPOINTMENTS') && (
              <NavLink to="/my-appointments">My appointments</NavLink>
            )}

            {can('VIEW_APPOINTMENTS') && <NavLink to="/admin/diary">Diary</NavLink>}
            {can('MANAGE_SERVICES') && <NavLink to="/admin/services">Services</NavLink>}
            {can('MANAGE_PROMOTIONS') && <NavLink to="/admin/promotions">Promotions</NavLink>}
            {can('MANAGE_SCHEDULE') && <NavLink to="/admin/schedule">Schedule</NavLink>}

            {isAuthenticated ? (
              <>
                <NavLink to="/profile">{user.firstname}</NavLink>
                <button type="button" className="secondary small" onClick={handleLogout}>
                  Log out
                </button>
              </>
            ) : (
              <>
                <NavLink to="/login">Log in</NavLink>
                <NavLink to="/register">Register</NavLink>
              </>
            )}
          </nav>
        </div>
      </header>

      <main className="main-content">
        <Outlet />
      </main>

      <footer className="site-footer">
        <div className="inner">
          Coding Factory 9 final project &middot; Athens University of Economics and Business
        </div>
      </footer>
    </>
  );
}
