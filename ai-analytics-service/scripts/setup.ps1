$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$venvPath = Join-Path $projectRoot "venv"
$venvPython = Join-Path $venvPath "Scripts\python.exe"

Push-Location $projectRoot
try {
    $python314 = py -3.14 -c "import sys; print(sys.executable)"
    if (-not $python314) {
        throw "Python 3.14 is required. Install it from https://www.python.org/downloads/"
    }

    if (Test-Path -LiteralPath $venvPython) {
        $version = & $venvPython -c "import sys; print(f'{sys.version_info.major}.{sys.version_info.minor}')"
        if ($version -ne "3.14") {
            Write-Host "Replacing Python $version virtual environment with Python 3.14..."
            $resolvedVenv = (Resolve-Path -LiteralPath $venvPath).Path
            if ($resolvedVenv -ne (Join-Path (Resolve-Path ".").Path "venv")) {
                throw "Refusing to remove unexpected path: $resolvedVenv"
            }
            Remove-Item -LiteralPath $resolvedVenv -Recurse -Force
        }
    }

    if (-not (Test-Path -LiteralPath $venvPython)) {
        py -3.14 -m venv $venvPath
    }

    & $venvPython -m pip install --upgrade pip
    & $venvPython -m pip install -r requirements-dev.txt

    if (-not (Test-Path -LiteralPath ".env")) {
        Copy-Item -LiteralPath ".env.example" -Destination ".env"
    }

    Write-Host "Setup complete. Run: .\venv\Scripts\python app/main.py"
}
finally {
    Pop-Location
}
