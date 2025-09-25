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

**✅ 총 111개 테스트 모두 통과**

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

## 🔧 테스트 실행 방법

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