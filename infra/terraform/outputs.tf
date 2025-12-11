output "bucket_name" {
  description = "Name of the S3 bucket for photo assets."
  value       = aws_s3_bucket.photos.bucket
}

output "queue_url" {
  description = "URL of the SQS queue receiving S3 event notifications."
  value       = aws_sqs_queue.photo_events.id
}

output "queue_arn" {
  description = "ARN of the SQS queue receiving S3 event notifications."
  value       = aws_sqs_queue.photo_events.arn
}

output "dynamodb_table_name" {
  description = "Name of the DynamoDB table storing face metadata."
  value       = aws_dynamodb_table.face_metadata.name
}

output "dynamodb_table_arn" {
  description = "ARN of the DynamoDB table storing face metadata."
  value       = aws_dynamodb_table.face_metadata.arn
}

output "rekognition_selfie_collection_id" {
  description = "ID of the Rekognition collection used for selfies."
  value       = aws_rekognition_collection.selfies.collection_id
}
