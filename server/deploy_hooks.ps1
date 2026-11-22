$pi = "jc14fr@192.168.1.79"
$dest = "~/pocketbase/pb_hooks/"
$dossier = Join-Path $PSScriptRoot "pb_hooks"

Get-ChildItem (Join-Path $dossier "*.js") | ForEach-Object {
    scp $_.FullName "${pi}:${dest}"
}

ssh $pi "cd ~/pocketbase && docker compose restart pocketbase && docker logs --tail 15 pocketbase"