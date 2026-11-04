param(
    [string]$Location = 'uksouth',
    [string]$StaticWebAppLocation = 'eastus2',
    [string]$Suffix = 'pmc01',
    [switch]$Cleanup
)

# One-run Azure setup for this repository. Azure CLI and GitHub CLI use the
# user's existing sign-in sessions; a browser sign-in is opened if either has
# expired or has not been set up yet.
$ErrorActionPreference = 'Continue'
$resourceGroupName = "rg-tss-tickets-$Suffix"
$tenantIdExpected = 'a395392c-c863-43cf-a48a-c987bae482ff'
$subscriptionIdExpected = '4c0164b9-4b03-4d9f-92ce-67bb24ae4751'
$templatePath = Join-Path $PSScriptRoot 'azure-setup-1.bicep'
$apiTemplatePath = Join-Path $PSScriptRoot 'azure-setup-2.bicep'
$githubAppName = "gh-deploy-tss-$Suffix"
$entraAppName = 'Tech Savvy Tickets'
$secretStateDirectory = Join-Path $env:LOCALAPPDATA 'TechSavvyTickets'
$secretStatePath = Join-Path $secretStateDirectory "postgres-$Suffix.secure"
$appSecretStatePath = Join-Path $secretStateDirectory "postgres-app-$Suffix.secure"

function Invoke-Az {
    param([string[]]$Arguments, [string]$Failure)
    $result = & az @Arguments
    if ($LASTEXITCODE -ne 0) { throw $Failure }
    return $result
}

function Invoke-AzJson {
    param([string[]]$Arguments, [string]$Failure)
    $result = Invoke-Az -Arguments $Arguments -Failure $Failure
    return (($result -join [Environment]::NewLine) | ConvertFrom-Json)
}

function Invoke-Gh {
    param([string[]]$Arguments, [string]$Failure)
    $result = & gh @Arguments
    if ($LASTEXITCODE -ne 0) { throw $Failure }
    return $result
}

function Set-GhSecret {
    param([string]$Name, [string]$Value, [string]$Repository)
    $Value | & gh secret set $Name --repo $Repository
    if ($LASTEXITCODE -ne 0) { throw "Could not save GitHub Actions secret $Name." }
}

function New-JsonPayloadFile {
    param([object]$Value)
    $path = Join-Path ([System.IO.Path]::GetTempPath()) ("tss-azure-" + [guid]::NewGuid().ToString('N') + '.json')
    $json = ConvertTo-Json -InputObject $Value -Depth 50 -Compress
    [System.IO.File]::WriteAllText($path, $json, [System.Text.UTF8Encoding]::new($false))
    return $path
}

function Invoke-GraphRequest {
    param([ValidateSet('PATCH', 'POST')][string]$Method, [string]$Uri, [object]$Body, [string]$Failure)
    $bodyFile = New-JsonPayloadFile -Value $Body
    try {
        $null = Invoke-Az -Arguments @('rest', '--method', $Method, '--uri', $Uri, '--headers', 'Content-Type=application/json', '--body', "@$bodyFile", '--output', 'none') -Failure $Failure
    } finally {
        Remove-Item -LiteralPath $bodyFile -Force -ErrorAction SilentlyContinue
    }
}

function Ensure-EntraApplication {
    param([string]$WebsiteUrl)

    $apps = Invoke-AzJson -Arguments @('ad', 'app', 'list', '--display-name', $entraAppName, '--output', 'json') -Failure 'Could not list Entra app registrations.'
    $app = @($apps | Where-Object { $_.displayName -eq $entraAppName } | Select-Object -First 1)[0]
    if ($null -eq $app) {
        $app = Invoke-AzJson -Arguments @('ad', 'app', 'create', '--display-name', $entraAppName, '--sign-in-audience', 'AzureADMyOrg', '--output', 'json') -Failure 'Could not create the Tech Savvy Tickets Entra app. Your account may need permission to register applications in this tenant.'
    }
    $clientId = [string]$app.appId
    $objectId = [string]$app.id
    if ([string]::IsNullOrWhiteSpace($clientId) -or [string]::IsNullOrWhiteSpace($objectId)) {
        throw 'The Tech Savvy Tickets Entra app registration did not return its IDs.'
    }

    $app = Invoke-AzJson -Arguments @('ad', 'app', 'show', '--id', $clientId, '--output', 'json') -Failure 'Could not read the Tech Savvy Tickets Entra app.'
    $scopes = @($app.api.oauth2PermissionScopes | Where-Object { $null -ne $_ })
    $scope = @($scopes | Where-Object { $_.value -eq 'tickets.access' } | Select-Object -First 1)[0]
    if ($null -eq $scope) {
        $scope = [pscustomobject]@{
            adminConsentDescription = 'Use the tickets API'
            adminConsentDisplayName = 'Use the tickets API'
            id = [guid]::NewGuid().ToString()
            isEnabled = $true
            type = 'User'
            userConsentDescription = 'Use the tickets API'
            userConsentDisplayName = 'Use the tickets API'
            value = 'tickets.access'
        }
        $scopes += $scope
    }

    $roles = @($app.appRoles | Where-Object { $null -ne $_ })
    foreach ($definition in @(
        @{ DisplayName = 'Engineer'; Value = 'ENGINEER'; Description = 'Tech Savvy engineer' },
        @{ DisplayName = 'Client'; Value = 'CLIENT'; Description = 'Client staff member' }
    )) {
        if (-not (@($roles | Where-Object { $_.value -eq $definition.Value }).Count)) {
            $roles += [pscustomobject]@{
                allowedMemberTypes = @('User')
                description = $definition.Description
                displayName = $definition.DisplayName
                id = [guid]::NewGuid().ToString()
                isEnabled = $true
                value = $definition.Value
            }
        }
    }

    $redirectUris = @($app.spa.redirectUris | Where-Object { $null -ne $_ })
    $productionOrigin = $WebsiteUrl.TrimEnd('/')
    $redirectCandidates = @('http://localhost:5173', 'http://localhost:5173/')
    if (-not [string]::IsNullOrWhiteSpace($productionOrigin)) { $redirectCandidates += @($productionOrigin, "$productionOrigin/") }
    foreach ($uri in $redirectCandidates) {
        if (-not [string]::IsNullOrWhiteSpace($uri) -and $redirectUris -notcontains $uri) { $redirectUris += $uri }
    }
    $identifierUris = @($app.identifierUris | Where-Object { $null -ne $_ })
    $apiIdentifier = "api://$clientId"
    if ($identifierUris -notcontains $apiIdentifier) { $identifierUris += $apiIdentifier }
    $requiredAccess = @($app.requiredResourceAccess | Where-Object { $null -ne $_ -and $_.resourceAppId -ne $clientId })
    $requiredAccess += [pscustomobject]@{
        resourceAppId = $clientId
        resourceAccess = @([pscustomobject]@{ id = $scope.id; type = 'Scope' })
    }
    $body = @{
        api = @{ requestedAccessTokenVersion = 2; oauth2PermissionScopes = $scopes }
        appRoles = $roles
        identifierUris = $identifierUris
        requiredResourceAccess = $requiredAccess
        spa = @{ redirectUris = $redirectUris }
    }
    Invoke-GraphRequest -Method PATCH -Uri "https://graph.microsoft.com/v1.0/applications/$objectId" -Body $body -Failure 'Could not configure the Entra app scope, roles, and redirect addresses. Check application registration permissions.'

    $servicePrincipal = $null
    $spOutput = & az ad sp show --id $clientId --output json 2>$null
    if ($LASTEXITCODE -eq 0) { $servicePrincipal = ($spOutput -join [Environment]::NewLine) | ConvertFrom-Json }
    if ($null -eq $servicePrincipal) {
        $servicePrincipal = Invoke-AzJson -Arguments @('ad', 'sp', 'create', '--id', $clientId, '--output', 'json') -Failure 'Could not create the Tech Savvy Tickets enterprise application.'
    }
    Invoke-GraphRequest -Method PATCH -Uri "https://graph.microsoft.com/v1.0/servicePrincipals/$($servicePrincipal.id)" -Body @{ appRoleAssignmentRequired = $true } -Failure 'Could not require an app role assignment for Entra sign-in.'

    $existingGrants = Invoke-AzJson -Arguments @('ad', 'app', 'permission', 'list-grants', '--id', $clientId, '--output', 'json') -Failure 'Could not check consent for the tickets.access API scope.'
    $hasScopeGrant = @($existingGrants | Where-Object { $_.resourceId -eq $servicePrincipal.id -and $_.consentType -eq 'AllPrincipals' -and (($_.scope -split '\s+') -contains 'tickets.access') }).Count -gt 0
    if (-not $hasScopeGrant) {
        $null = Invoke-Az -Arguments @('ad', 'app', 'permission', 'grant', '--id', $clientId, '--api', $clientId, '--scope', 'tickets.access', '--consent-type', 'AllPrincipals', '--output', 'none') -Failure 'Could not grant consent for the tickets.access API scope. The signed-in account needs permission to grant tenant-wide consent.'
    }

    $userId = [string](Invoke-Az -Arguments @('ad', 'signed-in-user', 'show', '--query', 'id', '--output', 'tsv') -Failure 'Could not identify the signed-in Azure user.')
    $engineerRole = @($roles | Where-Object { $_.value -eq 'ENGINEER' } | Select-Object -First 1)[0]
    $assignments = Invoke-AzJson -Arguments @('rest', '--method', 'GET', '--uri', "https://graph.microsoft.com/v1.0/servicePrincipals/$($servicePrincipal.id)/appRoleAssignedTo", '--output', 'json') -Failure 'Could not check the Engineer role assignment.'
    if (-not (@($assignments.value | Where-Object { $_.principalId -eq $userId -and $_.appRoleId -eq $engineerRole.id }).Count)) {
        $assignment = @{ principalId = $userId; resourceId = $servicePrincipal.id; appRoleId = $engineerRole.id }
        Invoke-GraphRequest -Method POST -Uri "https://graph.microsoft.com/v1.0/servicePrincipals/$($servicePrincipal.id)/appRoleAssignedTo" -Body $assignment -Failure 'Could not assign the Engineer role to the signed-in user. Your account may need permission to assign app roles.'
    }

    return $clientId
}

function Get-DatabasePassword {
    if (-not (Test-Path -LiteralPath $secretStatePath)) {
        New-Item -ItemType Directory -Path $secretStateDirectory -Force | Out-Null
        $bytes = New-Object byte[] 36
        $random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        try { $random.GetBytes($bytes) } finally { $random.Dispose() }
        $password = 'Tss!' + [Convert]::ToBase64String($bytes).Replace('+', 'X').Replace('/', 'y').TrimEnd('=') + 'a1'
        $secure = ConvertTo-SecureString -String $password -AsPlainText -Force
        ConvertFrom-SecureString -SecureString $secure | Set-Content -LiteralPath $secretStatePath -NoNewline
    }
    $securePassword = Get-Content -LiteralPath $secretStatePath -Raw | ConvertTo-SecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
}

function Get-DatabaseAppPassword {
    if (-not (Test-Path -LiteralPath $appSecretStatePath)) {
        New-Item -ItemType Directory -Path $secretStateDirectory -Force | Out-Null
        $bytes = New-Object byte[] 36
        $random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        try { $random.GetBytes($bytes) } finally { $random.Dispose() }
        $hex = ($bytes | ForEach-Object { $_.ToString('x2') }) -join ''
        $password = 'TssApp!' + $hex + 'a1'
        $secure = ConvertTo-SecureString -String $password -AsPlainText -Force
        ConvertFrom-SecureString -SecureString $secure | Set-Content -LiteralPath $appSecretStatePath -NoNewline
    }
    $securePassword = Get-Content -LiteralPath $appSecretStatePath -Raw | ConvertTo-SecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
}

function Ensure-GitHubCli {
    $ghLocations = @(
        (Join-Path $env:ProgramFiles 'GitHub CLI'),
        (Join-Path $env:LOCALAPPDATA 'Programs/GitHub CLI'),
        (Join-Path $env:LOCALAPPDATA 'Microsoft/WinGet/Links')
    )

    # winget can report the package as installed even when its PATH update has
    # not reached this already-open IntelliJ terminal. Check common install
    # locations before trying to install it again.
    if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
        foreach ($ghLocation in $ghLocations) {
            if (Test-Path -LiteralPath (Join-Path $ghLocation 'gh.exe')) {
                $env:Path = "$ghLocation;$env:Path"
                break
            }
        }
    }

    if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
        if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
            throw 'GitHub CLI is required to save Actions settings automatically. Install GitHub CLI once, then run this launcher again.'
        }
        & winget install --id GitHub.cli --exact --silent --accept-source-agreements --accept-package-agreements
        $wingetExitCode = $LASTEXITCODE
        foreach ($ghLocation in $ghLocations) {
            if (Test-Path -LiteralPath (Join-Path $ghLocation 'gh.exe')) {
                $env:Path = "$ghLocation;$env:Path"
                break
            }
        }
        if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
            throw "GitHub CLI is not available in this terminal. winget returned exit code $wingetExitCode and no gh.exe was found in the standard install locations. Restart IntelliJ after installing GitHub CLI, then run this launcher again."
        }
    }

    & gh auth status --hostname github.com 1>$null 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-Host 'Sign in to GitHub once in the browser window so the launcher can configure repository Actions settings.'
        & gh auth login --hostname github.com --git-protocol https --web
        if ($LASTEXITCODE -ne 0) { throw 'GitHub sign-in failed.' }
    }
}

function Ensure-AzureCli {
    $azLocations = @(
        (Join-Path $env:ProgramFiles 'Microsoft SDKs/Azure/CLI2/wbin'),
        (Join-Path ${env:ProgramFiles(x86)} 'Microsoft SDKs/Azure/CLI2/wbin')
    )

    # An installed Azure CLI may be absent from the PATH inherited by an
    # already-open IntelliJ terminal. Find the existing az.cmd before asking
    # winget to install or upgrade the package.
    if (-not (Get-Command az -ErrorAction SilentlyContinue)) {
        foreach ($azLocation in $azLocations) {
            if (Test-Path -LiteralPath (Join-Path $azLocation 'az.cmd')) {
                $env:Path = "$azLocation;$env:Path"
                break
            }
        }
    }

    if (-not (Get-Command az -ErrorAction SilentlyContinue)) {
        if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
            throw 'Azure CLI is missing and winget is unavailable. Install Azure CLI once, then rerun this launcher.'
        }
        & winget install --id Microsoft.AzureCLI --exact --silent --accept-source-agreements --accept-package-agreements
        $wingetExitCode = $LASTEXITCODE
        foreach ($azLocation in $azLocations) {
            if (Test-Path -LiteralPath (Join-Path $azLocation 'az.cmd')) {
                $env:Path = "$azLocation;$env:Path"
                break
            }
        }
        if (-not (Get-Command az -ErrorAction SilentlyContinue)) {
            throw "Azure CLI is not available in this terminal. winget returned exit code $wingetExitCode and no az.cmd was found in the standard install locations. Restart IntelliJ after installing Azure CLI, then run this launcher again."
        }
    }
}

Ensure-AzureCli
if (-not (Test-Path -LiteralPath $templatePath) -or -not (Test-Path -LiteralPath $apiTemplatePath)) {
    throw 'A Bicep template is missing from the infra folder.'
}
if ($Suffix -notmatch '^[a-z0-9]{3,8}$') { throw 'The configured resource suffix must be 3-8 lowercase letters or digits.' }

Write-Host 'Checking Azure sign-in...'
& az account show --output none 1>$null 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host 'Signing in to Azure in the browser...'
    & az config set core.enable_broker_on_windows=false 1>$null 2>$null
    try {
        & az login --tenant $tenantIdExpected
        if ($LASTEXITCODE -ne 0) { throw 'Azure sign-in failed.' }
    } finally {
        & az config set core.enable_broker_on_windows=true 1>$null 2>$null
    }
}
& az account set --subscription $subscriptionIdExpected
if ($LASTEXITCODE -ne 0) { throw "Could not select the project subscription $subscriptionIdExpected." }
$subscriptionId = [string](Invoke-Az -Arguments @('account', 'show', '--query', 'id', '--output', 'tsv') -Failure 'Could not read the active Azure subscription.')
$tenantId = [string](Invoke-Az -Arguments @('account', 'show', '--query', 'tenantId', '--output', 'tsv') -Failure 'Could not read the active Azure tenant.')
if ($subscriptionId -ne $subscriptionIdExpected -or $tenantId -ne $tenantIdExpected) { throw 'Azure CLI is signed in to a different tenant or subscription than this project.' }
if ($Cleanup) {
    $confirmation = Read-Host "Type DELETE to remove $resourceGroupName and its Azure resources"
    if ($confirmation -cne 'DELETE') { Write-Host 'Cleanup cancelled.'; exit 0 }
    $null = Invoke-Az -Arguments @('group', 'delete', '--name', $resourceGroupName, '--yes', '--no-wait') -Failure 'Could not start resource group deletion.'
    Write-Host "Deletion started for $resourceGroupName. Azure will remove it in the background."
    exit 0
}
Ensure-GitHubCli
$githubRepository = [string](Invoke-Gh -Arguments @('repo', 'view', '--json', 'nameWithOwner', '--jq', '.nameWithOwner') -Failure 'Open this project from its GitHub checkout and make sure its origin points to the repository you want to deploy.')
$repositoryMetadataJson = Invoke-Gh -Arguments @('api', "repos/$githubRepository", '--jq', '{fullName:.full_name,ownerLogin:.owner.login,ownerId:(.owner.id|tostring),repositoryName:.name,repositoryId:(.id|tostring)}') -Failure 'Could not read the GitHub repository IDs needed for workload identity federation.'
$repositoryMetadata = ($repositoryMetadataJson -join [Environment]::NewLine) | ConvertFrom-Json
$githubRepository = [string]$repositoryMetadata.fullName

Write-Host 'Preparing the repeatable setup...'
$githubApp = Invoke-AzJson -Arguments @('ad', 'app', 'list', '--display-name', $githubAppName, '--output', 'json') -Failure 'Could not look up the GitHub deployment app registration.'
$githubApp = @($githubApp | Where-Object { $_.displayName -eq $githubAppName } | Select-Object -First 1)[0]
if ($null -eq $githubApp) {
    $githubApp = Invoke-AzJson -Arguments @('ad', 'app', 'create', '--display-name', $githubAppName, '--sign-in-audience', 'AzureADMyOrg', '--output', 'json') -Failure 'Could not create the GitHub deployment app registration.'
}
$githubClientId = [string]$githubApp.appId
$githubPrincipal = $null
$spOutput = & az ad sp show --id $githubClientId --output json 2>$null
if ($LASTEXITCODE -eq 0) { $githubPrincipal = ($spOutput -join [Environment]::NewLine) | ConvertFrom-Json }
if ($null -eq $githubPrincipal) {
    $githubPrincipal = Invoke-AzJson -Arguments @('ad', 'sp', 'create', '--id', $githubClientId, '--output', 'json') -Failure 'Could not create the GitHub deployment service principal.'
}
$federated = Invoke-AzJson -Arguments @('ad', 'app', 'federated-credential', 'list', '--id', $githubClientId, '--output', 'json') -Failure 'Could not inspect the GitHub federated credential.'
$federatedCredential = @($federated | Where-Object { $_.name -eq 'github-main' } | Select-Object -First 1)[0]
$expectedSubject = 'repo:{0}@{1}/{2}@{3}:ref:refs/heads/main' -f $repositoryMetadata.ownerLogin, $repositoryMetadata.ownerId, $repositoryMetadata.repositoryName, $repositoryMetadata.repositoryId
$federatedParameters = @{
    issuer = 'https://token.actions.githubusercontent.com'
    subject = $expectedSubject
    description = "GitHub Actions for $githubRepository main branch"
    audiences = @('api://AzureADTokenExchange')
}
if ($null -eq $federatedCredential) {
    $createFederatedParameters = $federatedParameters.Clone()
    $createFederatedParameters.name = 'github-main'
    $federatedParametersFile = New-JsonPayloadFile -Value $createFederatedParameters
    try {
        $null = Invoke-Az -Arguments @('ad', 'app', 'federated-credential', 'create', '--id', $githubClientId, '--parameters', $federatedParametersFile, '--output', 'none') -Failure 'Could not create GitHub workload identity federation.'
    } finally {
        Remove-Item -LiteralPath $federatedParametersFile -Force -ErrorAction SilentlyContinue
    }
} elseif ($federatedCredential.subject -ne $expectedSubject) {
    $federatedParametersFile = New-JsonPayloadFile -Value $federatedParameters
    try {
        $null = Invoke-Az -Arguments @('ad', 'app', 'federated-credential', 'update', '--id', $githubClientId, '--federated-credential-id', $federatedCredential.id, '--parameters', $federatedParametersFile, '--output', 'none') -Failure 'Could not update GitHub workload identity federation to the repository subject currently issued by GitHub.'
    } finally {
        Remove-Item -LiteralPath $federatedParametersFile -Force -ErrorAction SilentlyContinue
    }
}

$entraClientId = Ensure-EntraApplication -WebsiteUrl ''
$postgresAdminPassword = Get-DatabasePassword
$containerAppExists = $false
$existingApiHostname = ''
$rgExists = [string](Invoke-Az -Arguments @('group', 'exists', '--name', $resourceGroupName, '--output', 'tsv') -Failure 'Could not check the Azure resource group.')
if ($rgExists -eq 'true') {
    $existingAppJson = & az containerapp show --name tickets-api --resource-group $resourceGroupName --output json 2>$null
    if ($LASTEXITCODE -eq 0) {
        $existingApp = ($existingAppJson -join [Environment]::NewLine) | ConvertFrom-Json
        $containerAppExists = $true
        $existingApiHostname = [string]$existingApp.properties.configuration.ingress.fqdn
        if ([string]::IsNullOrWhiteSpace($existingApiHostname)) {
            throw 'The existing API Container App does not have an ingress hostname; refusing to overwrite its configuration.'
        }
    }
}
$databaseAppPassword = if ($containerAppExists) { '' } else { Get-DatabaseAppPassword }

foreach ($provider in @('Microsoft.App', 'Microsoft.OperationalInsights', 'Microsoft.ContainerRegistry', 'Microsoft.DBforPostgreSQL', 'Microsoft.Web')) {
    $state = [string](Invoke-Az -Arguments @('provider', 'show', '--namespace', $provider, '--query', 'registrationState', '--output', 'tsv') -Failure "Could not check provider $provider.")
    if ($state -ne 'Registered') {
        Write-Host "Registering Azure provider $provider..."
        $null = Invoke-Az -Arguments @('provider', 'register', '--namespace', $provider, '--wait') -Failure "Could not register provider $provider."
    }
}

Write-Host 'Checking Bicep templates...'
$null = Invoke-Az -Arguments @('bicep', 'build', '--file', $templatePath, '--stdout') -Failure 'Phase 1 Bicep build failed; no deployment was started.'
$null = Invoke-Az -Arguments @('bicep', 'build', '--file', $apiTemplatePath, '--stdout') -Failure 'API Bicep build failed; no deployment was started.'
$postgresAdminPassword = Get-DatabasePassword
$githubObjectId = [string]$githubPrincipal.id
$deploymentName = "tss-$Suffix-$(Get-Date -Format 'yyyyMMddHHmmss')"
Write-Host "Provisioning or reconciling resources in $resourceGroupName..."
$null = Invoke-Az -Arguments @(
    'deployment', 'sub', 'create', '--name', $deploymentName, '--location', $Location,
    '--template-file', $templatePath, '--parameters',
    "suffix=$Suffix", "location=$Location", "staticWebAppLocation=$StaticWebAppLocation",
    "githubServicePrincipalObjectId=$githubObjectId", "postgresAdminPassword=$postgresAdminPassword",
    "deployContainerApp=$((!$containerAppExists).ToString().ToLowerInvariant())", "existingApiHostname=$existingApiHostname",
    '--output', 'none'
) -Failure 'Azure resource deployment failed.'
$resourceGroupId = [string](Invoke-Az -Arguments @('group', 'show', '--name', $resourceGroupName, '--query', 'id', '--output', 'tsv') -Failure 'Could not read the deployed resource group ID.')
$legacyContributorAssignments = Invoke-AzJson -Arguments @('role', 'assignment', 'list', '--assignee-object-id', $githubObjectId, '--all', '--output', 'json') -Failure 'Could not check for the previous GitHub Contributor assignment.'
foreach ($assignment in @($legacyContributorAssignments | Where-Object { $_.roleDefinitionName -eq 'Contributor' -and $_.scope -eq $resourceGroupId })) {
    $null = Invoke-Az -Arguments @('role', 'assignment', 'delete', '--ids', $assignment.id, '--output', 'none') -Failure 'Could not remove the previous resource-group Contributor assignment.'
}
$outputs = Invoke-AzJson -Arguments @('deployment', 'sub', 'show', '--name', $deploymentName, '--query', 'properties.outputs', '--output', 'json') -Failure 'Could not read Bicep deployment outputs.'
$websiteUrl = 'https://' + [string]$outputs.staticWebAppHostname.value
$entraClientId = Ensure-EntraApplication -WebsiteUrl ($websiteUrl + '/')

if ($containerAppExists) {
    Write-Host 'Preserving the existing API configuration, image, and database secrets.'
} else {
    Write-Host 'Configuring the initial API...'
    $apiDeploymentName = "tss-api-$Suffix-$(Get-Date -Format 'yyyyMMddHHmmss')"
    $null = Invoke-Az -Arguments @(
        'deployment', 'group', 'create', '--name', $apiDeploymentName, '--resource-group', $resourceGroupName,
        '--template-file', $apiTemplatePath, '--parameters',
        "suffix=$Suffix", "entraClientId=$entraClientId", "postgresAdminPassword=$postgresAdminPassword",
        "databaseAppPassword=$databaseAppPassword", "useDedicatedDatabaseUser=false",
        "containerImage=mcr.microsoft.com/k8se/quickstart:latest", '--output', 'none'
    ) -Failure 'API configuration deployment failed.'
}

$apiUrl = 'https://' + [string]$outputs.apiHostname.value
$staticWebAppName = [string]$outputs.staticWebAppName.value
$staticWebAppToken = [string](Invoke-Az -Arguments @('staticwebapp', 'secrets', 'list', '--name', $staticWebAppName, '--resource-group', $resourceGroupName, '--query', 'properties.apiKey', '--output', 'tsv') -Failure 'Could not retrieve the Static Web App deployment token.')

Write-Host 'Saving GitHub Actions secrets and variables...'
$secretValues = @{
    AZURE_CLIENT_ID = $githubClientId
    AZURE_TENANT_ID = $tenantId
    AZURE_SUBSCRIPTION_ID = $subscriptionId
    AZURE_STATIC_WEB_APPS_API_TOKEN = $staticWebAppToken
}
foreach ($entry in $secretValues.GetEnumerator()) {
    Set-GhSecret -Name $entry.Key -Value $entry.Value -Repository $githubRepository
}
$variableValues = @{
    ACR_NAME = [string]$outputs.acrName.value
    ACA_APP_NAME = [string]$outputs.containerAppName.value
    AZURE_RESOURCE_GROUP = $resourceGroupName
    VITE_API_BASE_URL = $apiUrl
    VITE_ENTRA_TENANT_ID = $tenantId
    VITE_ENTRA_CLIENT_ID = $entraClientId
}
foreach ($entry in $variableValues.GetEnumerator()) {
    $null = Invoke-Gh -Arguments @('variable', 'set', $entry.Key, '--repo', $githubRepository, '--body', $entry.Value) -Failure "Could not save GitHub Actions variable $($entry.Key)."
}

Write-Host 'Starting the API and website deployment workflows...'
$null = Invoke-Gh -Arguments @('workflow', 'run', 'deploy-backend.yml', '--repo', $githubRepository, '--ref', 'main') -Failure 'Could not start the API deployment workflow. Check that main contains deploy-backend.yml.'
$null = Invoke-Gh -Arguments @('workflow', 'run', 'deploy-frontend.yml', '--repo', $githubRepository, '--ref', 'main') -Failure 'Could not start the website deployment workflow. Check that main contains deploy-frontend.yml.'

Write-Host ''
Write-Host 'TECH SAVVY TICKETS AZURE SETUP COMPLETE'
Write-Host "Resource group: $resourceGroupName"
Write-Host "Website: $websiteUrl"
Write-Host "API: $apiUrl"
Write-Host "Entra app: $entraAppName ($entraClientId)"
Write-Host "GitHub Actions secrets and variables are configured for $githubRepository."
Write-Host 'The PostgreSQL administrator and application passwords are stored encrypted for this Windows user under LocalAppData; neither is written to the repository.'
Write-Host 'Both GitHub deployment workflows have been queued against main.'
