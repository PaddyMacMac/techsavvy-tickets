// Resource-group-scope API configuration, after the Entra app registration.
// Preview changes before deployment. This declares the full Container App
// resource, so containerImage must match the intended image when redeploying.
targetScope = 'resourceGroup'

@description('Resource-name suffix used by azure-setup-1.bicep.')
@minLength(3)
@maxLength(8)
param suffix string = 'pmc01'

@description('Microsoft Entra application (client) ID used by the API.')
param entraClientId string

@secure()
@description('The PostgreSQL administrator password used by the API.')
param postgresAdminPassword string

@description('Current image for tickets-api. Supply the intended image when reapplying after a GitHub deployment.')
param containerImage string = 'mcr.microsoft.com/k8se/quickstart:latest'

var normalizedSuffix = toLower(suffix)
var postgresName = 'pg-tss-${normalizedSuffix}'
var environmentName = 'cae-tss-${normalizedSuffix}'
var acrName = 'tssacr${normalizedSuffix}'
var staticWebAppName = 'swa-tss-${normalizedSuffix}'

resource postgres 'Microsoft.DBforPostgreSQL/flexibleServers@2024-08-01' existing = {
  name: postgresName
}
resource containerEnvironment 'Microsoft.App/managedEnvironments@2024-03-01' existing = {
  name: environmentName
}
resource acr 'Microsoft.ContainerRegistry/registries@2023-07-01' existing = {
  name: acrName
}
resource staticWebApp 'Microsoft.Web/staticSites@2025-03-01' existing = {
  name: staticWebAppName
}

resource containerApp 'Microsoft.App/containerApps@2024-03-01' = {
  name: 'tickets-api'
  location: resourceGroup().location
  identity: { type: 'SystemAssigned' }
  properties: {
    managedEnvironmentId: containerEnvironment.id
    configuration: {
      activeRevisionsMode: 'Single'
      ingress: { external: true, targetPort: 8080, transport: 'auto' }
      registries: [{ server: acr.properties.loginServer, identity: 'system' }]
      secrets: [{ name: 'db-password', value: postgresAdminPassword }]
    }
    template: {
      containers: [{
        name: 'tickets-api'
        image: containerImage
        resources: { cpu: json('0.5'), memory: '1Gi' }
        env: [
          { name: 'SPRING_PROFILES_ACTIVE', value: 'azure' }
          { name: 'DB_URL', value: 'jdbc:postgresql://${postgres.properties.fullyQualifiedDomainName}:5432/tickets?sslmode=require' }
          { name: 'DB_USER', value: 'tickets' }
          { name: 'DB_PASSWORD', secretRef: 'db-password' }
          { name: 'ENTRA_TENANT_ID', value: tenant().tenantId }
          { name: 'ENTRA_CLIENT_ID', value: entraClientId }
          { name: 'CORS_ALLOWED_ORIGINS', value: 'https://${staticWebApp.properties.defaultHostname}' }
        ]
      }]
      scale: { minReplicas: 0, maxReplicas: 2 }
    }
  }
}

output apiHostname string = containerApp.properties.configuration.ingress.fqdn
output staticWebAppHostname string = staticWebApp.properties.defaultHostname
