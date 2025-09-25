import React from 'react';
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  Avatar,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  Button,
  Paper,
} from '@mui/material';
import {
  LocalShipping,
  Assignment,
  TrendingUp,
  Warning,
  CheckCircle,
  Schedule,
} from '@mui/icons-material';
import { useQuery } from 'react-query';
import { parcelAPI } from '../../services/api';
import LoadingSpinner from '../Common/LoadingSpinner';
import ErrorMessage from '../Common/ErrorMessage';

const OperatorDashboard = () => {
  const { data: parcelsData, isLoading, error } = useQuery(
    'operatorParcels',
    () => parcelAPI.getAllParcels({ page: 0, size: 10, sortBy: 'updatedAt', sortDir: 'desc' }),
    {
      refetchInterval: 30000,
    }
  );

  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorMessage error={error} />;

  const parcels = parcelsData?.data?.parcels || [];

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

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Operator Dashboard
      </Typography>

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
                    {parcels.length}
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
                    In Transit
                  </Typography>
                  <Typography variant="h4">
                    {parcels.filter(p => ['IN_TRANSIT', 'LOADED_IN_TRUCK', 'OUT_FOR_DELIVERY'].includes(p.status)).length}
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
                    Delivered Today
                  </Typography>
                  <Typography variant="h4">
                    {parcels.filter(p => p.status === 'DELIVERED').length}
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
                <Avatar sx={{ bgcolor: 'error.main', mr: 2 }}>
                  <Warning />
                </Avatar>
                <Box>
                  <Typography color="textSecondary" gutterBottom>
                    Issues
                  </Typography>
                  <Typography variant="h4">
                    {parcels.filter(p => ['FAILED_DELIVERY', 'RETURNED'].includes(p.status)).length}
                  </Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Recent Parcels */}
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Recent Parcels
              </Typography>
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Parcel ID</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Priority</TableCell>
                      <TableCell>Destination</TableCell>
                      <TableCell>Last Updated</TableCell>
                      <TableCell>Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {parcels.slice(0, 10).map((parcel) => (
                      <TableRow key={parcel.parcelId}>
                        <TableCell sx={{ fontFamily: 'monospace' }}>
                          {parcel.parcelId}
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={parcel.status.replace('_', ' ')}
                            color={getStatusColor(parcel.status)}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={parcel.priority}
                            size="small"
                            variant="outlined"
                          />
                        </TableCell>
                        <TableCell>
                          {parcel.deliveryAddress?.city}, {parcel.deliveryAddress?.state}
                        </TableCell>
                        <TableCell>
                          {new Date(parcel.updatedAt).toLocaleDateString()}
                        </TableCell>
                        <TableCell>
                          <Button size="small" variant="outlined">
                            Update Status
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default OperatorDashboard;
