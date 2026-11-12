$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Speech
$speaker = New-Object System.Speech.Synthesis.SpeechSynthesizer
try {
    while ($null -ne ($line = [Console]::In.ReadLine())) {
        $speaker.SpeakAsyncCancelAll()
        if ($line -eq 'STOP') { continue }
        if (-not $line.StartsWith('SPEAK:')) { continue }
        try {
            $text = [System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($line.Substring(6)))
            $language = if ($text -match '[\u0400-\u04FF]') { 'ru' } else { 'en' }
            $voice = $speaker.GetInstalledVoices() | Where-Object { $_.Enabled -and $_.VoiceInfo.Culture.TwoLetterISOLanguageName -eq $language } | Select-Object -First 1
            if ($null -ne $voice) { $speaker.SelectVoice($voice.VoiceInfo.Name) }
            $null = $speaker.SpeakAsync($text)
        } catch { }
    }
} finally {
    $speaker.SpeakAsyncCancelAll()
    $speaker.Dispose()
}
