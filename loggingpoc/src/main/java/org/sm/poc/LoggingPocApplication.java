package org.sm.poc;

import com.codahale.metrics.MetricRegistry;
import io.dropwizard.Application;
import io.dropwizard.configuration.ResourceConfigurationSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.github.gcmonitor.GcMonitor;
import io.github.gcmonitor.integration.dropwizard.DropwizardAdapter;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import org.sm.poc.core.MetricsSender;
import org.sm.poc.health.TemplateHealthCheck;
import org.sm.poc.metrics.CounterExample;
import org.sm.poc.resources.EchoResource;
import org.sm.poc.resources.EnvResource;

import java.time.Duration;
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

//        CollectorRegistry.defaultRegistry.register(new DropwizardExports(new MetricRegistry()));

        CollectorRegistry collectorRegistry = new CollectorRegistry();
        collectorRegistry.register(new DropwizardExports(environment.metrics()));
        environment.admin()
                .addServlet("prometheusMetrics", new io.prometheus.client.exporter.MetricsServlet(collectorRegistry))
                .addMapping("/prometheusMetrics");

        startGcMonitor(environment.metrics());

    }

    private void startMetricsSender() {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);
        scheduledExecutorService.scheduleAtFixedRate(new MetricsSender(), 20,60, TimeUnit.SECONDS);
    }

    private void startGcMonitor(MetricRegistry registry) {
        GcMonitor gcMonitor = GcMonitor.builder()
                .addRollingWindow("15min", Duration.ofMinutes(15))
                .build();
        gcMonitor.start();

        registry.registerAll(DropwizardAdapter.toMetricSet("jvm-gc-monitor", gcMonitor));
    }

}
