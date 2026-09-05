import client from './client';

// --- public ---
export const getBarberProfile = () => client.get('/barber/profile');
export const getServices = () => client.get('/services');
export const getActivePromotions = () => client.get('/promotions/active');
export const getAvailability = (serviceUuid, date) =>
  client.get('/availability', { params: { serviceUuid, date } });

// --- auth ---
export const authenticate = (username, password) =>
  client.post('/auth/authenticate', { username, password });
export const register = (payload) => client.post('/users/register', payload);
export const getMe = () => client.get('/users/me');
export const updateMe = (payload) => client.put('/users/me', payload);

// --- appointments ---
export const bookAppointment = (payload) => client.post('/appointments', payload);
export const getOwnAppointments = (page = 0, size = 10) =>
  client.get('/appointments/me', { params: { page, size } });
export const getAppointments = (params) => client.get('/appointments', { params });
export const changeAppointmentStatus = (uuid, status) =>
  client.patch(`/appointments/${uuid}/status`, { status });
export const rescheduleAppointment = (uuid, startAt) =>
  client.patch(`/appointments/${uuid}/reschedule`, { startAt });
export const cancelAppointment = (uuid) => client.delete(`/appointments/${uuid}`);

// --- catalogue administration ---
export const createService = (payload) => client.post('/services', payload);
export const updateService = (uuid, payload) => client.put(`/services/${uuid}`, payload);
export const deleteService = (uuid) => client.delete(`/services/${uuid}`);

// --- promotions administration ---
export const getAllPromotions = (page = 0, size = 20) =>
  client.get('/promotions', { params: { page, size } });
export const createPromotion = (payload) => client.post('/promotions', payload);
export const updatePromotion = (uuid, payload) => client.put(`/promotions/${uuid}`, payload);
export const deletePromotion = (uuid) => client.delete(`/promotions/${uuid}`);

// --- schedule administration ---
export const getWorkingHours = () => client.get('/schedule/working-hours');
export const replaceWorkingHours = (blocks) => client.put('/schedule/working-hours', blocks);
export const getTimeOff = (from, to) => client.get('/schedule/time-off', { params: { from, to } });
export const addTimeOff = (payload) => client.post('/schedule/time-off', payload);
export const deleteTimeOff = (uuid) => client.delete(`/schedule/time-off/${uuid}`);
