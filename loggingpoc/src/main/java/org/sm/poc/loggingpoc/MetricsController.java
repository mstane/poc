package org.sm.poc.loggingpoc;

import com.google.api.LabelDescriptor;
import com.google.api.Metric;
import com.google.api.MetricDescriptor;
import com.google.api.MonitoredResource;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.gson.Gson;
import com.google.monitoring.v3.*;
import com.google.protobuf.util.Timestamps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/metrics")
public class MetricsController {

    private final Logger log = LoggerFactory.getLogger(MetricsController.class);

    private final AtomicLong counter = new AtomicLong();

    private static final String CUSTOM_METRIC_DOMAIN = "custom.googleapis.com";
    private static final Gson gson = new Gson();

    public static final String PROJECT_ID = "regnosys-dev-1";

    @GetMapping("/env")
    public LoggingPocBean env() throws IOException {
        long currentValue = counter.incrementAndGet();
        String pathString = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        String content = null;
        if (!StringUtils.isEmpty(pathString)) {
             content = Files.readString(Paths.get(pathString));
        }
        return new LoggingPocBean(currentValue, String.format("GOOGLE_APPLICATION_CREDENTIALS=[%s]; content=[%s]", pathString, content));
    }

    @GetMapping("/quick-start")
    public LoggingPocBean quickStart() throws IOException {
        // Your Google Cloud Platform project ID

        if (PROJECT_ID == null) {
            throw new IllegalArgumentException("Usage: QuickstartSample -DprojectId=YOUR_PROJECT_ID");
        }

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

        ProjectName name = ProjectName.of(PROJECT_ID);

        // Prepares the metric descriptor
        Map<String, String> metricLabels = new HashMap<String, String>();
        metricLabels.put("store_id", "Pittsburg");
        Metric metric = Metric.newBuilder()
                .setType("custom.googleapis.com/stores/daily_sales")
                .putAllLabels(metricLabels)
                .build();

        // Prepares the monitored resource descriptor
        Map<String, String> resourceLabels = new HashMap<String, String>();
        resourceLabels.put("project_id", PROJECT_ID);
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

        System.out.printf("Done writing time series data.%n");

        metricServiceClient.close();

        return new LoggingPocBean(currentValue, String.format("Custom metrics quick start: %d", currentValue));

    }


    /**
     * Creates a metric descriptor.
     * <p>
     * See: https://cloud.google.com/monitoring/api/ref_v3/rest/v3/projects.metricDescriptors/create
     *
     * @param type The metric type
     */
    @GetMapping("/create-metric-descriptor")
    public LoggingPocBean createMetricDescriptor(@RequestParam(value = "type") String type) throws IOException {
        // [START monitoring_create_metric]
        // Your Google Cloud Platform project ID
        String metricType = CUSTOM_METRIC_DOMAIN + "/" + type;

        final MetricServiceClient client = MetricServiceClient.create();
        ProjectName name = ProjectName.of(PROJECT_ID);

        MetricDescriptor descriptor = MetricDescriptor.newBuilder()
                .setType(metricType)
                .addLabels(LabelDescriptor
                        .newBuilder()
                        .setKey("store_id")
                        .setValueType(LabelDescriptor.ValueType.STRING))
                .setDescription("This is a simple example of a custom metric.")
                .setMetricKind(MetricDescriptor.MetricKind.GAUGE)
                .setValueType(MetricDescriptor.ValueType.DOUBLE)
                .build();

        CreateMetricDescriptorRequest request = CreateMetricDescriptorRequest.newBuilder()
                .setName(name.toString())
                .setMetricDescriptor(descriptor)
                .build();

        client.createMetricDescriptor(request);
        // [END monitoring_create_metric]

        long currentValue = counter.incrementAndGet();
        return new LoggingPocBean(currentValue, String.format("Created metric type: %s", metricType));

    }


    /**
     * Delete a metric descriptor.
     *
     * @param name Name of metric descriptor to delete
     */
    @GetMapping("/delete-metric-descriptor")
    public LoggingPocBean deleteMetricDescriptor(@RequestParam(value = "name") String name) throws IOException {
        // [START monitoring_delete_metric]
        final MetricServiceClient client = MetricServiceClient.create();
        MetricDescriptorName metricName = MetricDescriptorName.of(PROJECT_ID, name);
        client.deleteMetricDescriptor(metricName);
        // [END monitoring_delete_metric]

        long currentValue = counter.incrementAndGet();
        return new LoggingPocBean(currentValue, String.format("Deleted descriptor: %s", name));

    }

    /**
     * Demonstrates writing a time series value for the metric type
     * 'custom.google.apis.com/my_metric'.
     * <p>
     * This method assumes `my_metric` descriptor has already been created as a
     * DOUBLE value_type and GAUGE metric kind. If the metric descriptor
     * doesn't exist, it will be auto-created.
     */
    //CHECKSTYLE OFF: VariableDeclarationUsageDistance
    @GetMapping("/write-time-series")
    public LoggingPocBean writeTimeSeries() throws IOException {
        // [START monitoring_write_timeseries]
        String projectId = System.getProperty("projectId");
        // Instantiates a client
        MetricServiceClient metricServiceClient = MetricServiceClient.create();

        // Prepares an individual data point
        TimeInterval interval = TimeInterval.newBuilder()
                .setEndTime(Timestamps.fromMillis(System.currentTimeMillis()))
                .build();
        TypedValue value = TypedValue.newBuilder()
                .setDoubleValue(123.45)
                .build();
        Point point = Point.newBuilder()
                .setInterval(interval)
                .setValue(value)
                .build();

        List<Point> pointList = new ArrayList<>();
        pointList.add(point);

        ProjectName name = ProjectName.of(projectId);

        // Prepares the metric descriptor
        Map<String, String> metricLabels = new HashMap<>();
        Metric metric = Metric.newBuilder()
                .setType("custom.googleapis.com/my_metric")
                .putAllLabels(metricLabels)
                .build();

        // Prepares the monitored resource descriptor
        Map<String, String> resourceLabels = new HashMap<>();
        resourceLabels.put("instance_id", "1234567890123456789");
        resourceLabels.put("zone", "us-central1-f");

        MonitoredResource resource = MonitoredResource.newBuilder()
                .setType("gce_instance")
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
        System.out.println("Done writing time series value.");
        // [END monitoring_write_timeseries]

        long currentValue = counter.incrementAndGet();
        return new LoggingPocBean(currentValue, String.format("Write Time Series: %s", timeSeriesList));
    }
    //CHECKSTYLE ON: VariableDeclarationUsageDistance



}
