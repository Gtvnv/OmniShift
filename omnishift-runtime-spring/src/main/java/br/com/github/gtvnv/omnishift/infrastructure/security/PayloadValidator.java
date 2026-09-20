
package br.com.github.gtvnv.omnishift.infrastructure.security;

public class PayloadValidator {

 private static final int MAX_SIZE = 1000000;

 public void validate(String payload){

  if(payload.length() > MAX_SIZE){
   throw new RuntimeException("Payload too large");
  }

 }

}
