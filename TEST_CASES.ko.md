# LogPilot 테스트 케이스

## 📊 테스트 실행 결과

```bash
./gradlew :logpilot-core:test

> Task :logpilot-core:compileJava UP-TO-DATE
> Task :logpilot-core:processResources NO-SOURCE
> Task :logpilot-core:classes UP-TO-DATE
> Task :logpilot-core:compileTestJava
> Task :logpilot-core:processTestResources NO-SOURCE
> Task :logpilot-core:testClasses
> Task :logpilot-core:test

BUILD SUCCESSFUL in 2s
```

**✅ Core 모듈: 111개 테스트 모두 통과**
**✅ Server 모듈: 144개 테스트 모두 통과**
**✅ 전체: 255개 테스트 모두 통과**

## 🧪 테스트 파일별 상세 케이스

### 1. LogEntry 모델 테스트 (`LogEntryTest.java`)

#### 🎯 테스트 목적
LogEntry 클래스의 생성자, 빌더 패턴, 필드 접근자, equals/hashCode, toString 메서드 검증

#### 📋 테스트 케이스

| 테스트 메서드 | 설명 | 검증 사항 |
|---------------|------|-----------|
| `constructor_ShouldCreateLogEntryWithRequiredFields` | 기본 생성자로 LogEntry 생성 | 필수 필드 설정 및 타임스탬프 자동 생성 |
| `constructor_WithMeta_ShouldCreateLogEntryWithAllFields` | 메타데이터를 포함한 생성자 | 메타데이터 올바른 저장 |
| `defaultConstructor_ShouldCreateEmptyLogEntryWithTimestamp` | 파라미터 없는 기본 생성자 | 타임스탬프만 설정된 빈 객체 생성 |
| `settersAndGetters_ShouldWorkCorrectly` | Setter/Getter 메서드 동작 | 모든 필드의 설정/조회 |
| `builder_ShouldCreateLogEntryCorrectly` | 빌더 패턴으로 객체 생성 | 모든 필드를 빌더로 설정 |
| `builder_WithoutTimestamp_ShouldUseCurrentTime` | 타임스탬프 없이 빌더 사용 | 현재 시간 자동 설정 |
| `builder_WithoutOptionalFields_ShouldCreateMinimalEntry` | 필수 필드만으로 빌더 사용 | 선택 필드는 null, 필수 필드만 설정 |
| `equals_WithSameValues_ShouldReturnTrue` | 동일한 값을 가진 객체 비교 | equals 메서드 정상 동작 |
| `equals_WithDifferentValues_ShouldReturnFalse` | 다른 값을 가진 객체 비교 | 차이점 정확 감지 |
| `equals_WithNull_ShouldReturnFalse` | null과 비교 | null 안전성 |
| `equals_WithSameReference_ShouldReturnTrue` | 같은 참조 비교 | 자기 자신과의 비교 |
| `equals_WithDifferentClass_ShouldReturnFalse` | 다른 클래스와 비교 | 타입 안전성 |
| `hashCode_WithSameValues_ShouldBeEqual` | 동일한 값의 해시코드 | 일관된 해시코드 생성 |
| `hashCode_WithDifferentValues_ShouldBeDifferent` | 다른 값의 해시코드 | 해시코드 충돌 방지 |
| `toString_ShouldContainAllFields` | toString 메서드 출력 | 모든 필드 정보 포함 |
| `toString_WithNullMeta_ShouldHandleGracefully` | null 메타데이터 toString | null 안전성 |

---

### 2. LogLevel 열거형 테스트 (`LogLevelTest.java`)

#### 🎯 테스트 목적
LogLevel enum의 모든 값, valueOf, ordinal, compareTo 메서드 검증

#### 📋 테스트 케이스

| 테스트 메서드 | 설명 | 검증 사항 |
|---------------|------|-----------|
| `enum_ShouldContainAllExpectedValues` | 모든 enum 값 존재 확인 | DEBUG, INFO, WARN, ERROR 순서 |
| `valueOf_WithValidValues_ShouldReturnCorrectEnum` | 유효한 문자열로 변환 | 정확한 enum 반환 |
| `valueOf_WithInvalidValue_ShouldThrowException` | 잘못된 문자열 처리 | IllegalArgumentException 발생 |
| `valueOf_WithNull_ShouldThrowException` | null 처리 | NullPointerException 발생 |
| `name_ShouldReturnCorrectStrings` | name() 메서드 반환값 | 정확한 문자열 반환 |
| `toString_ShouldReturnCorrectStrings` | toString() 메서드 반환값 | name()과 동일한 결과 |
| `ordinal_ShouldReturnCorrectOrder` | 순서값 확인 | DEBUG=0, INFO=1, WARN=2, ERROR=3 |
| `compareTo_ShouldOrderCorrectly` | 비교 연산 | 올바른 순서 비교 |
| `equals_ShouldWorkCorrectly` | equals 메서드 | 동일성 및 차이점 확인 |
| `hashCode_ShouldBeConsistent` | hashCode 일관성 | 동일 값의 동일 해시코드 |


---

### 3. SQLite 스토리지 테스트 (`SqliteLogStorageTest.java`)

#### 🎯 테스트 목적
SQLite 기반 로그 저장소의 CRUD 연산, 트랜잭션, 컨슈머 오프셋 관리 검증

#### 📋 테스트 케이스

| 테스트 메서드 | 설명 | 검증 사항 |
|---------------|------|-----------|
| `initialize_ShouldCreateDatabase` | 데이터베이스 초기화 | 테이블 생성 및 연결 설정 |
| `store_WithValidLogEntry_ShouldStoreSuccessfully` | 단일 로그 저장 | 기본 저장 기능 |
| `store_WithMetadata_ShouldStoreMetadata` | 메타데이터 포함 저장 | JSON 메타데이터 직렬화/역직렬화 |
| `store_WithNullMetadata_ShouldStoreWithoutMeta` | null 메타데이터 처리 | null 값 안전 저장 |
| `storeLogs_WithValidEntries_ShouldStoreBatch` | 배치 저장 기능 | 트랜잭션 기반 배치 처리 |
| `storeLogs_WithEmptyList_ShouldNotFail` | 빈 리스트 처리 | 예외 없는 빈 데이터 처리 |
| `retrieve_WithNewConsumer_ShouldReturnAllLogs` | 신규 컨슈머 조회 | 모든 로그 반환 |
| `retrieve_WithExistingConsumer_ShouldReturnOnlyNewLogs` | 기존 컨슈머 조회 | 오프셋 기반 새 로그만 반환 |
| `retrieve_WithLimit_ShouldRespectLimit` | 제한 개수 조회 | LIMIT 쿼리 적용 |
| `retrieve_WithDifferentChannels_ShouldFilterByChannel` | 채널별 필터링 | 채널 기반 데이터 분리 |
| `retrieveAll_ShouldReturnAllLogsInDescendingOrder` | 전체 로그 조회 | 최신순 정렬 |
| `retrieveAll_WithLimit_ShouldRespectLimit` | 제한된 전체 조회 | 개수 제한 적용 |
| `retrieveAll_WithNoLogs_ShouldReturnEmptyList` | 빈 데이터베이스 조회 | 빈 리스트 반환 |
| `multipleConsumers_ShouldHaveIndependentOffsets` | 다중 컨슈머 오프셋 관리 | 독립적인 오프셋 유지 |
| `close_ShouldCloseConnection` | 연결 정리 | 리소스 해제 |
| `storageTimestampPersistence_ShouldMaintainTimestamp` | 타임스탬프 정밀도 | 타임스탬프 정확한 저장/복원 |


---

### 4. 파일 스토리지 테스트 (`FileLogStorageTest.java`)

#### 🎯 테스트 목적
파일 기반 로그 저장소의 파일 I/O, 채널별 분리, 컨슈머 오프셋 관리 검증

#### 📋 테스트 케이스

| 테스트 메서드 | 설명 | 검증 사항 |
|---------------|------|-----------|
| `initialize_ShouldCreateStorageDirectory` | 디렉토리 초기화 | 저장 디렉토리 및 오프셋 디렉토리 생성 |
| `store_WithValidLogEntry_ShouldCreateLogFile` | 로그 파일 생성 | JSON 형식 로그 파일 작성 |
| `store_WithSpecialCharactersInChannel_ShouldSanitizeFilename` | 특수문자 파일명 처리 | 안전한 파일명으로 변환 |
| `store_WithMetadata_ShouldStoreMetadataAsJson` | 메타데이터 JSON 저장 | 중첩 객체 포함 JSON 직렬화 |
| `storeLogs_WithMultipleEntries_ShouldStoreBatch` | 배치 파일 저장 | 채널별 파일 분리 저장 |
| `storeLogs_WithEmptyList_ShouldNotFail` | 빈 배치 처리 | 예외 없는 빈 데이터 처리 |
| `retrieve_WithNewConsumer_ShouldReturnAllLogs` | 신규 컨슈머 파일 읽기 | 전체 파일 내용 반환 |
| `retrieve_WithExistingConsumer_ShouldReturnOnlyNewLogs` | 기존 컨슈머 오프셋 | 라인 번호 기반 오프셋 관리 |
| `retrieve_WithLimit_ShouldRespectLimit` | 제한 개수 파일 읽기 | 지정된 개수만큼 반환 |
| `retrieve_WithNonexistentChannel_ShouldReturnEmptyList` | 존재하지 않는 채널 | 빈 리스트 반환 |
| `retrieveAll_ShouldReturnLogsFromAllChannels` | 모든 채널 통합 조회 | 여러 파일 통합 읽기 |
| `retrieveAll_WithLimit_ShouldRespectLimit` | 제한된 통합 조회 | 파일 간 개수 제한 |
| `retrieveAll_WithNoLogs_ShouldReturnEmptyList` | 빈 디렉토리 처리 | 빈 리스트 반환 |
| `multipleConsumers_ShouldHaveIndependentOffsets` | 다중 컨슈머 오프셋 | 독립적인 오프셋 파일 관리 |
| `consumerOffsetPersistence_ShouldSurviveRestart` | 오프셋 영속성 | 재시작 후 오프셋 복원 |
| `timestampPersistence_ShouldMaintainTimestamp` | 타임스탬프 정밀도 | ISO 형식 타임스탬프 보존 |
| `concurrentAccess_ShouldHandleMultipleOperations` | 동시 접근 처리 | 락 기반 안전한 동시 처리 |
| `malformedLogLine_ShouldBeSkippedGracefully` | 손상된 로그 처리 | 잘못된 JSON 라인 건너뛰기 |
| `close_ShouldSaveConsumerOffsets` | 종료 시 오프셋 저장 | 모든 오프셋 파일 저장 |


---

### 5. LogService 서비스 테스트 (`LogServiceTest.java`)

#### 🎯 테스트 목적
LogService 인터페이스의 비즈니스 로직, 유효성 검사, Mock 객체를 활용한 동작 검증

#### 📋 테스트 케이스

| 테스트 메서드 | 설명 | 검증 사항 |
|---------------|------|-----------|
| `storeLog_WithValidLogEntry_ShouldCallStorage` | 유효한 로그 저장 | Storage 호출 확인 |
| `storeLog_WithNullLogEntry_ShouldThrowException` | null 로그 입력 | IllegalArgumentException 발생 |
| `storeLogs_WithValidEntries_ShouldCallStorage` | 유효한 배치 저장 | Storage 배치 호출 확인 |
| `storeLogs_WithEmptyList_ShouldCallStorageWithEmptyList` | 빈 리스트 저장 | 빈 리스트도 Storage 전달 |
| `storeLogs_WithNullList_ShouldThrowException` | null 배치 입력 | IllegalArgumentException 발생 |
| `getLogsForConsumer_WithValidParameters_ShouldCallStorage` | 유효한 컨슈머 조회 | Storage 조회 호출 및 결과 반환 |
| `getLogsForConsumer_WithNullChannel_ShouldThrowException` | null 채널 입력 | IllegalArgumentException 발생 |
| `getLogsForConsumer_WithEmptyChannel_ShouldThrowException` | 빈 채널 입력 | IllegalArgumentException 발생 |
| `getLogsForConsumer_WithNullConsumerId_ShouldThrowException` | null 컨슈머 ID | IllegalArgumentException 발생 |
| `getLogsForConsumer_WithEmptyConsumerId_ShouldThrowException` | 빈 컨슈머 ID | IllegalArgumentException 발생 |
| `getLogsForConsumer_WithNegativeLimit_ShouldThrowException` | 음수 제한값 | IllegalArgumentException 발생 |
| `getLogsForConsumer_WithZeroLimit_ShouldCallStorage` | 0 제한값 처리 | 0도 유효한 값으로 처리 |
| `getAllLogs_WithValidLimit_ShouldCallStorage` | 유효한 전체 조회 | Storage 전체 조회 호출 |
| `getAllLogs_WithNegativeLimit_ShouldThrowException` | 음수 제한값 | IllegalArgumentException 발생 |
| `getAllLogs_WithZeroLimit_ShouldCallStorage` | 0 제한값 처리 | 0도 유효한 값으로 처리 |
| `getAllLogs_WithLargeLimit_ShouldCallStorage` | 큰 제한값 처리 | Integer.MAX_VALUE 처리 |


---

### 6. LogPilotProperties 설정 테스트 (`LogPilotPropertiesTest.java`)

#### 🎯 테스트 목적
애플리케이션 설정 클래스의 기본값, Setter/Getter, 중첩 클래스 동작 검증

#### 📋 테스트 케이스

| 테스트 메서드 | 설명 | 검증 사항 |
|---------------|------|-----------|
| `defaultValues_ShouldBeSet` | 기본 설정값 확인 | 모든 기본값 올바른 설정 |
| `setStorage_ShouldUpdateStorageConfiguration` | 스토리지 설정 변경 | Storage 객체 교체 |
| `setServer_ShouldUpdateServerConfiguration` | 서버 설정 변경 | Server 객체 교체 |
| `setGrpc_ShouldUpdateGrpcConfiguration` | gRPC 설정 변경 | Grpc 객체 교체 |
| `storageClass_ShouldSupportAllOperations` | Storage 클래스 기능 | 모든 필드 설정/조회 |
| `sqliteClass_ShouldSupportPathConfiguration` | SQLite 설정 기능 | 데이터베이스 경로 설정 |
| `serverClass_ShouldSupportPortConfiguration` | Server 설정 기능 | 포트 번호 설정 |
| `grpcClass_ShouldSupportPortConfiguration` | gRPC 설정 기능 | gRPC 포트 설정 |
| `storageType_ShouldContainAllExpectedValues` | StorageType enum 값 | FILE, SQLITE 존재 |
| `storageType_ValueOf_ShouldWorkCorrectly` | StorageType 문자열 변환 | 올바른 enum 변환 |
| `storageType_ValueOf_WithInvalidValue_ShouldThrowException` | 잘못된 StorageType | 예외 발생 확인 |
| `nestedClasses_ShouldHaveIndependentInstances` | 중첩 클래스 독립성 | 인스턴스 간 독립성 |
| `chainedConfiguration_ShouldWork` | 연쇄 설정 기능 | 복합 설정 체인 |
| `nullSafety_ShouldHandleNullAssignments` | null 안전성 | null 할당 처리 |


---

### 7. LogStorageFactory 팩토리 테스트 (`LogStorageFactoryTest.java`)

#### 🎯 테스트 목적
팩토리 패턴을 통한 스토리지 인스턴스 생성, 디렉토리 생성, 초기화 과정 검증

#### 📋 테스트 케이스

| 테스트 메서드 | 설명 | 검증 사항 |
|---------------|------|-----------|
| `createLogStorage_WithNullProperties_ShouldThrowException` | null 설정 입력 | IllegalArgumentException 발생 |
| `createLogStorage_WithSqliteType_ShouldReturnSqliteStorage` | SQLite 스토리지 생성 | SqliteLogStorage 인스턴스 반환 |
| `createLogStorage_WithFileType_ShouldReturnFileStorage` | 파일 스토리지 생성 | FileLogStorage 인스턴스 반환 |
| `createLogStorage_WithSqliteType_ShouldCreateParentDirectories` | SQLite 부모 디렉토리 생성 | 중첩 디렉토리 자동 생성 |
| `createLogStorage_WithFileType_ShouldCreateDirectory` | 파일 스토리지 디렉토리 생성 | 저장 디렉토리 자동 생성 |
| `createLogStorage_ShouldInitializeStorage` | 스토리지 초기화 | initialize() 메서드 호출 확인 |
| `createLogStorage_WithExistingSqliteParentDirectory_ShouldNotFail` | 기존 SQLite 디렉토리 처리 | 기존 디렉토리 무시하고 정상 처리 |
| `createLogStorage_WithExistingFileDirectory_ShouldNotFail` | 기존 파일 디렉토리 처리 | 기존 디렉토리 무시하고 정상 처리 |
| `createLogStorage_WithDifferentConfigurations_ShouldRespectSettings` | 다양한 설정 처리 | 각기 다른 설정으로 독립 인스턴스 생성 |
| `createLogStorage_SqliteWithNullPath_ShouldUseDefaultPath` | SQLite 기본 경로 사용 | 기본 설정값으로 생성 |
| `createLogStorage_FileWithNullDirectory_ShouldUseDefaultDirectory` | 파일 기본 디렉토리 사용 | 기본 설정값으로 생성 |
| `createLogStorage_SqliteWithFileInRootDirectory_ShouldWork` | 루트 디렉토리 SQLite 파일 | 단순 경로 처리 |


---

### 8. 통합 테스트 (`LogPilotCoreIntegrationTest.java`)

#### 🎯 테스트 목적
전체 시스템의 end-to-end 워크플로우, 대용량 데이터 처리, 복잡한 시나리오 검증

#### 📋 테스트 케이스

| 테스트 메서드 | 설명 | 검증 사항 |
|---------------|------|-----------|
| `endToEndWorkflow_WithSqliteStorage_ShouldWorkCorrectly` | SQLite 전체 워크플로우 | 완전한 CRUD 사이클 |
| `endToEndWorkflow_WithFileStorage_ShouldWorkCorrectly` | 파일 전체 워크플로우 | 완전한 CRUD 사이클 |
| `multipleChannels_WithMultipleConsumers_ShouldWorkIndependently` | 다중 채널/컨슈머 시나리오 | 독립적인 채널 및 컨슈머 관리 |
| `largeBatchProcessing_ShouldHandleCorrectly` | 대용량 배치 처리 | 1000개 로그 효율적 처리 (5초 이내) |
| `metadataHandling_ShouldPreserveComplexData` | 복잡한 메타데이터 처리 | 중첩 객체, 배열, 다양한 타입 보존 |
| `timestampPrecision_ShouldBePreserved` | 타임스탬프 정밀도 | 초 단위 정밀도 보존 |
| `storageTypeSwitch_ShouldWorkWithSameData` | 스토리지 타입 간 호환성 | SQLite-File 간 동일한 데이터 처리 |
| `edgeCases_ShouldBeHandledGracefully` | 엣지 케이스 처리 | 빈 메시지, 긴 메시지, 특수문자, 특수 채널명 |

#### 🔍 상세 검증 항목

**대용량 처리 성능:**
- 1,000개 로그 엔트리 배치 처리
- 5초 이내 처리 완료
- 페이지네이션 (100개씩 조회)

**복잡한 메타데이터:**
```json
{
  "userId": 12345,
  "userName": "john.doe@example.com",
  "timestamp": 1727251234567,
  "tags": ["urgent", "customer-service", "billing"],
  "request": {
    "requestId": "req-abc-123",
    "sessionId": "sess-xyz-789",
    "ipAddress": "192.168.1.100"
  }
}
```

**엣지 케이스:**
- 빈 메시지 (`""`)
- 매우 긴 메시지 (10,000자 `"A"` 반복)
- 특수문자 (`"äöü 中文 🚀 \n\t\r"`)
- 특수문자 채널명 (`"special/channel:name*with<chars>"`)


---

# LogPilot Server 모듈 테스트 케이스

## 📊 Server 모듈 테스트 실행 결과

```bash
./gradlew :logpilot-server:test

BUILD SUCCESSFUL
144/144 tests passed (100%)
Execution time: ~30 seconds
```

## 🧪 Server 모듈 테스트 파일별 상세 케이스

### 1. 애플리케이션 테스트 (`LogPilotServerApplicationTest.java`)

#### 🎯 테스트 목적
Spring Boot 애플리케이션의 시작, 컨텍스트 로딩, 메인 메서드 검증

#### 📋 테스트 케이스

| 테스트 메서드 | 설명 | 검증 사항 |
|---------------|------|-----------|
| `contextLoads` | Spring 애플리케이션 컨텍스트 로딩 | 정상적인 컨텍스트 초기화 |
| `mainMethod_WithArgs_ShouldNotThrow` | 메인 메서드 실행 (인수 포함) | 예외 없는 정상 실행 |
| `mainMethod_WithNullArgs_ShouldNotThrow` | 메인 메서드 실행 (null 인수) | null 안전성 |
| `applicationClass_ShouldHaveCorrectAnnotations` | 애플리케이션 클래스 어노테이션 | @SpringBootApplication 등 필수 어노테이션 |

---

### 2. 서버 설정 테스트 (`ServerConfigTest.java`)

#### 🎯 테스트 목적
Spring Bean 설정, LogStorage 팩토리 동작, 의존성 주입 검증

#### 📋 테스트 케이스

| 테스트 메서드 | 설명 | 검증 사항 |
|---------------|------|-----------|
| `serverConfig_ShouldHaveCorrectAnnotations` | 설정 클래스 어노테이션 | @Configuration, @EnableConfigurationProperties |
| `logStorage_WithSqliteProperties_ShouldReturnSqliteLogStorage` | SQLite 스토리지 빈 생성 | 적절한 타입의 인스턴스 반환 |
| `logStorage_WithFileProperties_ShouldReturnFileLogStorage` | 파일 스토리지 빈 생성 | 적절한 타입의 인스턴스 반환 |
| `logStorage_WithNullProperties_ShouldThrowException` | null 설정 처리 | IllegalArgumentException 발생 |
| `logStorage_WithDefaultProperties_ShouldReturnValidStorage` | 기본 설정값 처리 | 유효한 스토리지 인스턴스 생성 |
| `logStorageBean_ShouldHaveCorrectAnnotations` | 빈 메서드 어노테이션 | @Bean 어노테이션 확인 |
| `logStorage_ShouldCreateFunctionalStorage` | 생성된 스토리지 기능성 | 실제 동작 가능한 인스턴스 |
| `logStorage_WithDifferentConfigurations_ShouldCreateDifferentInstances` | 다른 설정별 인스턴스 | 설정에 따른 다른 타입 생성 |
| `logStorage_ShouldCreateNewInstanceEachTime` | 인스턴스 생성 정책 | 호출마다 새 인스턴스 생성 |

---

### 3. 조건부 어노테이션 테스트 (`ConditionalAnnotationsTest.java`)

#### 🎯 테스트 목적
프로토콜별 조건부 빈 활성화 어노테이션 검증

#### 📋 테스트 케이스

| 테스트 메서드 | 설명 | 검증 사항 |
|---------------|------|-----------|
| `conditionalOnGrpcProtocol_ShouldHaveCorrectAnnotations` | gRPC 조건부 어노테이션 | @ConditionalOnProperty 설정 |
| `conditionalOnRestProtocol_ShouldHaveCorrectAnnotations` | REST 조건부 어노테이션 | @ConditionalOnProperty 설정 |
| `conditionalAnnotations_ShouldHaveDifferentHavingValues` | 어노테이션별 차이점 | 서로 다른 havingValue 설정 |
| `conditionalAnnotations_ShouldTargetSameElements` | 적용 대상 일관성 | ElementType.TYPE, METHOD 공통 |
| `conditionalAnnotations_ShouldHaveRuntimeRetention` | 런타임 유지 정책 | RetentionPolicy.RUNTIME |
| `conditionalAnnotations_ShouldBeMetaAnnotatedWithConditionalOnProperty` | 메타 어노테이션 | @ConditionalOnProperty 상속 |
| `conditionalAnnotations_ShouldBeApplicableToClasses` | 클래스 적용 가능성 | TYPE 타겟 포함 |
| `conditionalAnnotations_ShouldBeApplicableToMethods` | 메서드 적용 가능성 | METHOD 타겟 포함 |

---

### 4. REST 컨트롤러 테스트 (`LogControllerTest.java`)

#### 🎯 테스트 목적
REST API 엔드포인트, HTTP 요청/응답 처리, 에러 핸들링 검증

#### 📋 테스트 케이스

| 테스트 메서드 | 설명 | 검증 사항 |
|---------------|------|-----------|
| `logController_ShouldHaveCorrectAnnotations` | 컨트롤러 어노테이션 | @RestController, @RequestMapping |
| `storeLog_WithValidLogEntry_ShouldReturnCreated` | 단일 로그 저장 API | 201 Created 응답 |
| `storeLog_WithInvalidJson_ShouldReturnBadRequest` | 잘못된 JSON 처리 | 400 Bad Request 응답 |
| `storeLog_WithEmptyBody_ShouldReturnBadRequest` | 빈 요청 본문 처리 | 400 Bad Request 응답 |
| `storeLogs_WithValidLogEntries_ShouldReturnCreated` | 배치 로그 저장 API | 201 Created 응답 |
| `storeLogs_WithEmptyList_ShouldReturnCreated` | 빈 배치 처리 | 201 Created 응답 |
| `getLogs_WithChannelAndConsumerId_ShouldReturnLogs` | 채널별 로그 조회 | 200 OK 및 로그 데이터 |
| `getLogs_WithChannelOnly_ShouldReturnAllLogs` | 채널 전체 로그 조회 | 모든 로그 반환 |
| `getLogs_WithDefaultLimit_ShouldUseDefaultValue` | 기본 제한값 사용 | 기본값 적용 |
| `getLogs_WithCustomLimit_ShouldUseCustomValue` | 사용자 정의 제한값 | 커스텀 값 적용 |
| `getAllLogs_ShouldReturnAllLogs` | 전체 로그 조회 API | 모든 채널 로그 반환 |
| `getAllLogs_WithCustomLimit_ShouldUseCustomValue` | 전체 로그 제한 조회 | 제한값 적용 |
| `getAllLogs_WithDefaultLimit_ShouldUseDefaultValue` | 전체 로그 기본 제한 | 기본값 적용 |
| `storeLog_WhenServiceThrowsException_ShouldReturnInternalServerError` | 서비스 예외 처리 | 500 Internal Server Error |
| `storeLogs_WhenServiceThrowsException_ShouldReturnInternalServerError` | 배치 서비스 예외 처리 | 500 Internal Server Error |
| `getLogs_WhenServiceThrowsException_ShouldReturnInternalServerError` | 조회 서비스 예외 처리 | 500 Internal Server Error |
| `storeLog_WithComplexLogEntry_ShouldHandleCorrectly` | 복잡한 로그 엔트리 | 메타데이터 포함 처리 |
| `getLogs_WithSpecialCharactersInChannel_ShouldHandleCorrectly` | 특수문자 채널명 | 인코딩 처리 |
| `getLogs_WithLargeLimit_ShouldAcceptValue` | 큰 제한값 처리 | 큰 숫자 허용 |
| `getLogs_WithZeroLimit_ShouldAcceptValue` | 0 제한값 처리 | 0값 허용 |
| `storeLog_WithMissingRequiredFields_ShouldReturnBadRequest` | 필수 필드 누락 | 400 Bad Request |

---

### 5. REST 서비스 테스트 (`RestLogServiceTest.java`)

#### 🎯 테스트 목적
REST 로그 서비스 비즈니스 로직, 의존성 주입, 예외 처리 검증

#### 📋 테스트 케이스

| 테스트 메서드 | 설명 | 검증 사항 |
|---------------|------|-----------|
| `restLogService_ShouldHaveCorrectAnnotations` | 서비스 어노테이션 | @Service, @ConditionalOnRestProtocol |
| `constructor_WithLogStorage_ShouldCreateInstance` | 생성자 의존성 주입 | 정상 인스턴스 생성 |
| `constructor_WithNullLogStorage_ShouldThrowException` | null 의존성 처리 | IllegalArgumentException |
| `storeLog_WithValidLogEntry_ShouldCallLogStorage` | 로그 저장 위임 | Storage 메서드 호출 확인 |
| `storeLog_WithNullLogEntry_ShouldCallLogStorageWithNull` | null 로그 위임 | null 값 그대로 전달 |
| `storeLog_WhenStorageThrowsException_ShouldPropagateException` | 저장 예외 전파 | 예외 그대로 전달 |
| `storeLogs_WithValidLogEntries_ShouldCallLogStorage` | 배치 저장 위임 | 배치 Storage 호출 |
| `storeLogs_WithEmptyList_ShouldCallLogStorage` | 빈 배치 위임 | 빈 리스트 전달 |
| `storeLogs_WithNullList_ShouldCallLogStorageWithNull` | null 배치 위임 | null 값 전달 |
| `storeLogs_WhenStorageThrowsException_ShouldPropagateException` | 배치 저장 예외 전파 | 예외 그대로 전달 |
| `getLogsForConsumer_WithValidParameters_ShouldCallLogStorage` | 컨슈머 조회 위임 | Storage 조회 호출 |
| `getLogsForConsumer_WithNullChannel_ShouldCallLogStorage` | null 채널 조회 | null 값 전달 |
| `getLogsForConsumer_WithNullConsumerId_ShouldCallLogStorage` | null 컨슈머 조회 | null 값 전달 |
| `getLogsForConsumer_WithZeroLimit_ShouldCallLogStorage` | 0 제한 조회 | 0 값 전달 |
| `getLogsForConsumer_WhenStorageThrowsException_ShouldPropagateException` | 조회 예외 전파 | 예외 그대로 전달 |
| `getAllLogs_WithValidLimit_ShouldCallLogStorage` | 전체 조회 위임 | Storage 전체 조회 |
| `getAllLogs_WithZeroLimit_ShouldCallLogStorage` | 0 제한 전체 조회 | 0 값 전달 |
| `getAllLogs_WithNegativeLimit_ShouldCallLogStorage` | 음수 제한 조회 | 음수 값 전달 |
| `getAllLogs_WhenStorageThrowsException_ShouldPropagateException` | 전체 조회 예외 전파 | 예외 그대로 전달 |
| `getAllLogs_WithLargeLimit_ShouldCallLogStorage` | 큰 제한값 조회 | 큰 숫자 처리 |
| `service_ShouldDelegateAllCallsToLogStorage` | 모든 호출 위임 | 완전한 위임 패턴 |
| `service_ShouldHandleMultipleConsecutiveCalls` | 연속 호출 처리 | 상태 유지 없는 처리 |
| `service_ShouldMaintainLogStorageReference` | Storage 참조 유지 | 의존성 참조 보존 |
| `service_ShouldHandleComplexLogEntries` | 복잡한 로그 처리 | 메타데이터 포함 로그 |

---

### 6. gRPC 핸들러 테스트 (`GrpcLogHandlerTest.java`)

#### 🎯 테스트 목적
gRPC 로그 핸들러 서비스 계층, 의존성 주입, 비즈니스 로직 검증

#### 📋 테스트 케이스

| 테스트 메서드 | 설명 | 검증 사항 |
|---------------|------|-----------|
| `grpcLogHandler_ShouldHaveCorrectAnnotations` | 핸들러 어노테이션 | @Service, @ConditionalOnGrpcProtocol |
| `constructor_WithLogStorage_ShouldCreateInstance` | 생성자 의존성 주입 | 정상 인스턴스 생성 |
| `constructor_WithNullLogStorage_ShouldThrowException` | null 의존성 처리 | IllegalArgumentException |
| `storeLog_WithValidLogEntry_ShouldCallLogStorage` | 로그 저장 위임 | Storage 메서드 호출 |
| `storeLog_WithNullLogEntry_ShouldCallLogStorageWithNull` | null 로그 위임 | null 값 전달 |
| `storeLog_WhenStorageThrowsException_ShouldPropagateException` | 저장 예외 전파 | 예외 그대로 전달 |
| `storeLogs_WithValidLogEntries_ShouldCallLogStorage` | 배치 저장 위임 | 배치 Storage 호출 |
| `storeLogs_WithEmptyList_ShouldCallLogStorage` | 빈 배치 위임 | 빈 리스트 전달 |
| `storeLogs_WithNullList_ShouldCallLogStorageWithNull` | null 배치 위임 | null 값 전달 |
| `storeLogs_WhenStorageThrowsException_ShouldPropagateException` | 배치 저장 예외 전파 | 예외 그대로 전달 |
| `getLogsForConsumer_WithValidParameters_ShouldCallLogStorage` | 컨슈머 조회 위임 | Storage 조회 호출 |
| `getLogsForConsumer_WithNullChannel_ShouldCallLogStorage` | null 채널 조회 | null 값 전달 |
| `getLogsForConsumer_WithNullConsumerId_ShouldCallLogStorage` | null 컨슈머 조회 | null 값 전달 |
| `getLogsForConsumer_WithZeroLimit_ShouldCallLogStorage` | 0 제한 조회 | 0 값 전달 |
| `getLogsForConsumer_WhenStorageThrowsException_ShouldPropagateException` | 조회 예외 전파 | 예외 그대로 전달 |
| `getAllLogs_WithValidLimit_ShouldCallLogStorage` | 전체 조회 위임 | Storage 전체 조회 |
| `getAllLogs_WithZeroLimit_ShouldCallLogStorage` | 0 제한 전체 조회 | 0 값 전달 |
| `getAllLogs_WithNegativeLimit_ShouldCallLogStorage` | 음수 제한 조회 | 음수 값 전달 |
| `getAllLogs_WhenStorageThrowsException_ShouldPropagateException` | 전체 조회 예외 전파 | 예외 그대로 전달 |
| `getAllLogs_WithLargeLimit_ShouldCallLogStorage` | 큰 제한값 조회 | 큰 숫자 처리 |
| `handler_ShouldDelegateAllCallsToLogStorage` | 모든 호출 위임 | 완전한 위임 패턴 |
| `handler_ShouldHandleMultipleConsecutiveCalls` | 연속 호출 처리 | 상태 유지 없는 처리 |
| `handler_ShouldMaintainLogStorageReference` | Storage 참조 유지 | 의존성 참조 보존 |
| `handler_ShouldHandleComplexLogEntries` | 복잡한 로그 처리 | 메타데이터 포함 로그 |
| `handler_ShouldImplementLogServiceInterface` | 인터페이스 구현 | LogService 인터페이스 구현 |
| `handler_ShouldHaveCorrectBeanName` | 빈 이름 확인 | 적절한 Spring 빈 이름 |
| `handler_ShouldBeConditionalOnGrpcProtocol` | 조건부 활성화 | gRPC 프로토콜 조건 확인 |

---

### 7. gRPC 서비스 테스트 (`LogPilotGrpcServiceTest.java`)

#### 🎯 테스트 목적
gRPC 프로토콜 처리, 메시지 변환, StreamObserver 패턴 검증

#### 📋 테스트 케이스

| 테스트 메서드 | 설명 | 검증 사항 |
|---------------|------|-----------|
| `grpcService_ShouldHaveCorrectAnnotations` | gRPC 서비스 어노테이션 | @Service, @ConditionalOnGrpcProtocol |
| `sendLog_WithValidRequest_ShouldReturnSuccessResponse` | 단일 로그 gRPC 전송 | SUCCESS 응답 반환 |
| `sendLog_WithMetadata_ShouldConvertCorrectly` | 메타데이터 포함 전송 | 메타데이터 변환 처리 |
| `sendLog_WithEmptyMessage_ShouldHandleGracefully` | 빈 메시지 처리 | 빈 값 안전 처리 |
| `sendLog_WhenServiceThrowsException_ShouldReturnErrorResponse` | 전송 예외 처리 | FAILED 응답 반환 |
| `sendLogs_WithValidRequests_ShouldReturnSuccessResponse` | 배치 로그 gRPC 전송 | 배치 SUCCESS 응답 |
| `sendLogs_WithEmptyList_ShouldReturnSuccessResponse` | 빈 배치 전송 | 빈 리스트 SUCCESS 처리 |
| `sendLogs_WhenServiceThrowsException_ShouldReturnErrorResponse` | 배치 예외 처리 | FAILED 응답 반환 |
| `listLogs_ShouldReturnProtoLogEntries` | 로그 목록 스트리밍 | StreamObserver 응답 |
| `listLogs_WithNoLogs_ShouldReturnEmptyResponse` | 빈 로그 목록 | 빈 응답 스트리밍 |
| `listLogs_WhenServiceThrowsException_ShouldCallOnError` | 목록 조회 예외 | onError 호출 |
| `fetchLogs_WithChannel_ShouldCallGetLogsForConsumer` | 채널 기반 조회 | 특정 채널 조회 |
| `fetchLogs_WithoutChannel_ShouldCallGetAllLogs` | 전체 로그 조회 | 채널 없는 전체 조회 |
| `fetchLogs_WithEmptyChannel_ShouldCallGetAllLogs` | 빈 채널 조회 | 빈 채널을 전체 조회로 처리 |
| `fetchLogs_WhenServiceThrowsException_ShouldCallOnError` | 조회 예외 처리 | onError 호출 |
| `convertLogRequestToLogEntry_ShouldMapAllFields` | gRPC 요청 변환 | 모든 필드 매핑 |
| `convertLogRequestToLogEntry_WithMetadata_ShouldConvertMap` | 메타데이터 맵 변환 | gRPC Map → Java Map |
| `convertToProtoLogEntry_ShouldMapAllFields` | Java → Proto 변환 | 모든 필드 역변환 |
| `convertStringToLogLevel_WithValidLevel_ShouldConvert` | 로그 레벨 문자열 변환 | 유효한 레벨 변환 |
| `convertStringToLogLevel_WithInvalidLevel_ShouldDefaultToInfo` | 잘못된 로그 레벨 | 기본값 INFO 사용 |

---

### 8. 통합 테스트 (`LogPilotServerIntegrationTest.java`)

#### 🎯 테스트 목적
전체 서버 모듈의 end-to-end 워크플로우, 다중 프로토콜, 동시성 검증

#### 📋 테스트 케이스

| 테스트 메서드 | 설명 | 검증 사항 |
|---------------|------|-----------|
| `contextLoads_WithAllProfiles_ShouldStartSuccessfully` | 전체 프로필 컨텍스트 | 모든 컴포넌트 정상 로딩 |
| `restEndpoints_ShouldWorkEndToEnd_WithFileStorage` | REST API 전체 워크플로우 | 저장→조회 완전한 사이클 |
| `restAndGrpc_ShouldWorkTogether_SameStorage` | REST-gRPC 통합 동작 | 동일 스토리지 공유 |
| `multipleClients_ShouldAccessConcurrently` | 다중 클라이언트 동시 접근 | 동시성 안전성 |
| `largeBatchRequests_ShouldProcessCorrectly` | 대용량 배치 처리 | 500개 로그 처리 성능 |
| `invalidRequests_ShouldReturnAppropriateErrors` | 잘못된 요청 처리 | 적절한 에러 응답 |
| `crossProtocolDataConsistency_ShouldMaintain` | 프로토콜 간 데이터 일관성 | REST-gRPC 데이터 동일성 |
| `applicationShutdown_ShouldCloseResourcesProperly` | 애플리케이션 종료 처리 | 리소스 정리 |
| `storagePerformance_ShouldMeetBasicRequirements` | 스토리지 성능 요구사항 | 기본 성능 기준 충족 |
| `errorHandling_ShouldBeConsistent` | 일관된 에러 처리 | 모든 계층 동일한 에러 처리 |
| `healthCheck_ShouldIndicateSystemStatus` | 헬스체크 기능 | 시스템 상태 정확 반영 |
| `dataIntegrity_ShouldBePreserved` | 데이터 무결성 | 저장/조회 데이터 일치 |
| `concurrentReadWrite_ShouldMaintainConsistency` | 동시 읽기/쓰기 일관성 | 동시 작업 시 데이터 정합성 |

---

### 9. 서버 설정 테스트 (`ServerConfigurationTest.java`)

#### 🎯 테스트 목적
다양한 환경 설정, 프로토콜별 빈 활성화, 스토리지 타입별 설정 검증

#### 📋 테스트 케이스

| 테스트 메서드 | 설명 | 검증 사항 |
|---------------|------|-----------|
| `RestOnlyConfigurationTest` | REST 전용 설정 | REST 빈만 활성화, gRPC 빈 비활성화 |
| `GrpcOnlyConfigurationTest` | gRPC 전용 설정 | gRPC 빈만 활성화, REST 빈 비활성화 |
| `AllProtocolsConfigurationTest` | 모든 프로토콜 설정 | 모든 빈 활성화 |
| `FileStorageConfigurationTest` | 파일 스토리지 설정 | FileLogStorage 빈 생성 |
| `SqliteStorageConfigurationTest` | SQLite 스토리지 설정 | SqliteLogStorage 빈 생성 |
| `CustomPortsConfigurationTest` | 커스텀 포트 설정 | 사용자 정의 포트 바인딩 |
| `DefaultPropertiesConfigurationTest` | 기본 설정값 | 기본값으로 정상 동작 |
| `LoggingLevelConfigurationTest` | 로깅 레벨 설정 | 커스텀 로깅 설정 적용 |
| `ActuatorConfigurationTest` | Actuator 설정 | 모니터링 엔드포인트 활성화 |
| `ProfileSwitchingConfigurationTest` | 프로필 전환 설정 | 프로필별 다른 설정 적용 |
| `ResourceCleanupConfigurationTest` | 리소스 정리 설정 | 적절한 리소스 관리 |

---

### 10. 성능 테스트 (`PerformanceTest.java`)

#### 🎯 테스트 목적
처리량, 응답시간, 메모리 사용량, 동시성 성능 검증

#### 📋 테스트 케이스

| 테스트 메서드 | 설명 | 검증 사항 |
|---------------|------|-----------|
| `restApi_ShouldHandleHighThroughput` | REST API 고처리량 | 100개 요청, 초당 10개 이상 처리 |
| `concurrentClients_ShouldMaintainPerformance` | 동시 클라이언트 성능 | 10개 클라이언트 동시 처리 |
| `largeBatches_ShouldProcessWithinTimeout` | 대용량 배치 성능 | 500개 배치 10초 이내 처리 |
| `memoryUsage_ShouldStayWithinLimits` | 메모리 사용량 제한 | 50MB 이내 메모리 증가 |
| `storagePerformance_ShouldMeetRequirements` | 스토리지 성능 | 쓰기 500ms, 읽기 200ms 이내 |
| `responseTime_ShouldMeetSLA` | 응답시간 SLA | 평균 1초, 95% 2초, 최대 5초 이내 |
| `errorRate_ShouldStayBelowThreshold` | 에러율 임계값 | 1% 미만 에러율 유지 |

#### 🔍 성능 기준

**처리량 (Throughput):**
- REST API: 초당 10개 이상 요청 처리
- 동시 클라이언트: 10개 클라이언트, 초당 15개 이상 요청
- 대용량 배치: 500개 로그 10초 이내 처리

**응답시간 (Response Time):**
- 평균 응답시간: 1초 이내
- 95th 백분위수: 2초 이내
- 최대 응답시간: 5초 이내

**리소스 사용량:**
- 메모리 증가: 50MB 이내
- 에러율: 1% 미만

---

## 🔧 Server 모듈 테스트 실행 방법

### 전체 테스트 실행
```bash
./gradlew :logpilot-server:test
```

### 카테고리별 테스트 실행
```bash
# 통합 테스트만
./gradlew :logpilot-server:test --tests "*IntegrationTest"

# 성능 테스트만
./gradlew :logpilot-server:test --tests "*PerformanceTest"

# 설정 테스트만
./gradlew :logpilot-server:test --tests "*ConfigurationTest"
```

### 특정 테스트 클래스 실행
```bash
./gradlew :logpilot-server:test --tests "com.logpilot.server.rest.LogControllerTest"
```

### 테스트 결과 보고서 확인
```bash
open logpilot-server/build/reports/tests/test/index.html
```

---

## 🔧 Core 모듈 테스트 실행 방법

### 전체 테스트 실행
```bash
./gradlew :logpilot-core:test
```

### 특정 테스트 클래스 실행
```bash
./gradlew :logpilot-core:test --tests "com.logpilot.core.model.LogEntryTest"
```

### 특정 테스트 메서드 실행
```bash
./gradlew :logpilot-core:test --tests "com.logpilot.core.model.LogEntryTest.constructor_ShouldCreateLogEntryWithRequiredFields"
```

### 테스트 결과 보고서 확인
```bash
open logpilot-core/build/reports/tests/test/index.html
```

---

# LogPilot 클라이언트 모듈 테스트 케이스

## 🧪 클라이언트 모듈 파일별 상세 테스트 케이스

### 1. 클라이언트 SDK 테스트 (`LogPilotClientTest.java`)

#### 🎯 테스트 목적
`LogPilotClient`의 기능, 동기 로깅, 비동기 배치 및 우아한 종료(Graceful Shutdown) 기능 검증.

#### 📋 테스트 케이스

| 테스트 메서드 | 설명 | 검증 사항 |
|---------------|------|-----------|
| `testBatchingTriggersFlush` | 비동기 배치 동작 | 로그 큐잉 및 배치 크기 도달 시 자동 전송 |
| `testFlushOnClose` | 우아한 종료 | 클라이언트 종료 시 대기 중인 로그 전송 |

## 🔧 클라이언트 모듈 테스트 실행 방법

### 전체 테스트 실행
```bash
./gradlew :logpilot-client:test
```