package br.com.github.gtvnv.omnishift.domain.ports;

import br.com.github.gtvnv.omnishift.domain.mapping.FieldMapping;

import java.util.List;

/**
 * Resolve um nome de perfil de mapeamento para a lista de FieldMapping que ele
 * representa. A definição dos perfis mora na infraestrutura (ex: application.yml
 * no omnishift-runtime-spring) — o core só conhece esta porta, mantendo a API de
 * entrada agnóstica: quem chama o OmniShift só precisa saber o nome do perfil,
 * nunca a sintaxe do DSL de mapeamento.
 */
@FunctionalInterface
public interface MappingProfileRepository {

    List<FieldMapping> findByName(String profileName);

    /**
     * Implementação padrão para quando nenhum repositório de perfis foi configurado.
     */
    static MappingProfileRepository none() {
        return profileName -> {
            throw new IllegalArgumentException("Nenhum perfil de mapeamento configurado: " + profileName);
        };
    }
}
