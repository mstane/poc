#!/bin/sh

set -e
set -u

kubectl -n "monitoring" patch "deployment" "prometheus-deployment" --type strategic --patch "
spec:
  template:
    spec:
      containers:
      - name: sidecar
        image: gcr.io/stackdriver-prometheus/stackdriver-prometheus-sidecar:0.7.0
        imagePullPolicy: Always
        args:
        - \"--stackdriver.project-id=mstane-dev-1\"
        - \"--prometheus.wal-directory=/prometheus/wal\"
        - \"--stackdriver.kubernetes.location=europe-west1-b\"
        - \"--stackdriver.kubernetes.cluster-name=my-cluster-1\"
        #- \"--stackdriver.generic.location=europe-west1-b\"
        #- \"--stackdriver.generic.namespace=monitoring\"
        ports:
        - name: sidecar
          containerPort: 9091
        volumeMounts:
        - name: prometheus-storage-volume
          mountPath: /prometheus/
"

