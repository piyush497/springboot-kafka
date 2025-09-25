import React, { useState } from 'react';
import { useParams } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Typography,
  TextField,
  Button,
  Timeline,
  TimelineItem,
  TimelineSeparator,
  TimelineConnector,
  TimelineContent,
  TimelineDot,
  Paper,
  Grid,
  Chip,
  Alert,
  CircularProgress,
  Divider,
} from '@mui/material';
import {
  Search,
  LocalShipping,
  Inventory,
  CheckCircle,
  Schedule,
  Error,
  LocationOn,
  Person,
  Phone,
  Email,
} from '@mui/icons-material';
import { useQuery } from 'react-query';
import { customerAPI } from '../../services/api';
import LoadingSpinner from '../Common/LoadingSpinner';

const ParcelTracking = () => {
  const { parcelId: urlParcelId } = useParams();
  const [parcelId, setParcelId] = useState(urlParcelId || '');
  const [searchParcelId, setSearchParcelId] = useState(urlParcelId || '');

  const { data: trackingData, isLoading, error, refetch } = useQuery(
    ['parcelTracking', searchParcelId],
    () => customerAPI.trackParcel(searchParcelId),
    {
      enabled: !!searchParcelId,
      refetchInterval: 30000, // Refresh every 30 seconds
    }
  );

  const handleSearch = () => {
    if (parcelId.trim()) {
      setSearchParcelId(parcelId.trim());
    }
  };

  const handleKeyPress = (event) => {
    if (event.key === 'Enter') {
      handleSearch();
    }
  };

  const getStatusIcon = (eventType) => {
    const icons = {
      REGISTERED: <Inventory />,
      PICKUP_SCHEDULED: <Schedule />,
      PICKED_UP: <LocalShipping />,
      IN_TRANSIT: <LocalShipping />,
      LOADED_IN_TRUCK: <LocalShipping />,
      OUT_FOR_DELIVERY: <LocalShipping />,
      DELIVERED: <CheckCircle />,
      FAILED_DELIVERY: <Error />,
      RETURNED_TO_FACILITY: <Error />,
      CANCELLED: <Error />,
    };
    return icons[eventType] || <LocalShipping />;
  };

  const getStatusColor = (eventType) => {
    const colors = {
      REGISTERED: 'info',
      PICKUP_SCHEDULED: 'warning',
      PICKED_UP: 'primary',
      IN_TRANSIT: 'primary',
      LOADED_IN_TRUCK: 'secondary',
      OUT_FOR_DELIVERY: 'warning',
      DELIVERED: 'success',
      FAILED_DELIVERY: 'error',
      RETURNED_TO_FACILITY: 'error',
      CANCELLED: 'error',
    };
    return colors[eventType] || 'default';
  };

  const getStatusDescription = (eventType) => {
    const descriptions = {
      REGISTERED: 'Parcel registered in the system',
      PICKUP_SCHEDULED: 'Pickup has been scheduled',
      PICKED_UP: 'Parcel has been picked up',
      IN_TRANSIT: 'Parcel is in transit',
      LOADED_IN_TRUCK: 'Parcel loaded in delivery truck',
      OUT_FOR_DELIVERY: 'Out for delivery',
      DELIVERED: 'Parcel has been delivered',
      FAILED_DELIVERY: 'Delivery attempt failed',
      RETURNED_TO_FACILITY: 'Returned to facility',
      CANCELLED: 'Parcel has been cancelled',
    };
    return descriptions[eventType] || 'Status update';
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleString();
  };

  const parcel = trackingData?.data?.parcel;
  const trackingHistory = trackingData?.data?.trackingHistory || [];

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Track Your Parcel
      </Typography>

      {/* Search Section */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} md={8}>
              <TextField
                fullWidth
                label="Enter Parcel ID"
                value={parcelId}
                onChange={(e) => setParcelId(e.target.value)}
                onKeyPress={handleKeyPress}
                placeholder="e.g., PKG-1234567890-ABCD1234"
                InputProps={{
                  startAdornment: <Search sx={{ mr: 1, color: 'action.active' }} />,
                }}
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <Button
                fullWidth
                variant="contained"
                onClick={handleSearch}
                disabled={!parcelId.trim() || isLoading}
                startIcon={isLoading ? <CircularProgress size={20} /> : <Search />}
              >
                {isLoading ? 'Searching...' : 'Track Parcel'}
              </Button>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* Error State */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error.response?.data?.message || 'Failed to track parcel. Please check the parcel ID and try again.'}
        </Alert>
      )}

      {/* Loading State */}
      {isLoading && searchParcelId && (
        <Box sx={{ display: 'flex', justifyContent: 'center', my: 4 }}>
          <LoadingSpinner />
        </Box>
      )}

      {/* Tracking Results */}
      {trackingData?.data && parcel && (
        <Grid container spacing={3}>
          {/* Parcel Information */}
          <Grid item xs={12} md={4}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Parcel Information
                </Typography>
                
                <Box sx={{ mb: 2 }}>
                  <Typography variant="subtitle2" color="textSecondary">
                    Parcel ID
                  </Typography>
                  <Typography variant="body1" sx={{ fontFamily: 'monospace' }}>
                    {parcel.parcelId}
                  </Typography>
                </Box>

                <Box sx={{ mb: 2 }}>
                  <Typography variant="subtitle2" color="textSecondary">
                    Current Status
                  </Typography>
                  <Chip
                    label={parcel.status.replace('_', ' ')}
                    color={getStatusColor(parcel.status)}
                    icon={getStatusIcon(parcel.status)}
                    sx={{ mt: 0.5 }}
                  />
                </Box>

                <Box sx={{ mb: 2 }}>
                  <Typography variant="subtitle2" color="textSecondary">
                    Priority
                  </Typography>
                  <Chip
                    label={parcel.priority}
                    color={parcel.priority === 'URGENT' ? 'error' : parcel.priority === 'EXPRESS' ? 'warning' : 'default'}
                    size="small"
                    sx={{ mt: 0.5 }}
                  />
                </Box>

                {parcel.description && (
                  <Box sx={{ mb: 2 }}>
                    <Typography variant="subtitle2" color="textSecondary">
                      Description
                    </Typography>
                    <Typography variant="body2">
                      {parcel.description}
                    </Typography>
                  </Box>
                )}

                <Box sx={{ mb: 2 }}>
                  <Typography variant="subtitle2" color="textSecondary">
                    Created
                  </Typography>
                  <Typography variant="body2">
                    {formatDate(parcel.createdAt)}
                  </Typography>
                </Box>

                {parcel.estimatedDeliveryDate && (
                  <Box sx={{ mb: 2 }}>
                    <Typography variant="subtitle2" color="textSecondary">
                      Estimated Delivery
                    </Typography>
                    <Typography variant="body2">
                      {formatDate(parcel.estimatedDeliveryDate)}
                    </Typography>
                  </Box>
                )}

                {parcel.actualDeliveryDate && (
                  <Box sx={{ mb: 2 }}>
                    <Typography variant="subtitle2" color="textSecondary">
                      Delivered On
                    </Typography>
                    <Typography variant="body2" color="success.main">
                      {formatDate(parcel.actualDeliveryDate)}
                    </Typography>
                  </Box>
                )}
              </CardContent>
            </Card>

            {/* Delivery Address */}
            <Card sx={{ mt: 2 }}>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  <LocationOn sx={{ mr: 1, verticalAlign: 'middle' }} />
                  Delivery Address
                </Typography>
                <Typography variant="body2">
                  {parcel.deliveryAddress?.streetAddress}<br />
                  {parcel.deliveryAddress?.city}, {parcel.deliveryAddress?.state} {parcel.deliveryAddress?.postalCode}<br />
                  {parcel.deliveryAddress?.country}
                </Typography>
              </CardContent>
            </Card>

            {/* Recipient Information */}
            {parcel.recipient && (
              <Card sx={{ mt: 2 }}>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    <Person sx={{ mr: 1, verticalAlign: 'middle' }} />
                    Recipient
                  </Typography>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                    <Person sx={{ mr: 1, fontSize: 16, color: 'text.secondary' }} />
                    <Typography variant="body2">
                      {parcel.recipient.name}
                    </Typography>
                  </Box>
                  <Box sx={{ display: 'flex', alignItems: 'center' }}>
                    <Phone sx={{ mr: 1, fontSize: 16, color: 'text.secondary' }} />
                    <Typography variant="body2">
                      {parcel.recipient.phone}
                    </Typography>
                  </Box>
                </CardContent>
              </Card>
            )}
          </Grid>

          {/* Tracking Timeline */}
          <Grid item xs={12} md={8}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Tracking History
                </Typography>
                
                {trackingHistory.length > 0 ? (
                  <Timeline>
                    {trackingHistory.map((event, index) => (
                      <TimelineItem key={event.id || index}>
                        <TimelineSeparator>
                          <TimelineDot color={getStatusColor(event.eventType)}>
                            {getStatusIcon(event.eventType)}
                          </TimelineDot>
                          {index < trackingHistory.length - 1 && <TimelineConnector />}
                        </TimelineSeparator>
                        <TimelineContent>
                          <Paper sx={{ p: 2, mb: 2 }}>
                            <Typography variant="subtitle1" gutterBottom>
                              {getStatusDescription(event.eventType)}
                            </Typography>
                            <Typography variant="body2" color="textSecondary" gutterBottom>
                              {event.description}
                            </Typography>
                            
                            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mt: 1 }}>
                              <Chip
                                label={formatDate(event.eventTimestamp)}
                                size="small"
                                variant="outlined"
                              />
                              {event.location && (
                                <Chip
                                  label={event.location}
                                  size="small"
                                  variant="outlined"
                                  icon={<LocationOn />}
                                />
                              )}
                              {event.vehicleId && (
                                <Chip
                                  label={`Vehicle: ${event.vehicleId}`}
                                  size="small"
                                  variant="outlined"
                                />
                              )}
                              {event.driverName && (
                                <Chip
                                  label={`Driver: ${event.driverName}`}
                                  size="small"
                                  variant="outlined"
                                />
                              )}
                            </Box>

                            {event.additionalInfo && (
                              <Typography variant="body2" sx={{ mt: 1, fontStyle: 'italic' }}>
                                {event.additionalInfo}
                              </Typography>
                            )}
                          </Paper>
                        </TimelineContent>
                      </TimelineItem>
                    ))}
                  </Timeline>
                ) : (
                  <Box sx={{ textAlign: 'center', py: 4 }}>
                    <Typography color="textSecondary">
                      No tracking events found for this parcel.
                    </Typography>
                  </Box>
                )}
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {/* No Results State */}
      {!isLoading && !error && searchParcelId && !trackingData?.data && (
        <Card>
          <CardContent>
            <Box sx={{ textAlign: 'center', py: 4 }}>
              <Search sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
              <Typography variant="h6" color="textSecondary" gutterBottom>
                No parcel found
              </Typography>
              <Typography color="textSecondary">
                Please check the parcel ID and try again.
              </Typography>
            </Box>
          </CardContent>
        </Card>
      )}

      {/* Initial State */}
      {!searchParcelId && (
        <Card>
          <CardContent>
            <Box sx={{ textAlign: 'center', py: 4 }}>
              <LocalShipping sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
              <Typography variant="h6" color="textSecondary" gutterBottom>
                Enter a parcel ID to track your shipment
              </Typography>
              <Typography color="textSecondary">
                You can find your parcel ID in your registration confirmation email or receipt.
              </Typography>
            </Box>
          </CardContent>
        </Card>
      )}
    </Box>
  );
};

export default ParcelTracking;
