
package br.com.github.gtvnv.omnishift.infrastructure.security;

import org.springframework.stereotype.Component;

@Component
public class PayloadValidator {

 private static final int MAX_SIZE = 1000000;

 public void validate(String payload){

  if(payload.length() > MAX_SIZE){
   // IllegalArgumentException: erro do cliente (payload grande demais), não interno.
   // O GlobalExceptionHandler já mapeia isso para HTTP 400.
   throw new IllegalArgumentException("Payload excede o tamanho máximo permitido (" + MAX_SIZE + " caracteres)");
  }

 }

}
