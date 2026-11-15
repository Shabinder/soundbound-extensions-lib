$ErrorActionPreference = 'Stop'
# Uninstall via the registry UninstallString with /S. This package installs all-users (Program Files,
# HKLM). Only that install is removed: a user's own per-user install (HKCU, LocalAppData) is someone
# else's and must not block or be taken by this package.
$pf = [Environment]::GetFolderPath('ProgramFiles')
[array]$keys = Get-UninstallRegistryKey -SoftwareName 'SoundBound*' |
  Where-Object { $_.InstallLocation -and $_.InstallLocation.StartsWith($pf, [StringComparison]::OrdinalIgnoreCase) }
if ($keys.Count -eq 1) {
  $file = ($keys[0].UninstallString -replace '"', '')
  Uninstall-ChocolateyPackage -PackageName $env:ChocolateyPackageName -FileType 'exe' `
    -SilentArgs '/S' -File $file -ValidExitCodes @(0)
} elseif ($keys.Count -eq 0) {
  Write-Warning "No all-users SoundBound install under $pf; it has already been uninstalled by other means."
} else {
  Write-Warning "$($keys.Count) all-users SoundBound installs found under $pf; not guessing which to remove:"
  $keys | ForEach-Object { Write-Warning "- $($_.DisplayName) $($_.DisplayVersion) at $($_.InstallLocation)" }
}
