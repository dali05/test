kubectl patch serviceaccount default \
  -n ns-wall-e-springboot \
  -p '{"imagePullSecrets":[{"name":"docker-registry-cred"}]}'


kubectl get sa default -n ns-wall-e-springboot -o yaml | sed -n '/imagePullSecrets/,+3p'


kubectl get job wall-e-db-bootstrap -n ns-wall-e-springboot -o jsonpath='{.spec.template.spec.serviceAccountName}{"\n"}'


kubectl patch serviceaccount <NOM_SA> \
  -n ns-wall-e-springboot \
  -p '{"imagePullSecrets":[{"name":"docker-registry-cred"}]}'