package br.com.github.gtvnv.omnishift.infrastructure.adapters.inbound.grpc;

import br.com.github.gtvnv.omnishift.application.dto.ShiftRequest;
import br.com.github.gtvnv.omnishift.domain.model.OmniNode;
import br.com.github.gtvnv.omnishift.domain.ports.DataSerializer;
import br.com.github.gtvnv.omnishift.infrastructure.adapters.inbound.grpc.generated.ShiftGrpcRequest;
import br.com.github.gtvnv.omnishift.infrastructure.adapters.inbound.grpc.generated.ShiftGrpcRequestChunk;
import br.com.github.gtvnv.omnishift.infrastructure.adapters.inbound.grpc.generated.ShiftGrpcResponse;
import br.com.github.gtvnv.omnishift.infrastructure.adapters.inbound.grpc.generated.ShiftGrpcResponseChunk;
import br.com.github.gtvnv.omnishift.infrastructure.adapters.inbound.grpc.generated.ShiftGrpcStreamMetadata;
import br.com.github.gtvnv.omnishift.infrastructure.adapters.inbound.grpc.generated.ShiftServiceGrpc;
import br.com.github.gtvnv.omnishift.application.usecase.ShiftDataUseCase;
import br.com.github.gtvnv.omnishift.infrastructure.security.PayloadValidator;
import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

/**
 * Adaptador de entrada gRPC.
 * Atua como a "Porta da Rua" para microsserviços internos que falam Protobuf.
 */
@GrpcService // Anotação específica que sobe um servidor gRPC na porta 9090 (padrão)
public class GrpcShiftInboundAdapter extends ShiftServiceGrpc.ShiftServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(GrpcShiftInboundAdapter.class);
    private final ShiftDataUseCase shiftDataUseCase;
    private final PayloadValidator payloadValidator;

    public GrpcShiftInboundAdapter(ShiftDataUseCase shiftDataUseCase, PayloadValidator payloadValidator) {
        this.shiftDataUseCase = shiftDataUseCase;
        this.payloadValidator = payloadValidator;
    }

    @Override
    public void shiftData(ShiftGrpcRequest grpcRequest, StreamObserver<ShiftGrpcResponse> responseObserver) {
        log.info("Recebendo chamada gRPC de conversao: {} -> {}",
                grpcRequest.getSourceFormat(), grpcRequest.getTargetFormat());

        try {
            // 0. Sanitização de conteúdo: rejeita payloads acima do limite antes de qualquer parsing
            payloadValidator.validate(grpcRequest.getRawPayload());

            // 1. Mapeamos o Request do gRPC para o nosso DTO de Domínio
            ShiftRequest internalRequest = new ShiftRequest(
                    grpcRequest.getRawPayload(),
                    grpcRequest.getSourceFormat(),
                    grpcRequest.getTargetFormat()
            );

            // 2. O UseCase faz a mágica (Ele não sabe que isso veio do gRPC!)
            String mappingProfile = grpcRequest.getMappingProfile();
            String convertedData = mappingProfile.isBlank()
                    ? shiftDataUseCase.execute(internalRequest)
                    : shiftDataUseCase.executeWithProfile(internalRequest, mappingProfile);

            // 3. Montamos a resposta gRPC
            ShiftGrpcResponse response = ShiftGrpcResponse.newBuilder()
                    .setConvertedPayload(convertedData)
                    .build();

            // 4. Enviamos a resposta com sucesso (onNext) e fechamos a conexão (onCompleted)
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Chamada gRPC concluída com sucesso.");

        } catch (IllegalArgumentException e) {
            // Tratamento de erro nível gRPC (Status.INVALID_ARGUMENT)
            log.warn("Payload ou formato invalido via gRPC: {}", e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());

        } catch (Exception e) {
            // Erro interno (Status.INTERNAL)
            log.error("Erro critico interno processando chamada gRPC", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Erro interno no motor OmniShift")
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    /**
     * Bidirecional: cliente envia o payload em pedaços (contorna o limite padrão de ~4MB
     * por mensagem do gRPC), servidor devolve o resultado também em pedaços. O OmniNode
     * ainda é montado inteiro em memória entre parse e serialize (mesmo limite já
     * documentado para o REST) — o que se ganha aqui é contornar o limite de tamanho de
     * mensagem do protocolo, não streaming incremental campo-a-campo.
     */
    @Override
    public StreamObserver<ShiftGrpcRequestChunk> shiftDataStream(StreamObserver<ShiftGrpcResponseChunk> responseObserver) {
        return new StreamObserver<>() {
            private String sourceFormat;
            private String targetFormat;
            private String mappingProfile;
            private PayloadValidator.ChunkedPayloadAccumulator accumulator;
            private boolean metadataReceived = false;
            private volatile boolean terminated = false;

            @Override
            public void onNext(ShiftGrpcRequestChunk chunk) {
                if (terminated) {
                    return;
                }
                try {
                    switch (chunk.getPayloadCase()) {
                        case METADATA -> {
                            if (metadataReceived) {
                                throw new IllegalArgumentException(
                                        "Metadados já recebidos; envie apenas uma vez, no início do stream.");
                            }
                            ShiftGrpcStreamMetadata metadata = chunk.getMetadata();
                            sourceFormat = metadata.getSourceFormat();
                            targetFormat = metadata.getTargetFormat();
                            mappingProfile = metadata.getMappingProfile();
                            accumulator = payloadValidator.newStreamingAccumulator();
                            metadataReceived = true;
                        }
                        case DATA_CHUNK -> {
                            if (!metadataReceived) {
                                throw new IllegalArgumentException(
                                        "Metadados (formato/perfil) devem ser enviados antes dos pedaços de dados.");
                            }
                            accumulator.append(chunk.getDataChunk().toByteArray());
                        }
                        default -> throw new IllegalArgumentException("Mensagem de streaming vazia ou desconhecida.");
                    }
                } catch (IllegalArgumentException e) {
                    fail(Status.INVALID_ARGUMENT, e.getMessage(), null);
                }
            }

            @Override
            public void onError(Throwable t) {
                terminated = true;
                log.warn("Stream gRPC de entrada encerrado com erro pelo cliente: {}", t.getMessage());
            }

            @Override
            public void onCompleted() {
                if (terminated) {
                    return;
                }
                try {
                    if (!metadataReceived) {
                        throw new IllegalArgumentException("Nenhum metadado recebido antes do encerramento do stream.");
                    }
                    InputStream rawPayload = new ByteArrayInputStream(accumulator.toByteArray());
                    boolean hasProfile = mappingProfile != null && !mappingProfile.isBlank();

                    OmniNode canonicalData = hasProfile
                            ? shiftDataUseCase.parseAndTransformWithProfile(rawPayload, sourceFormat, mappingProfile)
                            : shiftDataUseCase.parseAndTransform(rawPayload, sourceFormat, List.of());

                    DataSerializer serializer = shiftDataUseCase.resolveSerializer(targetFormat);
                    ByteArrayOutputStream serialized = new ByteArrayOutputStream();
                    serializer.serialize(canonicalData, serialized);

                    writeInChunks(serialized.toByteArray(), responseObserver);
                    terminated = true;
                    responseObserver.onCompleted();

                    log.info("Chamada gRPC de streaming concluida com sucesso.");

                } catch (IllegalArgumentException e) {
                    fail(Status.INVALID_ARGUMENT, e.getMessage(), null);
                } catch (Exception e) {
                    fail(Status.INTERNAL, "Erro interno no motor OmniShift", e);
                }
            }

            private void fail(Status status, String description, Throwable cause) {
                terminated = true;
                log.warn("Stream gRPC de entrada/saida encerrado com erro: {}", description);
                Status withDescription = status.withDescription(description);
                responseObserver.onError(
                        (cause != null ? withDescription.withCause(cause) : withDescription).asRuntimeException());
            }
        };
    }

    private static final int RESPONSE_CHUNK_SIZE = 256 * 1024; // bem abaixo do limite padrão de ~4MB/mensagem do gRPC

    private static void writeInChunks(byte[] data, StreamObserver<ShiftGrpcResponseChunk> observer) {
        int offset = 0;
        do {
            int length = Math.min(RESPONSE_CHUNK_SIZE, data.length - offset);
            observer.onNext(ShiftGrpcResponseChunk.newBuilder()
                    .setDataChunk(ByteString.copyFrom(data, offset, length))
                    .build());
            offset += length;
        } while (offset < data.length);
    }
}