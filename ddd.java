kubectl get pod -n ns-wall-e-springboot wall-e-backend-98fcb84bb-f5wwk \
  -o jsonpath='{range .spec.containers[*]}{.name}{"\n"}{end}'