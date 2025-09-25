import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  Chip,
  Button,
  IconButton,
  Menu,
  MenuItem,
  FormControl,
  InputLabel,
  Select,
  TextField,
  Grid,
  Paper,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Alert,
} from '@mui/material';
import {
  Visibility,
  MoreVert,
  Search,
  FilterList,
  Refresh,
  Cancel,
  LocalShipping,
  Add,
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from 'react-query';
import { toast } from 'react-toastify';
import { customerAPI } from '../../services/api';
import LoadingSpinner from '../Common/LoadingSpinner';
import ErrorMessage from '../Common/ErrorMessage';

const ParcelList = () => {
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [statusFilter, setStatusFilter] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState('createdAt');
  const [sortDir, setSortDir] = useState('desc');
  const [anchorEl, setAnchorEl] = useState(null);
  const [selectedParcel, setSelectedParcel] = useState(null);
  const [cancelDialogOpen, setCancelDialogOpen] = useState(false);
  const [cancelReason, setCancelReason] = useState('');

  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { data: parcelsData, isLoading, error, refetch } = useQuery(
    ['myParcels', page, rowsPerPage, statusFilter, searchTerm, sortBy, sortDir],
    () => customerAPI.getMyParcels({
      page,
      size: rowsPerPage,
      status: statusFilter || undefined,
      search: searchTerm || undefined,
      sortBy,
      sortDir,
    }),
    {
      keepPreviousData: true,
      refetchInterval: 60000, // Refresh every minute
    }
  );

  const cancelParcelMutation = useMutation(
    ({ parcelId, reason }) => customerAPI.cancelParcel(parcelId, reason),
    {
      onSuccess: () => {
        toast.success('Parcel cancelled successfully');
        queryClient.invalidateQueries('myParcels');
        queryClient.invalidateQueries('customerDashboard');
        setCancelDialogOpen(false);
        setCancelReason('');
        setSelectedParcel(null);
      },
      onError: (error) => {
        toast.error(error.response?.data?.message || 'Failed to cancel parcel');
      },
    }
  );

  const handleChangePage = (event, newPage) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  const handleMenuOpen = (event, parcel) => {
    setAnchorEl(event.currentTarget);
    setSelectedParcel(parcel);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
    setSelectedParcel(null);
  };

  const handleViewDetails = () => {
    if (selectedParcel) {
      navigate(`/parcels/track/${selectedParcel.parcelId}`);
    }
    handleMenuClose();
  };

  const handleCancelParcel = () => {
    setCancelDialogOpen(true);
    handleMenuClose();
  };

  const handleCancelConfirm = () => {
    if (selectedParcel && cancelReason.trim()) {
      cancelParcelMutation.mutate({
        parcelId: selectedParcel.parcelId,
        reason: cancelReason.trim(),
      });
    }
  };

  const handleCancelDialogClose = () => {
    setCancelDialogOpen(false);
    setCancelReason('');
    setSelectedParcel(null);
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

  const canCancelParcel = (status) => {
    return ['REGISTERED', 'PICKED_UP'].includes(status);
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString();
  };

  const parcels = parcelsData?.data?.parcels || [];
  const totalElements = parcelsData?.data?.totalElements || 0;

  if (isLoading && page === 0) return <LoadingSpinner />;
  if (error) return <ErrorMessage error={error} />;

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">
          My Parcels
        </Typography>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={() => navigate('/parcels/register')}
        >
          Register New Parcel
        </Button>
      </Box>

      {/* Filters */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={6} md={3}>
              <TextField
                fullWidth
                label="Search Parcels"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                InputProps={{
                  startAdornment: <Search sx={{ mr: 1, color: 'action.active' }} />,
                }}
              />
            </Grid>
            <Grid item xs={12} sm={6} md={2}>
              <FormControl fullWidth>
                <InputLabel>Status</InputLabel>
                <Select
                  value={statusFilter}
                  label="Status"
                  onChange={(e) => setStatusFilter(e.target.value)}
                >
                  <MenuItem value="">All Statuses</MenuItem>
                  <MenuItem value="REGISTERED">Registered</MenuItem>
                  <MenuItem value="PICKED_UP">Picked Up</MenuItem>
                  <MenuItem value="IN_TRANSIT">In Transit</MenuItem>
                  <MenuItem value="LOADED_IN_TRUCK">Loaded in Truck</MenuItem>
                  <MenuItem value="OUT_FOR_DELIVERY">Out for Delivery</MenuItem>
                  <MenuItem value="DELIVERED">Delivered</MenuItem>
                  <MenuItem value="CANCELLED">Cancelled</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6} md={2}>
              <FormControl fullWidth>
                <InputLabel>Sort By</InputLabel>
                <Select
                  value={sortBy}
                  label="Sort By"
                  onChange={(e) => setSortBy(e.target.value)}
                >
                  <MenuItem value="createdAt">Created Date</MenuItem>
                  <MenuItem value="updatedAt">Last Updated</MenuItem>
                  <MenuItem value="status">Status</MenuItem>
                  <MenuItem value="priority">Priority</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6} md={2}>
              <FormControl fullWidth>
                <InputLabel>Order</InputLabel>
                <Select
                  value={sortDir}
                  label="Order"
                  onChange={(e) => setSortDir(e.target.value)}
                >
                  <MenuItem value="desc">Newest First</MenuItem>
                  <MenuItem value="asc">Oldest First</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={12} md={3}>
              <Box sx={{ display: 'flex', gap: 1 }}>
                <Button
                  variant="outlined"
                  startIcon={<FilterList />}
                  onClick={() => {
                    setStatusFilter('');
                    setSearchTerm('');
                    setSortBy('createdAt');
                    setSortDir('desc');
                  }}
                >
                  Clear Filters
                </Button>
                <Button
                  variant="outlined"
                  startIcon={<Refresh />}
                  onClick={() => refetch()}
                >
                  Refresh
                </Button>
              </Box>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* Parcels Table */}
      <Card>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Parcel ID</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Priority</TableCell>
                <TableCell>Destination</TableCell>
                <TableCell>Created</TableCell>
                <TableCell>Last Updated</TableCell>
                <TableCell align="center">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {parcels.map((parcel) => (
                <TableRow key={parcel.parcelId} hover>
                  <TableCell>
                    <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                      {parcel.parcelId}
                    </Typography>
                    {parcel.description && (
                      <Typography variant="caption" color="textSecondary">
                        {parcel.description.length > 30 
                          ? `${parcel.description.substring(0, 30)}...` 
                          : parcel.description}
                      </Typography>
                    )}
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
                      color={parcel.priority === 'URGENT' ? 'error' : parcel.priority === 'EXPRESS' ? 'warning' : 'default'}
                      size="small"
                      variant="outlined"
                    />
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2">
                      {parcel.deliveryAddress?.city}, {parcel.deliveryAddress?.state}
                    </Typography>
                    <Typography variant="caption" color="textSecondary">
                      {parcel.deliveryAddress?.country}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2">
                      {formatDate(parcel.createdAt)}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2">
                      {formatDate(parcel.updatedAt)}
                    </Typography>
                  </TableCell>
                  <TableCell align="center">
                    <Tooltip title="View Details">
                      <IconButton
                        onClick={() => navigate(`/parcels/track/${parcel.parcelId}`)}
                        size="small"
                      >
                        <Visibility />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="More Actions">
                      <IconButton
                        onClick={(e) => handleMenuOpen(e, parcel)}
                        size="small"
                      >
                        <MoreVert />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>

        {parcels.length === 0 && !isLoading && (
          <Box sx={{ textAlign: 'center', py: 4 }}>
            <LocalShipping sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
            <Typography variant="h6" color="textSecondary" gutterBottom>
              No parcels found
            </Typography>
            <Typography color="textSecondary" gutterBottom>
              {statusFilter || searchTerm 
                ? 'Try adjusting your filters or search terms.'
                : 'You haven\'t registered any parcels yet.'}
            </Typography>
            <Button
              variant="contained"
              startIcon={<Add />}
              onClick={() => navigate('/parcels/register')}
              sx={{ mt: 2 }}
            >
              Register Your First Parcel
            </Button>
          </Box>
        )}

        <TablePagination
          rowsPerPageOptions={[5, 10, 25, 50]}
          component="div"
          count={totalElements}
          rowsPerPage={rowsPerPage}
          page={page}
          onPageChange={handleChangePage}
          onRowsPerPageChange={handleChangeRowsPerPage}
        />
      </Card>

      {/* Actions Menu */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleMenuClose}
      >
        <MenuItem onClick={handleViewDetails}>
          <Visibility sx={{ mr: 1 }} />
          View Details
        </MenuItem>
        {selectedParcel && canCancelParcel(selectedParcel.status) && (
          <MenuItem onClick={handleCancelParcel}>
            <Cancel sx={{ mr: 1 }} />
            Cancel Parcel
          </MenuItem>
        )}
      </Menu>

      {/* Cancel Confirmation Dialog */}
      <Dialog
        open={cancelDialogOpen}
        onClose={handleCancelDialogClose}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Cancel Parcel</DialogTitle>
        <DialogContent>
          <Alert severity="warning" sx={{ mb: 2 }}>
            Are you sure you want to cancel this parcel? This action cannot be undone.
          </Alert>
          
          {selectedParcel && (
            <Box sx={{ mb: 2 }}>
              <Typography variant="subtitle2" color="textSecondary">
                Parcel ID
              </Typography>
              <Typography variant="body1" sx={{ fontFamily: 'monospace' }}>
                {selectedParcel.parcelId}
              </Typography>
            </Box>
          )}

          <TextField
            fullWidth
            label="Cancellation Reason"
            multiline
            rows={3}
            value={cancelReason}
            onChange={(e) => setCancelReason(e.target.value)}
            placeholder="Please provide a reason for cancellation..."
            required
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCancelDialogClose}>
            Keep Parcel
          </Button>
          <Button
            onClick={handleCancelConfirm}
            color="error"
            variant="contained"
            disabled={!cancelReason.trim() || cancelParcelMutation.isLoading}
          >
            {cancelParcelMutation.isLoading ? 'Cancelling...' : 'Cancel Parcel'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default ParcelList;
