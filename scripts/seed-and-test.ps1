# WorkForge bulk sample-data seeder + API smoke tests
# Requires backend at http://localhost:8080 and seedadmin / SeedPass123!

$ErrorActionPreference = 'Stop'
$Base = 'http://localhost:8080/api/v1'
$Report = [System.Collections.Generic.List[string]]::new()

function Log($msg) { Write-Host $msg; $Report.Add($msg) }
function Api($method, $path, $body = $null, $token = $null, $contentType = 'application/json') {
    $headers = @{}
    if ($token) { $headers['Authorization'] = "Bearer $token" }
    $params = @{
        Uri             = "$Base$path"
        Method          = $method
        Headers         = $headers
        TimeoutSec      = 60
        UseBasicParsing = $true
    }
    if ($null -ne $body) {
        if ($contentType -eq 'application/json') {
            $params.Body = ($body | ConvertTo-Json -Depth 10 -Compress)
            $params.ContentType = 'application/json'
        } else {
            $params.Body = $body
            $params.ContentType = $contentType
        }
    }
    $raw = Invoke-RestMethod @params
    if (-not $raw.success) { throw "API failed $method $path : $($raw | ConvertTo-Json -Compress)" }
    return $raw.data
}

Log "=== 1) LOGIN ==="
$auth = Api POST '/auth/login' @{ login = 'seedadmin'; password = 'SeedPass123!' }
# Flat auth response
$token = $auth.accessToken
if (-not $token) { throw "No access token" }
$me = Api GET '/auth/me' $null $token
$userId = if ($me.user) { $me.user.id } else { $me.id }
Log "OK login seedadmin id=$userId"

Log "=== 2) REGISTER EXTRA USERS ==="
$users = @()
$names = @(
    @{u='avadhoot'; e='avadhoot@workforge.local'; n='Avadhoot Jagtap'},
    @{u='jane.dev'; e='jane@workforge.local'; n='Jane Developer'},
    @{u='john.pm'; e='john@workforge.local'; n='John Manager'},
    @{u='sara.qa'; e='sara@workforge.local'; n='Sara QA'},
    @{u='mike.ops'; e='mike@workforge.local'; n='Mike Ops'},
    @{u='lisa.ux'; e='lisa@workforge.local'; n='Lisa UX'},
    @{u='tom.be'; e='tom@workforge.local'; n='Tom Backend'},
    @{u='rita.fe'; e='rita@workforge.local'; n='Rita Frontend'}
)
foreach ($n in $names) {
    try {
        $reg = Api POST '/auth/register' @{
            username = $n.u; email = $n.e; password = 'UserPass123!'; fullName = $n.n
        }
        $users += $reg.user
        Log "  registered $($n.u)"
    } catch {
        Log "  skip $($n.u): $($_.Exception.Message)"
        # try list users later
    }
}

$allUsers = Api GET '/users?page=0&size=100' $null $token
$userList = $allUsers.content
Log "OK users total=$($allUsers.totalElements)"

Log "=== 3) ORGANIZATION ==="
try {
    $org = Api POST '/organizations' @{ key = 'IVL'; name = 'IVL Engineering'; description = 'Primary org for WorkForge demo' } $token
} catch {
    $orgs = Api GET '/organizations' $null $token
    $org = if ($orgs.content) { $orgs.content[0] } else { $orgs[0] }
}
Log "OK org $($org.key) id=$($org.id)"

Log "=== 4) PROJECTS ==="
$projectDefs = @(
    @{ key='MWS'; name='MWS Platform'; desc='Main WorkForge sample platform' },
    @{ key='PAY'; name='Payments Service'; desc='Payment gateway and settlements' },
    @{ key='MOB'; name='Mobile App'; desc='iOS and Android client' },
    @{ key='INF'; name='Infrastructure'; desc='Platform ops and SRE' },
    @{ key='AI'; name='AI Assist'; desc='Future AI features sandbox' }
)
$projects = @()
foreach ($p in $projectDefs) {
    try {
        $created = Api POST '/projects' @{
            key = $p.key; name = $p.name; description = $p.desc
            leadId = "$userId"; organizationId = "$($org.id)"
        } $token
        $projects += $created
        Log "  project $($p.key)"
    } catch {
        Log "  reuse/skip $($p.key): $($_.Exception.Message)"
        try {
            $existing = Api GET "/projects/$($p.key)" $null $token
            $projects += $existing
        } catch {}
    }
}
if ($projects.Count -eq 0) {
    $plist = Api GET '/projects?page=0&size=50' $null $token
    $projects = @($plist.content)
}
Log "OK projects=$($projects.Count)"

# Add members
$memberIds = @($userList | Where-Object { $_.id -ne $userId } | Select-Object -First 6)
foreach ($proj in $projects) {
    $roles = @('LEAD','MEMBER','MEMBER','VIEWER','MEMBER','MEMBER')
    $i = 0
    foreach ($m in $memberIds) {
        try {
            Api POST "/projects/$($proj.key)/members" @{ userId = "$($m.id)"; role = $roles[$i % $roles.Count] } $token | Out-Null
        } catch {}
        $i++
    }
}
Log "OK project members assigned"

Log "=== 5) LABELS + COMPONENTS ==="
$labelNames = @('backend','frontend','performance','security','urgent','tech-debt','api','ux','database','ci-cd')
$componentNames = @('Quote Service','Auth Service','UI Shell','Billing','Notifications','Search','Board','Reporting')
foreach ($proj in $projects) {
    $projectId = [long]$proj.id
    foreach ($ln in $labelNames) {
        try { Api POST '/labels' @{ projectId = $projectId; name = $ln; color = '#0d9488' } $token | Out-Null } catch {}
    }
    foreach ($cn in $componentNames) {
        try { Api POST '/components' @{ projectId = $projectId; name = $cn; description = "$cn for $($proj.key)" } $token | Out-Null } catch {}
    }
}
Log "OK labels/components"

Log "=== 6) SPRINTS ==="
$sprintsByProject = @{}
foreach ($proj in $projects) {
    $sprints = @()
    for ($s = 1; $s -le 4; $s++) {
        try {
            $sp = Api POST "/projects/$($proj.key)/sprints" @{
                name = "Sprint $s"
                goal = "Deliver increment $s for $($proj.key)"
            } $token
            $sprints += $sp
        } catch {
            Log "  sprint err $($proj.key) $s : $($_.Exception.Message)"
        }
    }
    # start first sprint if present
    if ($sprints.Count -gt 0) {
        try {
            Api POST "/sprints/$($sprints[0].id)/start" @{
                startDate = (Get-Date).ToUniversalTime().ToString('o')
                endDate = (Get-Date).AddDays(14).ToUniversalTime().ToString('o')
            } $token | Out-Null
            Log "  started sprint $($sprints[0].name) on $($proj.key)"
        } catch {
            Log "  start sprint skip: $($_.Exception.Message)"
        }
    }
    $sprintsByProject[$proj.key] = $sprints
}
Log "OK sprints"

Log "=== 7) ISSUES (bulk) ==="
$types = @('STORY','TASK','BUG','EPIC','SUBTASK')
$priorities = @('HIGHEST','HIGH','MEDIUM','LOW','LOWEST')
$summaries = @(
    'Improve API response latency for quotes',
    'Fix null pointer in auth refresh path',
    'Add dark mode polish to board cards',
    'Implement saved filter sharing',
    'Optimize PostgreSQL indexes for issue search',
    'Add attachment virus scan hook',
    'Refactor workflow transition validator',
    'Create dashboard gadget for sprint burndown',
    'Fix drag-drop status sync race',
    'Document OpenAPI for sprint endpoints',
    'Add rate limiting metrics',
    'Investigate memory spike on search',
    'Support bulk issue assign',
    'Add keyboard shortcuts to issue detail',
    'Harden file upload validation',
    'Migrate notification events for Kafka readiness',
    'Improve empty states across backlog',
    'Add project avatar upload',
    'Fix pagination on my-work',
    'Implement watcher email digests (stub)'
)

$issueCount = 0
$sampleIssueKeys = @()
foreach ($proj in $projects) {
    $labels = @()
    $comps = @()
    try { $labels = @(Api GET "/projects/$($proj.key)/labels" $null $token) } catch {}
    try { $comps = @(Api GET "/projects/$($proj.key)/components" $null $token) } catch {}
    $sprints = $sprintsByProject[$proj.key]
    $statuses = @()
    try { $statuses = @(Api GET "/projects/$($proj.key)/statuses" $null $token) } catch {}

    # 40 issues per project
    for ($i = 1; $i -le 40; $i++) {
        $type = $types[$i % $types.Count]
        if ($type -eq 'SUBTASK') { $type = 'TASK' }
        $pri = $priorities[$i % $priorities.Count]
        $assignee = $userList[$i % $userList.Count]
        $sprintId = $null
        if ($sprints -and $sprints.Count -gt 0 -and ($i % 3 -ne 0)) {
            $sprintId = "$($sprints[$i % $sprints.Count].id)"
        }
        $labelIds = @()
        if ($labels.Count -gt 0) {
            $labelIds = @("$($labels[$i % $labels.Count].id)")
            if ($labels.Count -gt 1) { $labelIds += "$($labels[($i+1) % $labels.Count].id)" }
        }
        $compIds = @()
        if ($comps.Count -gt 0) { $compIds = @("$($comps[$i % $comps.Count].id)") }

        try {
            $issue = Api POST "/projects/$($proj.key)/issues" @{
                projectKey = $proj.key
                type = $type
                summary = "$($summaries[$i % $summaries.Count]) #$i"
                description = "Sample description for $($proj.key) issue $i.`n`nAcceptance:`n- Persisted in PostgreSQL`n- Visible on board/backlog"
                priority = $pri
                assigneeId = "$($assignee.id)"
                sprintId = $sprintId
                labelIds = $labelIds
                componentIds = $compIds
                storyPoints = (1 + ($i % 8))
                dueDate = (Get-Date).AddDays(3 + ($i % 20)).ToString('yyyy-MM-dd')
            } $token
            $issueCount++
            if ($sampleIssueKeys.Count -lt 15) { $sampleIssueKeys += $issue.key }

            # transition some issues along workflow
            if ($statuses.Count -ge 2 -and ($i % 4 -eq 0)) {
                try { Api PATCH "/issues/$($issue.key)/status" @{ statusId = "$($statuses[1].id)" } $token | Out-Null } catch {}
            }
            if ($statuses.Count -ge 3 -and ($i % 8 -eq 0)) {
                try {
                    Api PATCH "/issues/$($issue.key)/status" @{ statusId = "$($statuses[1].id)" } $token | Out-Null
                    Api PATCH "/issues/$($issue.key)/status" @{ statusId = "$($statuses[2].id)" } $token | Out-Null
                } catch {}
            }
            if ($statuses.Count -ge 4 -and ($i % 16 -eq 0)) {
                try {
                    Api PATCH "/issues/$($issue.key)/status" @{ statusId = "$($statuses[1].id)" } $token | Out-Null
                    Api PATCH "/issues/$($issue.key)/status" @{ statusId = "$($statuses[2].id)" } $token | Out-Null
                    Api PATCH "/issues/$($issue.key)/status" @{ statusId = "$($statuses[3].id)" } $token | Out-Null
                } catch {}
            }
        } catch {
            Log "  issue fail $($proj.key) #$i : $($_.Exception.Message)"
        }
    }
}
Log "OK created ~$issueCount issues (sample keys: $($sampleIssueKeys -join ', '))"

Log "=== 8) COMMENTS / WATCH / ACTIVITY ==="
$commentCount = 0
foreach ($key in $sampleIssueKeys) {
    try {
        Api POST "/issues/$key/comments" @{ body = "Looks good - please verify on the board. ($key)" } $token | Out-Null
        Api POST "/issues/$key/comments" @{ body = "Updated acceptance criteria after review." } $token | Out-Null
        Api POST "/issues/$key/watch" $null $token | Out-Null
        $commentCount += 2
    } catch {
        Log "  comment/watch fail $key : $($_.Exception.Message)"
    }
}
Log "OK comments~$commentCount + watches"

Log "=== 9) SAVED FILTERS ==="
foreach ($name in @('My Open Bugs','High Priority Work','MWS In Progress','Recently Updated')) {
    $jql = switch -Wildcard ($name) {
        '*Bugs*' { 'type = BUG AND priority = HIGH' }
        '*High*' { 'priority = HIGH' }
        '*MWS*' { 'project = MWS AND status = "In Progress"' }
        default { 'project = MWS' }
    }
    try {
        Api POST '/filters' @{ name = $name; query = $jql; shared = $true } $token | Out-Null
        Log "  filter $name"
    } catch {
        Log "  filter skip $name : $($_.Exception.Message)"
    }
}

Log "=== 10) DASHBOARD + NOTIFICATIONS ==="
try {
    $dash = Api POST '/dashboards' @{ name = 'My WorkForge Home'; shared = $true } $token
    Log "  dashboard id=$($dash.id)"
    try {
        Api POST "/dashboards/$($dash.id)/gadgets" @{ type = 'MY_OPEN_ISSUES'; config = '{}'; position = 0 } $token | Out-Null
        Api POST "/dashboards/$($dash.id)/gadgets" @{ type = 'STATUS_DISTRIBUTION'; config = '{}'; position = 1 } $token | Out-Null
    } catch {
        Log "  gadgets skip: $($_.Exception.Message)"
    }
} catch {
    Log "  dashboard skip: $($_.Exception.Message)"
}
try {
    $notes = Api GET '/notifications?page=0&size=20' $null $token
    Log "  notifications total=$($notes.totalElements)"
} catch {
    Log "  notifications: $($_.Exception.Message)"
}

Log "=== 11) API SMOKE TESTS ==="
$pass = 0; $fail = 0
function Assert($name, $script) {
    try {
        & $script
        Log "PASS $name"
        $script:pass++
    } catch {
        Log "FAIL $name :: $($_.Exception.Message)"
        $script:fail++
    }
}

Assert 'auth/me' { Api GET '/auth/me' $null $token | Out-Null }
Assert 'users page' { $u = Api GET '/users?page=0&size=20' $null $token; if ($u.totalElements -lt 2) { throw 'too few users' } }
Assert 'projects list' { $p = Api GET '/projects?page=0&size=20' $null $token; if ($p.totalElements -lt 1) { throw 'no projects' } }
Assert 'projects/all' { Api GET '/projects/all' $null $token | Out-Null }
Assert 'issues list MWS' { $i = Api GET '/issues?projectKey=MWS&page=0&size=20' $null $token; if ($i.totalElements -lt 1) { throw 'no issues' } }
Assert 'issues/my-work' { Api GET '/issues/my-work?page=0&size=20' $null $token | Out-Null }
Assert 'issues/search jql' { Api GET '/issues/search?jql=project%20%3D%20MWS&page=0&size=20' $null $token | Out-Null }
Assert 'board MWS' { Api GET '/projects/MWS/board' $null $token | Out-Null }
Assert 'backlog MWS' { Api GET '/projects/MWS/backlog' $null $token | Out-Null }
Assert 'sprints MWS' { Api GET '/projects/MWS/sprints' $null $token | Out-Null }
Assert 'statuses MWS' { Api GET '/projects/MWS/statuses' $null $token | Out-Null }
Assert 'labels MWS' { Api GET '/projects/MWS/labels' $null $token | Out-Null }
Assert 'components MWS' { Api GET '/projects/MWS/components' $null $token | Out-Null }
if ($sampleIssueKeys.Count -gt 0) {
    $ik = $sampleIssueKeys[0]
    Assert "issue get $ik" { Api GET "/issues/$ik" $null $token | Out-Null }
    Assert "issue comments $ik" { Api GET "/issues/$ik/comments" $null $token | Out-Null }
    Assert "issue activity $ik" { Api GET "/issues/$ik/activity" $null $token | Out-Null }
}
Assert 'filters' { Api GET '/filters' $null $token | Out-Null }
Assert 'dashboards' { Api GET '/dashboards' $null $token | Out-Null }
Assert 'notifications' { Api GET '/notifications?page=0&size=10' $null $token | Out-Null }
Assert 'actuator health' {
    $h = Invoke-RestMethod 'http://localhost:8080/actuator/health'
    if ($h.status -ne 'UP') { throw $h.status }
}

Log "=== SUMMARY ==="
Log "Issues created (approx): $issueCount"
Log "Smoke PASS=$pass FAIL=$fail"
Log "Login: seedadmin / SeedPass123!"
Log "UI: http://localhost:5173  API: http://localhost:8080/swagger-ui.html"

$out = Join-Path $PSScriptRoot 'seed-and-test-report.txt'
if (-not $PSScriptRoot) { $out = 'd:\IVL\scripts\seed-and-test-report.txt' }
$Report | Set-Content -Path $out -Encoding UTF8
Log "Report written: $out"
if ($fail -gt 0) { exit 1 }
