kubectl describe job -n ns-wall-e-springboot vault-pg-debug
kubectl get events -n ns-wall-e-springboot --sort-by=.lastTimestamp | tail -n 80