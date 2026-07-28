#!/usr/bin/env bash
# Ciclo de vida do ambiente demo AWS (pausar / subir / apagar).
# Uso: AWS_REGION=us-east-1 ./demo-lifecycle.sh <comando>
set -euo pipefail

AWS_REGION="${AWS_REGION:-us-east-1}"
RDS_INSTANCE="${RDS_INSTANCE:-feedbacks-demo}"
ECR_REPOSITORY="${ECR_REPOSITORY:-feedbacks-api}"
API_STACK="${API_STACK:-FeedbacksApiStack}"
ALERTA_STACK="${ALERTA_STACK:-FeedbacksAlertaNotificationStack}"
RELATORIO_STACK="${RELATORIO_STACK:-FeedbacksRelatorioStack}"
ECS_CLUSTER="${ECS_CLUSTER:-feedbacks-api}"
ECS_SERVICE="${ECS_SERVICE:-feedbacks-api}"
CRON_DIARIO="${CRON_DIARIO:-feedbacks-report-diario}"
CRON_SEMANAL="${CRON_SEMANAL:-feedbacks-report-semanal}"

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

ensure_rds_available() {
  local status
  status=$(aws rds describe-db-instances \
    --db-instance-identifier "$RDS_INSTANCE" \
    --query 'DBInstances[0].DBInstanceStatus' --output text 2>/dev/null || echo "ABSENT")
  echo "RDS status: $status"
  case "$status" in
    ABSENT)
      echo "::error::RDS $RDS_INSTANCE não existe. Provisionar manualmente antes do deploy."
      exit 1
      ;;
    available)
      echo "RDS já available."
      ;;
    starting)
      echo "RDS em starting — aguardando available..."
      ;;
    stopping)
      echo "Aguardando RDS stopped antes de start..."
      aws rds wait db-instance-stopped --db-instance-identifier "$RDS_INSTANCE"
      aws rds start-db-instance --db-instance-identifier "$RDS_INSTANCE"
      ;;
    stopped)
      aws rds start-db-instance --db-instance-identifier "$RDS_INSTANCE"
      ;;
    *)
      echo "Status inesperado ($status) — aguardando available..."
      ;;
  esac
  aws rds wait db-instance-available --db-instance-identifier "$RDS_INSTANCE"
  aws rds describe-db-instances --db-instance-identifier "$RDS_INSTANCE" \
    --query 'DBInstances[0].{Status:DBInstanceStatus,Endpoint:Endpoint.Address}' --output table
}

stop_rds() {
  local status
  status=$(aws rds describe-db-instances \
    --db-instance-identifier "$RDS_INSTANCE" \
    --query 'DBInstances[0].DBInstanceStatus' --output text 2>/dev/null || echo "ABSENT")
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
  status=$(aws rds describe-db-instances \
    --db-instance-identifier "$RDS_INSTANCE" \
    --query 'DBInstances[0].DBInstanceStatus' --output text 2>/dev/null || echo "ABSENT")
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
  stack-status) stack_status "${2:?stack name}" ;;
  *)
    echo "Uso: $0 {ensure-rds-available|stop-rds|delete-rds|enable-crons|disable-crons|clean-orphan-ecr|repair-api-stack|destroy-stack <name>|pause|destroy-all [delete_rds] [delete_secrets]|smoke-health}"
    exit 2
    ;;
esac
