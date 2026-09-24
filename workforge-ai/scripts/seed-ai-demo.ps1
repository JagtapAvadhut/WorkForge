# Seeds WorkForge AI demo documents + optional long-term memory via the real demo API.
# Idempotent - safe to re-run. Does not modify the main WorkForge database.
param(
    [string]$BaseUrl = "http://localhost:8090/api/v1",
    [string]$SessionId = "demo-session"
)

$ErrorActionPreference = "Stop"

Write-Host "Checking AI backend at $BaseUrl ..."
try {
    $null = Invoke-RestMethod -Uri "$BaseUrl/ai/security/policies" -Method GET -TimeoutSec 10
} catch {
    Write-Error "AI backend not reachable at $BaseUrl. Start ai-backend on :8090 first. $_"
    exit 1
}

Write-Host "Seeding demo data (sessionId=$SessionId) ..."
$seedUrl = $BaseUrl + "/ai/demo/seed?sessionId=" + [uri]::EscapeDataString($SessionId)
$response = Invoke-RestMethod -Uri $seedUrl -Method POST -TimeoutSec 180

$data = $response.data
Write-Host ""
Write-Host "=== Demo seed summary ==="
Write-Host ("Documents created : {0}" -f $data.documentsCreated)
Write-Host ("Documents skipped : {0}" -f $data.documentsSkipped)
Write-Host ("Topics            : {0}" -f ($data.documentTopics -join ", "))
Write-Host ("Memory created    : {0}" -f $data.memoryCreated)
Write-Host ("Memory id         : {0}" -f $data.memoryId)

$wfBase = "http://localhost:8080/api/v1"
try {
    $null = Invoke-WebRequest -Uri "$wfBase/actuator/health" -Method GET -TimeoutSec 5 -UseBasicParsing
    Write-Host "WorkForge API (:8080) appears up - MWS tool demos should work if sample data exists."
} catch {
    Write-Host "Note: WorkForge API (:8080) not checked/reachable. Tool/MCP issue demos need MWS sample data."
}

Write-Host ""
Write-Host "Done. Open the AI frontend and use Samples / Load Demo on each tab."
