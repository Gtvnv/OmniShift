
package br.com.github.gtvnv.omnishift.domain.model;

public non-sealed class OmniValue implements OmniNode {

 private final Object value;

 public OmniValue(Object value){
  this.value=value;
 }

 public Object getValue(){
  return value;
 }

 @Override
 public NodeType getType(){
  return NodeType.VALUE;
 }

}
