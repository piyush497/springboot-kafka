import React from 'react';
import { Alert, AlertTitle, Box, Button } from '@mui/material';
import { Refresh } from '@mui/icons-material';

const ErrorMessage = ({ error, onRetry }) => {
  const getErrorMessage = () => {
    if (error?.response?.data?.message) {
      return error.response.data.message;
    } else if (error?.message) {
      return error.message;
    } else {
      return 'An unexpected error occurred. Please try again.';
    }
  };

  return (
    <Box sx={{ my: 2 }}>
      <Alert 
        severity="error" 
        action={
          onRetry && (
            <Button
              color="inherit"
              size="small"
              onClick={onRetry}
              startIcon={<Refresh />}
            >
              Retry
            </Button>
          )
        }
      >
        <AlertTitle>Error</AlertTitle>
        {getErrorMessage()}
      </Alert>
    </Box>
  );
};

export default ErrorMessage;
