kubectl exec -n ns-vault -it vault-0 -- sh
vault login <TON_TOKEN_ADMIN>
vault auth list