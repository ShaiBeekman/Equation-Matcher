[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$ZipPath,
    [ValidatePattern('^[A-Fa-f0-9]{64}$')][string]$ExpectedSha256 = '1E19FAE300E37DA503BFDE2363138562E8FF492D7BFD48D5E00081917AA88324',
    [Parameter(Mandatory)][string]$EvidenceDirectory
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$ZipPath = (Resolve-Path -LiteralPath $ZipPath).Path
New-Item -ItemType Directory -Force -Path $EvidenceDirectory | Out-Null
$EvidenceDirectory = (Resolve-Path -LiteralPath $EvidenceDirectory).Path
$observed = (Get-FileHash -LiteralPath $ZipPath -Algorithm SHA256).Hash
Write-Output "EXPECTED_SHA256=$ExpectedSha256"
Write-Output "OBSERVED_SHA256=$observed"
if ($observed -ne $ExpectedSha256) { throw 'Release ZIP hash mismatch. Nothing will be extracted or executed.' }
$os = Get-CimInstance Win32_OperatingSystem
$environment = [ordered]@{
    WindowsCaption=$os.Caption; WindowsVersion=$os.Version; WindowsBuild=$os.BuildNumber
    RunnerImage=$env:ImageOS; RunnerImageVersion=$env:ImageVersion; RunnerName=$env:RUNNER_NAME
    OriginalPATH=$env:PATH; OriginalJAVA_HOME=$env:JAVA_HOME
    OriginalJava=@(Get-Command java -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source)
    OriginalMaven=@(Get-Command mvn -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source)
    Zip=$ZipPath; ExpectedSHA256=$ExpectedSha256; ObservedSHA256=$observed
}
$environment | ConvertTo-Json | Tee-Object -FilePath "$EvidenceDirectory\environment.json"
$work = Join-Path ([IO.Path]::GetTempPath()) ('equation-release-gate-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $work | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
[IO.Compression.ZipFile]::ExtractToDirectory($ZipPath, "$work\extracted")
$image = "$work\extracted\EquationMatcher"
foreach ($relative in 'EquationMatcher.exe','app\EquationMatcher.cfg','runtime\bin\java.exe',
        'runtime\bin\javac.exe','runtime\bin\server\jvm.dll','RUNTIME_MODULES.txt','README.md','LICENSE','THIRD_PARTY_NOTICES.md') {
    if (-not (Test-Path -LiteralPath "$image\$relative")) { throw "Missing packaged file: $relative" }
}
$jar = @(Get-ChildItem -LiteralPath "$image\app" -Filter 'equation-matcher-*.jar')
if ($jar.Count -ne 1) { throw 'Expected one packaged application JAR.' }
$java = "$image\runtime\bin\java.exe"
$exe = "$image\EquationMatcher.exe"
Get-AuthenticodeSignature -LiteralPath $exe | Select-Object Status,StatusMessage | ConvertTo-Json |
    Set-Content -LiteralPath "$EvidenceDirectory\signature.json"
Get-Item -LiteralPath $ZipPath -Stream * | Select-Object Stream,Length | ConvertTo-Json |
    Set-Content -LiteralPath "$EvidenceDirectory\download-streams.json"

function Invoke-Checked([string]$File, [string[]]$Arguments, [string]$LogName, [int]$Seconds = 120) {
    $start = New-Object Diagnostics.ProcessStartInfo
    $start.FileName = $File
    # All arguments here are generated flags/paths, never source code or shell commands.
    $start.Arguments = ($Arguments | ForEach-Object { '"' + $_.Replace('"','\"') + '"' }) -join ' '
    $start.WorkingDirectory = $work
    $start.UseShellExecute = $false; $start.CreateNoWindow = $true
    $start.RedirectStandardOutput = $true; $start.RedirectStandardError = $true
    $process = New-Object Diagnostics.Process
    $process.StartInfo = $start
    if (-not $process.Start()) { throw "Could not launch $File" }
    $stdout = $process.StandardOutput.ReadToEndAsync(); $stderr = $process.StandardError.ReadToEndAsync()
    if (-not $process.WaitForExit($Seconds * 1000)) { $process.Kill(); throw "$LogName exceeded $Seconds seconds" }
    $output = $stdout.GetAwaiter().GetResult() + $stderr.GetAwaiter().GetResult()
    $output | Tee-Object -FilePath "$EvidenceDirectory\$LogName.txt" | Write-Output
    if ($process.ExitCode -ne 0) { throw "$LogName failed with exit code $($process.ExitCode)" }
    $process.Dispose()
}
function Get-PackagedProcesses {
    @(Get-CimInstance Win32_Process | Where-Object {
        $_.ExecutablePath -and $_.ExecutablePath.StartsWith($image + '\', [StringComparison]::OrdinalIgnoreCase)
    })
}
$variableNames = @('PATH','JAVA_HOME','JDK_HOME','MAVEN_HOME','M2_HOME','CLASSPATH',
    'JAVA_TOOL_OPTIONS','_JAVA_OPTIONS','JDK_JAVA_OPTIONS','TEMP','TMP','APPDATA','LOCALAPPDATA','USERPROFILE')
$saved = @{}
foreach ($name in $variableNames) { $saved[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
$passed = $false
try {
    New-Item -ItemType Directory -Path "$work\profile", "$work\tmp", "$work\roaming", "$work\local", "$work\helper" | Out-Null
    $env:PATH = "$env:SystemRoot\System32;$env:SystemRoot"
    foreach ($name in 'JAVA_HOME','JDK_HOME','MAVEN_HOME','M2_HOME','CLASSPATH','JAVA_TOOL_OPTIONS','_JAVA_OPTIONS','JDK_JAVA_OPTIONS') {
        [Environment]::SetEnvironmentVariable($name, $null, 'Process')
    }
    $env:TEMP="$work\tmp"; $env:TMP=$env:TEMP; $env:USERPROFILE="$work\profile"
    $env:APPDATA="$work\roaming"; $env:LOCALAPPDATA="$work\local"
    # Windows account identity does not change with USERPROFILE. Explicit user.home ensures an empty JavaFX cache.
    $env:JAVA_TOOL_OPTIONS='-Duser.home="' + "$work\profile" + '"'
    if (Get-Command java,mvn -ErrorAction SilentlyContinue) { throw 'External Java or Maven remains on restricted PATH.' }
    Write-Output "TEST_PATH=$env:PATH"
    Write-Output "TEST_JAVA_HOME=$env:JAVA_HOME"
    Write-Output "EXTRACTED_IMAGE=$image"
    Invoke-Checked $java @('-version') 'java-version'
    Invoke-Checked $java @('--list-modules') 'runtime-modules'
    # No production code is rebuilt: only the external helper, using the release compiler and release API.
    Copy-Item -LiteralPath "$PSScriptRoot\ReleaseGateVerification.java" -Destination "$work\helper"
    Invoke-Checked "$image\runtime\bin\javac.exe" @('--add-modules','javafx.controls,jdk.compiler','-cp',
        $jar[0].FullName,'-d',"$work\helper","$work\helper\ReleaseGateVerification.java") 'helper-compile'
    Invoke-Checked $java @('--add-modules','javafx.controls,jdk.compiler','--enable-native-access=javafx.graphics,ALL-UNNAMED',
        '-cp',"$work\helper;$($jar[0].FullName)",'ReleaseGateVerification',$image) 'backend-verification'
    if (@(Get-PackagedProcesses).Count -ne 0) { throw 'Packaged process leaked after backend verification.' }

    # Primary application check: the native EXE, never Maven or java -jar.
    $launcher = Start-Process -FilePath $exe -WorkingDirectory $image -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput "$EvidenceDirectory\exe-stdout.txt" -RedirectStandardError "$EvidenceDirectory\exe-stderr.txt"
    $deadline = [DateTime]::UtcNow.AddSeconds(40)
    $window = $null
    while ([DateTime]::UtcNow -lt $deadline) {
        foreach ($item in Get-PackagedProcesses) {
            if ($item.ExecutablePath -eq $exe) {
                $candidate = Get-Process -Id $item.ProcessId -ErrorAction SilentlyContinue
                if ($candidate -and $candidate.MainWindowTitle -eq 'Equation Matcher') { $window = $candidate; break }
            }
        }
        if ($window) { break }
        Start-Sleep -Milliseconds 250
    }
    if (-not $window) { throw 'Native EXE did not initialize its Equation Matcher window within 40 seconds.' }
    Start-Sleep -Seconds 3
    $window.Refresh()
    if ($window.HasExited) { throw 'Native EXE exited after initialization.' }
    $nativeModules = @($window.Modules | Where-Object { $_.ModuleName -match '^(jvm|glass|prism_d3d|prism_sw|javafx_font)\.dll$' })
    $nativeModules | Select-Object ModuleName,FileName | ConvertTo-Json | Tee-Object -FilePath "$EvidenceDirectory\native-modules.json"
    $jvm = @($nativeModules | Where-Object ModuleName -eq 'jvm.dll')
    if ($jvm.Count -ne 1 -or $jvm[0].FileName -ne "$image\runtime\bin\server\jvm.dll") { throw 'EXE did not load bundled JVM.' }
    if (-not ($nativeModules | Where-Object ModuleName -eq 'glass.dll')) { throw 'Native JavaFX glass library not loaded.' }
    foreach ($module in $nativeModules) {
        if (-not $module.FileName.StartsWith($work + '\', [StringComparison]::OrdinalIgnoreCase)) { throw "Unexpected external native dependency: $($module.FileName)" }
    }
    Write-Output "PASS: Native EquationMatcher.exe initialized window; PID=$($window.Id); bundled JVM and freshly extracted JavaFX natives"
    if (-not $window.CloseMainWindow()) { throw 'Could not request normal native application shutdown.' }
    $deadline = [DateTime]::UtcNow.AddSeconds(15)
    while (@(Get-PackagedProcesses).Count -gt 0 -and [DateTime]::UtcNow -lt $deadline) { Start-Sleep -Milliseconds 250 }
    if (@(Get-PackagedProcesses).Count -gt 0) { throw 'Packaged application or worker survived normal shutdown.' }
    Write-Output 'PASS: Normal native shutdown; no packaged processes remain'
    if ((Get-FileHash -LiteralPath $ZipPath -Algorithm SHA256).Hash -ne $ExpectedSha256) { throw 'ZIP changed during verification.' }
    $passed = $true
} finally {
    # Scope cleanup strictly to this run's extracted image; never touch unrelated runner Java processes.
    $leftovers = @(Get-PackagedProcesses)
    $leftovers | Select-Object ProcessId,ParentProcessId,ExecutablePath,CommandLine | ConvertTo-Json |
        Set-Content -LiteralPath "$EvidenceDirectory\leftover-processes.json"
    foreach ($item in $leftovers) { Stop-Process -Id $item.ProcessId -Force -ErrorAction SilentlyContinue }
    foreach ($name in $variableNames) { [Environment]::SetEnvironmentVariable($name, $saved[$name], 'Process') }
    [ordered]@{Passed=$passed;ExpectedSHA256=$ExpectedSha256;ObservedSHA256=$observed;Image=$image;
        BundledJava=$java;RunnerImage=$environment.RunnerImage;RunnerImageVersion=$environment.RunnerImageVersion;
        Windows=$environment.WindowsCaption;WindowsVersion=$environment.WindowsVersion;
        NativeGUI='EXE window initialization and normal shutdown; no interactive visual review in CI';
        SmartScreen='Not evaluated: Actions downloads do not reproduce consumer interactive reputation prompts'} |
        ConvertTo-Json | Set-Content -LiteralPath "$EvidenceDirectory\result.json"
}
if (-not $passed) { throw 'Release verification did not pass.' }
Write-Output 'PASS: EXACT-HASH WINDOWS RELEASE GATE'
