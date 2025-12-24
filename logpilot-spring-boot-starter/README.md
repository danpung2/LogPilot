# LogPilot Spring Boot Starter

A Spring Boot Starter that auto-configures the LogPilot **Producer and Consumer** clients based on your application properties.

## Features
- **Auto Configuration**: Automatically configures `LogPilotClient` bean if configuration properties are present.
- **Easy Setup**: Just add the dependency and properties; no manual bean definition required.

## Usage

1. Add dependency:
```groovy
implementation project(':logpilot-spring-boot-starter')
```

2. Configure `application.yml`:
```yaml
logpilot:
  client:
    service-name: my-service-app
    pilot-server-address: localhost
    pilot-server-port: 50051
```

Once configured, the `LogPilotClient` bean is available in your application context.

