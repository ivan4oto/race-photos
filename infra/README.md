# Infrastructure (Terraform)

Simple Terraform stack for S3 + SQS notifications, DynamoDB (face metadata), and a Rekognition selfie collection.

## Prerequisites
- Terraform >= 1.6
- AWS credentials configured (env vars or profile) with rights to manage S3, SQS, DynamoDB, Rekognition.

## Key Files
- `terraform/main.tf` – resources
- `terraform/variables.tf` – inputs (`environment`, `aws_region`, `dynamodb_table_name`, `rekognition_selfie_collection_id`)
- `terraform/outputs.tf` – exported values

## Typical Workflow
```bash
cd infra/terraform

# 1) Initialize providers
terraform init

# 2) Review changes
terraform plan -var 'environment=dev'

# 3) Apply
terraform apply -var 'environment=dev'

# 4) Destroy (revert)
terraform destroy -var 'environment=dev'
```

## Importing Existing Resources
If a resource already exists, import it before apply so Terraform manages it without recreating:
```bash
terraform import aws_s3_bucket.photos race-photos-dev
terraform import aws_sqs_queue.photo_events https://sqs.eu-central-1.amazonaws.com/<account>/race-photos-dev
terraform import aws_s3_bucket_notification.photos race-photos-dev
terraform import aws_sqs_queue_policy.photo_events https://sqs.eu-central-1.amazonaws.com/<account>/race-photos-dev
terraform import aws_dynamodb_table.face_metadata race-photos-face-metadata
terraform import aws_rekognition_collection.selfies selfies
```
Then run `terraform plan` to ensure it’s a no-op before applying changes.

## Useful Commands
- Format: `terraform fmt`
- Validate: `terraform validate`
- Show state: `terraform state list`
- Targeted plan (avoid for steady state): `terraform plan -target=aws_s3_bucket.photos`

## Notes
- Bucket is `race-photos-${environment}` with SSE-S3, public access blocked, temp/ objects expire after 1 day, and `in/` create events sent to the SQS queue.
- DynamoDB table defaults to `race-photos-face-metadata` (on-demand, PK `faceId`).
- Rekognition collection defaults to `selfies`; apps may create additional collections on demand.
