# Comprehensive WorkForge API audit
$ErrorActionPreference = 'Continue'
$Base = 'http://localhost:8080/api/v1'
$Results = New-Object System.Collections.Generic.List[object]

function Rec($method, $path, $status, $result, $notes) {
    $Results.Add([pscustomobject]@{ Method=$method; Path=$path; Status=$status; Result=$result; Notes=$notes })
    "{0,-5} {1,-6} {2,-45} {3}" -f $result, $status, "$method $path", $notes
}

$loginBody = @{ login='seedadmin'; password='SeedPass123!' } | ConvertTo-Json
$login = Invoke-RestMethod -Uri "$Base/auth/login" -Method POST -Body $loginBody -ContentType 'application/json'
$token = $login.data.accessToken
$H = @{ Authorization = "Bearer $token" }

function Call($method, $path, $body=$null, $auth=$true, $expectSuccess=$true, $expectStatus=$null) {
    $headers = @{}
    if ($auth) { $headers = $H.Clone() }
    $params = @{ Uri = "$Base$path"; Method = $method; Headers = $headers; UseBasicParsing = $true; TimeoutSec = 45 }
    if ($null -ne $body) { $params.Body = ($body | ConvertTo-Json -Depth 8 -Compress); $params.ContentType = 'application/json' }
    try {
        $resp = Invoke-WebRequest @params
        $json = $resp.Content | ConvertFrom-Json
        $code = [int]$resp.StatusCode
        if ($expectStatus -and $code -ne $expectStatus) {
            Rec $method $path $code 'FAIL' "expected HTTP $expectStatus"
            return $null
        }
        if ($expectSuccess -and -not $json.success) {
            Rec $method $path $code 'FAIL' "success=false $($json.message)"
            return $null
        }
        if (-not $expectSuccess -and $json.success) {
            Rec $method $path $code 'FAIL' "expected failure but succeeded"
            return $null
        }
        Rec $method $path $code 'PASS' $(if ($json.message) { $json.message } else { 'ok' })
        return $json.data
    } catch {
        $code = 0
        try { $code = [int]$_.Exception.Response.StatusCode.value__ } catch {}
        $errBody = ''
        try {
            $stream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $errBody = $reader.ReadToEnd()
        } catch {}
        if ($expectStatus -and $code -eq $expectStatus) {
            Rec $method $path $code 'PASS' "expected error $code"
            return $null
        }
        if (-not $expectSuccess -and $code -ge 400) {
            Rec $method $path $code 'PASS' "expected client/server error"
            return $null
        }
        Rec $method $path $code 'FAIL' ($errBody.Substring(0, [Math]::Min(120, $errBody.Length)))
        return $null
    }
}

Write-Host "`n=== AUTH ==="
Call POST '/auth/login' @{ login='seedadmin'; password='SeedPass123!' } $false
Call POST '/auth/login' @{ login='seedadmin'; password='wrong' } $false $false 401
Call GET '/auth/me'
Call POST '/auth/refresh' @{ refreshToken = $login.data.refreshToken } $false
Call GET '/auth/me' $null $false $false 401

Write-Host "`n=== USERS / ORGS / PROJECTS ==="
Call GET '/users?page=0&size=5'
Call GET '/users/2'
Call GET '/users/99999' $null $true $false 404
Call GET '/organizations'
Call GET '/projects?page=0&size=10'
Call GET '/projects/all'
Call GET '/projects/MWS'
Call GET '/projects/MWS/members'
Call GET '/projects/ZZZZ' $null $true $false 404

Write-Host "`n=== DASHBOARD HOME ==="
$stats = Call GET '/dashboard/stats'
Call GET '/dashboard/my-open-issues'
Call GET '/dashboard/assigned-to-me'
Call GET '/dashboard/activity'
Call GET '/dashboard/missing' $null $true $false 404

Write-Host "`n=== ISSUES ==="
$issues = Call GET '/issues?projectKey=MWS&page=0&size=5'
Call GET '/issues/my-work?page=0&size=5'
Call GET '/issues/search?jql=project%20%3D%20MWS&page=0&size=5'
Call GET '/issues/search?project=MWS&status=To%20Do&page=0&size=5'
Call GET '/issues/MWS-1'
Call GET '/issues/MWS-1/comments'
Call GET '/issues/MWS-1/activity'
Call GET '/issues/MWS-1/subtasks'
Call GET '/issues/NOPE-999' $null $true $false 404

Write-Host "`n=== WORKSPACE ==="
Call GET '/projects/MWS/board'
Call GET '/projects/MWS/backlog'
Call GET '/projects/MWS/sprints'
Call GET '/projects/MWS/statuses'
Call GET '/projects/MWS/labels'
Call GET '/projects/MWS/components'

Write-Host "`n=== SEARCH / FILTERS / NOTIFS / DASHBOARDS ==="
Call GET '/search?q=MWS'
Call GET '/search?q='
Call GET '/filters'
Call GET '/notifications?page=0&size=5'
Call GET '/notifications/unread-count'
Call GET '/dashboards'

Write-Host "`n=== WORKFLOW / BOARDS / SPRINTS ==="
Call GET '/workflows/1/statuses'
Call GET '/workflows/1/transitions'
Call GET '/boards?projectId=1'
Call GET '/sprints?projectId=1'
Call GET '/labels?projectId=1'
Call GET '/components?projectId=1'

Write-Host "`n=== MUTATIONS (safe) ==="
$created = Call POST '/projects/MWS/issues' @{
    projectKey='MWS'; type='TASK'; summary='QA Audit Temp Issue'; description='Created by audit'; priority='LOW'
}
if ($created -and $created.key) {
    $ik = $created.key
    Call PATCH "/issues/$ik/priority" @{ priority='HIGH' }
    $statuses = Invoke-RestMethod -Uri "$Base/projects/MWS/statuses" -Headers $H
    $todo = $statuses.data | Where-Object { $_.name -eq 'To Do' } | Select-Object -First 1
    $ip = $statuses.data | Where-Object { $_.name -eq 'In Progress' } | Select-Object -First 1
    $done = $statuses.data | Where-Object { $_.name -eq 'Done' } | Select-Object -First 1
    if ($ip) { Call PATCH "/issues/$ik/status" @{ statusId = "$($ip.id)" } }
    # invalid transition TODO->DONE if currently IN_PROGRESS, try Done from wrong state after resetting... 
    # From In Progress, Done may be invalid; try
    if ($done) { Call PATCH "/issues/$ik/status" @{ statusId = "$($done.id)" } $true $false 400 }
    Call POST "/issues/$ik/comments" @{ body = 'QA audit comment' }
    Call POST "/issues/$ik/watch"
    Call DELETE "/issues/$ik/watch"
    Call DELETE "/issues/$ik"
}

Write-Host "`n=== ACTUATOR ==="
try {
    $h = Invoke-RestMethod 'http://localhost:8080/actuator/health'
    Rec 'GET' '/actuator/health' 200 $(if($h.status -eq 'UP'){'PASS'}else{'FAIL'}) $h.status
} catch { Rec 'GET' '/actuator/health' 0 'FAIL' $_.Exception.Message }

$pass = ($Results | Where-Object Result -eq 'PASS').Count
$fail = ($Results | Where-Object Result -eq 'FAIL').Count
Write-Host "`nTOTAL=$($Results.Count) PASS=$pass FAIL=$fail"
$Results | Export-Csv -Path 'd:\IVL\docs\api-audit-results.csv' -NoTypeInformation
$Results | ConvertTo-Json -Depth 4 | Set-Content 'd:\IVL\docs\api-audit-results.json'
