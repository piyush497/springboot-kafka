import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Grid,
  Card,
  CardContent,
  Typography,
  TextField,
  Button,
  Box,
  Stepper,
  Step,
  StepLabel,
  Paper,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Alert,
  CircularProgress,
  Divider,
} from '@mui/material';
import {
  Person,
  LocationOn,
  Inventory,
  Send,
  ArrowBack,
  ArrowForward,
} from '@mui/icons-material';
import { useForm, Controller } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import { useMutation, useQueryClient } from 'react-query';
import { toast } from 'react-toastify';
import { customerAPI } from '../../services/api';
import { useAuth } from '../../contexts/AuthContext';

const steps = ['Sender Details', 'Recipient Details', 'Parcel Information', 'Review & Submit'];

const schema = yup.object({
  sender: yup.object({
    name: yup.string().required('Sender name is required'),
    email: yup.string().email('Invalid email').required('Sender email is required'),
    phone: yup.string().required('Sender phone is required'),
  }),
  recipient: yup.object({
    name: yup.string().required('Recipient name is required'),
    email: yup.string().email('Invalid email').required('Recipient email is required'),
    phone: yup.string().required('Recipient phone is required'),
  }),
  pickupAddress: yup.object({
    streetAddress: yup.string().required('Street address is required'),
    city: yup.string().required('City is required'),
    state: yup.string().required('State is required'),
    postalCode: yup.string().required('Postal code is required'),
    country: yup.string().required('Country is required'),
  }),
  deliveryAddress: yup.object({
    streetAddress: yup.string().required('Street address is required'),
    city: yup.string().required('City is required'),
    state: yup.string().required('State is required'),
    postalCode: yup.string().required('Postal code is required'),
    country: yup.string().required('Country is required'),
  }),
  parcelDetails: yup.object({
    description: yup.string().required('Description is required'),
    weight: yup.number().positive('Weight must be positive').required('Weight is required'),
    dimensions: yup.string().required('Dimensions are required'),
  }),
  serviceOptions: yup.object({
    priority: yup.string().required('Priority is required'),
  }),
});

const ParcelRegistration = () => {
  const [activeStep, setActiveStep] = useState(0);
  const [error, setError] = useState('');
  const { user } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const {
    control,
    handleSubmit,
    formState: { errors },
    watch,
    setValue,
    trigger,
  } = useForm({
    resolver: yupResolver(schema),
    defaultValues: {
      sender: {
        name: user?.firstName ? `${user.firstName} ${user.lastName}` : '',
        email: user?.email || '',
        phone: user?.phone || '',
      },
      recipient: {
        name: '',
        email: '',
        phone: '',
      },
      pickupAddress: {
        streetAddress: '',
        city: '',
        state: '',
        postalCode: '',
        country: 'USA',
      },
      deliveryAddress: {
        streetAddress: '',
        city: '',
        state: '',
        postalCode: '',
        country: 'USA',
      },
      parcelDetails: {
        description: '',
        weight: '',
        dimensions: '',
      },
      serviceOptions: {
        priority: 'STANDARD',
      },
    },
  });

  const registerParcelMutation = useMutation(customerAPI.registerParcel, {
    onSuccess: (response) => {
      if (response.data.success) {
        toast.success('Parcel registered successfully!');
        queryClient.invalidateQueries('customerDashboard');
        queryClient.invalidateQueries('recentParcels');
        navigate('/parcels/my');
      }
    },
    onError: (error) => {
      const message = error.response?.data?.message || 'Failed to register parcel';
      setError(message);
      toast.error(message);
    },
  });

  const handleNext = async () => {
    const fieldsToValidate = getFieldsForStep(activeStep);
    const isStepValid = await trigger(fieldsToValidate);
    
    if (isStepValid) {
      setActiveStep((prevActiveStep) => prevActiveStep + 1);
      setError('');
    }
  };

  const handleBack = () => {
    setActiveStep((prevActiveStep) => prevActiveStep - 1);
    setError('');
  };

  const onSubmit = (data) => {
    const parcelData = {
      ...data,
      ediReference: `CUST-${user?.id}-${Date.now()}`,
    };
    registerParcelMutation.mutate(parcelData);
  };

  const getFieldsForStep = (step) => {
    switch (step) {
      case 0:
        return ['sender'];
      case 1:
        return ['recipient', 'pickupAddress'];
      case 2:
        return ['deliveryAddress', 'parcelDetails', 'serviceOptions'];
      default:
        return [];
    }
  };

  const copyToDelivery = () => {
    const pickupAddress = watch('pickupAddress');
    setValue('deliveryAddress', pickupAddress);
  };

  const renderStepContent = (step) => {
    switch (step) {
      case 0:
        return (
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <Typography variant="h6" gutterBottom>
                <Person sx={{ mr: 1, verticalAlign: 'middle' }} />
                Sender Information
              </Typography>
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="sender.name"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    fullWidth
                    label="Full Name"
                    error={!!errors.sender?.name}
                    helperText={errors.sender?.name?.message}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="sender.email"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    fullWidth
                    label="Email Address"
                    type="email"
                    error={!!errors.sender?.email}
                    helperText={errors.sender?.email?.message}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12}>
              <Controller
                name="sender.phone"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    fullWidth
                    label="Phone Number"
                    error={!!errors.sender?.phone}
                    helperText={errors.sender?.phone?.message}
                  />
                )}
              />
            </Grid>
          </Grid>
        );

      case 1:
        return (
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <Typography variant="h6" gutterBottom>
                <Person sx={{ mr: 1, verticalAlign: 'middle' }} />
                Recipient Information
              </Typography>
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="recipient.name"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    fullWidth
                    label="Full Name"
                    error={!!errors.recipient?.name}
                    helperText={errors.recipient?.name?.message}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="recipient.email"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    fullWidth
                    label="Email Address"
                    type="email"
                    error={!!errors.recipient?.email}
                    helperText={errors.recipient?.email?.message}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12}>
              <Controller
                name="recipient.phone"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    fullWidth
                    label="Phone Number"
                    error={!!errors.recipient?.phone}
                    helperText={errors.recipient?.phone?.message}
                  />
                )}
              />
            </Grid>

            <Grid item xs={12}>
              <Divider sx={{ my: 2 }} />
              <Typography variant="h6" gutterBottom>
                <LocationOn sx={{ mr: 1, verticalAlign: 'middle' }} />
                Pickup Address
              </Typography>
            </Grid>
            <Grid item xs={12}>
              <Controller
                name="pickupAddress.streetAddress"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    fullWidth
                    label="Street Address"
                    error={!!errors.pickupAddress?.streetAddress}
                    helperText={errors.pickupAddress?.streetAddress?.message}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="pickupAddress.city"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    fullWidth
                    label="City"
                    error={!!errors.pickupAddress?.city}
                    helperText={errors.pickupAddress?.city?.message}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="pickupAddress.state"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    fullWidth
                    label="State/Province"
                    error={!!errors.pickupAddress?.state}
                    helperText={errors.pickupAddress?.state?.message}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="pickupAddress.postalCode"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    fullWidth
                    label="Postal Code"
                    error={!!errors.pickupAddress?.postalCode}
                    helperText={errors.pickupAddress?.postalCode?.message}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="pickupAddress.country"
                control={control}
                render={({ field }) => (
                  <FormControl fullWidth>
                    <InputLabel>Country</InputLabel>
                    <Select {...field} label="Country">
                      <MenuItem value="USA">United States</MenuItem>
                      <MenuItem value="CAN">Canada</MenuItem>
                      <MenuItem value="MEX">Mexico</MenuItem>
                    </Select>
                  </FormControl>
                )}
              />
            </Grid>
          </Grid>
        );

      case 2:
        return (
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="h6" gutterBottom>
                  <LocationOn sx={{ mr: 1, verticalAlign: 'middle' }} />
                  Delivery Address
                </Typography>
                <Button variant="outlined" size="small" onClick={copyToDelivery}>
                  Copy from Pickup
                </Button>
              </Box>
            </Grid>
            <Grid item xs={12}>
              <Controller
                name="deliveryAddress.streetAddress"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    fullWidth
                    label="Street Address"
                    error={!!errors.deliveryAddress?.streetAddress}
                    helperText={errors.deliveryAddress?.streetAddress?.message}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="deliveryAddress.city"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    fullWidth
                    label="City"
                    error={!!errors.deliveryAddress?.city}
                    helperText={errors.deliveryAddress?.city?.message}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="deliveryAddress.state"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    fullWidth
                    label="State/Province"
                    error={!!errors.deliveryAddress?.state}
                    helperText={errors.deliveryAddress?.state?.message}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="deliveryAddress.postalCode"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    fullWidth
                    label="Postal Code"
                    error={!!errors.deliveryAddress?.postalCode}
                    helperText={errors.deliveryAddress?.postalCode?.message}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="deliveryAddress.country"
                control={control}
                render={({ field }) => (
                  <FormControl fullWidth>
                    <InputLabel>Country</InputLabel>
                    <Select {...field} label="Country">
                      <MenuItem value="USA">United States</MenuItem>
                      <MenuItem value="CAN">Canada</MenuItem>
                      <MenuItem value="MEX">Mexico</MenuItem>
                    </Select>
                  </FormControl>
                )}
              />
            </Grid>

            <Grid item xs={12}>
              <Divider sx={{ my: 2 }} />
              <Typography variant="h6" gutterBottom>
                <Inventory sx={{ mr: 1, verticalAlign: 'middle' }} />
                Parcel Details
              </Typography>
            </Grid>
            <Grid item xs={12}>
              <Controller
                name="parcelDetails.description"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    fullWidth
                    label="Package Description"
                    multiline
                    rows={3}
                    error={!!errors.parcelDetails?.description}
                    helperText={errors.parcelDetails?.description?.message}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="parcelDetails.weight"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    fullWidth
                    label="Weight (kg)"
                    type="number"
                    inputProps={{ step: 0.1, min: 0 }}
                    error={!!errors.parcelDetails?.weight}
                    helperText={errors.parcelDetails?.weight?.message}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="parcelDetails.dimensions"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    fullWidth
                    label="Dimensions (L x W x H cm)"
                    placeholder="e.g., 30x20x15"
                    error={!!errors.parcelDetails?.dimensions}
                    helperText={errors.parcelDetails?.dimensions?.message}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12}>
              <Controller
                name="serviceOptions.priority"
                control={control}
                render={({ field }) => (
                  <FormControl fullWidth>
                    <InputLabel>Priority</InputLabel>
                    <Select {...field} label="Priority">
                      <MenuItem value="STANDARD">Standard (3-5 business days)</MenuItem>
                      <MenuItem value="EXPRESS">Express (1-2 business days)</MenuItem>
                      <MenuItem value="URGENT">Urgent (Same day)</MenuItem>
                    </Select>
                  </FormControl>
                )}
              />
            </Grid>
          </Grid>
        );

      case 3:
        const formData = watch();
        return (
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <Typography variant="h6" gutterBottom>
                Review Your Parcel Information
              </Typography>
            </Grid>
            
            <Grid item xs={12} md={6}>
              <Paper sx={{ p: 2 }}>
                <Typography variant="subtitle1" gutterBottom>
                  Sender
                </Typography>
                <Typography variant="body2">
                  {formData.sender?.name}<br />
                  {formData.sender?.email}<br />
                  {formData.sender?.phone}
                </Typography>
              </Paper>
            </Grid>

            <Grid item xs={12} md={6}>
              <Paper sx={{ p: 2 }}>
                <Typography variant="subtitle1" gutterBottom>
                  Recipient
                </Typography>
                <Typography variant="body2">
                  {formData.recipient?.name}<br />
                  {formData.recipient?.email}<br />
                  {formData.recipient?.phone}
                </Typography>
              </Paper>
            </Grid>

            <Grid item xs={12} md={6}>
              <Paper sx={{ p: 2 }}>
                <Typography variant="subtitle1" gutterBottom>
                  Pickup Address
                </Typography>
                <Typography variant="body2">
                  {formData.pickupAddress?.streetAddress}<br />
                  {formData.pickupAddress?.city}, {formData.pickupAddress?.state} {formData.pickupAddress?.postalCode}<br />
                  {formData.pickupAddress?.country}
                </Typography>
              </Paper>
            </Grid>

            <Grid item xs={12} md={6}>
              <Paper sx={{ p: 2 }}>
                <Typography variant="subtitle1" gutterBottom>
                  Delivery Address
                </Typography>
                <Typography variant="body2">
                  {formData.deliveryAddress?.streetAddress}<br />
                  {formData.deliveryAddress?.city}, {formData.deliveryAddress?.state} {formData.deliveryAddress?.postalCode}<br />
                  {formData.deliveryAddress?.country}
                </Typography>
              </Paper>
            </Grid>

            <Grid item xs={12}>
              <Paper sx={{ p: 2 }}>
                <Typography variant="subtitle1" gutterBottom>
                  Parcel Details
                </Typography>
                <Typography variant="body2">
                  <strong>Description:</strong> {formData.parcelDetails?.description}<br />
                  <strong>Weight:</strong> {formData.parcelDetails?.weight} kg<br />
                  <strong>Dimensions:</strong> {formData.parcelDetails?.dimensions} cm<br />
                  <strong>Priority:</strong> {formData.serviceOptions?.priority}
                </Typography>
              </Paper>
            </Grid>
          </Grid>
        );

      default:
        return 'Unknown step';
    }
  };

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Register New Parcel
      </Typography>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Stepper activeStep={activeStep} alternativeLabel>
            {steps.map((label) => (
              <Step key={label}>
                <StepLabel>{label}</StepLabel>
              </Step>
            ))}
          </Stepper>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          {error && (
            <Alert severity="error" sx={{ mb: 3 }}>
              {error}
            </Alert>
          )}

          <form onSubmit={handleSubmit(onSubmit)}>
            {renderStepContent(activeStep)}

            <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 4 }}>
              <Button
                disabled={activeStep === 0}
                onClick={handleBack}
                startIcon={<ArrowBack />}
              >
                Back
              </Button>

              {activeStep === steps.length - 1 ? (
                <Button
                  type="submit"
                  variant="contained"
                  disabled={registerParcelMutation.isLoading}
                  startIcon={registerParcelMutation.isLoading ? <CircularProgress size={20} /> : <Send />}
                >
                  {registerParcelMutation.isLoading ? 'Registering...' : 'Register Parcel'}
                </Button>
              ) : (
                <Button
                  variant="contained"
                  onClick={handleNext}
                  endIcon={<ArrowForward />}
                >
                  Next
                </Button>
              )}
            </Box>
          </form>
        </CardContent>
      </Card>
    </Box>
  );
};

export default ParcelRegistration;
