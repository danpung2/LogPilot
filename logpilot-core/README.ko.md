# LogPilot Core

LogPilot 이벤트 브로커에서 공통으로 사용하는 도메인 모델, 저장소 추상화, 메시징 프로토콜을 포함하는 핵심 모듈입니다.

## 주요 기능
- **저장소 추상화**: 메시지 스트림 영속화를 위한 플러그형 저장소 인터페이스(SQLite, File) 정의.
- **도메인 모델**: 생산자(Producer)와 소비자(Consumer) 간에 사용되는 `LogEntry` 등 핵심 엔티티 정의.
- **오프셋 관리**: 소비자별 처리 진행 상태(Offset) 추적 및 영속화 로직 기능.

## 설치 방법

이 모듈은 다른 LogPilot 모듈의 의존성으로 사용됩니다.

```groovy
implementation project(':logpilot-core')
```

