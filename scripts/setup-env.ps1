# StockPulse - Set Environment Variables for Current PowerShell Session
# Updated to read values from .env file instead of hardcoding

Write-Host "Setting StockPulse AI Environment Variables from .env..." -ForegroundColor Cyan

# Define the path to the .env file (relative to project root)
$envFilePath = "..\.env"

# Check if .env file exists
if (-not (Test-Path $envFilePath)) {
    Write-Host "ERROR: .env file not found at $envFilePath" -ForegroundColor Red
    Write-Host "Please create a .env file based on ..\.env.template" -ForegroundColor Yellow
    exit 1
}

# Function to parse .env file
function Parse-EnvFile {
    param (
        [string]$Path
    )
    
    $envVars = @{}
    
    Get-Content $Path | ForEach-Object {
        # Skip empty lines and comments
        if ($_ -match "^\s*#" -or $_ -match "^\s*$") {
            return
        }
        
        # Parse KEY=VALUE pairs
        if ($_ -match "^([^=]+)=(.*)$") {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()
            
            # Remove quotes if present
            if ($value -match '^"(.*)"$' -or $value -match "^'(.*)'$") {
                $value = $matches[1]
            }
            
            $envVars[$key] = $value
        }
    }
    
    return $envVars
}

# Load environment variables from .env file
try {
    $envVars = Parse-EnvFile -Path $envFilePath
    
    # Set environment variables
    foreach ($key in $envVars.Keys) {
        $env:$key = $envVars[$key]
        Write-Host "Set $key = $($envVars[$key])" -ForegroundColor Green
    }
    
    Write-Host "Environment configured successfully from .env file!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Active Environment Variables:" -ForegroundColor Cyan
    Write-Host "LLM_PROVIDER   = $env:LLM_PROVIDER"
    Write-Host "LLM_MODEL      = $env:LLM_MODEL"
    Write-Host "LLM_BASE_URL   = $env:LLM_BASE_URL"
    Write-Host "LLM_API_KEY    = (set securely from .env)"
    Write-Host ""
    Write-Host "You can now run: cd ..\Backend; .\mvnw.cmd spring-boot:run" -ForegroundColor Yellow
}
catch {
    Write-Host "ERROR: Failed to load environment variables from .env file" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}
