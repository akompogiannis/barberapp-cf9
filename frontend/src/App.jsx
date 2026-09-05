import { Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import ProtectedRoute from './auth/ProtectedRoute';

import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import Book from './pages/Book';
import MyAppointments from './pages/MyAppointments';
import Profile from './pages/Profile';
import NotFound from './pages/NotFound';

import Diary from './pages/admin/Diary';
import Services from './pages/admin/Services';
import Promotions from './pages/admin/Promotions';
import Schedule from './pages/admin/Schedule';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        {/* public */}
        <Route index element={<Home />} />
        <Route path="login" element={<Login />} />
        <Route path="register" element={<Register />} />

        {/* customer */}
        <Route
          path="book"
          element={
            <ProtectedRoute capability="BOOK_APPOINTMENT">
              <Book />
            </ProtectedRoute>
          }
        />
        <Route
          path="my-appointments"
          element={
            <ProtectedRoute capability="VIEW_OWN_APPOINTMENTS">
              <MyAppointments />
            </ProtectedRoute>
          }
        />
        <Route
          path="profile"
          element={
            <ProtectedRoute>
              <Profile />
            </ProtectedRoute>
          }
        />

        {/* barber */}
        <Route
          path="admin/diary"
          element={
            <ProtectedRoute capability="VIEW_APPOINTMENTS">
              <Diary />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/services"
          element={
            <ProtectedRoute capability="MANAGE_SERVICES">
              <Services />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/promotions"
          element={
            <ProtectedRoute capability="MANAGE_PROMOTIONS">
              <Promotions />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/schedule"
          element={
            <ProtectedRoute capability="MANAGE_SCHEDULE">
              <Schedule />
            </ProtectedRoute>
          }
        />

        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
