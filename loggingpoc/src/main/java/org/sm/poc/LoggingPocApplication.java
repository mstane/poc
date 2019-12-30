package org.sm.poc;

import io.dropwizard.Application;
import io.dropwizard.configuration.ResourceConfigurationSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sm.poc.core.MetricsSender;
import org.sm.poc.health.TemplateHealthCheck;
import org.sm.poc.resources.EchoResource;
import org.sm.poc.resources.EnvResource;

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

        startMetricsSender();

    }

    private void startMetricsSender() {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);
        scheduledExecutorService.scheduleAtFixedRate(new MetricsSender(), 20,60, TimeUnit.SECONDS);
    }

}
