terraform {
  required_version = ">= 1.6"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

locals {
  bucket_name = "race-photos-${var.environment}"
}

resource "aws_sqs_queue" "photo_events" {
  name = "race-photos-${var.environment}"

  # Defaults keep this a standard queue with no DLQ configured.
}

resource "aws_s3_bucket" "photos" {
  bucket = local.bucket_name
}

resource "aws_s3_bucket_public_access_block" "photos" {
  bucket                  = aws_s3_bucket.photos.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "photos" {
  bucket = aws_s3_bucket.photos.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "photos" {
  bucket = aws_s3_bucket.photos.id

  rule {
    id     = "expire-temp-uploads"
    status = "Enabled"

    filter {
      prefix = "temp/"
    }

    expiration {
      days = 1
    }
  }
}

data "aws_iam_policy_document" "sqs_allow_s3" {
  statement {
    sid = "AllowS3ToSend"

    principals {
      type        = "Service"
      identifiers = ["s3.amazonaws.com"]
    }

    actions = ["sqs:SendMessage"]

    resources = [aws_sqs_queue.photo_events.arn]

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values   = [aws_s3_bucket.photos.arn]
    }
  }
}

resource "aws_sqs_queue_policy" "photo_events" {
  queue_url = aws_sqs_queue.photo_events.id
  policy    = data.aws_iam_policy_document.sqs_allow_s3.json
}

resource "aws_s3_bucket_notification" "photos" {
  bucket = aws_s3_bucket.photos.id

  queue {
    queue_arn     = aws_sqs_queue.photo_events.arn
    events        = ["s3:ObjectCreated:*"]
    filter_prefix = "in/"
  }

  depends_on = [aws_sqs_queue_policy.photo_events]
}

resource "aws_dynamodb_table" "face_metadata" {
  name         = var.dynamodb_table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "faceId"

  attribute {
    name = "faceId"
    type = "S"
  }
}

resource "aws_rekognition_collection" "selfies" {
  collection_id = var.rekognition_selfie_collection_id
}
