
package br.com.github.gtvnv.omnishift.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public non-sealed class OmniObject implements OmniNode {

 private final Map<String,OmniNode> properties = new LinkedHashMap<>();

 public void put(String key, OmniNode value){
  properties.put(key,value);
 }

 @Override
 public NodeType getType(){
  return NodeType.OBJECT;
 }

    public Map<String, OmniNode> getProperties() {
        // Retorna uma visão imutável. Se alguém tentar alterar, toma um UnsupportedOperationException
        return Collections.unmodifiableMap(properties);
    }

    // Adicionar um buscador direto (facilita muito a vida no TransformationEngine)
    public OmniNode get(String key) {
        return properties.getOrDefault(key, OmniNull.INSTANCE); // Evita retornos null nativos do Java!
    }

}
