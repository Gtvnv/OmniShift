package br.com.github.gtvnv.omnishift.infrastructure.adapters.inbound.grpc;

import br.com.github.gtvnv.omnishift.infrastructure.security.ApiKeyValidator;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Registrado automaticamente em todos os serviços gRPC pelo grpc-server-spring-boot-starter
 * (via @GrpcGlobalServerInterceptor) — nem o método unário nem o streaming de
 * GrpcShiftInboundAdapter precisam saber que autenticação existe.
 */
@GrpcGlobalServerInterceptor
public class ApiKeyServerInterceptor implements ServerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyServerInterceptor.class);
    private static final Metadata.Key<String> API_KEY_METADATA_KEY =
            Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER);

    private final ApiKeyValidator apiKeyValidator;

    public ApiKeyServerInterceptor(ApiKeyValidator apiKeyValidator) {
        this.apiKeyValidator = apiKeyValidator;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        Optional<String> clientName = apiKeyValidator.resolveClientName(headers.get(API_KEY_METADATA_KEY));

        if (clientName.isEmpty()) {
            log.warn("Chamada gRPC rejeitada: metadata x-api-key ausente ou invalida ({})",
                    call.getMethodDescriptor().getFullMethodName());
            call.close(Status.UNAUTHENTICATED.withDescription("Metadata x-api-key ausente ou inválida."),
                    new Metadata());
            return new ServerCall.Listener<>() {
            };
        }

        log.info("Chamada gRPC autenticada: cliente={}", clientName.get());
        return next.startCall(call, headers);
    }
}
