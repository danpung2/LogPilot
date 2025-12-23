# LogPilot Core

LogPilot의 클라이언트와 서버에서 공통으로 사용하는 도메인 모델, 유효성 검사기, 설정 클래스 등을 포함하는 핵심 모듈입니다.

## 주요 기능
- **설정 관리 (Configuration)**: `LogPilotProperties`를 통해 Type-safe한 설정을 제공합니다.
- **도메인 모델**: `LogEvent`, `LogMessage` 등 시스템 전반에서 사용되는 핵심 엔티티를 정의합니다.
- **유틸리티**: 데이터 검증 및 처리를 위한 공통 유틸리티 클래스를 제공합니다.

## 설치 방법

이 모듈은 다른 LogPilot 모듈의 의존성으로 사용됩니다.

```groovy
implementation project(':logpilot-core')
```

