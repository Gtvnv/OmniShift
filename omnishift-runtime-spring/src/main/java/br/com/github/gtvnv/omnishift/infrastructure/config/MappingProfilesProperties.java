package br.com.github.gtvnv.omnishift.infrastructure.config;

import br.com.github.gtvnv.omnishift.domain.mapping.FieldMapping;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Perfis de mapeamento nomeados, configurados em application.yml sob o prefixo
 * "omnishift.mapping-profiles". Cada perfil é uma lista de FieldMapping — o cliente
 * da API só precisa referenciar o nome do perfil (ver X-Mapping-Profile no
 * ShiftController), nunca a definição dos mappings em si.
 */
@ConfigurationProperties(prefix = "omnishift")
public class MappingProfilesProperties {

    private Map<String, List<FieldMapping>> mappingProfiles = new HashMap<>();

    public Map<String, List<FieldMapping>> getMappingProfiles() {
        return mappingProfiles;
    }

    public void setMappingProfiles(Map<String, List<FieldMapping>> mappingProfiles) {
        this.mappingProfiles = mappingProfiles;
    }
}
