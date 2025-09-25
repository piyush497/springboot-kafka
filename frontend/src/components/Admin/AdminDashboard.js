import React from 'react';
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  Avatar,
  Paper,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Divider,
} from '@mui/material';
import {
  People,
  LocalShipping,
  TrendingUp,
  Settings,
  Security,
  Storage,
  Assessment,
  Notifications,
} from '@mui/icons-material';
import { useQuery } from 'react-query';
import { adminAPI } from '../../services/api';
import LoadingSpinner from '../Common/LoadingSpinner';
import ErrorMessage from '../Common/ErrorMessage';

const AdminDashboard = () => {
  const { data: statsData, isLoading, error } = useQuery(
    'systemStats',
    adminAPI.getSystemStats,
    {
      refetchInterval: 60000,
    }
  );

  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorMessage error={error} />;

  const stats = statsData?.data || {};

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Admin Dashboard
      </Typography>

      <Grid container spacing={3}>
        {/* System Stats */}
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center' }}>
                <Avatar sx={{ bgcolor: 'primary.main', mr: 2 }}>
                  <People />
                </Avatar>
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Total Users
                  </Typography>
                  <Typography variant="h4">
                    {stats.totalUsers || 0}
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
                  <LocalShipping />
                </Avatar>
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Total Parcels
                  </Typography>
                  <Typography variant="h4">
                    {stats.totalParcels || 0}
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
                  <TrendingUp />
                </Avatar>
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    This Month
                  </Typography>
                  <Typography variant="h4">
                    {stats.parcelsThisMonth || 0}
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
                  <Assessment />
                </Avatar>
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Success Rate
                  </Typography>
                  <Typography variant="h4">
                    {stats.successRate || '0'}%
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Quick Actions */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                System Management
              </Typography>
              <List>
                <ListItem button>
                  <ListItemIcon>
                    <People />
                  </ListItemIcon>
                  <ListItemText
                    primary="User Management"
                    secondary="Manage users, roles, and permissions"
                  />
                </ListItem>
                <Divider />
                <ListItem button>
                  <ListItemIcon>
                    <Settings />
                  </ListItemIcon>
                  <ListItemText
                    primary="System Configuration"
                    secondary="Configure system settings and parameters"
                  />
                </ListItem>
                <Divider />
                <ListItem button>
                  <ListItemIcon>
                    <Security />
                  </ListItemIcon>
                  <ListItemText
                    primary="Security Settings"
                    secondary="Manage security policies and access controls"
                  />
                </ListItem>
                <Divider />
                <ListItem button>
                  <ListItemIcon>
                    <Storage />
                  </ListItemIcon>
                  <ListItemText
                    primary="Database Management"
                    secondary="Monitor database performance and health"
                  />
                </ListItem>
              </List>
            </CardContent>
          </Card>
        </Grid>

        {/* System Health */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                System Health
              </Typography>
              <List>
                <ListItem>
                  <ListItemIcon>
                    <Storage color="success" />
                  </ListItemIcon>
                  <ListItemText
                    primary="Database"
                    secondary="Operational - All connections healthy"
                  />
                </ListItem>
                <Divider />
                <ListItem>
                  <ListItemIcon>
                    <Notifications color="success" />
                  </ListItemIcon>
                  <ListItemText
                    primary="Kafka/Event Hub"
                    secondary="Operational - All topics accessible"
                  />
                </ListItem>
                <Divider />
                <ListItem>
                  <ListItemIcon>
                    <Security color="success" />
                  </ListItemIcon>
                  <ListItemText
                    primary="Authentication"
                    secondary="Operational - JWT service running"
                  />
                </ListItem>
                <Divider />
                <ListItem>
                  <ListItemIcon>
                    <Assessment color="warning" />
                  </ListItemIcon>
                  <ListItemText
                    primary="ABC Transport Integration"
                    secondary="Monitoring - Some delayed responses"
                  />
                </ListItem>
              </List>
            </CardContent>
          </Card>
        </Grid>

        {/* Recent Activity */}
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Recent System Activity
              </Typography>
              <Paper sx={{ p: 2, bgcolor: 'grey.50' }}>
                <Typography variant="body2" color="textSecondary">
                  System activity monitoring and logs would be displayed here.
                  This includes user logins, parcel registrations, status updates,
                  and system events.
                </Typography>
              </Paper>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default AdminDashboard;
