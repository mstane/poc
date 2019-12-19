package org.sm.poc.loggingpoc;

import com.google.api.Metric;
import com.google.api.MonitoredResource;
import com.google.cloud.MetadataConfig;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.monitoring.v3.*;
import com.google.protobuf.util.Timestamps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/metrics")
public class MetricsController {

    private final Logger log = LoggerFactory.getLogger(MetricsController.class);

    private final AtomicLong counter = new AtomicLong();

    private MonitoredResource resource;
    private String projectName;

    @GetMapping("/env")
    public LoggingPocBean env() throws IOException {
        long currentValue = counter.incrementAndGet();
        String pathString = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        String output = null;
        if (!StringUtils.isEmpty(pathString)) {
            output = Files.readString(Paths.get(pathString));
        }
        output += printOutMetadata();
        output += printOutSystemProperties();
        output += printOutHostData();
        return new LoggingPocBean(currentValue, String.format("GOOGLE_APPLICATION_CREDENTIALS=[%s]; content=[%s]; ", pathString, output));
    }

    private String printOutMetadata() {
        String instanceId = com.google.cloud.MetadataConfig.getInstanceId();
        String namespaceId = com.google.cloud.MetadataConfig.getNamespaceId();
        String projectId = com.google.cloud.MetadataConfig.getProjectId();
        String clusterName = com.google.cloud.MetadataConfig.getClusterName();
        String containerName = com.google.cloud.MetadataConfig.getContainerName();
        String zone = com.google.cloud.MetadataConfig.getZone();
        return String.format("instanceId:%s; namespaceId:%s; projectId:%s; clusterName:%s; containerName:%s; zone:%s",
                instanceId, namespaceId, projectId, clusterName, containerName, zone);
    }

    private String printOutSystemProperties() {
        StringBuilder sb = new StringBuilder();
        Properties properties = System.getProperties();
        properties.forEach((k, v) -> sb.append(k + ": " + v + "; "));
        return sb.toString();
    }

    private String printOutHostData() {
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            return String.format("----- address = %s; hostname = %s -------", inetAddress.getHostAddress(), inetAddress.getHostName());
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    @GetMapping("/quick-start")
    public LoggingPocBean quickStart() throws IOException {
        // Instantiates a client
        MetricServiceClient metricServiceClient = MetricServiceClient.create();

        long currentValue = counter.incrementAndGet();
        // Prepares an individual data point
        TimeInterval interval = TimeInterval.newBuilder()
                .setEndTime(Timestamps.fromMillis(System.currentTimeMillis()))
                .build();
        TypedValue value = TypedValue.newBuilder()
                .setInt64Value(currentValue)
                .build();
        Point point = Point.newBuilder()
                .setInterval(interval)
                .setValue(value)
                .build();

        List<Point> pointList = new ArrayList<>();
        pointList.add(point);

        String projectId  = MetadataConfig.getProjectId();
        ProjectName name = ProjectName.of(projectId);

        // Prepares the metric descriptor
        Map<String, String> metricLabels = new HashMap<String, String>();
        metricLabels.put("store_id", "Pittsburg");
        Metric metric = Metric.newBuilder()
                .setType("custom.googleapis.com/stores/daily_sales")
                .putAllLabels(metricLabels)
                .build();

        // Prepares the monitored resource descriptor
        Map<String, String> resourceLabels = new HashMap<String, String>();
        resourceLabels.put("project_id", projectId);
        MonitoredResource resource = MonitoredResource.newBuilder()
                .setType("global")
                .putAllLabels(resourceLabels)
                .build();

        // Prepares the time series request
        TimeSeries timeSeries = TimeSeries.newBuilder()
                .setMetric(metric)
                .setResource(resource)
                .addAllPoints(pointList)
                .build();
        List<TimeSeries> timeSeriesList = new ArrayList<>();
        timeSeriesList.add(timeSeries);

        CreateTimeSeriesRequest request = CreateTimeSeriesRequest.newBuilder()
                .setName(name.toString())
                .addAllTimeSeries(timeSeriesList)
                .build();

        // Writes time series data
        metricServiceClient.createTimeSeries(request);

        log.info("Done writing time series data.%n");

        metricServiceClient.close();

        return new LoggingPocBean(currentValue, String.format("Custom metrics quick start: %d", currentValue));

    }

    @PostConstruct
    public void init() {
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

    @Scheduled(fixedRate = 60000)
    public void sendMetrics() {
        log.info(">>> Sending metrics <<<");
        prepareMetrics(
                MetricTypes.stream()
                        .map(this::prepareMetricTimeSeries)
                        .collect(Collectors.toList())
        );
    }


}
