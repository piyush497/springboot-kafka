{{/*
Expand the name of the chart.
*/}}
{{- define "courier-microservices.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "courier-microservices.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "courier-microservices.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "courier-microservices.labels" -}}
helm.sh/chart: {{ include "courier-microservices.chart" . }}
{{ include "courier-microservices.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "courier-microservices.selectorLabels" -}}
app.kubernetes.io/name: {{ include "courier-microservices.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "courier-microservices.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "courier-microservices.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Generate database URL
*/}}
{{- define "courier-microservices.databaseUrl" -}}
{{- printf "jdbc:postgresql://%s:%d/%s" .Values.database.host (.Values.database.port | int) .Values.database.name }}
{{- end }}

{{/*
Generate Kafka bootstrap servers
*/}}
{{- define "courier-microservices.kafkaBootstrapServers" -}}
{{- printf "%s:%d" .Values.messaging.kafka.host (.Values.messaging.kafka.port | int) }}
{{- end }}

{{/*
Generate Redis URL
*/}}
{{- define "courier-microservices.redisUrl" -}}
{{- if .Values.cache.redis.enabled }}
{{- printf "%s:%d" .Values.cache.redis.host (.Values.cache.redis.port | int) }}
{{- else }}
{{- printf "" }}
{{- end }}
{{- end }}

{{/*
Common environment variables for all services
*/}}
{{- define "courier-microservices.commonEnv" -}}
- name: ENVIRONMENT
  value: {{ .Values.environment | quote }}
- name: NAMESPACE
  value: {{ .Values.namespace | quote }}
- name: DATABASE_URL
  value: {{ include "courier-microservices.databaseUrl" . | quote }}
- name: DATABASE_USERNAME
  value: {{ .Values.database.username | quote }}
- name: DATABASE_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ .Values.database.existingSecret }}
      key: {{ .Values.database.secretKeys.password }}
- name: KAFKA_BOOTSTRAP_SERVERS
  value: {{ include "courier-microservices.kafkaBootstrapServers" . | quote }}
{{- if .Values.cache.redis.enabled }}
- name: REDIS_HOST
  value: {{ .Values.cache.redis.host | quote }}
- name: REDIS_PORT
  value: {{ .Values.cache.redis.port | quote }}
{{- if .Values.cache.redis.existingSecret }}
- name: REDIS_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ .Values.cache.redis.existingSecret }}
      key: {{ .Values.cache.redis.secretKeys.password }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Common resource limits
*/}}
{{- define "courier-microservices.resources" -}}
{{- if . }}
resources:
  {{- if .requests }}
  requests:
    {{- if .requests.memory }}
    memory: {{ .requests.memory }}
    {{- end }}
    {{- if .requests.cpu }}
    cpu: {{ .requests.cpu }}
    {{- end }}
  {{- end }}
  {{- if .limits }}
  limits:
    {{- if .limits.memory }}
    memory: {{ .limits.memory }}
    {{- end }}
    {{- if .limits.cpu }}
    cpu: {{ .limits.cpu }}
    {{- end }}
  {{- end }}
{{- end }}
{{- end }}

{{/*
Common security context
*/}}
{{- define "courier-microservices.securityContext" -}}
securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  runAsGroup: 1000
  allowPrivilegeEscalation: false
  readOnlyRootFilesystem: true
  capabilities:
    drop:
    - ALL
{{- end }}

{{/*
Common volume mounts
*/}}
{{- define "courier-microservices.volumeMounts" -}}
volumeMounts:
- name: logs
  mountPath: /var/log/courier
- name: temp
  mountPath: /tmp
{{- end }}

{{/*
Common volumes
*/}}
{{- define "courier-microservices.volumes" -}}
volumes:
- name: logs
  emptyDir: {}
- name: temp
  emptyDir: {}
{{- end }}

{{/*
Common node selector
*/}}
{{- define "courier-microservices.nodeSelector" -}}
nodeSelector:
  kubernetes.io/os: linux
{{- end }}

{{/*
Common tolerations
*/}}
{{- define "courier-microservices.tolerations" -}}
tolerations:
- key: "kubernetes.azure.com/scalesetpriority"
  operator: "Equal"
  value: "spot"
  effect: "NoSchedule"
{{- end }}

{{/*
Pod anti-affinity for a service
*/}}
{{- define "courier-microservices.podAntiAffinity" -}}
{{- $serviceName := . }}
affinity:
  podAntiAffinity:
    preferredDuringSchedulingIgnoredDuringExecution:
    - weight: 100
      podAffinityTerm:
        labelSelector:
          matchExpressions:
          - key: app
            operator: In
            values:
            - {{ $serviceName }}
        topologyKey: kubernetes.io/hostname
{{- end }}
