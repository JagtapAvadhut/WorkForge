# Seeds WorkForge AI knowledge documents via the document ingestion API.
# Does not hardcode knowledge into the Java application.
param(
    [string]$BaseUrl = "http://localhost:8090/api/v1",
    [string]$KnowledgeFile = (Join-Path $PSScriptRoot "..\data\rag-knowledge.json")
)

$ErrorActionPreference = "Stop"
if (-not (Test-Path $KnowledgeFile)) {
    throw "Knowledge file not found: $KnowledgeFile"
}

$docs = Get-Content $KnowledgeFile -Raw | ConvertFrom-Json
Write-Host "Seeding $($docs.Count) documents into $BaseUrl/ai/documents"

foreach ($doc in $docs) {
    $body = @{
        content  = $doc.content
        metadata = $doc.metadata
    } | ConvertTo-Json -Depth 5

    $response = Invoke-RestMethod -Uri "$BaseUrl/ai/documents" `
        -Method POST `
        -ContentType "application/json" `
        -Body $body `
        -TimeoutSec 120

    Write-Host ("OK topic={0} id={1} dims={2}" -f $doc.metadata.topic, $response.data.id, $response.data.embeddingDimensions)
}

Write-Host "Done."
