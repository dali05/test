kubectl exec -n ns-wall-e-springboot -it vault-pg-debug-bqtxv -c vault-agent -- sh -lc '
echo "=== /etc/secrets/pg.env ==="
ls -la /etc/secrets
echo "--------------------------"
cat /etc/secrets/pg.env
'