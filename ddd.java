kubectl exec -n ns-vault -it vault-0 -- sh -lc '
export VAULT_ADDR=http://127.0.0.1:8200
export VAULT_NAMESPACE=ns-wall-e-springboot
vault login <TON_TOKEN>
vault read auth/kubernetes_kube001_local/role/ns-wall-e-springboot-local-ap11236-java-application-liquibase
'