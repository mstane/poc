package org.sm.poc.loggingpoc;

import com.google.api.Metric;
import com.sun.management.UnixOperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

public enum MetricTypes {

    JVM_HEAP_MEMORY_USED("jvmHeapMemoryUsed", () -> ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed()),
    JVM_HEAP_MEMORY_INIT("jvmHeapMemoryInit", () -> ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getInit()),
    JVM_HEAP_MEMORY_MAX("jvmHeapMemoryMax", () -> ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax()),
    JVM_HEAP_MEMORY_COMMITTED("jvmHeapMemoryCommitted", () -> ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getCommitted()),

    JVM_NON_HEAP_MEMORY_USED("jvmNonHeapMemoryUsed", () -> ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed()),
    JVM_NON_HEAP_MEMORY_INIT("jvmNonHeapMemoryInit", () -> ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getInit()),
    JVM_NON_HEAP_MEMORY_MAX("jvmNonHeapMemoryMax", () -> ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getMax()),
    JVM_NON_HEAP_MEMORY_COMMITTED("jvmNonHeapMemoryCommitted", () -> ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getCommitted()),

    JVM_THREAD_COUNT("jvmThreadCount", () -> Long.valueOf(ManagementFactory.getThreadMXBean().getThreadCount())),
    JVM_DAEMON_THREAD_COUNT("jvmDaemonThreadCount", () -> Long.valueOf(ManagementFactory.getThreadMXBean().getDaemonThreadCount())),
    JVM_PEAK_THREAD_COUNT("jvmPeakThreadCount", () -> Long.valueOf(ManagementFactory.getThreadMXBean().getPeakThreadCount())),

    JVM_OPEN_FILE_DESCRIPTOR_COUNT("jvmOpenFileDescriptorCount", () -> {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        if(os instanceof UnixOperatingSystemMXBean){
            return ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount();
        }
        return 0L;
    }),

    JVM_GC_TIME("jvmGcTime", () -> ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(g -> g.getCollectionTime()).sum()),
    JVM_GC_COUNT("jvmGcCount", () -> ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(g -> g.getCollectionCount()).sum());

    private static final String METRIC_ROOT_DOMAIN = "custom.googleapis.com/my/";

    private final Metric metric;
    private final Supplier<Long> function;

    MetricTypes(String title, Supplier<Long> function) {
        this.metric = Metric.newBuilder()
                .setType(METRIC_ROOT_DOMAIN + title)
                .putAllLabels(new HashMap<>())
                .build();
        this.function = function;
    }

    public Metric getMetricDescriptor() {
        return this.metric;
    }

    public Supplier<Long> getFunction() {
        return this.function;
    }

    public static Stream<MetricTypes> stream() {
        return Stream.of(MetricTypes.values());
    }

}
