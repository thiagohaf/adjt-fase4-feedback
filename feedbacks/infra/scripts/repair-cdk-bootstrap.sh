#!/usr/bin/env bash
# Repara CDKToolkit em DELETE_FAILED e remove assets órfãos (S3/ECR bootstrap).
# Uso: AWS_ACCOUNT_ID=... AWS_REGION=us-east-1 ./repair-cdk-bootstrap.sh
set -euo pipefail

ACCOUNT="${AWS_ACCOUNT_ID:?AWS_ACCOUNT_ID obrigatório}"
REGION="${AWS_REGION:-us-east-1}"
BUCKET="cdk-hnb659fds-assets-${ACCOUNT}-${REGION}"
REPO="cdk-hnb659fds-container-assets-${ACCOUNT}-${REGION}"

STATUS=$(aws cloudformation describe-stacks --stack-name CDKToolkit \
  --query 'Stacks[0].StackStatus' --output text 2>/dev/null || echo "ABSENT")
echo "CDKToolkit status: $STATUS"

empty_ecr() {
  if ! aws ecr describe-repositories --repository-names "$REPO" >/dev/null 2>&1; then
    return 0
  fi
  aws ecr list-images --repository-name "$REPO" --output json > /tmp/ecr-list.json
  python3 -c '
import json, subprocess, os
repo = os.environ["REPO"]
ids = json.load(open("/tmp/ecr-list.json")).get("imageIds") or []
print(f"ECR images: {len(ids)}")
if ids:
    open("/tmp/ecr-ids.json", "w").write(json.dumps(ids))
    subprocess.check_call([
        "aws", "ecr", "batch-delete-image",
        "--repository-name", repo,
        "--image-ids", "file:///tmp/ecr-ids.json",
    ])
'
}

delete_bucket_if_present() {
  if ! aws s3api head-bucket --bucket "$BUCKET" 2>/dev/null; then
    return 0
  fi
  echo "Removendo bucket $BUCKET..."
  aws s3 rm "s3://${BUCKET}" --recursive || true
  python3 -c '
import json, subprocess, os
bucket = os.environ["BUCKET"]
while True:
    out = subprocess.check_output(
        ["aws", "s3api", "list-object-versions", "--bucket", bucket, "--output", "json"],
        text=True,
    )
    d = json.loads(out)
    objs = [{"Key": v["Key"], "VersionId": v["VersionId"]} for v in (d.get("Versions") or [])]
    objs += [{"Key": m["Key"], "VersionId": m["VersionId"]} for m in (d.get("DeleteMarkers") or [])]
    if not objs:
        break
    chunk = objs[:900]
    open("/tmp/del.json", "w").write(json.dumps({"Objects": chunk, "Quiet": True}))
    subprocess.check_call(
        ["aws", "s3api", "delete-objects", "--bucket", bucket, "--delete", "file:///tmp/del.json"]
    )
subprocess.check_call(["aws", "s3api", "delete-bucket", "--bucket", bucket])
print("bucket deleted")
'
}

export BUCKET REPO

case "$STATUS" in
  CREATE_COMPLETE|UPDATE_COMPLETE)
    echo "Bootstrap OK — sem reparo."
    ;;
  ABSENT|DELETE_COMPLETE)
    delete_bucket_if_present
    empty_ecr
    aws ecr delete-repository --repository-name "$REPO" --force 2>/dev/null || true
    ;;
  DELETE_FAILED|ROLLBACK_COMPLETE|ROLLBACK_FAILED)
    empty_ecr
    aws cloudformation delete-stack --stack-name CDKToolkit --retain-resources StagingBucket \
      || aws cloudformation delete-stack --stack-name CDKToolkit
    aws cloudformation wait stack-delete-complete --stack-name CDKToolkit || true
    delete_bucket_if_present
    aws ecr delete-repository --repository-name "$REPO" --force 2>/dev/null || true
    echo "CDKToolkit limpa."
    ;;
  *)
    echo "Status $STATUS — bootstrap tentará seguir."
    ;;
esac
