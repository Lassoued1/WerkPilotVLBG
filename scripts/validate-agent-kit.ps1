$ErrorActionPreference = "Stop"

function Assert-True {
    param(
        [bool] $Condition,
        [string] $Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Assert-Contains {
    param(
        [string] $Text,
        [string] $Pattern,
        [string] $Message
    )

    Assert-True ($Text -match $Pattern) $Message
}

function Get-CommandOutput {
    param([string[]] $Command)

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $rawOutput = & $Command[0] @($Command | Select-Object -Skip 1) 2>&1
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }

    $output = ($rawOutput | ForEach-Object {
        if ($_ -is [System.Management.Automation.ErrorRecord]) {
            $_.Exception.Message
        } else {
            $_.ToString()
        }
    }) -join [Environment]::NewLine

    if ($exitCode -ne 0) {
        throw "Command failed: $($Command -join ' ')`n$output"
    }
    return $output
}

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$machinePath = [Environment]::GetEnvironmentVariable("Path", "Machine")
$userPath = [Environment]::GetEnvironmentVariable("Path", "User")
$env:Path = @($env:Path, $machinePath, $userPath) -join ";"

Assert-True (Test-Path ".git") "Repository must be initialized with Git."
Assert-True (Test-Path "backend/pom.xml") "Missing backend/pom.xml."
Assert-True (Test-Path "backend/mvnw.cmd") "Missing backend Maven Wrapper."
Assert-True (Test-Path "frontend/package.json") "Missing frontend/package.json."
Assert-True (Test-Path "frontend/tsconfig.json") "Missing frontend/tsconfig.json."
Assert-True (Test-Path "docs/ARCHITECTURE.md") "Missing docs/ARCHITECTURE.md."
Assert-True (Test-Path "docs/REQUIREMENTS_TRACEABILITY.md") "Missing docs/REQUIREMENTS_TRACEABILITY.md."
Assert-True (Test-Path "docs/requirements/REQUIREMENTS.csv") "Missing requirements inventory."

$javaVersion = Get-CommandOutput @("java", "-version")
Assert-True ($javaVersion -match 'version "25') "Java 25 is required. Actual output: $javaVersion"

$nodeVersion = Get-CommandOutput @("node", "--version")
Assert-True ($nodeVersion -match '^v(1[8-9]|[2-9][0-9])\.') "Node 18+ is required. Actual output: $nodeVersion"

$npmVersion = Get-CommandOutput @("npm", "--version")
Assert-True ($npmVersion.Length -gt 0) "npm must be available."

$gitVersion = Get-CommandOutput @("git", "--version")
Assert-True ($gitVersion -match '^git version ') "Git must be available."

$mavenVersion = Get-CommandOutput @("mvn", "-version")
Assert-True ($mavenVersion -match 'Apache Maven 3\.9\.') "Apache Maven 3.9.x is required. Actual output: $mavenVersion"
Assert-True ($mavenVersion -match 'Java version: 25') "Maven must run on Java 25. Actual output: $mavenVersion"

[xml] $pom = Get-Content "backend/pom.xml"
$javaProperty = $pom.project.properties.'java.version'
$springBootParentVersion = $pom.project.parent.version
Assert-True ($javaProperty -eq "25") "backend/pom.xml must declare java.version 25."
Assert-True ($springBootParentVersion -match '^4\.1\.') "Spring Boot parent must be 4.1.x."

$frontendPackage = Get-Content "frontend/package.json" -Raw | ConvertFrom-Json
Assert-True ($null -ne $frontendPackage.dependencies.react) "Frontend package must declare React."
Assert-True ($null -ne $frontendPackage.dependencies.'react-dom') "Frontend package must declare React DOM."
Assert-True ($null -ne $frontendPackage.devDependencies.vite) "Frontend package must declare Vite."
Assert-True ($null -ne $frontendPackage.devDependencies.typescript) "Frontend package must declare TypeScript."

$tsconfig = Get-Content "frontend/tsconfig.json" -Raw | ConvertFrom-Json
Assert-True ($tsconfig.compilerOptions.strict -eq $true) "Frontend TypeScript must be strict."

$requirements = Import-Csv "docs/requirements/REQUIREMENTS.csv"
Assert-True ($requirements.Count -eq 106) "Requirement inventory must contain 106 IDs; found $($requirements.Count)."

$duplicateIds = $requirements | Group-Object id | Where-Object { $_.Count -ne 1 }
Assert-True (@($duplicateIds).Count -eq 0) "Requirement IDs must be unique."

$expectedRequirementKindCounts = @{
    "business_objective" = 5
    "scope" = 12
    "out_of_scope" = 15
    "validation" = 4
    "master_data" = 8
    "production" = 6
    "energy" = 6
    "energy_rule" = 4
    "downtime_scrap" = 6
    "maintenance" = 7
    "analytics" = 5
    "reporting" = 5
    "non_functional" = 8
    "test_requirement" = 5
    "acceptance" = 10
}

foreach ($kind in $expectedRequirementKindCounts.Keys) {
    $actualCount = @($requirements | Where-Object { $_.kind -eq $kind }).Count
    Assert-True ($actualCount -eq $expectedRequirementKindCounts[$kind]) "Requirement kind '$kind' must contain $($expectedRequirementKindCounts[$kind]) rows; found $actualCount."
}

for ($index = 1; $index -le 15; $index++) {
    $id = "OOS-{0:D2}" -f $index
    Assert-True (@($requirements | Where-Object { $_.id -eq $id }).Count -eq 1) "Missing required out-of-scope ID $id."
}

$architecture = Get-Content "docs/ARCHITECTURE.md" -Raw
$requiredArchitectureSections = @(
    "## Architectural position",
    "## System context",
    "## Use-case view",
    "## Container and deployment view",
    "## Backend component view",
    "## Module ownership",
    "## Package dependency rules",
    "## Critical flows",
    "## Cross-cutting rules"
)

foreach ($section in $requiredArchitectureSections) {
    Assert-True ($architecture.Contains($section)) "Architecture baseline is missing section '$section'."
}

$mermaidCount = ([regex]::Matches($architecture, '```mermaid')).Count
Assert-True ($mermaidCount -ge 5) "Architecture baseline must contain at least five Mermaid diagrams; found $mermaidCount."

$requiredModules = @(
    "identity",
    "masterdata",
    "importing",
    "production",
    "energy",
    "quality",
    "downtime",
    "analytics",
    "maintenance",
    "reporting",
    "audit",
    "shared"
)

foreach ($module in $requiredModules) {
    Assert-Contains $architecture "\b$module\b" "Architecture baseline must mention module '$module'."
}

$requiredArchitectureBoundaries = @(
    "PLC",
    "SCADA",
    "HMI",
    "OPC UA",
    "MQTT",
    "Modbus",
    "industrial gateways",
    "never actuates a machine"
)

foreach ($boundary in $requiredArchitectureBoundaries) {
    Assert-True ($architecture.Contains($boundary)) "Architecture baseline must preserve boundary '$boundary'."
}

$requiredDependencyRules = @(
    "Controllers call application services",
    "Application services own use cases",
    "Domain owns entities",
    "Persistence implements repository ports",
    "JPA entities never cross the REST boundary",
    "Cross-module cycles"
)

foreach ($rule in $requiredDependencyRules) {
    Assert-True ($architecture.Contains($rule)) "Architecture baseline is missing dependency rule '$rule'."
}

$traceability = Get-Content "docs/REQUIREMENTS_TRACEABILITY.md" -Raw
Assert-True ($traceability.Contains("106 explicit IDs")) "Traceability baseline must state the 106-ID inventory."
Assert-True ($traceability.Contains("OOS | 15")) "Traceability baseline must state the 15 out-of-scope IDs."
Assert-True ($traceability.Contains("Traceability chain")) "Traceability baseline must define the traceability chain."

Write-Host "WerkPilot agent kit validation passed."
Write-Host "Java: $($javaVersion -split "`n" | Select-Object -First 1)"
Write-Host "Node: $nodeVersion"
Write-Host "npm: $npmVersion"
Write-Host "Git: $gitVersion"
Write-Host "Maven: $($mavenVersion -split "`n" | Select-Object -First 1)"
Write-Host "Spring Boot parent: $springBootParentVersion"
Write-Host "Requirements: $($requirements.Count) unique IDs"
Write-Host "Architecture diagrams: $mermaidCount Mermaid blocks"
