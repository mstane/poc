package org.sm.poc;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.*;
import com.codahale.metrics.servlets.MetricsServlet;
import io.dropwizard.Application;
import io.dropwizard.configuration.ResourceConfigurationSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.github.gcmonitor.GcMonitor;
import io.github.gcmonitor.integration.dropwizard.DropwizardAdapter;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import org.apache.commons.lang3.StringUtils;
import org.sm.poc.core.MetricsSender;
import org.sm.poc.health.TemplateHealthCheck;
import org.sm.poc.metrics.CounterExample;
import org.sm.poc.resources.EchoResource;
import org.sm.poc.resources.EnvResource;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LoggingPocApplication extends Application<LoggingPocConfiguration> {

    public static void main(final String[] args) throws Exception {
        new LoggingPocApplication().run(args);
    }

    @Override
    public String getName() {
        return "loggingpoc";
    }

    @Override
    public void initialize(final Bootstrap<LoggingPocConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(new ResourceConfigurationSourceProvider());
    }

    @Override
    public void run(final LoggingPocConfiguration configuration,
                    final Environment environment) {
        final EchoResource echoResource = new EchoResource(
                configuration.getTemplate(),
                configuration.getDefaultName()
        );
        environment.jersey().register(echoResource);

        final EnvResource envResource = new EnvResource();
        environment.jersey().register(envResource);

        final TemplateHealthCheck healthCheck =
                new TemplateHealthCheck(configuration.getTemplate());
        environment.healthChecks().register("template", healthCheck);

//        startMetricsSender();

        // Custom metric example
        new CounterExample(environment.metrics());

        setPrometheusMetricAll(environment);

        setPrometheusMetricRequestManual(environment);

        setMetricRequestManual(environment);

        startGcMonitor(environment.metrics());

//        startConsoleReporterAll(environment);

        startConsoleReporterManualSetRequests();

        startFilteredMetrics(environment);

    }

    private void setPrometheusMetricAll(Environment environment) {
        CollectorRegistry collectorRegistry = new CollectorRegistry();
        collectorRegistry.register(new DropwizardExports(environment.metrics()));
        environment.admin()
                .addServlet("prometheusMetrics", new io.prometheus.client.exporter.MetricsServlet(collectorRegistry))
                .addMapping("/prometheusMetrics");
    }

    private void setPrometheusMetricRequestManual(Environment environment) {
        MetricRegistry metrics = new MetricRegistry();
        metrics.register("jvm.attribute", new JvmAttributeGaugeSet());
        metrics.register("jvm.buffers", new BufferPoolMetricSet(ManagementFactory
                .getPlatformMBeanServer()));
        metrics.register("jvm.classloader", new ClassLoadingGaugeSet());
        metrics.register("jvm.filedescriptor", new FileDescriptorRatioGauge());
        metrics.register("jvm.gc", new GarbageCollectorMetricSet());
        metrics.register("jvm.memory", new MemoryUsageGaugeSet());
        metrics.register("jvm.threads", new ThreadStatesGaugeSet());

        CollectorRegistry collectorRegistry = new CollectorRegistry();
        collectorRegistry.register(new DropwizardExports(metrics));
        environment.admin()
                .addServlet("prometheusMetrics2", new io.prometheus.client.exporter.MetricsServlet(collectorRegistry))
                .addMapping("/prometheusMetrics2");

        // Making requests metric (meter type) with value 2
        Meter requests = metrics.meter("requests");
        requests.mark();
        requests.mark();
    }

    private void setMetricRequestManual(Environment environment) {
        MetricRegistry metrics = new MetricRegistry();
        metrics.register("jvm.attribute", new JvmAttributeGaugeSet());
        metrics.register("jvm.buffers", new BufferPoolMetricSet(ManagementFactory
                .getPlatformMBeanServer()));
        metrics.register("jvm.classloader", new ClassLoadingGaugeSet());
        metrics.register("jvm.filedescriptor", new FileDescriptorRatioGauge());
        metrics.register("jvm.gc", new GarbageCollectorMetricSet());
        metrics.register("jvm.memory", new MemoryUsageGaugeSet());
        metrics.register("jvm.threads", new ThreadStatesGaugeSet());

        environment.admin()
                .addServlet("m2", new MetricsServlet(metrics))
                .addMapping("/m2");

        // Making requests metric (meter type) with value 2
        Meter requests = metrics.meter("requests");
        requests.mark();
        requests.mark();
    }

    private void startFilteredMetrics(Environment environment) {
        MetricRegistry adminMetricRegistry = environment.metrics();
        MetricRegistry metricRegistry = new MetricRegistry();

        getMetrics(adminMetricRegistry, "jvm.attribute").forEach(
                (k, v) -> metricRegistry.register(k, v)
        );

        environment.admin()
                .addServlet("m3", new MetricsServlet(metricRegistry))
                .addMapping("/m3");

    }


    @SuppressWarnings("unchecked")
    private Map<String, Metric> getMetrics(MetricRegistry metricRegistry, String prefix) {
        Map<String, Metric> allMetrics = metricRegistry.getMetrics();
        if (StringUtils.isEmpty(prefix)) {
            return allMetrics;
        } else {
            Map<String, Metric> res = new HashMap<>();
            for (Map.Entry<String, Metric> entry : allMetrics.entrySet()) {
                if (entry.getKey().startsWith(prefix)) {
                    res.put(entry.getKey(), entry.getValue());
                }
            }
            return res;
        }
    }


    private void startMetricsSender() {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);
        scheduledExecutorService.scheduleAtFixedRate(new MetricsSender(), 20, 60, TimeUnit.SECONDS);
    }

    private void startGcMonitor(MetricRegistry registry) {
        GcMonitor gcMonitor = GcMonitor.builder()
                .addRollingWindow("15min", Duration.ofMinutes(15))
                .build();
        gcMonitor.start();

        registry.registerAll(DropwizardAdapter.toMetricSet("jvm-gc-monitor", gcMonitor));
    }

    private void startConsoleReporterAll(Environment environment) {
        ConsoleReporter reporter = ConsoleReporter.forRegistry(environment.metrics())
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(10, TimeUnit.SECONDS);
    }

    private void startConsoleReporterManualSetRequests() {
        MetricRegistry metrics = new MetricRegistry();

        ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(10, TimeUnit.SECONDS);

        // Making requests metric (meter type) with value 2
        Meter requests = metrics.meter("requests");
        requests.mark();
        requests.mark();
    }


}
