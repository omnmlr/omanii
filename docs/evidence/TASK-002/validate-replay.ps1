param([string]$Records = "$PSScriptRoot/../../../fixtures/network/repair-golden.jsonl")
$ErrorActionPreference = 'Stop'
$states = @('AVAILABLE','UNKNOWN','NOT_EVALUATED','NOT_SUPPORTED','PERMISSION_DENIED','REDACTED','TEMPORARILY_UNAVAILABLE','NOT_APPLICABLE')
function Assert-Semantics($value) {
    if ($null -eq $value) { return }
    if ($value -is [System.Collections.IDictionary]) {
        if ($value.Contains('availability')) {
            if ($value.availability -notin $states) { throw 'Invalid availability' }
            if (($value.availability -eq 'AVAILABLE') -ne ($null -ne $value.value)) { throw 'Contradictory availability/value' }
        }
        if ($value.Contains('received_at_ns')) {
            if ($value.received_at_ns -lt 0 -or [string]::IsNullOrWhiteSpace($value.source)) { throw 'Invalid receive timing/source' }
            if ($value.age_known -ne ($null -ne $value.measurement_at_ns)) { throw 'Contradictory age' }
            if ($null -ne $value.measurement_at_ns) {
                if ($value.measurement_at_ns -gt $value.received_at_ns -or $value.measurement_at_ns -lt 0) { throw 'Invalid measurement time' }
                if ($value.source_timestamp.meaning -ne 'MEASUREMENT' -or $value.source_timestamp.domain -ne 'ANDROID_ELAPSED_REALTIME' -or $value.source_timestamp.timestamp_ns -ne $value.measurement_at_ns) { throw 'Invalid measurement provenance' }
            }
            if ($null -ne $value.source_timestamp -and $value.source_timestamp.meaning -eq 'PLATFORM_RECEIPT' -and $value.age_known) { throw 'Receipt promoted to measurement' }
        }
        foreach ($entry in $value.GetEnumerator()) {
            if ($entry.Key -match '^(network_token|path_token|ssid_token|bssid_token|cell_token|active_data_subscription_token)$' -and $null -ne $entry.Value.value) {
                if ($entry.Value.value -notmatch '^(network|path|ssid|bssid|cell|subscription)_tok_[0-9a-f]{64}$') { throw 'Raw or invalid identity token' }
            }
            Assert-Semantics $entry.Value
        }
    } elseif ($value -is [array]) { foreach ($item in $value) { Assert-Semantics $item } }
}
$count = 0
$sessions = @{}
foreach ($line in Get-Content -LiteralPath $Records) {
    if ([string]::IsNullOrWhiteSpace($line)) { continue }
    $record = ConvertFrom-Json -AsHashtable $line -Depth 100
    if ($record.export_type -ne 'development_research' -or $record.synthetic -ne $true) { throw 'Fixture must be labeled synthetic development evidence' }
    foreach ($field in @('record_id','session_id','producer','app_build','schema_version','protocol_version','collection_id')) {
        if ([string]::IsNullOrWhiteSpace($record.metadata[$field])) { throw "Missing metadata $field" }
    }
    if ($record.metadata.schema_version -ne 'omanii-task002-development-v2' -or $record.metadata.protocol_version -ne 'task002-passive-platform-cache-v2' -or $record.metadata.collection_id -ne $record.metadata.protocol_version) { throw 'Wrong collection identity' }
    if ($record.metadata.test_profile.availability -ne 'NOT_APPLICABLE') { throw 'Passive collection claims active profile' }
    Assert-Semantics $record
    if ($record.record_type -eq 'radio_observation') {
        $p = $record.payload
        if ($p.epoch.session_id -ne $record.metadata.session_id) { throw 'Cross-session epoch' }
        if ($p.independence.availability -ne 'UNKNOWN' -or $null -ne $p.independence.value) { throw 'Polling proves independence' }
        if ($p.read.path.transport.value -eq 'NONE') {
            foreach ($field in @('network_token','internet','validated','metered','vpn','private_dns_active','path_token')) {
                if ($p.read.path[$field].availability -ne 'NOT_APPLICABLE') { throw 'No active network has factual path fields' }
            }
        }
        if ($p.read.path.transport.availability -eq 'PERMISSION_DENIED' -and $null -ne $p.read.path.vpn.value) { throw 'Denied access claims factual VPN flag' }
        foreach ($cell in $p.read.cells.value) {
            if ($cell.active_data_attribution.availability -ne 'UNKNOWN' -or $cell.timing.age_known) { throw 'Cell attribution/age overclaim' }
        }
        $session = $p.epoch.session_id
        if ($sessions.ContainsKey($session) -and $p.timing.received_at_ns -lt $sessions[$session]) { throw 'Regressing time in session' }
        $sessions[$session] = $p.timing.received_at_ns
    }
    if ($line -match '(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}' -or $line -match 'synthetic-only-fixture-salt') { throw 'Sensitive identifier/salt leaked' }
    $count++
}
if ($count -eq 0) { throw 'No records parsed' }
# Check that the recursive validator actually rejects contradictory semantics.
$badCases = @(
    @{ availability='AVAILABLE'; value=$null },
    @{ availability='UNKNOWN'; value=0 },
    @{ received_at_ns=100; source='synthetic'; measurement_at_ns=50; age_known=$true; source_timestamp=@{ meaning='PLATFORM_RECEIPT'; domain='ANDROID_ELAPSED_REALTIME'; timestamp_ns=50 } }
)
foreach ($bad in $badCases) {
    $rejected = $false
    try { Assert-Semantics $bad } catch { $rejected = $true }
    if (-not $rejected) { throw 'Validator accepted an invalid semantic case' }
}
"PASS: $count parsed synthetic records; $($sessions.Count) sessions; availability/timing/metadata/epoch/attribution/independence/privacy checks; $($badCases.Count) negative cases rejected."
