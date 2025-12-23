# LogPilot Spring Boot Starter

애플리케이션 설정을 기반으로 LogPilot Client를 자동으로 구성해주는 Spring Boot Starter입니다.

## 주요 기능
- **자동 구성 (Auto Configuration)**: 설정 속성이 존재하면 `LogPilotClient` 빈을 자동으로 등록합니다.
- **간편한 설정**: 의존성을 추가하고 프로퍼티만 설정하면 별도의 빈(Bean) 정의 없이 바로 사용할 수 있습니다.

## 사용법

1. 의존성 추가:
```groovy
implementation project(':logpilot-spring-boot-starter')
```

2. `application.yml` 설정:
```yaml
logpilot:
  client:
    service-name: my-service-app
    pilot-server-address: localhost
    pilot-server-port: 50051
```

설정이 완료되면 `LogPilotClient` 빈이 애플리케이션 컨텍스트에 자동으로 등록되어 사용 가능해집니다.

