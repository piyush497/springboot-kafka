import axios from 'axios';
import { toast } from 'react-toastify';

// API Base URLs
const MAIN_API_BASE_URL = process.env.REACT_APP_MAIN_API_URL || 'http://localhost:8080/api/v1';
const CUSTOMER_API_BASE_URL = process.env.REACT_APP_CUSTOMER_API_URL || 'http://localhost:8081/api/v1/customer';

// Create axios instances
const mainAPI = axios.create({
  baseURL: MAIN_API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

const customerAPI = axios.create({
  baseURL: CUSTOMER_API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add auth token
const addAuthInterceptor = (apiInstance) => {
  apiInstance.interceptors.request.use(
    (config) => {
      const token = localStorage.getItem('token');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    },
    (error) => {
      return Promise.reject(error);
    }
  );
};

// Response interceptor for error handling
const addResponseInterceptor = (apiInstance) => {
  apiInstance.interceptors.response.use(
    (response) => response,
    (error) => {
      if (error.response?.status === 401) {
        // Token expired or invalid
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.location.href = '/login';
      } else if (error.response?.status === 403) {
        toast.error('Access denied. Insufficient permissions.');
      } else if (error.response?.status >= 500) {
        toast.error('Server error. Please try again later.');
      }
      return Promise.reject(error);
    }
  );
};

// Apply interceptors
addAuthInterceptor(mainAPI);
addAuthInterceptor(customerAPI);
addResponseInterceptor(mainAPI);
addResponseInterceptor(customerAPI);

// Authentication API
export const authAPI = {
  login: (credentials) => mainAPI.post('/auth/signin', credentials),
  register: (userData) => mainAPI.post('/auth/signup', userData),
  logout: () => mainAPI.post('/auth/signout'),
  refreshToken: (refreshToken) => mainAPI.post('/auth/refresh', { refreshToken }),
  validateToken: (token) => mainAPI.get('/auth/validate', {
    headers: { Authorization: `Bearer ${token}` }
  }),
  getCurrentUser: () => mainAPI.get('/auth/me'),
};

// Customer API (using customer interface microservice)
export const customerAPI = {
  // Parcel operations
  registerParcel: (parcelData) => customerAPI.post('/parcels/register', parcelData),
  getMyParcels: (params = {}) => customerAPI.get('/parcels/my', { params }),
  getParcelDetails: (parcelId) => customerAPI.get(`/parcels/${parcelId}`),
  trackParcel: (parcelId) => customerAPI.get(`/parcels/${parcelId}/track`),
  cancelParcel: (parcelId, reason) => customerAPI.put(`/parcels/${parcelId}/cancel`, { reason }),
  getDashboard: () => customerAPI.get('/parcels/dashboard'),
};

// Main service API (for operators and admins)
export const parcelAPI = {
  // EDI operations
  processEDI: (ediData) => mainAPI.post('/edi/process', ediData),
  submitEDI: (ediData) => mainAPI.post('/edi/submit', ediData),
  getEDIStatus: (ediReference) => mainAPI.get(`/edi/status/${ediReference}`),
  getSampleEDI: () => mainAPI.get('/edi/sample'),
  
  // Parcel management
  getAllParcels: (params = {}) => mainAPI.get('/parcels', { params }),
  getParcelById: (parcelId) => mainAPI.get(`/parcels/${parcelId}`),
  updateParcelStatus: (parcelId, statusData) => mainAPI.put(`/parcels/${parcelId}/status`, statusData),
  searchParcels: (searchParams) => mainAPI.get('/parcels/search', { params: searchParams }),
  getParcelsByStatus: (status) => mainAPI.get(`/parcels/status/${status}`),
  
  // Tracking
  getTrackingHistory: (parcelId) => mainAPI.get(`/parcels/${parcelId}/tracking`),
  addTrackingEvent: (parcelId, eventData) => mainAPI.post(`/parcels/${parcelId}/tracking`, eventData),
};

// Admin API
export const adminAPI = {
  // User management
  getAllUsers: (params = {}) => mainAPI.get('/admin/users', { params }),
  getUserById: (userId) => mainAPI.get(`/admin/users/${userId}`),
  updateUser: (userId, userData) => mainAPI.put(`/admin/users/${userId}`, userData),
  deleteUser: (userId) => mainAPI.delete(`/admin/users/${userId}`),
  
  // System statistics
  getSystemStats: () => mainAPI.get('/admin/stats'),
  getReports: (reportType, params = {}) => mainAPI.get(`/admin/reports/${reportType}`, { params }),
  
  // Configuration
  getSystemConfig: () => mainAPI.get('/admin/config'),
  updateSystemConfig: (config) => mainAPI.put('/admin/config', config),
};

// Public API (no authentication required)
export const publicAPI = {
  trackParcel: (parcelId) => mainAPI.get(`/public/track/${parcelId}`),
  getHealth: () => mainAPI.get('/health'),
  getServiceInfo: () => mainAPI.get('/public/info'),
};

// Utility functions
export const apiUtils = {
  // Handle file upload
  uploadFile: async (file, endpoint, onProgress) => {
    const formData = new FormData();
    formData.append('file', file);
    
    return mainAPI.post(endpoint, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        if (onProgress) {
          const percentCompleted = Math.round(
            (progressEvent.loaded * 100) / progressEvent.total
          );
          onProgress(percentCompleted);
        }
      },
    });
  },
  
  // Download file
  downloadFile: async (url, filename) => {
    try {
      const response = await mainAPI.get(url, {
        responseType: 'blob',
      });
      
      const blob = new Blob([response.data]);
      const downloadUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(downloadUrl);
    } catch (error) {
      toast.error('Failed to download file');
      throw error;
    }
  },
  
  // Format error message
  getErrorMessage: (error) => {
    if (error.response?.data?.message) {
      return error.response.data.message;
    } else if (error.message) {
      return error.message;
    } else {
      return 'An unexpected error occurred';
    }
  },
  
  // Check if error is network related
  isNetworkError: (error) => {
    return !error.response && error.code === 'NETWORK_ERROR';
  },
};

// Export API instances for direct use if needed
export { mainAPI, customerAPI };

export default {
  auth: authAPI,
  customer: customerAPI,
  parcel: parcelAPI,
  admin: adminAPI,
  public: publicAPI,
  utils: apiUtils,
};
