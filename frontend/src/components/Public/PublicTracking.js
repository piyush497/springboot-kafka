import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Container,
  Paper,
  TextField,
  Button,
  Typography,
  Box,
  Alert,
  CircularProgress,
  Card,
  CardContent,
  Chip,
  Timeline,
  TimelineItem,
  TimelineSeparator,
  TimelineConnector,
  TimelineContent,
  TimelineDot,
} from '@mui/material';
import {
  Search,
  LocalShipping,
  Home,
  LocationOn,
  CheckCircle,
  Schedule,
  Error,
  Inventory,
} from '@mui/icons-material';
import { useQuery } from 'react-query';
import { publicAPI } from '../../services/api';

const PublicTracking = () => {
  const [parcelId, setParcelId] = useState('');
  const [searchParcelId, setSearchParcelId] = useState('');

  const { data: trackingData, isLoading, error } = useQuery(
    ['publicTracking', searchParcelId],
    () => publicAPI.trackParcel(searchParcelId),
    {
      enabled: !!searchParcelId,
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

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleString();
  };

  const parcel = trackingData?.data?.parcel;
  const trackingHistory = trackingData?.data?.trackingHistory || [];

  return (
    <Container component="main" maxWidth="md">
      <Box
        sx={{
          marginTop: 4,
          marginBottom: 4,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
        }}
      >
        {/* Header */}
        <Paper
          elevation={3}
          sx={{
            padding: 4,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            width: '100%',
            mb: 3,
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
            <LocalShipping sx={{ fontSize: 40, color: 'primary.main', mr: 1 }} />
            <Typography component="h1" variant="h4" color="primary">
              CourierPro
            </Typography>
          </Box>
          
          <Typography component="h2" variant="h5" sx={{ mb: 3 }}>
            Track Your Parcel
          </Typography>

          <Box sx={{ display: 'flex', gap: 2, width: '100%', maxWidth: 600 }}>
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
            <Button
              variant="contained"
              onClick={handleSearch}
              disabled={!parcelId.trim() || isLoading}
              startIcon={isLoading ? <CircularProgress size={20} /> : <Search />}
              sx={{ minWidth: 120 }}
            >
              {isLoading ? 'Searching...' : 'Track'}
            </Button>
          </Box>

          <Box sx={{ mt: 2, textAlign: 'center' }}>
            <Typography variant="body2" color="text.secondary">
              Have an account?{' '}
              <Link
                to="/login"
                style={{
                  color: '#1976d2',
                  textDecoration: 'none',
                  fontWeight: 500,
                }}
              >
                Sign in for more features
              </Link>
            </Typography>
          </Box>
        </Paper>

        {/* Error State */}
        {error && (
          <Alert severity="error" sx={{ width: '100%', mb: 3 }}>
            {error.response?.data?.message || 'Parcel not found. Please check the parcel ID and try again.'}
          </Alert>
        )}

        {/* Tracking Results */}
        {trackingData?.data && parcel && (
          <Paper elevation={3} sx={{ width: '100%', p: 3 }}>
            {/* Parcel Summary */}
            <Card sx={{ mb: 3 }}>
              <CardContent>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
                  <Box>
                    <Typography variant="h6" gutterBottom>
                      Parcel Information
                    </Typography>
                    <Typography variant="body2" color="textSecondary" sx={{ fontFamily: 'monospace' }}>
                      {parcel.parcelId}
                    </Typography>
                  </Box>
                  <Chip
                    label={parcel.status.replace('_', ' ')}
                    color={getStatusColor(parcel.status)}
                    icon={getStatusIcon(parcel.status)}
                  />
                </Box>

                {parcel.description && (
                  <Typography variant="body2" sx={{ mb: 2 }}>
                    <strong>Description:</strong> {parcel.description}
                  </Typography>
                )}

                <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
                  <Chip
                    label={`Priority: ${parcel.priority}`}
                    color={parcel.priority === 'URGENT' ? 'error' : parcel.priority === 'EXPRESS' ? 'warning' : 'default'}
                    size="small"
                    variant="outlined"
                  />
                  <Chip
                    label={`Created: ${formatDate(parcel.createdAt)}`}
                    size="small"
                    variant="outlined"
                  />
                  {parcel.estimatedDeliveryDate && (
                    <Chip
                      label={`Est. Delivery: ${formatDate(parcel.estimatedDeliveryDate)}`}
                      size="small"
                      variant="outlined"
                    />
                  )}
                </Box>
              </CardContent>
            </Card>

            {/* Tracking Timeline */}
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
                          {event.eventType.replace('_', ' ')}
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
                        </Box>
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
          </Paper>
        )}

        {/* No Results State */}
        {!isLoading && !error && searchParcelId && !trackingData?.data && (
          <Paper elevation={3} sx={{ width: '100%', p: 4, textAlign: 'center' }}>
            <Search sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
            <Typography variant="h6" color="textSecondary" gutterBottom>
              No parcel found
            </Typography>
            <Typography color="textSecondary">
              Please check the parcel ID and try again.
            </Typography>
          </Paper>
        )}

        {/* Initial State */}
        {!searchParcelId && (
          <Paper elevation={3} sx={{ width: '100%', p: 4, textAlign: 'center' }}>
            <LocalShipping sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
            <Typography variant="h6" color="textSecondary" gutterBottom>
              Enter a parcel ID to track your shipment
            </Typography>
            <Typography color="textSecondary" gutterBottom>
              You can find your parcel ID in your registration confirmation email or receipt.
            </Typography>
            <Button
              variant="outlined"
              startIcon={<Home />}
              component={Link}
              to="/login"
              sx={{ mt: 2 }}
            >
              Sign In for Full Features
            </Button>
          </Paper>
        )}
      </Box>
    </Container>
  );
};

export default PublicTracking;
