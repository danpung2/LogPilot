# LogPilot Server

이벤트 스트림을 수신하고 저장하며 관리하는 LogPilot의 중앙 메시지 브로커 컴포넌트입니다.

## 주요 기능
- **멀티 프로토콜**: 이벤트 발행을 위해 고성능 gRPC와 사용이 간편한 REST API를 모두 지원합니다.
- **저장소 (Storage)**: 현재 경량화 및 설정 최소화를 위해 SQLite를 기본 지원합니다.
- **고성능**: 벌크 인서트(Bulk Insert)와 WAL 모드를 적용하여 쓰기 중심의 워크로드에 최적화되어 있습니다.
- **속도 제한 (Rate Limiting)**: 클라이언트의 과도한 로그 전송으로부터 서버를 보호하는 기능이 내장되어 있습니다.

## 사용법

서버를 빌드하고 실행합니다:

```bash
./gradlew :logpilot-server:bootRun
```

`application.yml`을 통해 포트, 저장소 경로 등 세부 설정을 변경할 수 있습니다.

