# Scripts Directory

This directory contains utility scripts for setting up and running the StockPulse application.

## Environment Setup Scripts

These scripts help load environment variables from the `.env` file located in the project root directory.

### PowerShell Script (Windows)
- **File**: `setup-env.ps1`
- **Usage**: 
  ```powershell
  .\setup-env.ps1
  ```

### Bash Script (Linux/macOS)
- **File**: `setup-env.sh`
- **Usage**: 
  ```bash
  source ./setup-env.sh
  ```
  or
  ```bash
  . ./setup-env.sh
  ```

### Batch Script (Windows Command Prompt)
- **File**: `setup-env.bat`
- **Usage**: 
  ```cmd
  setup-env.bat
  ```

## Prerequisites

Before using these scripts, make sure you have:

1. Created a `.env` file in the project root directory based on `.env.template`
2. Updated the `.env` file with your actual credentials

## How It Works

1. The scripts look for a `.env` file in the parent directory (`../.env`)
2. They parse the file, skipping comments and empty lines
3. They set each `KEY=VALUE` pair as an environment variable in the current session
4. They display the loaded variables (except sensitive ones like API keys)

## Security Note

Never commit your `.env` file to version control. The `.env` file is included in `.gitignore` to prevent accidental commits.