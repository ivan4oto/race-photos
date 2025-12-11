variable "environment" {
  description = "Deployment environment name (used in resource names)."
  type        = string
  default     = "dev"
}

variable "aws_region" {
  description = "AWS region to deploy into."
  type        = string
  default     = "eu-central-1"
}

variable "dynamodb_table_name" {
  description = "DynamoDB table for face metadata."
  type        = string
  default     = "race-photos-face-metadata"
}

variable "rekognition_selfie_collection_id" {
  description = "Rekognition collection ID for selfies (used by the app for user selfies)."
  type        = string
  default     = "selfies"
}
