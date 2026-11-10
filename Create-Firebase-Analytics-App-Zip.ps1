
# ============================================================
# Firebase Analytics Android App - Source ZIP Generator
# Windows PowerShell 5.1 / PowerShell 7+
#
# Purpose:
# Package Android source code responsible for sending
# Firebase Analytics / GA4 events.
#
# ZIP size target: 100 MB per archive
# ============================================================

$ErrorActionPreference = "Stop"

# ---------------- CONFIGURATION ----------------

$MaxFileSizeMB = 10
$MaxZipSizeMB = 100
$OutputFolderName = "Firebase_Analytics_Source"

$ProjectRoot = $PSScriptRoot

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Get-Location).Path
}

$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
$ProjectName = Split-Path $ProjectRoot -Leaf
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"

$OutputFolder = Join-Path $ProjectRoot $OutputFolderName

New-Item -ItemType Directory -Path $OutputFolder -Force |
    Out-Null

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host " FIREBASE ANALYTICS SOURCE ZIP GENERATOR" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Project: $ProjectName"
Write-Host ""

# ---------------- EXCLUDED DIRECTORIES ----------------

$ExcludedDirectories = @(
    ".git",
    ".gradle",
    ".idea",
    ".kotlin",
    ".vscode",
    ".vs",
    "build",
    "out",
    "node_modules",
    "captures",
    ".cxx",
    ".externalNativeBuild",
    "target",
    "venv",
    ".venv",
    "__pycache__",
    "dist",
    "Pods",
    "Firebase_Analytics_Source"
)

# ---------------- ALLOWED EXTENSIONS ----------------

$AllowedExtensions = @(
    ".kt",
    ".kts",
    ".java",
    ".xml",
    ".gradle",
    ".properties",
    ".toml",
    ".pro",
    ".rules",
    ".md",
    ".txt",
    ".json",
    ".yaml",
    ".yml",
    ".proto",
    ".sh",
    ".bat",
    ".ps1",
    ".conf",
    ".ini"
)

# ---------------- EXCLUDED FILES ----------------

$ExcludedFilePatterns = @(
    "local.properties",
    "secrets.properties",
    "google-services.json",
    "*.jks",
    "*.keystore",
    "*.p12",
    "*.pfx",
    "*.db",
    "*.sqlite",
    "*.sqlite3",
    "*.log",
    "*.apk",
    "*.aab",
    "*.aar",
    "*.jar",
    "*.class",
    "*.dex",
    "*.tflite",
    "*.pth",
    "*.onnx",
    "*.bin",
    "*.png",
    "*.jpg",
    "*.jpeg",
    "*.webp",
    "*.gif",
    "*.mp4",
    "*.pdf",
    "*.xlsx",
    "*.xls",
    "*.csv"
)

# ---------------- SENSITIVE FILES ----------------

$SensitiveFilePatterns = @(
    ".env",
    ".env.*",
    "*service-account*.json",
    "*firebase-adminsdk*.json",
    "*credentials*.json",
    "*access-token*",
    "*refresh-token*",
    "*private-key*"
)

# ---------------- HELPER: RELATIVE PATH ----------------

function Get-RelativePath {
    param(
        [string]$BasePath,
        [string]$FullPath
    )

    return $FullPath.Substring(
        $BasePath.Length
    ).TrimStart('\', '/')
}

# ---------------- HELPER: FILE VALIDATION ----------------

function Test-EligibleFile {
    param(
        [System.IO.FileInfo]$File
    )

    $RelativePath = Get-RelativePath `
        -BasePath $ProjectRoot `
        -FullPath $File.FullName

    $Segments = $RelativePath -split '[\\/]'

    foreach ($Segment in $Segments) {
        if ($ExcludedDirectories -contains $Segment) {
            return $false
        }
    }

    if ($AllowedExtensions -notcontains $File.Extension.ToLowerInvariant()) {
        return $false
    }

    foreach ($Pattern in $ExcludedFilePatterns) {
        if ($File.Name -like $Pattern) {
            return $false
        }
    }

    foreach ($Pattern in $SensitiveFilePatterns) {
        if ($File.Name -like $Pattern) {
            return $false
        }
    }

    if ($File.Length -gt ($MaxFileSizeMB * 1MB)) {
        return $false
    }

    return $true
}

# ---------------- COLLECT FILES ----------------

Write-Host "Scanning Android application..." -ForegroundColor Yellow

$AllFiles = Get-ChildItem `
    -LiteralPath $ProjectRoot `
    -Recurse `
    -File `
    -Force

$IncludedFiles = New-Object System.Collections.Generic.List[System.IO.FileInfo]

$ExcludedCount = 0

foreach ($File in $AllFiles) {

    if (Test-EligibleFile -File $File) {
        $IncludedFiles.Add($File)
    }
    else {
        $ExcludedCount++
    }
}

if ($IncludedFiles.Count -eq 0) {
    throw "No eligible source files found."
}

Write-Host "Included files: $($IncludedFiles.Count)" -ForegroundColor Green
Write-Host "Excluded files: $ExcludedCount" -ForegroundColor Yellow

# ---------------- IDENTIFY IMPORTANT FILES ----------------

$ImportantPatterns = @(
    "*Analytics*",
    "*Firebase*",
    "*Event*",
    "*Tracking*",
    "*Telemetry*",
    "*Purchase*",
    "*Checkout*",
    "*Cart*",
    "*Screen*",
    "*Repository*",
    "*Api*",
    "*Manifest*",
    "libs.versions.toml",
    "settings.gradle*",
    "build.gradle*",
    "gradle.properties",
    "README*"
)

$ImportantFiles = New-Object System.Collections.Generic.List[string]

foreach ($File in $IncludedFiles) {

    foreach ($Pattern in $ImportantPatterns) {

        if ($File.Name -like $Pattern) {
            $ImportantFiles.Add(
                (Get-RelativePath `
                    -BasePath $ProjectRoot `
                    -FullPath $File.FullName)
            )
            break
        }
    }
}

# ---------------- CREATE ZIP PACKAGES ----------------

Add-Type -AssemblyName System.IO.Compression.FileSystem

$CreatedArchives = New-Object System.Collections.Generic.List[string]

$FileGroups = @(
    @{
        Name = "01_Analytics_And_Firebase"
        Files = @(
            $IncludedFiles | Where-Object {
                $_.FullName -match '[\\/](analytics|firebase|tracking|telemetry)[\\/]?' -or
                $_.Name -match 'Analytics|Firebase|Event|Tracking|Telemetry'
            }
        )
    },
    @{
        Name = "02_Android_Application"
        Files = @(
            $IncludedFiles | Where-Object {
                $_.FullName -match '[\\/](app|androidApp)[\\/]'
            }
        )
    },
    @{
        Name = "03_Network_And_Data"
        Files = @(
            $IncludedFiles | Where-Object {
                $_.Name -match 'Api|Retrofit|Ktor|Repository|DataSource|Dto|Request|Response'
            }
        )
    },
    @{
        Name = "04_Gradle_And_Configuration"
        Files = @(
            $IncludedFiles | Where-Object {
                $_.Name -match 'gradle|Manifest|README|\.toml$'
            }
        )
    }
)

function Create-SourceZip {
    param(
        [string]$ZipName,
        [System.IO.FileInfo[]]$Files
    )

    if ($Files.Count -eq 0) {
        Write-Host "Skipped empty package: $ZipName" -ForegroundColor DarkYellow
        return
    }

    $ZipPath = Join-Path $OutputFolder (
        "${ProjectName}_${ZipName}_${Timestamp}.zip"
    )

    if (Test-Path -LiteralPath $ZipPath) {
        Remove-Item -LiteralPath $ZipPath -Force
    }

    $Archive = [System.IO.Compression.ZipFile]::Open(
        $ZipPath,
        [System.IO.Compression.ZipArchiveMode]::Create
    )

    try {

        foreach ($File in $Files) {

            $RelativePath = Get-RelativePath `
                -BasePath $ProjectRoot `
                -FullPath $File.FullName

            $EntryName = $RelativePath.Replace('\', '/')

            [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
                $Archive,
                $File.FullName,
                $EntryName,
                [System.IO.Compression.CompressionLevel]::Optimal
            ) | Out-Null
        }
    }
    finally {
        $Archive.Dispose()
    }

    $SizeMB = (
        (Get-Item -LiteralPath $ZipPath).Length / 1MB
    )

    if ($SizeMB -gt $MaxZipSizeMB) {
        Remove-Item -LiteralPath $ZipPath -Force
        throw "Package exceeds 100 MB: $ZipName. Use smaller source groups."
    }

    $CreatedArchives.Add($ZipPath)

    Write-Host (
        "Created: {0} | {1:N2} MB | {2} files" -f `
        $ZipName,
        $SizeMB,
        $Files.Count
    ) -ForegroundColor Green
}

foreach ($Group in $FileGroups) {

    Create-SourceZip `
        -ZipName $Group.Name `
        -Files @($Group.Files)
}

# ---------------- CREATE COMPLETE SOURCE PACKAGE ----------------

# Include all source files in one ZIP when it fits.
$CompleteZipName = "05_Complete_Android_Source"

Create-SourceZip `
    -ZipName $CompleteZipName `
    -Files @($IncludedFiles)

# ---------------- CREATE IMPORTANT FILE MANIFEST ----------------

$ManifestPath = Join-Path $OutputFolder "IMPORTANT_FILES.txt"

$ImportantFiles |
    Sort-Object -Unique |
    Set-Content -LiteralPath $ManifestPath -Encoding UTF8

# ---------------- VERIFY ZIP FILES ----------------

Write-Host ""
Write-Host "Verifying generated archives..." -ForegroundColor Yellow

foreach ($Zip in $CreatedArchives) {

    $Archive = [System.IO.Compression.ZipFile]::OpenRead($Zip)

    try {

        if ($Archive.Entries.Count -eq 0) {
            throw "Empty ZIP archive: $Zip"
        }

        foreach ($Entry in $Archive.Entries) {

            $Stream = $Entry.Open()

            try {

                $Buffer = New-Object byte[] 81920

                while ($Stream.Read(
                    $Buffer,
                    0,
                    $Buffer.Length
                ) -gt 0) {
                    # Read the full compressed entry.
                }
            }
            finally {
                $Stream.Dispose()
            }
        }

        Write-Host (
            "VERIFIED: {0}" -f (Split-Path $Zip -Leaf)
        ) -ForegroundColor Green
    }
    finally {
        $Archive.Dispose()
    }
}

# ---------------- FINAL SUMMARY ----------------

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "         ZIP CREATION COMPLETED" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

Write-Host "Project: $ProjectName"
Write-Host "Included source files: $($IncludedFiles.Count)"
Write-Host "Excluded files: $ExcludedCount"
Write-Host "Generated ZIP files: $($CreatedArchives.Count)"
Write-Host "Output folder: $OutputFolder"

Write-Host ""
Write-Host "IMPORTANT FILES:"
Write-Host $ManifestPath

Write-Host ""
Write-Host "All generated ZIP archives passed integrity checks." -ForegroundColor Green
