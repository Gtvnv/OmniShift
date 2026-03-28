package br.com.github.gtvnv.omnishift.application.dto;

/**
 * Objeto de Transferência de Dados (DTO) que carrega as instruções 
 * e o payload bruto para o motor de conversão do OmniShift.
 */
public record ShiftRequest(
        String rawPayload,
        String sourceFormat,
        String targetFormat
) {
    // Usamos o 'record' do Java (introduzido no Java 14 e consolidado no 21).
    // Ele já cria automaticamente os construtores, getters, equals, hashCode e toString.
    // Isso mantém o código extremamente limpo e imutável, o que é perfeito para DTOs e transitações Thread-Safe.

    public ShiftRequest {
        // Validação compacta do record (executada na construção)
        if (rawPayload == null || rawPayload.isBlank()) {
            throw new IllegalArgumentException("O payload não pode ser nulo ou vazio.");
        }
        if (sourceFormat == null || sourceFormat.isBlank()) {
            throw new IllegalArgumentException("O formato de origem deve ser informado.");
        }
        if (targetFormat == null || targetFormat.isBlank()) {
            throw new IllegalArgumentException("O formato de destino deve ser informado.");
        }
    }
}