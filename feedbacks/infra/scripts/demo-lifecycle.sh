#!/usr/bin/env bash
# Ciclo de vida do ambiente demo AWS (pausar / subir / apagar).
# Uso: AWS_REGION=us-east-1 ./demo-lifecycle.sh <comando>
set -euo pipefail

AWS_REGION="${AWS_REGION:-us-east-1}"
RDS_INSTANCE="${RDS_INSTANCE:-feedbacks-demo}"
RDS_DB_NAME="${RDS_DB_NAME:-feedbacks}"
RDS_MASTER_USER="${RDS_MASTER_USER:-feedbacks_admin}"
RDS_INSTANCE_CLASS="${RDS_INSTANCE_CLASS:-db.t4g.micro}"
RDS_SG_NAME="${RDS_SG_NAME:-feedbacks-rds-demo}"
RDS_SUBNET_GROUP="${RDS_SUBNET_GROUP:-feedbacks-demo}"
ECR_REPOSITORY="${ECR_REPOSITORY:-feedbacks-api}"
API_STACK="${API_STACK:-FeedbacksApiStack}"
ALERTA_STACK="${ALERTA_STACK:-FeedbacksAlertaNotificationStack}"
RELATORIO_STACK="${RELATORIO_STACK:-FeedbacksRelatorioStack}"
ECS_CLUSTER="${ECS_CLUSTER:-feedbacks-api}"
ECS_SERVICE="${ECS_SERVICE:-feedbacks-api}"
CRON_DIARIO="${CRON_DIARIO:-feedbacks-report-diario}"
CRON_SEMANAL="${CRON_SEMANAL:-feedbacks-report-semanal}"
DB_SECRET_NAME="${DB_SECRET_NAME:-feedbacks/db}"
JWT_SECRET_NAME="${JWT_SECRET_NAME:-feedbacks/jwt}"
ADMIN_EMAIL_SECRET_NAME="${ADMIN_EMAIL_SECRET_NAME:-feedbacks/adminEmail}"

export AWS_DEFAULT_REGION="$AWS_REGION"

stack_status() {
  local name="$1"
  aws cloudformation describe-stacks --stack-name "$name" \
    --query 'Stacks[0].StackStatus' --output text 2>/dev/null || echo "ABSENT"
}

wait_stack_gone() {
  local name="$1"
  local status
  status=$(stack_status "$name")
  case "$status" in
    ABSENT|DELETE_COMPLETE) echo "Stack $name ausente."; return 0 ;;
    DELETE_IN_PROGRESS)
      echo "Aguardando delete de $name..."
      aws cloudformation wait stack-delete-complete --stack-name "$name"
      ;;
    *)
      echo "Deletando stack $name (status=$status)..."
      aws cloudformation delete-stack --stack-name "$name"
      aws cloudformation wait stack-delete-complete --stack-name "$name"
      ;;
  esac
  echo "Stack $name removida."
}

rds_status() {
  aws rds describe-db-instances \
    --db-instance-identifier "$RDS_INSTANCE" \
    --query 'DBInstances[0].DBInstanceStatus' --output text 2>/dev/null || echo "ABSENT"
}

# AWS CLI não tem waiter db-instance-stopped — poll manual.
wait_rds_status() {
  local want="$1"
  local max_attempts="${2:-60}"
  local attempt=1
  local status
  while [ "$attempt" -le "$max_attempts" ]; do
    status=$(rds_status)
    echo "RDS wait ($attempt/$max_attempts): $status (alvo=$want)"
    if [ "$status" = "$want" ]; then
      return 0
    fi
    if [ "$status" = "ABSENT" ]; then
      echo "::error::RDS $RDS_INSTANCE sumiu durante wait ($want)."
      return 1
    fi
    sleep 15
    attempt=$((attempt + 1))
  done
  echo "::error::Timeout aguardando RDS=$want (último=$status)"
  return 1
}

gen_alnum() {
  local len="${1:-32}"
  openssl rand -base64 $((len * 2)) | tr -dc 'A-Za-z0-9' | head -c "$len"
}

put_json_secret() {
  local name="$1"
  local json="$2"
  if aws secretsmanager describe-secret --secret-id "$name" >/dev/null 2>&1; then
    aws secretsmanager put-secret-value --secret-id "$name" --secret-string "$json" >/dev/null
    echo "Secret $name atualizado."
  else
    aws secretsmanager create-secret --name "$name" --secret-string "$json" >/dev/null
    echo "Secret $name criado."
  fi
}

ensure_rds_network() {
  local vpc_id sg_id subnet_ids
  vpc_id=$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true \
    --query 'Vpcs[0].VpcId' --output text)
  if [ -z "$vpc_id" ] || [ "$vpc_id" = "None" ]; then
    echo "::error::VPC default não encontrada em $AWS_REGION"
    exit 1
  fi
  echo "VPC default: $vpc_id"

  sg_id=$(aws ec2 describe-security-groups \
    --filters Name=group-name,Values="$RDS_SG_NAME" Name=vpc-id,Values="$vpc_id" \
    --query 'SecurityGroups[0].GroupId' --output text 2>/dev/null || echo "None")
  if [ -z "$sg_id" ] || [ "$sg_id" = "None" ]; then
    sg_id=$(aws ec2 create-security-group \
      --group-name "$RDS_SG_NAME" \
      --description "Demo feedbacks RDS — ingress só ECS + Lambda report (harden-rds-sg)" \
      --vpc-id "$vpc_id" \
      --query 'GroupId' --output text)
    # Bootstrap temporário (seed CI / primeiro deploy); harden-rds-sg remove 0.0.0.0/0
    aws ec2 authorize-security-group-ingress \
      --group-id "$sg_id" --protocol tcp --port 5432 --cidr 0.0.0.0/0 >/dev/null || true
    echo "SG criado: $sg_id (bootstrap 0.0.0.0/0 até harden-rds-sg)"
  else
    echo "SG existente: $sg_id"
  fi
  echo "$sg_id" > /tmp/feedbacks-rds-sg-id.txt

  if aws rds describe-db-subnet-groups --db-subnet-group-name "$RDS_SUBNET_GROUP" >/dev/null 2>&1; then
    echo "Subnet group $RDS_SUBNET_GROUP OK."
  else
    subnet_ids=$(aws ec2 describe-subnets \
      --filters Name=vpc-id,Values="$vpc_id" Name=default-for-az,Values=true \
      --query 'Subnets[].SubnetId' --output text)
    if [ -z "$subnet_ids" ]; then
      subnet_ids=$(aws ec2 describe-subnets --filters Name=vpc-id,Values="$vpc_id" \
        --query 'Subnets[].SubnetId' --output text)
    fi
    # shellcheck disable=SC2086
    aws rds create-db-subnet-group \
      --db-subnet-group-name "$RDS_SUBNET_GROUP" \
      --db-subnet-group-description "Demo feedbacks RDS public subnets" \
      --subnet-ids $subnet_ids >/dev/null
    echo "Subnet group $RDS_SUBNET_GROUP criado."
  fi
}

# Autoriza ECS + Lambda report no SG do RDS e remove 0.0.0.0/0 (pós cdk deploy + seed).
harden_rds_sg() {
  local sg_id ecs_sg report_sg
  sg_id=$(aws ec2 describe-security-groups \
    --filters Name=group-name,Values="$RDS_SG_NAME" \
    --query 'SecurityGroups[0].GroupId' --output text 2>/dev/null || echo "None")
  if [ -z "$sg_id" ] || [ "$sg_id" = "None" ]; then
    echo "SG $RDS_SG_NAME ausente — nada a endurecer."
    return 0
  fi

  ecs_sg=$(aws cloudformation describe-stacks --stack-name "$API_STACK" \
    --query "Stacks[0].Outputs[?OutputKey=='ApiEcsSecurityGroupId'].OutputValue | [0]" \
    --output text 2>/dev/null || echo "None")
  report_sg=$(aws cloudformation describe-stacks --stack-name "$RELATORIO_STACK" \
    --query "Stacks[0].Outputs[?OutputKey=='ReportLambdaSecurityGroupId'].OutputValue | [0]" \
    --output text 2>/dev/null || echo "None")

  if [ -n "$ecs_sg" ] && [ "$ecs_sg" != "None" ] && [ "$ecs_sg" != "null" ]; then
    aws ec2 authorize-security-group-ingress \
      --group-id "$sg_id" --protocol tcp --port 5432 \
      --source-group "$ecs_sg" >/dev/null 2>&1 \
      && echo "Ingress RDS ← ECS SG $ecs_sg" \
      || echo "Ingress ECS já presente ou falhou (ok se duplicado)."
  else
    echo "ApiEcsSecurityGroupId ausente — rode cdk deploy da ApiStack antes."
  fi

  if [ -n "$report_sg" ] && [ "$report_sg" != "None" ] && [ "$report_sg" != "null" ]; then
    aws ec2 authorize-security-group-ingress \
      --group-id "$sg_id" --protocol tcp --port 5432 \
      --source-group "$report_sg" >/dev/null 2>&1 \
      && echo "Ingress RDS ← Report SG $report_sg" \
      || echo "Ingress Report já presente ou falhou (ok se duplicado)."
  else
    echo "ReportLambdaSecurityGroupId ausente — rode cdk deploy do RelatorioStack antes."
  fi

  # Revoga abertura mundial se existir
  if aws ec2 revoke-security-group-ingress \
      --group-id "$sg_id" --protocol tcp --port 5432 --cidr 0.0.0.0/0 >/dev/null 2>&1; then
    echo "Revogado 0.0.0.0/0:5432 no SG $sg_id"
  else
    echo "Sem regra 0.0.0.0/0 para revogar (já endurecido)."
  fi

  aws ec2 describe-security-groups --group-ids "$sg_id" \
    --query 'SecurityGroups[0].IpPermissions' --output table || true
}

create_rds_instance() {
  local password sg_id
  ensure_rds_network
  sg_id=$(cat /tmp/feedbacks-rds-sg-id.txt)
  password=$(gen_alnum 32)
  echo "$password" > /tmp/feedbacks-rds-master-password.txt
  chmod 600 /tmp/feedbacks-rds-master-password.txt

  echo "Criando RDS $RDS_INSTANCE ($RDS_INSTANCE_CLASS / postgres)..."
  aws rds create-db-instance \
    --db-instance-identifier "$RDS_INSTANCE" \
    --db-instance-class "$RDS_INSTANCE_CLASS" \
    --engine postgres \
    --engine-version 16.14 \
    --master-username "$RDS_MASTER_USER" \
    --master-user-password "$password" \
    --allocated-storage 20 \
    --storage-type gp3 \
    --db-name "$RDS_DB_NAME" \
    --db-subnet-group-name "$RDS_SUBNET_GROUP" \
    --vpc-security-group-ids "$sg_id" \
    --publicly-accessible \
    --backup-retention-period 0 \
    --no-multi-az \
    --no-deletion-protection \
    --storage-encrypted >/dev/null
  echo "RDS create solicitado — aguardando available (pode levar ~10–15 min)..."
}

sync_db_secret_from_rds() {
  local password endpoint port secret_json
  endpoint=$(aws rds describe-db-instances --db-instance-identifier "$RDS_INSTANCE" \
    --query 'DBInstances[0].Endpoint.Address' --output text)
  port=$(aws rds describe-db-instances --db-instance-identifier "$RDS_INSTANCE" \
    --query 'DBInstances[0].Endpoint.Port' --output text)

  if [ -f /tmp/feedbacks-rds-master-password.txt ]; then
    password=$(cat /tmp/feedbacks-rds-master-password.txt)
  elif aws secretsmanager describe-secret --secret-id "$DB_SECRET_NAME" >/dev/null 2>&1; then
    echo "Secret $DB_SECRET_NAME já existe e a senha do RDS não foi regenerada — mantém."
    # Atualiza só o endpoint se necessário
    secret_json=$(aws secretsmanager get-secret-value --secret-id "$DB_SECRET_NAME" \
      --query SecretString --output text)
    password=$(SECRET="$secret_json" python3 - <<'PY'
import json, os
print(json.loads(os.environ["SECRET"])["dbPassword"])
PY
)
  else
    echo "Secret ausente e senha desconhecida — resetando master password do RDS..."
    password=$(gen_alnum 32)
    echo "$password" > /tmp/feedbacks-rds-master-password.txt
    chmod 600 /tmp/feedbacks-rds-master-password.txt
    aws rds modify-db-instance \
      --db-instance-identifier "$RDS_INSTANCE" \
      --master-user-password "$password" \
      --apply-immediately >/dev/null
    # modify pode ir para resetting-master-credentials / modifying
    sleep 20
    aws rds wait db-instance-available --db-instance-identifier "$RDS_INSTANCE"
  fi

  secret_json=$(PASSWORD="$password" ENDPOINT="$endpoint" PORT="$port" \
    DB_NAME="$RDS_DB_NAME" DB_USER="$RDS_MASTER_USER" python3 - <<'PY'
import json, os
print(json.dumps({
    "dbUrl": f"jdbc:postgresql://{os.environ['ENDPOINT']}:{os.environ['PORT']}/{os.environ['DB_NAME']}",
    "dbUser": os.environ["DB_USER"],
    "dbPassword": os.environ["PASSWORD"],
}))
PY
)
  put_json_secret "$DB_SECRET_NAME" "$secret_json"
  rm -f /tmp/feedbacks-rds-master-password.txt
}

resolve_admin_email() {
  local email
  if [ -n "${FEEDBACKS_ADMIN_EMAIL:-}" ]; then
    echo "$FEEDBACKS_ADMIN_EMAIL"
    return 0
  fi
  if aws secretsmanager describe-secret --secret-id "$ADMIN_EMAIL_SECRET_NAME" >/dev/null 2>&1; then
    aws secretsmanager get-secret-value --secret-id "$ADMIN_EMAIL_SECRET_NAME" \
      --query SecretString --output text | python3 -c 'import json,sys; print(json.load(sys.stdin)["adminEmail"])'
    return 0
  fi
  email=$(aws ses list-identities --identity-type EmailAddress \
    --query 'Identities[0]' --output text 2>/dev/null || echo "")
  if [ -n "$email" ] && [ "$email" != "None" ]; then
    echo "$email"
    return 0
  fi
  echo "::error::Defina FEEDBACKS_ADMIN_EMAIL (secret GHA) ou verifique uma identidade SES."
  return 1
}

ensure_secrets() {
  local admin_email jwt_secret
  echo "==> Garantindo secrets demo"
  sync_db_secret_from_rds

  if aws secretsmanager describe-secret --secret-id "$JWT_SECRET_NAME" >/dev/null 2>&1; then
    echo "Secret $JWT_SECRET_NAME já existe."
  else
    jwt_secret=$(gen_alnum 48)
    put_json_secret "$JWT_SECRET_NAME" "{\"jwtSecret\":\"$jwt_secret\"}"
  fi

  admin_email=$(resolve_admin_email)
  put_json_secret "$ADMIN_EMAIL_SECRET_NAME" "{\"adminEmail\":\"$admin_email\"}"
  echo "adminEmail=$admin_email"
}

ensure_rds_available() {
  local status
  status=$(rds_status)
  echo "RDS status: $status"
  case "$status" in
    ABSENT)
      echo "RDS ausente — provisionando do zero (pós destroy)..."
      create_rds_instance
      ;;
    available)
      echo "RDS já available."
      ;;
    starting|creating|backing-up|modifying|configuring-enhanced-monitoring|storage-optimization)
      echo "RDS em $status — aguardando available..."
      ;;
    stopping)
      echo "Aguardando RDS stopped antes de start..."
      wait_rds_status stopped 90
      aws rds start-db-instance --db-instance-identifier "$RDS_INSTANCE"
      ;;
    stopped)
      aws rds start-db-instance --db-instance-identifier "$RDS_INSTANCE"
      ;;
    *)
      echo "Status inesperado ($status) — aguardando available..."
      ;;
  esac
  # creating pode demorar: wait nativo + fallback poll longo
  if ! aws rds wait db-instance-available --db-instance-identifier "$RDS_INSTANCE"; then
    echo "Waiter nativo falhou — poll estendido..."
    wait_rds_status available 120
  fi
  aws rds describe-db-instances --db-instance-identifier "$RDS_INSTANCE" \
    --query 'DBInstances[0].{Status:DBInstanceStatus,Endpoint:Endpoint.Address}' --output table
}

stop_rds() {
  local status
  status=$(rds_status)
  echo "RDS status: $status"
  case "$status" in
    ABSENT)
      echo "RDS ausente — nada a parar."
      ;;
    stopped|stopping)
      echo "RDS já $status."
      ;;
    available|starting|backing-up|modifying|storage-optimization)
      aws rds stop-db-instance --db-instance-identifier "$RDS_INSTANCE" >/dev/null
      echo "RDS stop solicitado."
      ;;
    *)
      echo "Status $status — tentando stop mesmo assim."
      aws rds stop-db-instance --db-instance-identifier "$RDS_INSTANCE" >/dev/null || true
      ;;
  esac
}

delete_rds() {
  local status
  status=$(rds_status)
  if [ "$status" = "ABSENT" ]; then
    echo "RDS ausente."
    return 0
  fi
  echo "Apagando RDS $RDS_INSTANCE (status=$status, sem snapshot final)..."
  aws rds delete-db-instance \
    --db-instance-identifier "$RDS_INSTANCE" \
    --skip-final-snapshot \
    --delete-automated-backups >/dev/null
  aws rds wait db-instance-deleted --db-instance-identifier "$RDS_INSTANCE" || true
  echo "RDS removido."
}

enable_crons() {
  for rule in "$CRON_DIARIO" "$CRON_SEMANAL"; do
    if aws events describe-rule --name "$rule" >/dev/null 2>&1; then
      aws events enable-rule --name "$rule"
      echo "Enabled $rule"
    else
      echo "Rule $rule ausente (será criada no deploy do RelatorioStack)."
    fi
  done
  aws events list-rules --name-prefix feedbacks-report \
    --query 'Rules[].{Name:Name,State:State}' --output table 2>/dev/null || true
}

disable_crons() {
  for rule in "$CRON_DIARIO" "$CRON_SEMANAL"; do
    if aws events describe-rule --name "$rule" >/dev/null 2>&1; then
      aws events disable-rule --name "$rule"
      echo "Disabled $rule"
    else
      echo "Rule $rule ausente."
    fi
  done
}

# Remove ECR feedbacks-api se a ApiStack não o gerencia (órfão que quebra cdk deploy).
clean_orphan_ecr() {
  local status
  status=$(stack_status "$API_STACK")
  case "$status" in
    CREATE_COMPLETE|UPDATE_COMPLETE|UPDATE_ROLLBACK_COMPLETE)
      echo "ApiStack ativa ($status) — mantém ECR $ECR_REPOSITORY."
      return 0
      ;;
  esac
  if aws ecr describe-repositories --repository-names "$ECR_REPOSITORY" >/dev/null 2>&1; then
    echo "Removendo ECR órfão $ECR_REPOSITORY (ApiStack=$status)..."
    aws ecr delete-repository --repository-name "$ECR_REPOSITORY" --force >/dev/null
    echo "ECR órfão removido."
  else
    echo "ECR $ECR_REPOSITORY ausente."
  fi
}

# Apaga ApiStack em estados que impedem cdk deploy (changeset falho, create failed, etc.).
repair_api_stack() {
  local status
  status=$(stack_status "$API_STACK")
  echo "ApiStack status: $status"
  case "$status" in
    ABSENT|DELETE_COMPLETE|CREATE_COMPLETE|UPDATE_COMPLETE|UPDATE_ROLLBACK_COMPLETE)
      echo "ApiStack OK para deploy ($status)."
      ;;
    *)
      echo "ApiStack em estado inválido ($status) — removendo antes do deploy..."
      # ChangeSets órfãos (REVIEW_IN_PROGRESS) bloqueiam delete se não limpos.
      for cs in $(aws cloudformation list-change-sets --stack-name "$API_STACK" \
          --query 'Summaries[].ChangeSetName' --output text 2>/dev/null || true); do
        [ -n "$cs" ] && [ "$cs" != "None" ] || continue
        echo "Removendo changeset $cs"
        aws cloudformation delete-change-set --stack-name "$API_STACK" --change-set-name "$cs" || true
      done
      wait_stack_gone "$API_STACK"
      clean_orphan_ecr
      ;;
  esac
}

scale_ecs_zero() {
  if ! aws ecs describe-clusters --clusters "$ECS_CLUSTER" \
      --query 'clusters[0].status' --output text 2>/dev/null | grep -qx ACTIVE; then
    echo "ECS cluster $ECS_CLUSTER não ACTIVE — skip scale."
    return 0
  fi
  if ! aws ecs describe-services --cluster "$ECS_CLUSTER" --services "$ECS_SERVICE" \
      --query 'services[0].status' --output text 2>/dev/null | grep -qx ACTIVE; then
    echo "ECS service $ECS_SERVICE não ACTIVE — skip scale."
    return 0
  fi
  aws ecs update-service --cluster "$ECS_CLUSTER" --service "$ECS_SERVICE" \
    --desired-count 0 >/dev/null
  echo "ECS desiredCount=0."
}

pause_demo() {
  echo "==> Pausando demo (custo horário)"
  disable_crons
  scale_ecs_zero
  wait_stack_gone "$API_STACK"
  clean_orphan_ecr
  stop_rds
  echo "==> Pausa concluída. Stacks Alerta/Relatório permanecem (custo ~0)."
  echo "    RDS storage + secrets ainda geram custo residual mínimo."
}

destroy_all() {
  local delete_rds_flag="${1:-false}"
  local delete_secrets_flag="${2:-false}"

  echo "==> Destruindo recursos demo"
  disable_crons || true
  scale_ecs_zero || true

  for stack in "$API_STACK" "$RELATORIO_STACK" "$ALERTA_STACK"; do
    wait_stack_gone "$stack"
  done

  clean_orphan_ecr

  if [ "$delete_rds_flag" = "true" ]; then
    delete_rds
  else
    echo "RDS preservado (delete_rds=false)."
  fi

  if [ "$delete_secrets_flag" = "true" ]; then
    for secret in feedbacks/db feedbacks/jwt feedbacks/adminEmail; do
      if aws secretsmanager describe-secret --secret-id "$secret" >/dev/null 2>&1; then
        aws secretsmanager delete-secret --secret-id "$secret" --force-delete-without-recovery >/dev/null
        echo "Secret $secret apagado."
      fi
    done
  else
    echo "Secrets preservados (delete_secrets=false)."
  fi

  echo "==> Destroy concluído."
}

smoke_health() {
  local health_url alb_dns
  alb_dns=$(aws cloudformation describe-stacks --stack-name "$API_STACK" \
    --query "Stacks[0].Outputs[?OutputKey=='ApiAlbDns'].OutputValue | [0]" --output text)
  health_url=$(aws cloudformation describe-stacks --stack-name "$API_STACK" \
    --query "Stacks[0].Outputs[?OutputKey=='ApiHealthUrl'].OutputValue | [0]" --output text)
  echo "ALB_DNS=$alb_dns"
  echo "HEALTH_URL=$health_url"
  if [ -n "${GITHUB_OUTPUT:-}" ]; then
    echo "alb_dns=$alb_dns" >> "$GITHUB_OUTPUT"
    echo "health_url=$health_url" >> "$GITHUB_OUTPUT"
  fi
  for i in $(seq 1 36); do
    CODE=$(curl -s -o /tmp/health.json -w "%{http_code}" "$health_url" || echo "000")
    BODY=$(cat /tmp/health.json 2>/dev/null || true)
    echo "try $i: HTTP $CODE $BODY"
    if [ "$CODE" = "200" ] && echo "$BODY" | grep -q '"status":"UP"'; then
      echo "Health OK"
      return 0
    fi
    sleep 10
  done
  echo "Health não ficou UP a tempo"
  return 1
}

cmd="${1:-}"
case "$cmd" in
  ensure-rds-available) ensure_rds_available ;;
  ensure-secrets) ensure_secrets ;;
  stop-rds) stop_rds ;;
  delete-rds) delete_rds ;;
  enable-crons) enable_crons ;;
  disable-crons) disable_crons ;;
  clean-orphan-ecr) clean_orphan_ecr ;;
  repair-api-stack) repair_api_stack ;;
  destroy-stack) wait_stack_gone "${2:?stack name}" ;;
  pause) pause_demo ;;
  destroy-all) destroy_all "${2:-false}" "${3:-false}" ;;
  smoke-health) smoke_health ;;
  harden-rds-sg) harden_rds_sg ;;
  stack-status) stack_status "${2:?stack name}" ;;
  *)
    echo "Uso: $0 {ensure-rds-available|ensure-secrets|stop-rds|delete-rds|enable-crons|disable-crons|clean-orphan-ecr|repair-api-stack|destroy-stack <name>|pause|destroy-all [delete_rds] [delete_secrets]|smoke-health|harden-rds-sg}"
    exit 2
    ;;
esac
