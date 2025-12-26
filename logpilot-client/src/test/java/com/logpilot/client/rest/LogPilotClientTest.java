package com.logpilot.client.rest;

import com.logpilot.core.model.LogLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ScheduledExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class LogPilotClientTest {

    @Mock
    private HttpClient httpClient;
    @Mock
    private ScheduledExecutorService scheduler;
    @Mock
    private HttpResponse<String> httpResponse;

    private LogPilotRestClient client;
    private final String serverUrl = "http://localhost:8080";

    @BeforeEach
    void setUp() throws IOException, InterruptedException {
        // 성공 응답에 대한 기본 Stubbing (lenient)
        lenient().when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        lenient().when(httpResponse.statusCode()).thenReturn(200);
    }

    @Test
    void testBatchingTriggersFlush() throws IOException, InterruptedException {
        int batchSize = 5;
        client = new LogPilotRestClient(serverUrl, httpClient, scheduler, true, batchSize, 3);

        // 배치 크기 - 1개 전송
        for (int i = 0; i < batchSize - 1; i++) {
            client.log("test-channel", LogLevel.INFO, "msg " + i);
        }

        // 아직 요청이 전송되지 않았는지 확인
        verify(httpClient, never()).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));

        // 정확히 배치 크기만큼 전송
        client.log("test-channel", LogLevel.INFO, "msg trigger");

        // flush()는 내부 ExecutorService를 통해 비동기로 실행되므로,
        // Mockito의 timeout()을 사용하여 비동기 호출을 검증합니다.
        verify(httpClient, timeout(2000).times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));

        // 내용 확인
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testFlushOnClose() throws IOException, InterruptedException {
        client = new LogPilotRestClient(serverUrl, httpClient, scheduler, true, 10, 3);

        client.log("channel", LogLevel.INFO, "msg");

        verify(httpClient, never()).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));

        client.close();

        // Close는 주로 메인 스레드에서 flush를 호출함 (flush() 호출 후 스케줄러 종료)
        // 실제로 flush()는 close()를 호출하는 스레드에서 호출됨.
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testSeekToBeginning() throws IOException, InterruptedException {
        client = new LogPilotRestClient(serverUrl, httpClient, scheduler, false, 10, 3);
        client.seekToBeginning("test-channel", "test-consumer");

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));

        HttpRequest request = captor.getValue();
        assert request.uri().toString().endsWith("/api/logs/seek");
        assert request.method().equals("POST");
    }

    @Test
    void testSeekToEnd() throws IOException, InterruptedException {
        client = new LogPilotRestClient(serverUrl, httpClient, scheduler, false, 10, 3);
        client.seekToEnd("test-channel", "test-consumer");

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));

        HttpRequest request = captor.getValue();
        assert request.uri().toString().endsWith("/api/logs/seek");
    }

    @Test
    void testSeekToId() throws IOException, InterruptedException {
        client = new LogPilotRestClient(serverUrl, httpClient, scheduler, false, 10, 3);
        client.seekToId("test-channel", "test-consumer", 100L);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));

        HttpRequest request = captor.getValue();
        assert request.uri().toString().endsWith("/api/logs/seek");
    }
}
