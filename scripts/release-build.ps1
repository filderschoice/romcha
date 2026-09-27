<#
.SYNOPSIS
    Romcha の署名済みリリース APK を 1 コマンドでビルドする（BL-063）。

.DESCRIPTION
    次の順に実行し、途中で失敗したらその時点で止める。手順の詳細は docs/RELEASE.md。

      1. 事前確認: local.properties の署名情報（RELEASE_STORE_FILE・RELEASE_STORE_PASSWORD・
         RELEASE_KEY_ALIAS・RELEASE_KEY_PASSWORD）とキーストアのファイルがあるか。値は表示しない。
      2. 版の更新（-VersionName を指定した時だけ）: app/build.gradle.kts の versionName と versionCode
         （MAJOR × 10000 + MINOR × 100 + PATCH。docs/RELEASE.md 2章）を書き換える。
         ビルドに失敗した場合は元の内容へ戻す。成功した場合は変更をコミットする必要がある（スクリプトはコミットしない）。
      3. 品質ゲート（Gradle 分）: ktlintCheck detekt lintDebug compileDebugKotlin と単体テスト（-SkipChecks で省略）。
      4. :app:releaseDist で app/build/dist/ に romcha-vX.Y.Z.apk と .sha256 を出力する。
      5. apksigner で署名を確かめ、証明書の SHA-256 を表示する（前回の版と同じ鍵かの確認用）。
      6. タグの作成・push と GitHub Releases への公開のコマンド例を表示する（実行はしない）。

    git push・タグの作成・Releases の公開は行わない（人が判断して実行する）。

.PARAMETER VersionName
    ビルドする版（SemVer の MAJOR.MINOR.PATCH。MINOR・PATCH は 0〜99）。省略時は app/build.gradle.kts の現在の版。

.PARAMETER SkipChecks
    品質ゲート（静的解析・型検査・単体テスト）を省略する。

.PARAMETER AllowUnsigned
    署名情報が無くても止めずに、未署名の APK（romcha-vX.Y.Z-unsigned.apk）をビルドする（動作確認用。公開しないこと）。

.EXAMPLE
    scripts\release-build.bat
    現在の版で、品質ゲートを通してから署名済み APK をビルドする。

.EXAMPLE
    scripts\release-build.bat -VersionName 1.0.1
    版を 1.0.1（versionCode 10001）へ上げてビルドする。
#>
param(
    [string]$VersionName,
    [switch]$SkipChecks,
    [switch]$AllowUnsigned
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$gradleFile = Join-Path $repoRoot "app\build.gradle.kts"
$localPropertiesFile = Join-Path $repoRoot "local.properties"
$gradlew = Join-Path $repoRoot "gradlew.bat"
$distDir = Join-Path $repoRoot "app\build\dist"
$signingKeys = @("RELEASE_STORE_FILE", "RELEASE_STORE_PASSWORD", "RELEASE_KEY_ALIAS", "RELEASE_KEY_PASSWORD")

function Write-Step([string]$message) {
    Write-Host ""
    Write-Host "== $message" -ForegroundColor Cyan
}

# properties 形式の最小限の読み取り（key=value と key:value、# ! のコメント、\ のエスケープ）。値は呼び出し側で表示しない
function Read-Properties([string]$path) {
    $result = @{}
    if (-not (Test-Path -LiteralPath $path)) { return $result }
    foreach ($line in [System.IO.File]::ReadAllLines($path)) {
        $trimmed = $line.Trim()
        if ($trimmed -eq "" -or $trimmed.StartsWith("#") -or $trimmed.StartsWith("!")) { continue }
        if ($trimmed -match '^([^=:\s]+)\s*[=:]\s*(.*)$') {
            $result[$Matches[1]] = [regex]::Replace($Matches[2], '\\(.)', '$1')
        }
    }
    return $result
}

function Get-VersionCode([string]$name) {
    if ($name -notmatch '^(\d{1,4})\.(\d{1,2})\.(\d{1,2})$') {
        throw "版の形式が違います: '$name'（MAJOR.MINOR.PATCH。MINOR・PATCH は 0〜99。docs/RELEASE.md 2章）"
    }
    return [int]$Matches[1] * 10000 + [int]$Matches[2] * 100 + [int]$Matches[3]
}

function Find-Apksigner {
    $sdkDirs = @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT)
    $sdkDir = (Read-Properties $localPropertiesFile)["sdk.dir"]
    if ($sdkDir) { $sdkDirs += $sdkDir }
    foreach ($dir in $sdkDirs | Where-Object { $_ }) {
        $buildTools = Join-Path $dir "build-tools"
        if (-not (Test-Path -LiteralPath $buildTools)) { continue }
        $candidate = Get-ChildItem -LiteralPath $buildTools -Directory |
            Sort-Object { try { [version]$_.Name } catch { [version]"0.0" } } -Descending |
            ForEach-Object { Join-Path $_.FullName "apksigner.bat" } |
            Where-Object { Test-Path -LiteralPath $_ } |
            Select-Object -First 1
        if ($candidate) { return $candidate }
    }
    return $null
}

function Invoke-Gradle([string[]]$arguments) {
    Push-Location $repoRoot
    try {
        & $gradlew @arguments
        if ($LASTEXITCODE -ne 0) { throw "Gradle が失敗しました（終了コード: $LASTEXITCODE）: $($arguments -join ' ')" }
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path -LiteralPath $gradlew)) { throw "gradlew.bat が見つかりません: $gradlew" }

# ---- 1. 事前確認 ----
Write-Step "署名情報の確認（local.properties）"
$properties = Read-Properties $localPropertiesFile
$missing = @($signingKeys | Where-Object { [string]::IsNullOrWhiteSpace($properties[$_]) })
$signed = $missing.Count -eq 0
if ($signed) {
    $storeFile = $properties["RELEASE_STORE_FILE"].Trim()
    if (-not [System.IO.Path]::IsPathRooted($storeFile)) { $storeFile = Join-Path $repoRoot $storeFile }
    if (-not (Test-Path -LiteralPath $storeFile -PathType Leaf)) {
        throw "RELEASE_STORE_FILE のキーストアが見つかりません: $storeFile"
    }
    Write-Host "署名情報: あり（キーストア: $storeFile）"
} elseif ($AllowUnsigned) {
    Write-Warning "署名情報が不足しています（$($missing -join ', ')）。-AllowUnsigned のため未署名でビルドします（公開しないでください）。"
} else {
    throw "local.properties に署名情報がありません: $($missing -join ', ')。docs/RELEASE.md 1章を参照してください（未署名で試すなら -AllowUnsigned）。"
}

$gitStatus = git -C $repoRoot status --porcelain 2>$null
if ($gitStatus) {
    Write-Warning "コミットしていない変更があります。公開する APK はコミット済みの内容からビルドしてください。"
}

# ---- 2. 版の更新 ----
$originalGradle = [System.IO.File]::ReadAllText($gradleFile)
if ($originalGradle -notmatch 'versionName\s*=\s*"([^"]+)"') { throw "app/build.gradle.kts に versionName が見つかりません" }
$currentVersionName = $Matches[1]
$newVersionName = if ($PSBoundParameters.ContainsKey("VersionName")) { $VersionName.Trim().TrimStart("v", "V") } else { $currentVersionName }
$newVersionCode = Get-VersionCode $newVersionName

$latestTag = git -C $repoRoot tag --list "v*" --sort=-v:refname 2>$null | Select-Object -First 1
if ($latestTag -and $latestTag -match '^v(\d+\.\d+\.\d+)$') {
    $latestCode = Get-VersionCode $Matches[1]
    if ($newVersionCode -le $latestCode) {
        Write-Warning "版 $newVersionName（versionCode $newVersionCode）は既存のタグ $latestTag 以下です。上書きインストールできない・同じ版を二重に公開する恐れがあります。"
    }
}

$updatedGradle = $originalGradle -replace 'versionName\s*=\s*"[^"]+"', "versionName = `"$newVersionName`""
$updatedGradle = $updatedGradle -replace 'versionCode\s*=\s*\d+', "versionCode = $newVersionCode"
$versionChanged = $updatedGradle -ne $originalGradle
Write-Step "版: $newVersionName（versionCode $newVersionCode）$(if ($versionChanged) { '※ app/build.gradle.kts を更新します' })"
if ($versionChanged) {
    # 元のファイルの改行（LF）と BOM なし UTF-8 を保つ
    [System.IO.File]::WriteAllText($gradleFile, $updatedGradle, (New-Object System.Text.UTF8Encoding $false))
}

$succeeded = $false
try {
    # ---- 3. 品質ゲート ----
    if ($SkipChecks) {
        Write-Step "品質ゲート: 省略（-SkipChecks）"
    } else {
        Write-Step "品質ゲート（静的解析・型検査・単体テスト）"
        # local.properties は lint の解析タスクの入力に含まれず、書き換えてもビルドキャッシュの古い結果（PropertyEscape 等）が
        # 使われることがあるため、app の解析だけはやり直す（--rerun は直前のタスクにだけ効く）
        Invoke-Gradle @("ktlintCheck", "detekt", ":app:lintAnalyzeDebug", "--rerun", "lintDebug", "compileDebugKotlin",
            "testDebugUnitTest", ":core:chat:test", ":core:sync:test")
    }

    # ---- 4. リリース APK ----
    Write-Step "リリース APK のビルド（:app:releaseDist）"
    Invoke-Gradle @(":app:releaseDist")
    $succeeded = $true
} finally {
    if (-not $succeeded -and $versionChanged) {
        [System.IO.File]::WriteAllText($gradleFile, $originalGradle, (New-Object System.Text.UTF8Encoding $false))
        Write-Warning "失敗したため app/build.gradle.kts の版を $currentVersionName へ戻しました。"
    }
}

$baseName = "romcha-v$newVersionName$(if ($signed) { '' } else { '-unsigned' })"
$apk = Join-Path $distDir "$baseName.apk"
$sha = Join-Path $distDir "$baseName.apk.sha256"
if (-not (Test-Path -LiteralPath $apk)) { throw "配布物が見つかりません: $apk（署名情報を Gradle が読めていない可能性があります）" }

# ---- 5. 署名の確認 ----
if ($signed) {
    Write-Step "署名の確認（apksigner）"
    $apksigner = Find-Apksigner
    if ($apksigner) {
        $certs = & $apksigner verify --print-certs $apk
        if ($LASTEXITCODE -ne 0) { throw "apksigner の検証に失敗しました: $apk" }
        $certs | Where-Object { $_ -match "certificate (DN|SHA-256 digest)" } | ForEach-Object { Write-Host $_ }
        Write-Host "証明書の SHA-256 が前回の版と同じであることを確かめてください（違う場合は公開しない。docs/RELEASE.md 3章）。"
    } else {
        Write-Warning "apksigner が見つかりません（ANDROID_HOME の build-tools）。docs/RELEASE.md 3章の手順で手動で確かめてください。"
    }
}

# ---- 6. 結果と次の手順 ----
Write-Step "完了"
Write-Host "APK    : $apk"
Write-Host "SHA-256: $((Get-Content -LiteralPath $sha -Raw).Trim())"
if (-not $signed) {
    Write-Warning "未署名の APK です。公開しないでください。"
    exit 0
}
Write-Host ""
Write-Host "次の手順（docs/RELEASE.md 4〜7章。いずれも人が実行する）:"
if ($versionChanged) {
    Write-Host "  0. 版の変更（app/build.gradle.kts）をコミットし、Pull Request で main へマージする"
}
Write-Host "  1. 実機で確認する: adb install -r `"$apk`""
Write-Host "  2. タグを作って push する:"
Write-Host "       git tag -a v$newVersionName -m `"Romcha v$newVersionName`""
Write-Host "       git push origin v$newVersionName"
Write-Host "  3. GitHub Releases に公開する:"
Write-Host "       gh release create v$newVersionName `"$apk`" `"$sha`" --title `"Romcha v$newVersionName`" --notes-file <リリースノート>"
