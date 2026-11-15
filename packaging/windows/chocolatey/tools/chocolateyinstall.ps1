$ErrorActionPreference = 'Stop'
# Placeholders (__VERSION__, __URL__, __SHA256__) are filled in at release time.
# All-users install (/ALLUSERS -> Program Files + HKLM): Chocolatey runs elevated, often as another
# admin or SYSTEM, where the installer's default per-user mode would land in THAT profile.
$packageArgs = @{
  packageName    = $env:ChocolateyPackageName
  fileType       = 'exe'
  url64bit       = '__URL__'
  checksum64     = '__SHA256__'
  checksumType64 = 'sha256'
  silentArgs     = '/S /ALLUSERS'
  validExitCodes = @(0)
  softwareName   = 'SoundBound*'
}
Install-ChocolateyPackage @packageArgs
