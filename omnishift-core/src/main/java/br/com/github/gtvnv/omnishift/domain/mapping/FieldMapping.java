package br.com.github.gtvnv.omnishift.domain.mapping;

/**
 * Uma regra de mapeamento de campo: de onde ler no nó de origem (sourcePath)
 * para onde escrever no nó de destino (targetPath). Sintaxe de caminho: segmentos
 * separados por '.', com suporte a índice de array na origem (ex: "itens[0].nome").
 * O destino não suporta índice de array (ver OmniPath).
 */
public record FieldMapping(String sourcePath, String targetPath) {

    public FieldMapping {
        if (sourcePath == null || sourcePath.isBlank()) {
            throw new IllegalArgumentException("O caminho de origem (sourcePath) não pode ser nulo ou vazio.");
        }
        if (targetPath == null || targetPath.isBlank()) {
            throw new IllegalArgumentException("O caminho de destino (targetPath) não pode ser nulo ou vazio.");
        }
    }
}
