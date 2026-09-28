@echo off
setlocal enabledelayedexpansion

echo Setting StockPulse AI Environment Variables from .env...

set "ENV_FILE=..\.env"
if not exist "%ENV_FILE%" set "ENV_FILE=.env"
if not exist "%ENV_FILE%" set "ENV_FILE=..\env"
if not exist "%ENV_FILE%" set "ENV_FILE=env"

if not exist "%ENV_FILE%" (
    echo ERROR: .env file not found.
    echo Please create a .env file based on .env.template
    exit /b 1
)

for /f "usebackq tokens=1,* delims==" %%a in ("%ENV_FILE%") do (
    set "line=%%a"
    if not "!line:~0,1!"=="#" (
        if not "%%a"=="" (
            endlocal
            set "%%a=%%b"
            setlocal enabledelayedexpansion
        )
    )
)

echo Environment configured successfully from .env file!
echo Active Variables:
echo LLM_PROVIDER   = %LLM_PROVIDER%
echo LLM_MODEL      = %LLM_MODEL%
echo LLM_BASE_URL   = %LLM_BASE_URL%
echo LLM_API_KEY    = (set securely from .env)
echo.
echo You can now run: cd ..\Backend ^& .\mvnw.cmd spring-boot:run
endlocal
