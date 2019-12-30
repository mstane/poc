package org.sm.poc.core;

import com.google.api.MonitoredResource;
import com.google.cloud.MetadataConfig;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.monitoring.v3.*;
import com.google.protobuf.util.Timestamps;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MetricsSender implements Runnable {

    private final Logger log = LoggerFactory.getLogger(MetricsSender.class);

    private MonitoredResource resource;
    private String projectName = null;

    public MetricsSender() {
        try {
            String deploymentName = "loggingpoc";
            String projectId = MetadataConfig.getProjectId();

            if (!StringUtils.isEmpty(projectId)) {
                ProjectName projectNameObject = ProjectName.of(MetadataConfig.getProjectId());

                projectName = projectNameObject.toString();

                Map<String, String> resourceLabels = new HashMap<>();
                resourceLabels.put("project_id", MetadataConfig.getProjectId());
                resourceLabels.put("location", MetadataConfig.getZone());
                resourceLabels.put("namespace", MetadataConfig.getClusterName());
                resourceLabels.put("job", deploymentName);
                resourceLabels.put("task_id", getHostname());
                resource = MonitoredResource.newBuilder()
                        .setType("generic_task")
                        .putAllLabels(resourceLabels)
                        .build();
            }
        } catch (Exception e) {
            projectName = null;
            log.error("", e);
        }
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    private Point getPoint(Supplier<Long> fn) {
        return Point.newBuilder()
                .setInterval(
                        TimeInterval.newBuilder()
                                .setEndTime(
                                        Timestamps.fromMillis(System.currentTimeMillis())
                                )
                                .build()
                )
                .setValue(
                        TypedValue.newBuilder()
                                .setInt64Value(fn.get())
                                .build()
                )
                .build();
    }

    public TimeSeries prepareMetricTimeSeries(MetricTypes metricTypes) {
        log.info(">>> Prepare metrics type: {} <<<", metricTypes.getMetricDescriptor().getType());
        return TimeSeries.newBuilder()
                .setMetric(metricTypes.getMetricDescriptor())
                .setResource(resource)
                .addAllPoints(List.of(getPoint(metricTypes.getFunction())))
                .build();
    }

    public void prepareMetrics(List<TimeSeries> timeSeriesList) {
        CreateTimeSeriesRequest request = CreateTimeSeriesRequest.newBuilder()
                .setName(projectName)
                .addAllTimeSeries(timeSeriesList)
                .build();
        try {
            MetricServiceClient metricServiceClient = MetricServiceClient.create();
            metricServiceClient.createTimeSeries(request);
            metricServiceClient.close();
        } catch (IOException e) {
            log.warn(e.getMessage());
        }

    }

    public void run() {
        log.info(">>> Sending metrics <<<");
        if (projectName != null) {
            prepareMetrics(
                    MetricTypes.stream()
                            .map(this::prepareMetricTimeSeries)
                            .collect(Collectors.toList())
            );
        }
    }

}
