import React from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Grid,
  Card,
  CardContent,
  Typography,
  Box,
  Button,
  Avatar,
  List,
  ListItem,
  ListItemText,
  ListItemAvatar,
  Chip,
  LinearProgress,
  Paper,
} from '@mui/material';
import {
  LocalShipping,
  Add,
  Search,
  TrendingUp,
  Schedule,
  CheckCircle,
  Error,
  Inventory,
} from '@mui/icons-material';
import { useQuery } from 'react-query';
import { PieChart, Pie, Cell, ResponsiveContainer, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend } from 'recharts';
import { customerAPI } from '../../services/api';
import { useAuth } from '../../contexts/AuthContext';
import LoadingSpinner from '../Common/LoadingSpinner';
import ErrorMessage from '../Common/ErrorMessage';

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884D8'];

const CustomerDashboard = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const { data: dashboardData, isLoading, error } = useQuery(
    'customerDashboard',
    customerAPI.getDashboard,
    {
      refetchInterval: 30000, // Refresh every 30 seconds
    }
  );

  const { data: recentParcelsData } = useQuery(
    'recentParcels',
    () => customerAPI.getMyParcels({ page: 0, size: 5, sortBy: 'createdAt', sortDir: 'desc' }),
    {
      refetchInterval: 60000, // Refresh every minute
    }
  );

  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorMessage error={error} />;

  const dashboard = dashboardData?.data || {};
  const recentParcels = recentParcelsData?.data?.parcels || [];

  // Prepare chart data
  const statusData = Object.entries(dashboard.parcelsByStatus || {}).map(([status, count]) => ({
    name: status.replace('_', ' '),
    value: count,
  }));

  const monthlyData = [
    { month: 'This Month', parcels: dashboard.parcelsThisMonth || 0 },
    { month: 'Total', parcels: dashboard.totalParcels || 0 },
  ];

  const getStatusColor = (status) => {
    const colors = {
      REGISTERED: 'info',
      PICKED_UP: 'warning',
      IN_TRANSIT: 'primary',
      LOADED_IN_TRUCK: 'secondary',
      OUT_FOR_DELIVERY: 'warning',
      DELIVERED: 'success',
      FAILED_DELIVERY: 'error',
      RETURNED: 'default',
      CANCELLED: 'error',
    };
    return colors[status] || 'default';
  };

  const getStatusIcon = (status) => {
    const icons = {
      REGISTERED: <Inventory />,
      PICKED_UP: <LocalShipping />,
      IN_TRANSIT: <LocalShipping />,
      LOADED_IN_TRUCK: <LocalShipping />,
      OUT_FOR_DELIVERY: <LocalShipping />,
      DELIVERED: <CheckCircle />,
      FAILED_DELIVERY: <Error />,
      RETURNED: <Error />,
      CANCELLED: <Error />,
    };
    return icons[status] || <LocalShipping />;
  };

  return (
    <Box>
      {/* Welcome Section */}
      <Paper sx={{ p: 3, mb: 3, background: 'linear-gradient(135deg, #1976d2 0%, #42a5f5 100%)', color: 'white' }}>
        <Typography variant="h4" gutterBottom>
          Welcome back, {user?.firstName}!
        </Typography>
        <Typography variant="body1" sx={{ opacity: 0.9 }}>
          Manage your parcels and track deliveries from your dashboard
        </Typography>
      </Paper>

      <Grid container spacing={3}>
        {/* Quick Stats */}
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center' }}>
                <Avatar sx={{ bgcolor: 'primary.main', mr: 2 }}>
                  <LocalShipping />
                </Avatar>
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Total Parcels
                  </Typography>
                  <Typography variant="h4">
                    {dashboard.totalParcels || 0}
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center' }}>
                <Avatar sx={{ bgcolor: 'success.main', mr: 2 }}>
                  <CheckCircle />
                </Avatar>
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Delivered
                  </Typography>
                  <Typography variant="h4">
                    {dashboard.parcelsByStatus?.DELIVERED || 0}
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center' }}>
                <Avatar sx={{ bgcolor: 'warning.main', mr: 2 }}>
                  <Schedule />
                </Avatar>
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Active Deliveries
                  </Typography>
                  <Typography variant="h4">
                    {dashboard.activeDeliveries || 0}
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center' }}>
                <Avatar sx={{ bgcolor: 'info.main', mr: 2 }}>
                  <TrendingUp />
                </Avatar>
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    This Month
                  </Typography>
                  <Typography variant="h4">
                    {dashboard.parcelsThisMonth || 0}
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Quick Actions */}
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Quick Actions
              </Typography>
              <Grid container spacing={2}>
                <Grid item xs={12} sm={6} md={3}>
                  <Button
                    fullWidth
                    variant="contained"
                    startIcon={<Add />}
                    onClick={() => navigate('/parcels/register')}
                    sx={{ py: 1.5 }}
                  >
                    Register New Parcel
                  </Button>
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                  <Button
                    fullWidth
                    variant="outlined"
                    startIcon={<Search />}
                    onClick={() => navigate('/parcels/track')}
                    sx={{ py: 1.5 }}
                  >
                    Track Parcel
                  </Button>
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                  <Button
                    fullWidth
                    variant="outlined"
                    startIcon={<LocalShipping />}
                    onClick={() => navigate('/parcels/my')}
                    sx={{ py: 1.5 }}
                  >
                    View All Parcels
                  </Button>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </Grid>

        {/* Charts Section */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Parcel Status Distribution
              </Typography>
              {statusData.length > 0 ? (
                <ResponsiveContainer width="100%" height={300}>
                  <PieChart>
                    <Pie
                      data={statusData}
                      cx="50%"
                      cy="50%"
                      labelLine={false}
                      label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                      outerRadius={80}
                      fill="#8884d8"
                      dataKey="value"
                    >
                      {statusData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip />
                  </PieChart>
                </ResponsiveContainer>
              ) : (
                <Box sx={{ textAlign: 'center', py: 4 }}>
                  <Typography color="textSecondary">
                    No parcel data available
                  </Typography>
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Parcel Activity
              </Typography>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={monthlyData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="month" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="parcels" fill="#1976d2" />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>

        {/* Recent Parcels */}
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6">
                  Recent Parcels
                </Typography>
                <Button
                  variant="text"
                  onClick={() => navigate('/parcels/my')}
                >
                  View All
                </Button>
              </Box>
              
              {recentParcels.length > 0 ? (
                <List>
                  {recentParcels.map((parcel, index) => (
                    <ListItem
                      key={parcel.parcelId}
                      divider={index < recentParcels.length - 1}
                      sx={{ px: 0 }}
                    >
                      <ListItemAvatar>
                        <Avatar sx={{ bgcolor: 'primary.main' }}>
                          {getStatusIcon(parcel.status)}
                        </Avatar>
                      </ListItemAvatar>
                      <ListItemText
                        primary={
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Typography variant="subtitle1">
                              {parcel.parcelId}
                            </Typography>
                            <Chip
                              label={parcel.status.replace('_', ' ')}
                              color={getStatusColor(parcel.status)}
                              size="small"
                            />
                          </Box>
                        }
                        secondary={
                          <Box>
                            <Typography variant="body2" color="textSecondary">
                              To: {parcel.deliveryAddress?.city}, {parcel.deliveryAddress?.state}
                            </Typography>
                            <Typography variant="caption" color="textSecondary">
                              Created: {new Date(parcel.createdAt).toLocaleDateString()}
                            </Typography>
                          </Box>
                        }
                      />
                      <Button
                        variant="outlined"
                        size="small"
                        onClick={() => navigate(`/parcels/track/${parcel.parcelId}`)}
                      >
                        Track
                      </Button>
                    </ListItem>
                  ))}
                </List>
              ) : (
                <Box sx={{ textAlign: 'center', py: 4 }}>
                  <Typography color="textSecondary" gutterBottom>
                    No parcels found
                  </Typography>
                  <Button
                    variant="contained"
                    startIcon={<Add />}
                    onClick={() => navigate('/parcels/register')}
                  >
                    Register Your First Parcel
                  </Button>
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default CustomerDashboard;
