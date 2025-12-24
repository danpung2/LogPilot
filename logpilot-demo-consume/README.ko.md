# LogPilot Demo Consumer: 이벤트 분석 서비스

이 모듈은 LogPilot에 저장된 **이벤트 스트림을 소비(Consume)하고 처리**하는 방법을 gRPC 클라이언트를 통해 보여줍니다.

## 개요

이 애플리케이션은 **로그 소비자(Log Consumer)** 역할을 합니다. LogPilot Server로부터 주기적으로 로그를 가져와 거의 실시간에 가까운 분석을 수행합니다. `LogPilotClient`(gRPC)를 사용하여 페이징 및 오프셋 추적과 함께 로그를 검색하는 방법을 시연합니다.

## 주요 기능

- **로그 폴링 (Log Polling)**: 스케줄링된 작업을 통해 매초 새로운 로그를 폴링합니다.
- **오프셋 관리 (Offset Management)**: 마지막으로 처리한 로그 ID를 추적하여 중복 처리를 방지하고 순차적인 처리를 보장합니다.
- **인메모리 분석 (In-Memory Analytics)**: 수집된 로그의 메타데이터(`MDC`)를 파싱하여 실시간 통계를 집계합니다.
    - **동작 방식**:
        1. `gRPC` 클라이언트로 로그를 수신합니다.
        2. 로그의 `Level`이 `ERROR`인 경우 에러 카운트를 증가시킵니다.
        3. 로그의 `Meta` 데이터에서 `action` 키를 확인합니다.
            - `action="VIEW_JOB"`: 해당 `jobId`의 조회수(`jobViewCounts`)를 `AtomicLong`으로 증가시킵니다.
            - `action="APPLY_JOB"`: 해당 `jobId`의 지원 횟수(`jobApplicationCounts`)를 증가시킵니다.
    - **제공 지표**:
        - **총 채용 공고 조회 수 (Total Views)**: 모든 Job ID에 대한 조회수 합계.
        - **총 지원 횟수 (Total Applications)**: 모든 Job ID에 대한 지원 횟수 합계.
        - **지원 전환율 (Conversion Rate)**: `(총 지원 횟수 / 총 조회 수) * 100` (%)
        - **에러 발생 수 (Error Count)**: 시스템 내 발생한 에러 로그 총합.
    - **기술적 특징**: `ConcurrentHashMap`과 `AtomicLong`을 사용하여 멀티스레드 환경에서도 안전하게 집계(Thread-safe Aggregation)를 수행합니다.

## 사용법

### 1. 애플리케이션 실행
애플리케이션은 `8083` 포트에서 실행됩니다.

```bash
./gradlew :logpilot-demo-consume:bootRun
```

### 2. 분석 데이터 확인
REST API를 통해 분석된 데이터를 조회할 수 있습니다.

- **현재 통계 조회**:
  ```bash
  curl http://localhost:8083/api/analytics/stats
  ```

응답 예시:
```json
{
  "totalViews": 150,
  "totalApplications": 12,
  "conversionRate": "8.00%",
  "errorCount": 0
}
```
