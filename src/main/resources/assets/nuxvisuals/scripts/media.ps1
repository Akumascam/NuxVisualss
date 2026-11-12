# NuxVisuals: reads "Now playing" from Windows media sessions (Spotify, browsers...) and controls the player.
# Output: one JSON line per state. Commands: lines in a command file (toggle / next / prev / quit).
# NOTE: [Console]::In.ReadLineAsync() blocks in Windows PowerShell, so stdin is NOT used.
$ErrorActionPreference = 'SilentlyContinue'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Add-Type -AssemblyName System.Runtime.WindowsRuntime
Add-Type -AssemblyName System.Drawing

$asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
    $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
})[0]

function Await($op, [Type]$type) {
    if ($op -eq $null) { return $null }
    $task = $asTaskGeneric.MakeGenericMethod($type).Invoke($null, @($op))
    if ($task.Wait(3000)) { return $task.Result }
    return $null
}

$null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType=WindowsRuntime]
$null = [Windows.Storage.Streams.IRandomAccessStreamWithContentType, Windows.Storage.Streams, ContentType=WindowsRuntime]

$mgr = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
if ($mgr -eq $null) {
    [Console]::Out.WriteLine('{"error":"smtc"}')
    [Console]::Out.Flush()
    exit
}

$coverPath = $args[0]
$cmdPath = $args[1]
$parentPid = 0
if ($args.Count -gt 2) { $parentPid = [int]$args[2] }

# pick the best session: Spotify playing > any playing > Spotify > current
function Pick-Session {
    $best = $null
    $score = -1
    $sessions = $mgr.GetSessions()
    foreach ($x in $sessions) {
        $sc = 0
        $info = $x.GetPlaybackInfo()
        if ($info -ne $null -and [int]$info.PlaybackStatus -eq 4) { $sc += 10 }
        if (([string]$x.SourceAppUserModelId).ToLower().Contains('spotify')) { $sc += 5 }
        if ($sc -gt $score) { $score = $sc; $best = $x }
    }
    if ($best -eq $null) { $best = $mgr.GetCurrentSession() }
    return $best
}
$lastKey = ''
$coverVer = 0
$hasCover = $false
$coverTries = 0
$tick = 0

while ($true) {
    $force = $false
    if ($cmdPath -and (Test-Path -LiteralPath $cmdPath)) {
        $cmds = @(Get-Content -LiteralPath $cmdPath)
        Remove-Item -LiteralPath $cmdPath -Force
        $s = Pick-Session
        foreach ($cmd in $cmds) {
            $c = ([string]$cmd).Trim()
            if ($c -eq 'quit') { exit }
            if ($s -eq $null) { continue }
            switch ($c) {
                'toggle' { $null = Await ($s.TryTogglePlayPauseAsync()) ([bool]) }
                'next' { $null = Await ($s.TrySkipNextAsync()) ([bool]) }
                'prev' { $null = Await ($s.TrySkipPreviousAsync()) ([bool]) }
            }
        }
        $force = $true
        Start-Sleep -Milliseconds 150
    }
    # exit together with Minecraft
    if ($parentPid -gt 0 -and ($tick % 20) -eq 0) {
        if (-not (Get-Process -Id $parentPid -ErrorAction SilentlyContinue)) { exit }
    }

    if ($force -or ($tick % 4) -eq 0) {
        $o = [ordered]@{ title = ''; artist = ''; app = ''; status = 0; pos = 0.0; dur = 0.0; cover = 0; hasCover = $false }
        $s = Pick-Session
        if ($s -ne $null) {
            $p = Await ($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
            if ($p -ne $null) {
                $o.title = [string]$p.Title
                $o.artist = [string]$p.Artist
                $key = ([string]$s.SourceAppUserModelId) + '|' + $o.title + '|' + $o.artist
                if ($key -ne $lastKey) {
                    $lastKey = $key
                    $hasCover = $false
                    $coverTries = 0
                }
                if (-not $hasCover -and ($coverTries -eq 0 -or ($tick % 20) -eq 0) -and $p.Thumbnail -ne $null) {
                    $coverTries++
                    try {
                        $st = $null; $net = $null; $img = $null; $bmp = $null; $graphics = $null
                        try {
                            $st = Await ($p.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])
                            if ($st -eq $null) { throw 'thumbnail not ready' }
                            $net = [System.IO.WindowsRuntimeStreamExtensions]::AsStreamForRead($st)
                            $img = [System.Drawing.Image]::FromStream($net)
                            $bmp = New-Object System.Drawing.Bitmap -ArgumentList 128,128
                            $graphics = [System.Drawing.Graphics]::FromImage($bmp)
                            $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                            $side = [Math]::Min($img.Width,$img.Height)
                            $src = New-Object System.Drawing.Rectangle -ArgumentList ([int](($img.Width-$side)/2)),([int](($img.Height-$side)/2)),([int]$side),([int]$side)
                            $dest = New-Object System.Drawing.Rectangle -ArgumentList 0,0,128,128
                            $graphics.DrawImage($img,$dest,$src,[System.Drawing.GraphicsUnit]::Pixel)
                            $tmp = $coverPath + '.new'
                            $bmp.Save($tmp,[System.Drawing.Imaging.ImageFormat]::Png)
                            Move-Item -LiteralPath $tmp -Destination $coverPath -Force
                            $coverVer++
                            $hasCover = $true
                        } finally {
                            foreach ($resource in @($graphics,$bmp,$img,$net,$st)) {
                                if ($null -ne $resource -and $resource -is [System.IDisposable]) { $resource.Dispose() }
                            }
                        }
                    } catch { }
                }
            }
            $info = $s.GetPlaybackInfo()
            if ($info -ne $null) { $o.status = [int]$info.PlaybackStatus }
            $tl = $s.GetTimelineProperties()
            if ($tl -ne $null) {
                $pos = $tl.Position.TotalSeconds
                if ($o.status -eq 4 -and $tl.LastUpdatedTime.Year -gt 2000) {
                    $pos += ([DateTimeOffset]::Now - $tl.LastUpdatedTime).TotalSeconds
                }
                $o.pos = [Math]::Round($pos, 2)
                $o.dur = [Math]::Round(($tl.EndTime - $tl.StartTime).TotalSeconds, 2)
            }
            $o.app = [string]$s.SourceAppUserModelId
            $o.cover = $coverVer
            $o.hasCover = $hasCover
        }
        [Console]::Out.WriteLine(($o | ConvertTo-Json -Compress))
        [Console]::Out.Flush()
    }
    $tick++
    Start-Sleep -Milliseconds 100
}
