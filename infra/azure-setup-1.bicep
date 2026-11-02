// Subscription-scope entry point: create the resource group, then deploy its
// contents through a resource-group-scope module.
targetScope = 'subscription'

@description('3-8 lowercase letters or digits used to make resource names.')
@minLength(3)
@maxLength(8)
param suffix string = 'pmc01'

@description('Region for the resource group and core services.')
param location string = 'uksouth'

@description('Region for Azure Static Web Apps.')
param staticWebAppLocation string = 'eastus2'

@secure()
@description('PostgreSQL administrator password. Supply securely at deployment time.')
param postgresAdminPassword string

@description('Optional object ID of an existing GitHub deployment service principal.')
param githubServicePrincipalObjectId string = ''

var normalizedSuffix = toLower(suffix)
var resourceGroupName = 'rg-tss-tickets-${normalizedSuffix}'

resource rg 'Microsoft.Resources/resourceGroups@2022-09-01' = {
  name: resourceGroupName
  location: location
  tags: {
    application: 'techsavvy-tickets'
  }
}

module resources './azure-setup-1-resources.bicep' = {
  name: 'tss-resources-${normalizedSuffix}'
  scope: rg
  params: {
    suffix: normalizedSuffix
    location: location
    staticWebAppLocation: staticWebAppLocation
    postgresAdminPassword: postgresAdminPassword
    githubServicePrincipalObjectId: githubServicePrincipalObjectId
  }
}

output resourceGroupName string = rg.name
output acrName string = resources.outputs.acrName
output acrLoginServer string = resources.outputs.acrLoginServer
output postgresServerName string = resources.outputs.postgresServerName
output postgresHost string = resources.outputs.postgresHost
output containerEnvironmentName string = resources.outputs.containerEnvironmentName
output containerAppName string = resources.outputs.containerAppName
output apiHostname string = resources.outputs.apiHostname
output staticWebAppName string = resources.outputs.staticWebAppName
output staticWebAppHostname string = resources.outputs.staticWebAppHostname
