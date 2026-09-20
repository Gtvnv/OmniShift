package br.com.github.gtvnv.omnishift.infrastructure.adapters.inbound.grpc;

import br.com.github.gtvnv.omnishift.application.factory.ParserFactory;
import br.com.github.gtvnv.omnishift.application.factory.SerializerFactory;
import br.com.github.gtvnv.omnishift.application.usecase.ShiftDataUseCase;
import br.com.github.gtvnv.omnishift.domain.ports.MetricsRecorder;
import br.com.github.gtvnv.omnishift.infrastructure.adapters.inbound.grpc.generated.ShiftGrpcRequestChunk;
import br.com.github.gtvnv.omnishift.infrastructure.adapters.inbound.grpc.generated.ShiftGrpcResponseChunk;
import br.com.github.gtvnv.omnishift.infrastructure.adapters.inbound.grpc.generated.ShiftGrpcStreamMetadata;
import br.com.github.gtvnv.omnishift.infrastructure.security.PayloadValidator;
import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sem infraestrutura de teste gRPC (grpc-testing/servidor in-process): chama
 * shiftDataStream(fakeResponseObserver) direto e invoca onNext/onCompleted manualmente
 * no StreamObserver de requisição retornado — mesmo estilo de fakes já usado no projeto
 * (sem Mockito). ParserFactory.discover()/SerializerFactory.discover() encontram os
 * adapters JSON/XML reais via SPI porque omnishift-runtime-spring já depende deles.
 */
class GrpcShiftInboundAdapterTest {

    private final FakeMetricsRecorder metricsRecorder = new FakeMetricsRecorder();
    private final GrpcShiftInboundAdapter adapter = new GrpcShiftInboundAdapter(
            new ShiftDataUseCase(ParserFactory.discover(), SerializerFactory.discover()),
            new PayloadValidator(),
            metricsRecorder);

    @Test
    void streamJsonParaXmlComPayloadDivididoEmVariosChunks() {
        FakeResponseObserver responseObserver = new FakeResponseObserver();
        StreamObserver<ShiftGrpcRequestChunk> requestObserver = adapter.shiftDataStream(responseObserver);

        requestObserver.onNext(metadataChunk("JSON", "XML", ""));
        byte[] bytes = "{\"nome\":\"Tavera\"}".getBytes(StandardCharsets.UTF_8);
        int meio = bytes.length / 2;
        requestObserver.onNext(dataChunk(bytes, 0, meio));
        requestObserver.onNext(dataChunk(bytes, meio, bytes.length - meio));
        requestObserver.onCompleted();

        assertTrue(responseObserver.completed);
        assertNull(responseObserver.error);
        assertFalse(responseObserver.chunks.isEmpty());
        assertTrue(concatenarChunks(responseObserver.chunks).contains("<nome>Tavera</nome>"));

        assertEquals(1, metricsRecorder.recordings.size());
        assertTrue(metricsRecorder.recordings.get(0).success());
        assertEquals("JSON", metricsRecorder.recordings.get(0).sourceFormat());
        assertEquals("XML", metricsRecorder.recordings.get(0).targetFormat());
    }

    @Test
    void dataChunkAntesDeMetadataFalhaComInvalidArgument() {
        FakeResponseObserver responseObserver = new FakeResponseObserver();
        StreamObserver<ShiftGrpcRequestChunk> requestObserver = adapter.shiftDataStream(responseObserver);

        requestObserver.onNext(dataChunk("qualquer".getBytes(StandardCharsets.UTF_8), 0, 8));

        assertNotNull(responseObserver.error);
        assertEquals(Status.INVALID_ARGUMENT.getCode(), Status.fromThrowable(responseObserver.error).getCode());
        assertEquals(1, metricsRecorder.recordings.size());
        assertFalse(metricsRecorder.recordings.get(0).success());
    }

    @Test
    void onCompletedSemMetadataFalhaComInvalidArgument() {
        FakeResponseObserver responseObserver = new FakeResponseObserver();
        StreamObserver<ShiftGrpcRequestChunk> requestObserver = adapter.shiftDataStream(responseObserver);

        requestObserver.onCompleted();

        assertNotNull(responseObserver.error);
        assertEquals(Status.INVALID_ARGUMENT.getCode(), Status.fromThrowable(responseObserver.error).getCode());
        assertEquals(1, metricsRecorder.recordings.size());
        assertFalse(metricsRecorder.recordings.get(0).success());
    }

    @Test
    void duasMensagensDeMetadataFalhaComInvalidArgument() {
        FakeResponseObserver responseObserver = new FakeResponseObserver();
        StreamObserver<ShiftGrpcRequestChunk> requestObserver = adapter.shiftDataStream(responseObserver);

        requestObserver.onNext(metadataChunk("JSON", "XML", ""));
        requestObserver.onNext(metadataChunk("JSON", "XML", ""));

        assertNotNull(responseObserver.error);
        assertEquals(Status.INVALID_ARGUMENT.getCode(), Status.fromThrowable(responseObserver.error).getCode());
        assertEquals(1, metricsRecorder.recordings.size());
        assertFalse(metricsRecorder.recordings.get(0).success());
    }

    private static ShiftGrpcRequestChunk metadataChunk(String source, String target, String profile) {
        return ShiftGrpcRequestChunk.newBuilder()
                .setMetadata(ShiftGrpcStreamMetadata.newBuilder()
                        .setSourceFormat(source)
                        .setTargetFormat(target)
                        .setMappingProfile(profile)
                        .build())
                .build();
    }

    private static ShiftGrpcRequestChunk dataChunk(byte[] bytes, int offset, int length) {
        return ShiftGrpcRequestChunk.newBuilder()
                .setDataChunk(ByteString.copyFrom(bytes, offset, length))
                .build();
    }

    private static String concatenarChunks(List<ShiftGrpcResponseChunk> chunks) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (ShiftGrpcResponseChunk chunk : chunks) {
            out.writeBytes(chunk.getDataChunk().toByteArray());
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    private static final class FakeMetricsRecorder implements MetricsRecorder {
        record Recording(String sourceFormat, String targetFormat, long durationNanos, boolean success) { }

        final List<Recording> recordings = new ArrayList<>();

        @Override
        public void recordConversion(String sourceFormat, String targetFormat, long durationNanos, boolean success) {
            recordings.add(new Recording(sourceFormat, targetFormat, durationNanos, success));
        }
    }

    private static final class FakeResponseObserver implements StreamObserver<ShiftGrpcResponseChunk> {
        final List<ShiftGrpcResponseChunk> chunks = new ArrayList<>();
        boolean completed = false;
        Throwable error;

        @Override
        public void onNext(ShiftGrpcResponseChunk value) {
            chunks.add(value);
        }

        @Override
        public void onError(Throwable t) {
            error = t;
        }

        @Override
        public void onCompleted() {
            completed = true;
        }
    }
}
