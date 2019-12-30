package org.sm.poc.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.api.Metric;
import com.google.api.MonitoredResource;
import com.google.cloud.MetadataConfig;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.monitoring.v3.*;
import com.google.protobuf.util.Timestamps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sm.poc.api.LoggingPocBean;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Path("/quick-start")
@Produces(MediaType.APPLICATION_JSON)
public class QuickStartResource {

    private final Logger log = LoggerFactory.getLogger(QuickStartResource.class);

    private final AtomicLong counter = new AtomicLong();

    public QuickStartResource() {
    }

    @GET
    @Timed
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

        String projectId = MetadataConfig.getProjectId();
        ProjectName name = ProjectName.of(projectId);

        // Prepares the metric descriptor
        Map<String, String> metricLabels = new HashMap<String, String>();
        metricLabels.put("store_wiz_id", "Pittsburg");
        Metric metric = Metric.newBuilder()
                .setType("custom.googleapis.com/storesWiz/daily_sales_wiz")
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


}