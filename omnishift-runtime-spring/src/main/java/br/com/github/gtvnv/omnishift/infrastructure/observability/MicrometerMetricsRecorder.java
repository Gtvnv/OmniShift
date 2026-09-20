package br.com.github.gtvnv.omnishift.infrastructure.observability;

import br.com.github.gtvnv.omnishift.domain.ports.MetricsRecorder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class MicrometerMetricsRecorder implements MetricsRecorder {

    private final MeterRegistry registry;

    public MicrometerMetricsRecorder(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void recordConversion(String sourceFormat, String targetFormat, long durationNanos, boolean success) {
        // Um único Timer (não um Counter separado) com a tag "outcome" já dá contagem e
        // latência juntos, na mesma convenção do http.server.requests do próprio Spring.
        Timer.builder("omnishift.conversions")
                .description("Duração e contagem de conversões processadas pelo OmniShift")
                .tag("source_format", normalize(sourceFormat))
                .tag("target_format", normalize(targetFormat))
                .tag("outcome", success ? "success" : "error")
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    private String normalize(String format) {
        return format == null ? "UNKNOWN" : format.toUpperCase();
    }
}
