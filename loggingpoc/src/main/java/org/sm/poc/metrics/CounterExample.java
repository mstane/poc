package org.sm.poc.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

import java.util.concurrent.atomic.AtomicInteger;

public class CounterExample {

    private AtomicInteger counter = new AtomicInteger();

    public CounterExample(MetricRegistry metrics) {
        metrics.register(MetricRegistry.name(CounterExample.class, "numberOfCalls"),
                new Gauge<Integer>() {
                    @Override
                    public Integer getValue() {
                        return counter.incrementAndGet();
                    }
                });
    }

}
