"""
Custom Exception Classes
Defines custom exceptions for the donation fraud detection system.
"""


class FraudDetectionError(Exception):
    """Base exception class for fraud detection system."""
    pass


class DataGenerationError(FraudDetectionError):
    """Exception raised for errors during data generation."""

    def __init__(self, message: str, data_type: str = None):
        self.data_type = data_type
        self.message = message
        super().__init__(self.message)

    def __str__(self):
        if self.data_type:
            return f"DataGenerationError [{self.data_type}]: {self.message}"
        return f"DataGenerationError: {self.message}"


class ModelError(FraudDetectionError):
    """Exception raised for errors during model operations."""

    def __init__(self, message: str, model_name: str = None):
        self.model_name = model_name
        self.message = message
        super().__init__(self.message)

    def __str__(self):
        if self.model_name:
            return f"ModelError [{self.model_name}]: {self.message}"
        return f"ModelError: {self.message}"


class TrainingError(FraudDetectionError):
    """Exception raised for errors during model training."""

    def __init__(self, message: str, epoch: int = None):
        self.epoch = epoch
        self.message = message
        super().__init__(self.message)

    def __str__(self):
        if self.epoch is not None:
            return f"TrainingError [Epoch {self.epoch}]: {self.message}"
        return f"TrainingError: {self.message}"


class PredictionError(FraudDetectionError):
    """Exception raised for errors during prediction."""

    def __init__(self, message: str, transaction_id: str = None):
        self.transaction_id = transaction_id
        self.message = message
        super().__init__(self.message)

    def __str__(self):
        if self.transaction_id:
            return f"PredictionError [Transaction {self.transaction_id}]: {self.message}"
        return f"PredictionError: {self.message}"


class ConfigurationError(FraudDetectionError):
    """Exception raised for configuration-related errors."""

    def __init__(self, message: str, config_file: str = None):
        self.config_file = config_file
        self.message = message
        super().__init__(self.message)

    def __str__(self):
        if self.config_file:
            return f"ConfigurationError [{self.config_file}]: {self.message}"
        return f"ConfigurationError: {self.message}"


class DataValidationError(FraudDetectionError):
    """Exception raised for data validation errors."""

    def __init__(self, message: str, field_name: str = None):
        self.field_name = field_name
        self.message = message
        super().__init__(self.message)

    def __str__(self):
        if self.field_name:
            return f"DataValidationError [{self.field_name}]: {self.message}"
        return f"DataValidationError: {self.message}"


class FeatureEngineeringError(FraudDetectionError):
    """Exception raised for errors during feature engineering."""

    def __init__(self, message: str, feature_name: str = None):
        self.feature_name = feature_name
        self.message = message
        super().__init__(self.message)

    def __str__(self):
        if self.feature_name:
            return f"FeatureEngineeringError [{self.feature_name}]: {self.message}"
        return f"FeatureEngineeringError: {self.message}"
