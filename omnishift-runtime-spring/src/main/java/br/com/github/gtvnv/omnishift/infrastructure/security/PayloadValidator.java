
package br.com.github.gtvnv.omnishift.infrastructure.security;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
public class PayloadValidator {

 private static final int MAX_SIZE = 1000000;

 // Bem acima do cap acima (que existe para manter REST/gRPC unário pequenos por natureza),
 // mas ainda finito: o OmniNode é montado inteiro em memória antes de serializar, então um
 // cliente mal-intencionado ainda poderia esgotar heap sem algum limite no streaming gRPC.
 private static final long MAX_STREAMING_SIZE = 50_000_000;

 public void validate(String payload){

  if(payload.length() > MAX_SIZE){
   // IllegalArgumentException: erro do cliente (payload grande demais), não interno.
   // O GlobalExceptionHandler já mapeia isso para HTTP 400.
   throw new IllegalArgumentException("Payload excede o tamanho máximo permitido (" + MAX_SIZE + " caracteres)");
  }

 }

 /**
  * Versão streaming do limite de tamanho: em vez de checar uma String já inteira em
  * memória, envolve o InputStream para que o limite seja aplicado durante a leitura.
  * Funciona mesmo sem Content-Length confiável (ex: Transfer-Encoding: chunked).
  */
 public InputStream limit(InputStream input) {
  return new BoundedInputStream(input, MAX_SIZE);
 }

 /**
  * Versão "push" do limite de tamanho, para o streaming gRPC: os pedaços chegam via
  * callbacks (onNext) conforme a rede entrega, não como um InputStream de leitura sob
  * demanda — por isso um acumulador em vez de reaproveitar o BoundedInputStream acima.
  */
 public ChunkedPayloadAccumulator newStreamingAccumulator() {
  return new ChunkedPayloadAccumulator(MAX_STREAMING_SIZE);
 }

 public static final class ChunkedPayloadAccumulator {

  private final long maxBytes;
  private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

  private ChunkedPayloadAccumulator(long maxBytes) {
   this.maxBytes = maxBytes;
  }

  public void append(byte[] chunk) {
   if (buffer.size() + chunk.length > maxBytes) {
    throw new IllegalArgumentException(
            "Payload em streaming excede o tamanho máximo permitido (" + maxBytes + " bytes)");
   }
   buffer.writeBytes(chunk);
  }

  public byte[] toByteArray() {
   return buffer.toByteArray();
  }
 }

 private static final class BoundedInputStream extends FilterInputStream {

  private final int maxBytes;
  private long count = 0;

  BoundedInputStream(InputStream in, int maxBytes) {
   super(in);
   this.maxBytes = maxBytes;
  }

  @Override
  public int read() throws IOException {
   int b = super.read();
   if (b != -1) {
    checkLimit(1);
   }
   return b;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
   int n = super.read(b, off, len);
   if (n > 0) {
    checkLimit(n);
   }
   return n;
  }

  private void checkLimit(int justRead) {
   count += justRead;
   if (count > maxBytes) {
    throw new IllegalArgumentException("Payload excede o tamanho máximo permitido (" + maxBytes + " bytes)");
   }
  }
 }

}
