package com.logpilot.client.grpc;

import com.logpilot.grpc.proto.LogPilotProto;
import com.logpilot.grpc.proto.LogServiceGrpc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogPilotGrpcClientTest {

    @Mock
    private LogServiceGrpc.LogServiceBlockingStub blockingStub;
    @Mock
    private ScheduledExecutorService scheduler;
    @Mock
    private io.grpc.ManagedChannel channel;

    private LogPilotGrpcClient client;

    @BeforeEach
    void setUp() {
        client = new LogPilotGrpcClient(channel, blockingStub, 3);
    }

    @Test
    void testSeekToBeginning() {
        LogPilotProto.SeekResponse successResponse = LogPilotProto.SeekResponse.newBuilder()
                .setStatus("success")
                .build();
        when(blockingStub.seek(any(LogPilotProto.SeekRequest.class))).thenReturn(successResponse);

        client.seekToBeginning("test-channel", "test-consumer");

        ArgumentCaptor<LogPilotProto.SeekRequest> captor = ArgumentCaptor.forClass(LogPilotProto.SeekRequest.class);
        verify(blockingStub).seek(captor.capture());

        LogPilotProto.SeekRequest request = captor.getValue();
        assertEquals("test-channel", request.getChannel());
        assertEquals("test-consumer", request.getConsumerId());
        assertEquals("EARLIEST", request.getOperation());
    }

    @Test
    void testSeekToEnd() {
        LogPilotProto.SeekResponse successResponse = LogPilotProto.SeekResponse.newBuilder()
                .setStatus("success")
                .build();
        when(blockingStub.seek(any(LogPilotProto.SeekRequest.class))).thenReturn(successResponse);

        client.seekToEnd("test-channel", "test-consumer");

        ArgumentCaptor<LogPilotProto.SeekRequest> captor = ArgumentCaptor.forClass(LogPilotProto.SeekRequest.class);
        verify(blockingStub).seek(captor.capture());

        LogPilotProto.SeekRequest request = captor.getValue();
        assertEquals("test-channel", request.getChannel());
        assertEquals("test-consumer", request.getConsumerId());
        assertEquals("LATEST", request.getOperation());
    }

    @Test
    void testSeekToId() {
        LogPilotProto.SeekResponse successResponse = LogPilotProto.SeekResponse.newBuilder()
                .setStatus("success")
                .build();
        when(blockingStub.seek(any(LogPilotProto.SeekRequest.class))).thenReturn(successResponse);

        client.seekToId("test-channel", "test-consumer", 12345L);

        ArgumentCaptor<LogPilotProto.SeekRequest> captor = ArgumentCaptor.forClass(LogPilotProto.SeekRequest.class);
        verify(blockingStub).seek(captor.capture());

        LogPilotProto.SeekRequest request = captor.getValue();
        assertEquals("test-channel", request.getChannel());
        assertEquals("test-consumer", request.getConsumerId());
        assertEquals("SPECIFIC", request.getOperation());
        assertEquals(12345L, request.getLogId());
    }
}
