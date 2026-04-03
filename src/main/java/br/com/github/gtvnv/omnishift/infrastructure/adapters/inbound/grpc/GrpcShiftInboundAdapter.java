package br.com.github.gtvnv.omnishift.infrastructure.adapters.inbound.grpc;

import br.com.github.gtvnv.omnishift.application.dto.ShiftRequest;
import br.com.github.gtvnv.omnishift.infrastructure.adapters.inbound.grpc.generated.ShiftGrpcRequest;
import br.com.github.gtvnv.omnishift.infrastructure.adapters.inbound.grpc.generated.ShiftGrpcResponse;
import br.com.github.gtvnv.omnishift.infrastructure.adapters.inbound.grpc.generated.ShiftServiceGrpc;
import br.com.github.gtvnv.omnishift.application.usecase.ShiftDataUseCase;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adaptador de entrada gRPC.
 * Atua como a "Porta da Rua" para microsserviços internos que falam Protobuf.
 */
@GrpcService // Anotação específica que sobe um servidor gRPC na porta 9090 (padrão)
public class GrpcShiftInboundAdapter extends ShiftServiceGrpc.ShiftServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(GrpcShiftInboundAdapter.class);
    private final ShiftDataUseCase shiftDataUseCase;

    public GrpcShiftInboundAdapter(ShiftDataUseCase shiftDataUseCase) {
        this.shiftDataUseCase = shiftDataUseCase;
    }

    @Override
    public void shiftData(ShiftGrpcRequest grpcRequest, StreamObserver<ShiftGrpcResponse> responseObserver) {
        log.info("Recebendo chamada gRPC de conversao: {} -> {}",
                grpcRequest.getSourceFormat(), grpcRequest.getTargetFormat());

        try {
            // 1. Mapeamos o Request do gRPC para o nosso DTO de Domínio
            ShiftRequest internalRequest = new ShiftRequest(
                    grpcRequest.getRawPayload(),
                    grpcRequest.getSourceFormat(),
                    grpcRequest.getTargetFormat()
            );

            // 2. O UseCase faz a mágica (Ele não sabe que isso veio do gRPC!)
            String convertedData = shiftDataUseCase.execute(internalRequest);

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
}