# LogPilot Demo Application: 채용 사이트 시뮬레이션

이 애플리케이션은 LogPilot의 기능을 실제와 유사한 환경에서 테스트하기 위해, 고트래픽 "채용 사이트(Recruitment Site)"를 시뮬레이션합니다.

## 시뮬레이션 시나리오
사용자 행동(채용 공고 조회, 지원)을 기반으로 두 가지 트래픽 모드를 지원합니다.

1.  **STEADY 모드 (평상시)**:
    - 일반적인 주간 트래픽을 시뮬레이션합니다.
    - 사용자가 공고를 조회(View)하고 가끔 지원합니다.
    - 안정적인 요청 속도를 유지합니다.
2.  **PEAK 모드 (마감 임박)**:
    - "지원 마감 시간" 트래픽을 시뮬레이션합니다.
    - 높은 동시성과 빠른 요청 속도를 발생시킵니다.
    - 쓰기 트래픽(지원서 제출)이 증가하며, 간헐적인 시스템 에러 상황도 시뮬레이션됩니다.

## 사용법

### 1. 애플리케이션 실행
```bash
./gradlew :logpilot-demo:bootRun
```

### 2. 시뮬레이션 제어
제공되는 REST API를 사용하여 트래픽 생성기를 제어할 수 있습니다.

- **Steady 모드 시작**:
  ```bash
  curl -X POST "http://localhost:8080/simulation/start?mode=STEADY"
  ```
- **Peak 모드 시작**:
  ```bash
  curl -X POST "http://localhost:8080/simulation/start?mode=PEAK"
  ```
- **시뮬레이션 중지**:
  ```bash
  curl -X POST http://localhost:8080/simulation/stop
  ```
- **상태 확인**:
  ```bash
  curl http://localhost:8080/simulation/status
  ```

