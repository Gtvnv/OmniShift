package br.com.github.gtvnv.omnishift.domain.ports;

/**
 * Porta de observabilidade: grava a duração e o resultado de uma conversão processada
 * pelo OmniShift. Implementação real (Micrometer) fica em omnishift-runtime-spring —
 * este módulo não sabe (nem precisa saber) qual biblioteca de métricas está por trás.
 */
@FunctionalInterface
public interface MetricsRecorder {

    void recordConversion(String sourceFormat, String targetFormat, long durationNanos, boolean success);

    static MetricsRecorder noop() {
        return (sourceFormat, targetFormat, durationNanos, success) -> { };
    }
}
