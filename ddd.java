kubectl -n ns-wall-e-springboot get pods | grep wall-e-liquibase-schema
kubectl -n ns-wall-e-springboot describe pod wall-e-liquibase-schema-2bs2t | sed -n '/Init Containers:/,/Containers:/p'
kubectl -n ns-wall-e-springboot describe pod wall-e-liquibase-schema-2bs2t | tail -n 50