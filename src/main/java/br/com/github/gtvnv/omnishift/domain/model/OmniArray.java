
package br.com.github.gtvnv.omnishift.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public non-sealed class OmniArray implements OmniNode {

 private final List<OmniNode> elements = new ArrayList<>();

 public void add(OmniNode node){
  elements.add(node);
 }

 @Override
 public NodeType getType(){
  return NodeType.ARRAY;
 }

    public List<OmniNode> getElements() {
        return Collections.unmodifiableList(elements);
    }

    // Adicionar um buscador por índice
    public OmniNode get(int index) {
        if (index >= 0 && index < elements.size()) {
            return elements.get(index);
        }
        return OmniNull.INSTANCE; // Index out of bounds vira um OmniNull elegante
    }

}
