// Resources deployed inside the resource group created by azure-setup-1.bicep.
targetScope = 'resourceGroup'

@description('3-8 lowercase letters or digits used to make resource names.')
@minLength(3)
@maxLength(8)
param suffix string

@description('Region for the core services.')
param location string

@description('Region for Azure Static Web Apps.')
param staticWebAppLocation string

@secure()
@description('PostgreSQL administrator password.')
param postgresAdminPassword string

@description('Optional object ID of an existing GitHub deployment service principal.')
param githubServicePrincipalObjectId string = ''

var normalizedSuffix = toLower(suffix)
var acrName = 'tssacr${normalizedSuffix}'
var postgresName = 'pg-tss-${normalizedSuffix}'
var environmentName = 'cae-tss-${normalizedSuffix}'
var containerAppName = 'tickets-api'
var staticWebAppName = 'swa-tss-${normalizedSuffix}'
var logWorkspaceName = 'law-tss-${normalizedSuffix}'
var acrPullRoleDefinitionId = '7f951dda-4ed3-4680-a7ca-43fe172d538d'
var contributorRoleDefinitionId = 'b24988ac-6180-42a0-ab88-20f7382dd24c'
var acrPushRoleDefinitionId = '8311e382-0749-4cb8-b61a-304f252e45ec'

resource acr 'Microsoft.ContainerRegistry/registries@2023-07-01' = {
  name: acrName
  location: location
  sku: { name: 'Basic' }
  properties: { adminUserEnabled: false }
}

resource pgServer 'Microsoft.DBforPostgreSQL/flexibleServers@2024-08-01' = {
  name: postgresName
  location: location
  sku: { name: 'Standard_B1ms', tier: 'Burstable' }
  properties: {
    administratorLogin: 'tickets'
    administratorLoginPassword: postgresAdminPassword
    availabilityZone: '3'
    backup: { backupRetentionDays: 7, geoRedundantBackup: 'Disabled' }
    highAvailability: { mode: 'Disabled' }
    network: { publicNetworkAccess: 'Enabled' }
    storage: { storageSizeGB: 32, autoGrow: 'Enabled' }
    version: '17'
  }
}

resource pgFirewall 'Microsoft.DBforPostgreSQL/flexibleServers/firewallRules@2024-08-01' = {
  parent: pgServer
  name: 'AllowAzureServices'
  properties: { startIpAddress: '0.0.0.0', endIpAddress: '0.0.0.0' }
}

resource ticketsDatabase 'Microsoft.DBforPostgreSQL/flexibleServers/databases@2024-08-01' = {
  parent: pgServer
  name: 'tickets'
  properties: { charset: 'UTF8', collation: 'en_US.utf8' }
}

resource logWorkspace 'Microsoft.OperationalInsights/workspaces@2022-10-01' = {
  name: logWorkspaceName
  location: location
  properties: {
    features: { disableLocalAuth: false, enableLogAccessUsingOnlyResourcePermissions: true }
    publicNetworkAccessForIngestion: 'Enabled'
    publicNetworkAccessForQuery: 'Enabled'
    retentionInDays: 30
    sku: { name: 'PerGB2018' }
    workspaceCapping: { dailyQuotaGb: -1 }
  }
}

resource containerEnvironment 'Microsoft.App/managedEnvironments@2024-03-01' = {
  name: environmentName
  location: location
  properties: {
    appLogsConfiguration: {
      destination: 'log-analytics'
      logAnalyticsConfiguration: {
        customerId: logWorkspace.properties.customerId
        sharedKey: logWorkspace.listKeys().primarySharedKey
      }
    }
    vnetConfiguration: {}
  }
}

resource containerApp 'Microsoft.App/containerApps@2024-03-01' = {
  name: containerAppName
  location: location
  identity: { type: 'SystemAssigned' }
  properties: {
    managedEnvironmentId: containerEnvironment.id
    configuration: {
      activeRevisionsMode: 'Single'
      ingress: { external: true, targetPort: 80, transport: 'auto' }
    }
    template: {
      containers: [{
        name: 'tickets-api'
        image: 'mcr.microsoft.com/k8se/quickstart:latest'
        resources: { cpu: json('0.5'), memory: '1Gi' }
      }]
      scale: { minReplicas: 0, maxReplicas: 2 }
    }
  }
}

resource acrPullAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  scope: acr
  name: guid(acr.id, containerApp.id, acrPullRoleDefinitionId)
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', acrPullRoleDefinitionId)
    principalId: containerApp.identity.principalId
    principalType: 'ServicePrincipal'
  }
}

resource staticWebApp 'Microsoft.Web/staticSites@2025-03-01' = {
  name: staticWebAppName
  location: staticWebAppLocation
  sku: { name: 'Free', tier: 'Free' }
  properties: {}
}

resource githubContributorAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = if (!empty(githubServicePrincipalObjectId)) {
  scope: resourceGroup()
  name: guid(resourceGroup().id, githubServicePrincipalObjectId, contributorRoleDefinitionId)
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', contributorRoleDefinitionId)
    principalId: githubServicePrincipalObjectId
    principalType: 'ServicePrincipal'
  }
}

resource githubAcrPushAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = if (!empty(githubServicePrincipalObjectId)) {
  scope: acr
  name: guid(acr.id, githubServicePrincipalObjectId, acrPushRoleDefinitionId)
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', acrPushRoleDefinitionId)
    principalId: githubServicePrincipalObjectId
    principalType: 'ServicePrincipal'
  }
}

output resourceGroupName string = resourceGroup().name
output acrName string = acr.name
output acrLoginServer string = acr.properties.loginServer
output postgresServerName string = pgServer.name
output postgresHost string = pgServer.properties.fullyQualifiedDomainName
output containerEnvironmentName string = containerEnvironment.name
output containerAppName string = containerApp.name
output apiHostname string = containerApp.properties.configuration.ingress.fqdn
output staticWebAppName string = staticWebApp.name
output staticWebAppHostname string = staticWebApp.properties.defaultHostname
