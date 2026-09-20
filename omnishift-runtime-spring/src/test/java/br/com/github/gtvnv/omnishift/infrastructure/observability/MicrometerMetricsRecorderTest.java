package br.com.github.gtvnv.omnishift.infrastructure.observability;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MicrometerMetricsRecorderTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final MicrometerMetricsRecorder recorder = new MicrometerMetricsRecorder(registry);

    @Test
    void recordConversionDeSucessoGravaTimerComTagsCorretas() {
        recorder.recordConversion("json", "xml", TimeUnit.MILLISECONDS.toNanos(10), true);

        Timer timer = registry.find("omnishift.conversions")
                .tag("source_format", "JSON")
                .tag("target_format", "XML")
                .tag("outcome", "success")
                .timer();

        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void recordConversionDeErroGravaTimerComOutcomeError() {
        recorder.recordConversion("json", "xml", TimeUnit.MILLISECONDS.toNanos(5), false);

        Timer timer = registry.find("omnishift.conversions")
                .tag("source_format", "JSON")
                .tag("target_format", "XML")
                .tag("outcome", "error")
                .timer();

        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void sucessoEErroSaoContadosSeparadamentePorOutcome() {
        recorder.recordConversion("json", "xml", TimeUnit.MILLISECONDS.toNanos(1), true);
        recorder.recordConversion("json", "xml", TimeUnit.MILLISECONDS.toNanos(1), true);
        recorder.recordConversion("json", "xml", TimeUnit.MILLISECONDS.toNanos(1), false);

        Timer sucesso = registry.find("omnishift.conversions").tag("outcome", "success").timer();
        Timer erro = registry.find("omnishift.conversions").tag("outcome", "error").timer();

        assertEquals(2, sucesso.count());
        assertEquals(1, erro.count());
    }
}
