import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { QueryClient, QueryClientProvider } from 'react-query';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

// Context
import { AuthProvider } from './contexts/AuthContext';
import { useAuth } from './contexts/AuthContext';

// Components
import Layout from './components/Layout/Layout';
import Login from './components/Auth/Login';
import Register from './components/Auth/Register';
import CustomerDashboard from './components/Customer/CustomerDashboard';
import ParcelRegistration from './components/Customer/ParcelRegistration';
import ParcelTracking from './components/Customer/ParcelTracking';
import ParcelList from './components/Customer/ParcelList';
import OperatorDashboard from './components/Operator/OperatorDashboard';
import AdminDashboard from './components/Admin/AdminDashboard';
import PublicTracking from './components/Public/PublicTracking';
import ProtectedRoute from './components/Common/ProtectedRoute';
import LoadingSpinner from './components/Common/LoadingSpinner';

// Create theme
const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2',
      light: '#42a5f5',
      dark: '#1565c0',
    },
    secondary: {
      main: '#dc004e',
      light: '#ff5983',
      dark: '#9a0036',
    },
    background: {
      default: '#f5f5f5',
    },
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
    h4: {
      fontWeight: 600,
    },
    h5: {
      fontWeight: 500,
    },
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          borderRadius: 8,
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 12,
          boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
        },
      },
    },
  },
});

// Create QueryClient
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      staleTime: 5 * 60 * 1000, // 5 minutes
    },
  },
});

function AppRoutes() {
  const { user, loading } = useAuth();

  if (loading) {
    return <LoadingSpinner />;
  }

  return (
    <Routes>
      {/* Public Routes */}
      <Route path="/login" element={!user ? <Login /> : <Navigate to="/dashboard" />} />
      <Route path="/register" element={!user ? <Register /> : <Navigate to="/dashboard" />} />
      <Route path="/track" element={<PublicTracking />} />
      
      {/* Protected Routes */}
      <Route path="/" element={<Layout />}>
        <Route index element={<Navigate to="/dashboard" />} />
        
        {/* Customer Routes */}
        <Route 
          path="/dashboard" 
          element={
            <ProtectedRoute requiredRole="CUSTOMER">
              <CustomerDashboard />
            </ProtectedRoute>
          } 
        />
        <Route 
          path="/parcels/register" 
          element={
            <ProtectedRoute requiredRole="CUSTOMER">
              <ParcelRegistration />
            </ProtectedRoute>
          } 
        />
        <Route 
          path="/parcels/my" 
          element={
            <ProtectedRoute requiredRole="CUSTOMER">
              <ParcelList />
            </ProtectedRoute>
          } 
        />
        <Route 
          path="/parcels/track/:parcelId?" 
          element={
            <ProtectedRoute requiredRole="CUSTOMER">
              <ParcelTracking />
            </ProtectedRoute>
          } 
        />
        
        {/* Operator Routes */}
        <Route 
          path="/operator/dashboard" 
          element={
            <ProtectedRoute requiredRole="OPERATOR">
              <OperatorDashboard />
            </ProtectedRoute>
          } 
        />
        
        {/* Admin Routes */}
        <Route 
          path="/admin/dashboard" 
          element={
            <ProtectedRoute requiredRole="ADMIN">
              <AdminDashboard />
            </ProtectedRoute>
          } 
        />
      </Route>
      
      {/* Catch all route */}
      <Route path="*" element={<Navigate to="/dashboard" />} />
    </Routes>
  );
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <AuthProvider>
          <Router>
            <AppRoutes />
            <ToastContainer
              position="top-right"
              autoClose={5000}
              hideProgressBar={false}
              newestOnTop={false}
              closeOnClick
              rtl={false}
              pauseOnFocusLoss
              draggable
              pauseOnHover
              theme="light"
            />
          </Router>
        </AuthProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );
}

export default App;
