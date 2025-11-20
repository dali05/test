kubectl create namespace ingress-nginx --dry-run=client -o yaml | kubectl apply -f -

kubectl delete job -n ingress-nginx --all

helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx