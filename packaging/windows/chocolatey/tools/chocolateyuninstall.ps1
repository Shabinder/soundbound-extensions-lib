$ErrorActionPreference = 'Stop'
# Uninstall via the registry UninstallString with /S. The package installs all-users (HKLM); HKCU is
# also searched so a manual per-user install is reported rather than silently left behind.
[array]$keys = Get-UninstallRegistryKey -SoftwareName 'SoundBound*'
if ($keys.Count -eq 1) {
  $file = ($keys[0].UninstallString -replace '"', '')
  Uninstall-ChocolateyPackage -PackageName $env:ChocolateyPackageName -FileType 'exe' `
    -SilentArgs '/S' -File $file -ValidExitCodes @(0)
} elseif ($keys.Count -eq 0) {
  Write-Warning "SoundBound has already been uninstalled by other means."
} else {
  Write-Warning "$($keys.Count) SoundBound installs found (per-user and all-users?); not guessing which to remove:"
  $keys | ForEach-Object { Write-Warning "- $($_.DisplayName) $($_.DisplayVersion) at $($_.InstallLocation)" }
}
