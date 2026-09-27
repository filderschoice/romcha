@echo off
rem Romcha の署名済みリリース APK をビルドするバッチファイル（BL-063）。
rem 実処理は release-build.ps1（PowerShell）に委譲する。手順は docs/RELEASE.md を参照。
rem pwsh（PowerShell 7）が使える場合はそちらを優先する
rem （Windows PowerShell 5.1 はスクリプト内の日本語の扱いで問題が起きることがあるため）。
rem 例: scripts\release-build.bat
rem 例: scripts\release-build.bat -VersionName 1.0.1
rem 例: scripts\release-build.bat -SkipChecks -AllowUnsigned
where pwsh >nul 2>nul
if %errorlevel%==0 (
    pwsh -NoProfile -ExecutionPolicy Bypass -File "%~dp0release-build.ps1" %*
) else (
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0release-build.ps1" %*
)
exit /b %errorlevel%
