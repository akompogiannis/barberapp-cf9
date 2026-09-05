import axios from 'axios';

export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1';

const TOKEN_KEY = 'barberapp.token';

export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

const client = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

// Attach the token here, so a new endpoint cannot be written without auth by accident.
client.interceptors.request.use((config) => {
  const token = tokenStorage.get();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Registered by AuthContext, so this module does not have to import React.
let onUnauthorized = () => {};
export const setUnauthorizedHandler = (handler) => {
  onUnauthorized = handler;
};

client.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;

    // A 401 means the token is gone or expired - drop it and bounce to the login form.
    if (status === 401) {
      tokenStorage.clear();
      onUnauthorized();
    }

    // Normalise the backend's ErrorResponseDTO into something components can
    // render without each one re-deriving the shape.
    const data = error.response?.data;
    error.apiError = {
      code: data?.code ?? 'NETWORK_ERROR',
      message: data?.description ?? error.message ?? 'Something went wrong',
      fieldErrors: data?.errors ?? null,
      status,
    };

    return Promise.reject(error);
  },
);

export default client;
