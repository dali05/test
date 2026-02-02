kubectl config current-context
helm get manifest wall-e -n ns-wall-e-springboot | head
kubectl get ns | grep ns-wall-e-springboot


kubectl get cm -A | grep wall-e-vault-agent-config