# LogPilot Test Cases

## 📊 Test Execution Results

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

**✅ Core Module: All 111 tests passed**
**✅ Server Module: All 144 tests passed**
**✅ Total: All 255 tests passed**

## 🧪 Detailed Test Cases by File

### 1. LogEntry Model Tests (`LogEntryTest.java`)

#### 🎯 Test Purpose
Verify LogEntry class constructors, builder pattern, field accessors, equals/hashCode, and toString methods.

#### 📋 Test Cases

| Test Method | Description | Verification Item |
|-------------|-------------|-------------------|
| `constructor_ShouldCreateLogEntryWithRequiredFields` | Create LogEntry with default constructor | Required fields set and timestamp auto-generated |
| `constructor_WithMeta_ShouldCreateLogEntryWithAllFields` | Constructor with metadata | Correct storage of metadata |
| `defaultConstructor_ShouldCreateEmptyLogEntryWithTimestamp` | Default no-arg constructor | Create empty object with only timestamp set |
| `settersAndGetters_ShouldWorkCorrectly` | Setter/Getter methods | Setting/getting all fields |
| `builder_ShouldCreateLogEntryCorrectly` | Create object via Builder pattern | All fields set via Builder |
| `builder_WithoutTimestamp_ShouldUseCurrentTime` | Builder without timestamp | Auto-set current time |
| `builder_WithoutOptionalFields_ShouldCreateMinimalEntry` | Builder with only required fields | Optional fields null, required fields set |
| `equals_WithSameValues_ShouldReturnTrue` | Compare objects with same values | equals method works correctly |
| `equals_WithDifferentValues_ShouldReturnFalse` | Compare objects with different values | Detect differences accurately |
| `equals_WithNull_ShouldReturnFalse` | Compare with null | Null safety |
| `equals_WithSameReference_ShouldReturnTrue` | Compare same reference | Comparison with self |
| `equals_WithDifferentClass_ShouldReturnFalse` | Compare with different class | Type safety |
| `hashCode_WithSameValues_ShouldBeEqual` | HashCode of same values | Consistent hashCode generation |
| `hashCode_WithDifferentValues_ShouldBeDifferent` | HashCode of different values | HashCode collision prevention |
| `toString_ShouldContainAllFields` | toString method output | Includes all field info |
| `toString_WithNullMeta_ShouldHandleGracefully` | toString with null metadata | Null safety |

---

### 2. LogLevel Enum Tests (`LogLevelTest.java`)

#### 🎯 Test Purpose
Verify all values of LogLevel enum, valueOf, ordinal, and compareTo methods.

#### 📋 Test Cases

| Test Method | Description | Verification Item |
|-------------|-------------|-------------------|
| `enum_ShouldContainAllExpectedValues` | Check existence of all enum values | Order: DEBUG, INFO, WARN, ERROR |
| `valueOf_WithValidValues_ShouldReturnCorrectEnum` | Convert valid string | Return correct enum |
| `valueOf_WithInvalidValue_ShouldThrowException` | Handle invalid string | Throw IllegalArgumentException |
| `valueOf_WithNull_ShouldThrowException` | Handle null | Throw NullPointerException |
| `name_ShouldReturnCorrectStrings` | name() return value | Return correct string |
| `toString_ShouldReturnCorrectStrings` | toString() return value | Same result as name() |
| `ordinal_ShouldReturnCorrectOrder` | Check ordinal values | DEBUG=0, INFO=1, WARN=2, ERROR=3 |
| `compareTo_ShouldOrderCorrectly` | Comparison operation | Correct order comparison |
| `equals_ShouldWorkCorrectly` | equals method | Check equality and difference |
| `hashCode_ShouldBeConsistent` | hashCode consistency | Same hashCode for same value |

---

### 3. SQLite Storage Tests (`SqliteLogStorageTest.java`)

#### 🎯 Test Purpose
Verify CRUD operations, transactions, and consumer offset management in SQLite-based log storage.

#### 📋 Test Cases

| Test Method | Description | Verification Item |
|-------------|-------------|-------------------|
| `initialize_ShouldCreateDatabase` | Initialize database | Create tables and establish connection |
| `store_WithValidLogEntry_ShouldStoreSuccessfully` | Store single log | Basic storage function |
| `store_WithMetadata_ShouldStoreMetadata` | Store with metadata | JSON metadata serialization/deserialization |
| `store_WithNullMetadata_ShouldStoreWithoutMeta` | Handle null metadata | Safe storage of null value |
| `storeLogs_WithValidEntries_ShouldStoreBatch` | Batch storage function | Transaction-based batch processing |
| `storeLogs_WithEmptyList_ShouldNotFail` | Handle empty list | Process empty data without exception |
| `retrieve_WithNewConsumer_ShouldReturnAllLogs` | Retrieve for new consumer | Return all logs |
| `retrieve_WithExistingConsumer_ShouldReturnOnlyNewLogs` | Retrieve for existing consumer | Return only new logs based on offset |
| `retrieve_WithLimit_ShouldRespectLimit` | Retrieve with limit | Apply LIMIT query |
| `retrieve_WithDifferentChannels_ShouldFilterByChannel` | Filter by channel | Separate data based on channel |
| `retrieveAll_ShouldReturnAllLogsInDescendingOrder` | Retrieve all logs | Sort by latest |
| `retrieveAll_WithLimit_ShouldRespectLimit` | Retrieve all with limit | Apply count limit |
| `retrieveAll_WithNoLogs_ShouldReturnEmptyList` | Retrieve empty database | Return empty list |
| `multipleConsumers_ShouldHaveIndependentOffsets` | Manage multiple consumer offsets | Maintain independent offsets |
| `close_ShouldCloseConnection` | Cleanup connection | Release resources |
| `storageTimestampPersistence_ShouldMaintainTimestamp` | Timestamp precision | Accurate timestamp storage/restoration |

---

### 4. File Storage Tests (`FileLogStorageTest.java`)

#### 🎯 Test Purpose
Verify file I/O, channel separation, and consumer offset management in File-based log storage.

#### 📋 Test Cases

| Test Method | Description | Verification Item |
|-------------|-------------|-------------------|
| `initialize_ShouldCreateStorageDirectory` | Initialize directory | Create storage and offset directories |
| `store_WithValidLogEntry_ShouldCreateLogFile` | Create log file | Write JSON format log file |
| `store_WithSpecialCharactersInChannel_ShouldSanitizeFilename` | Handle special chars in filename | Convert to safe filename |
| `store_WithMetadata_ShouldStoreMetadataAsJson` | Store metadata as JSON | JSON serialization including nested objects |
| `storeLogs_WithMultipleEntries_ShouldStoreBatch` | Store batch files | Separate storage by channel files |
| `storeLogs_WithEmptyList_ShouldNotFail` | Handle empty batch | Process empty data without exception |
| `retrieve_WithNewConsumer_ShouldReturnAllLogs` | Read file for new consumer | Return full file content |
| `retrieve_WithExistingConsumer_ShouldReturnOnlyNewLogs` | Existing consumer offset | Offset management based on line number |
| `retrieve_WithLimit_ShouldRespectLimit` | Read file with limit | Return specified count |
| `retrieve_WithNonexistentChannel_ShouldReturnEmptyList` | Non-existent channel | Return empty list |
| `retrieveAll_ShouldReturnLogsFromAllChannels` | Integrate retrieval from all channels | Read multiple files integrated |
| `retrieveAll_WithLimit_ShouldRespectLimit` | Integrated retrieval with limit | Count limit across files |
| `retrieveAll_WithNoLogs_ShouldReturnEmptyList` | Handle empty directory | Return empty list |
| `multipleConsumers_ShouldHaveIndependentOffsets` | Multiple consumer offsets | Manage independent offset files |
| `consumerOffsetPersistence_ShouldSurviveRestart` | Offset persistence | Restore offset after restart |
| `timestampPersistence_ShouldMaintainTimestamp` | Timestamp precision | Preserve ISO format timestamp |
| `concurrentAccess_ShouldHandleMultipleOperations` | Concurrent access handling | Safe concurrent processing via locks |
| `malformedLogLine_ShouldBeSkippedGracefully` | Handle malformed log | Skip invalid JSON lines |
| `close_ShouldSaveConsumerOffsets` | Save offsets on close | Save all offset files |

---

### 5. LogService Tests (`LogServiceTest.java`)

#### 🎯 Test Purpose
Verify business logic, validation, and operations using Mock objects for LogService interface.

#### 📋 Test Cases

| Test Method | Description | Verification Item |
|-------------|-------------|-------------------|
| `storeLog_WithValidLogEntry_ShouldCallStorage` | Store valid log | Verify Storage call |
| `storeLog_WithNullLogEntry_ShouldThrowException` | Input null log | Throw IllegalArgumentException |
| `storeLogs_WithValidEntries_ShouldCallStorage` | Store valid batch | Verify Storage batch call |
| `storeLogs_WithEmptyList_ShouldCallStorageWithEmptyList` | Store empty list | Pass empty list to Storage |
| `storeLogs_WithNullList_ShouldThrowException` | Input null batch | Throw IllegalArgumentException |
| `getLogsForConsumer_WithValidParameters_ShouldCallStorage` | Retrieve valid consumer | Call Storage retrieval and return result |
| `getLogsForConsumer_WithNullChannel_ShouldThrowException` | Input null channel | Throw IllegalArgumentException |
| `getLogsForConsumer_WithEmptyChannel_ShouldThrowException` | Input empty channel | Throw IllegalArgumentException |
| `getLogsForConsumer_WithNullConsumerId_ShouldThrowException` | Null consumer ID | Throw IllegalArgumentException |
| `getLogsForConsumer_WithEmptyConsumerId_ShouldThrowException` | Empty consumer ID | Throw IllegalArgumentException |
| `getLogsForConsumer_WithNegativeLimit_ShouldThrowException` | Negative limit | Throw IllegalArgumentException |
| `getLogsForConsumer_WithZeroLimit_ShouldCallStorage` | Handle zero limit | Treat 0 as valid value |
| `getAllLogs_WithValidLimit_ShouldCallStorage` | Retrieve all valid | Call Storage retrieve all |
| `getAllLogs_WithNegativeLimit_ShouldThrowException` | Negative limit | Throw IllegalArgumentException |
| `getAllLogs_WithZeroLimit_ShouldCallStorage` | Handle zero limit | Treat 0 as valid value |
| `getAllLogs_WithLargeLimit_ShouldCallStorage` | Handle large limit | Handle Integer.MAX_VALUE |

---

### 6. LogPilotProperties Configuration Tests (`LogPilotPropertiesTest.java`)

#### 🎯 Test Purpose
Verify default values, Setter/Getter, and nested class behavior of application configuration class.

#### 📋 Test Cases

| Test Method | Description | Verification Item |
|-------------|-------------|-------------------|
| `defaultValues_ShouldBeSet` | Check default configuration values | All default values set correctly |
| `setStorage_ShouldUpdateStorageConfiguration` | Change storage config | Replace Storage object |
| `setServer_ShouldUpdateServerConfiguration` | Change server config | Replace Server object |
| `setGrpc_ShouldUpdateGrpcConfiguration` | Change gRPC config | Replace Grpc object |
| `storageClass_ShouldSupportAllOperations` | Storage class functionality | Set/get all fields |
| `sqliteClass_ShouldSupportPathConfiguration` | SQLite config functionality | Set database path |
| `serverClass_ShouldSupportPortConfiguration` | Server config functionality | Set port number |
| `grpcClass_ShouldSupportPortConfiguration` | gRPC config functionality | Set gRPC port |
| `storageType_ShouldContainAllExpectedValues` | StorageType enum values | Assume FILE, SQLITE exist |
| `storageType_ValueOf_ShouldWorkCorrectly` | StorageType string conversion | Correct enum conversion |
| `storageType_ValueOf_WithInvalidValue_ShouldThrowException` | Invalid StorageType | Verify exception thrown |
| `nestedClasses_ShouldHaveIndependentInstances` | Nested class independence | Independence between instances |
| `chainedConfiguration_ShouldWork` | Chained configuration | Complex config chain |
| `nullSafety_ShouldHandleNullAssignments` | Null safety | Handle null assignment |

---

### 7. LogStorageFactory Tests (`LogStorageFactoryTest.java`)

#### 🎯 Test Purpose
Verify storage instance creation, directory creation, and initialization process via Factory pattern.

#### 📋 Test Cases

| Test Method | Description | Verification Item |
|-------------|-------------|-------------------|
| `createLogStorage_WithNullProperties_ShouldThrowException` | Input null properties | Throw IllegalArgumentException |
| `createLogStorage_WithSqliteType_ShouldReturnSqliteStorage` | Create SQLite storage | Return SqliteLogStorage instance |
| `createLogStorage_WithFileType_ShouldReturnFileStorage` | Create File storage | Return FileLogStorage instance |
| `createLogStorage_WithSqliteType_ShouldCreateParentDirectories` | Create SQLite parent directory | Auto-create nested directories |
| `createLogStorage_WithFileType_ShouldCreateDirectory` | Create File storage directory | Auto-create storage directory |
| `createLogStorage_ShouldInitializeStorage` | Initialize storage | Verify initialize() method call |
| `createLogStorage_WithExistingSqliteParentDirectory_ShouldNotFail` | Handle existing SQLite directory | Ignore existing dir and process normally |
| `createLogStorage_WithExistingFileDirectory_ShouldNotFail` | Handle existing File directory | Ignore existing dir and process normally |
| `createLogStorage_WithDifferentConfigurations_ShouldRespectSettings` | Handle various configs | Create independent instances per config |
| `createLogStorage_SqliteWithNullPath_ShouldUseDefaultPath` | Use SQLite default path | Create with default settings |
| `createLogStorage_FileWithNullDirectory_ShouldUseDefaultDirectory` | Use File default directory | Create with default settings |
| `createLogStorage_SqliteWithFileInRootDirectory_ShouldWork` | Root directory SQLite file | Simple path handling |

---

### 8. Integration Tests (`LogPilotCoreIntegrationTest.java`)

#### 🎯 Test Purpose
Verify end-to-end workflow, large data processing, and complex scenarios of the entire system.

#### 📋 Test Cases

| Test Method | Description | Verification Item |
|-------------|-------------|-------------------|
| `endToEndWorkflow_WithSqliteStorage_ShouldWorkCorrectly` | SQLite Full Workflow | Complete CRUD cycle |
| `endToEndWorkflow_WithFileStorage_ShouldWorkCorrectly` | File Full Workflow | Complete CRUD cycle |
| `multipleChannels_WithMultipleConsumers_ShouldWorkIndependently` | Multi-channel/consumer scenario | Independent channel and consumer management |
| `largeBatchProcessing_ShouldHandleCorrectly` | Large batch processing | Efficient processing of 1000 logs (within 5s) |
| `metadataHandling_ShouldPreserveComplexData` | Complex metadata handling | Preserve nested objects, arrays, various types |
| `timestampPrecision_ShouldBePreserved` | Timestamp precision | Preserve second-level precision |
| `storageTypeSwitch_ShouldWorkWithSameData` | Compatibility between storage types | Identical data processing between SQLite-File |
| `edgeCases_ShouldBeHandledGracefully` | Edge case handling | Empty message, long message, special chars, special channel names |

#### 🔍 Detailed Verification Items

**Large Scale Processing Performance:**
- Batch processing of 1,000 log entries
- Completion within 5 seconds
- Pagination (Retrieve 100 at a time)

**Complex Metadata:**
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

**Edge Cases:**
- Empty message (`""`)
- Very long message (10,000 `"A"`s)
- Special characters (`"äöü 中文 🚀 \n\t\r"`)
- Special char channel name (`"special/channel:name*with<chars>"`)

---

# LogPilot Server Module Test Cases

## 📊 Server Module Test Results

```bash
./gradlew :logpilot-server:test

BUILD SUCCESSFUL
144/144 tests passed (100%)
Execution time: ~30 seconds
```

## 🧪 Server Module Detailed Test Cases by File

### 1. Application Tests (`LogPilotServerApplicationTest.java`)

#### 🎯 Test Purpose
Verify Spring Boot application startup, context loading, and main method.

#### 📋 Test Cases

| Test Method | Description | Verification Item |
|-------------|-------------|-------------------|
| `contextLoads` | Spring Application Context Loading | Normal context initialization |
| `mainMethod_WithArgs_ShouldNotThrow` | Main method execution (with args) | Normal execution without exception |
| `mainMethod_WithNullArgs_ShouldNotThrow` | Main method execution (null args) | Null safety |
| `applicationClass_ShouldHaveCorrectAnnotations` | Application class annotations | Mandatory annotations like @SpringBootApplication |

---

### 2. Server Configuration Tests (`ServerConfigTest.java`)

#### 🎯 Test Purpose
Verify Spring Bean configuration, LogStorage factory behavior, and dependency injection.

#### 📋 Test Cases

| Test Method | Description | Verification Item |
|-------------|-------------|-------------------|
| `serverConfig_ShouldHaveCorrectAnnotations` | Config class annotations | @Configuration, @EnableConfigurationProperties |
| `logStorage_WithSqliteProperties_ShouldReturnSqliteLogStorage` | Create SQLite storage bean | Return appropriate type instance |
| `logStorage_WithFileProperties_ShouldReturnFileLogStorage` | Create File storage bean | Return appropriate type instance |
| `logStorage_WithNullProperties_ShouldThrowException` | Handle null properties | Throw IllegalArgumentException |
| `logStorage_WithDefaultProperties_ShouldReturnValidStorage` | Handle default properties | Create valid storage instance |
| `logStorageBean_ShouldHaveCorrectAnnotations` | Bean method annotations | Verification of @Bean annotation |
| `logStorage_ShouldCreateFunctionalStorage` | Created storage functionality | Actual functional instance |
| `logStorage_WithDifferentConfigurations_ShouldCreateDifferentInstances` | Instances per different config | Create different types based on config |
| `logStorage_ShouldCreateNewInstanceEachTime` | Instance creation policy | Create new instance on each call |

---

### 3. Conditional Annotation Tests (`ConditionalAnnotationsTest.java`)

#### 🎯 Test Purpose
Verify conditional bean activation annotations per protocol.

#### 📋 Test Cases

| Test Method | Description | Verification Item |
|-------------|-------------|-------------------|
| `conditionalOnGrpcProtocol_ShouldHaveCorrectAnnotations` | gRPC conditional annotation | @ConditionalOnProperty setting |
| `conditionalOnRestProtocol_ShouldHaveCorrectAnnotations` | REST conditional annotation | @ConditionalOnProperty setting |
| `conditionalAnnotations_ShouldHaveDifferentHavingValues` | Difference between annotations | Different havingValue settings |
| `conditionalAnnotations_ShouldTargetSameElements` | Target consistency | Common ElementType.TYPE, METHOD |
| `conditionalAnnotations_ShouldHaveRuntimeRetention` | Runtime retention policy | RetentionPolicy.RUNTIME |
| `conditionalAnnotations_ShouldBeMetaAnnotatedWithConditionalOnProperty` | Meta-annotation | Inherit @ConditionalOnProperty |
| `conditionalAnnotations_ShouldBeApplicableToClasses` | Class applicability | Include TYPE target |
| `conditionalAnnotations_ShouldBeApplicableToMethods` | Method applicability | Include METHOD target |

---

### 4. REST Controller Tests (`LogControllerTest.java`)

#### 🎯 Test Purpose
Verify REST API endpoints, HTTP request/response handling, and error handling.

#### 📋 Test Cases

| Test Method | Description | Verification Item |
|-------------|-------------|-------------------|
| `logController_ShouldHaveCorrectAnnotations` | Controller annotations | @RestController, @RequestMapping |
| `storeLog_WithValidLogEntry_ShouldReturnCreated` | Store single log API | 201 Created response |
| `storeLog_WithInvalidJson_ShouldReturnBadRequest` | Handle invalid JSON | 400 Bad Request response |
| `storeLog_WithEmptyBody_ShouldReturnBadRequest` | Handle empty request body | 400 Bad Request response |
| `storeLogs_WithValidLogEntries_ShouldReturnCreated` | Store batch logs API | 201 Created response |
| `storeLogs_WithEmptyList_ShouldReturnCreated` | Handle empty batch | 201 Created response |
| `getLogs_WithChannelAndConsumerId_ShouldReturnLogs` | Retrieve logs by channel | 200 OK and log data |
| `getLogs_WithChannelOnly_ShouldReturnAllLogs` | Retrieve all channel logs | Return all logs |
| `getLogs_WithDefaultLimit_ShouldUseDefaultValue` | Use default limit | Apply default value |
| `getLogs_WithCustomLimit_ShouldUseCustomValue` | Custom limit | Apply custom value |
| `getAllLogs_ShouldReturnAllLogs` | Retrieve all logs API | Return logs from all channels |
| `getAllLogs_WithCustomLimit_ShouldUseCustomValue` | Retrieve all logs with limit | Apply limit value |
| `getAllLogs_WithDefaultLimit_ShouldUseDefaultValue` | Retrieve all logs default limit | Apply default value |
| `storeLog_WhenServiceThrowsException_ShouldReturnInternalServerError` | Service exception handling | 500 Internal Server Error |
| `storeLogs_WhenServiceThrowsException_ShouldReturnInternalServerError` | Batch service exception handling | 500 Internal Server Error |
| `getLogs_WhenServiceThrowsException_ShouldReturnInternalServerError` | Retrieval service exception handling | 500 Internal Server Error |
| `storeLog_WithComplexLogEntry_ShouldHandleCorrectly` | Complex log entry | Handle with metadata |
| `getLogs_WithSpecialCharactersInChannel_ShouldHandleCorrectly` | Special char channel name | Encoding handling |
| `getLogs_WithLargeLimit_ShouldAcceptValue` | Handle large limit | Allow large numbers |
| `getLogs_WithZeroLimit_ShouldAcceptValue` | Handle zero limit | Allow 0 value |
| `storeLog_WithMissingRequiredFields_ShouldReturnBadRequest` | Missing required fields | 400 Bad Request |

---

### 5. REST Service Tests (`RestLogServiceTest.java`)

#### 🎯 Test Purpose
Verify REST log service business logic, dependency injection, and exception handling.

#### 📋 Test Cases

| Test Method | Description | Verification Item |
|-------------|-------------|-------------------|
| `restLogService_ShouldHaveCorrectAnnotations` | Service annotations | @Service, @ConditionalOnRestProtocol |
| `constructor_WithLogStorage_ShouldCreateInstance` | Constructor dependency injection | Create normal instance |
| `constructor_WithNullLogStorage_ShouldThrowException` | Handle null dependency | IllegalArgumentException |
| `storeLog_WithValidLogEntry_ShouldCallLogStorage` | Delegate log storage | Verify Storage method call |
| `storeLog_WithNullLogEntry_ShouldCallLogStorageWithNull` | Delegate null log | Pass null value as is |
| `storeLog_WhenStorageThrowsException_ShouldPropagateException` | Propagate storage exception | Pass exception as is |
| `storeLogs_WithValidLogEntries_ShouldCallLogStorage` | Delegate batch storage | Call Batch Storage |
| `storeLogs_WithEmptyList_ShouldCallLogStorage` | Delegate empty batch | Pass empty list |
| `storeLogs_WithNullList_ShouldCallLogStorageWithNull` | Delegate null batch | Pass null value |
| `storeLogs_WhenStorageThrowsException_ShouldPropagateException` | Propagate batch exception | Pass exception as is |
| `getLogsForConsumer_WithValidParameters_ShouldCallLogStorage` | Delegate consumer retrieval | Call Storage retrieval |
| `getLogsForConsumer_WithNullChannel_ShouldCallLogStorage` | Retrieve null channel | Pass null value |
| `getLogsForConsumer_WithNullConsumerId_ShouldCallLogStorage` | Retrieve null consumer | Pass null value |
| `getLogsForConsumer_WithZeroLimit_ShouldCallLogStorage` | Retrieve zero limit | Pass 0 value |
| `getLogsForConsumer_WhenStorageThrowsException_ShouldPropagateException` | Propagate retrieval exception | Pass exception as is |
| `getAllLogs_WithValidLimit_ShouldCallLogStorage` | Delegate retrieve all | Storage retrieve all |
| `getAllLogs_WithZeroLimit_ShouldCallLogStorage` | Retrieve all zero limit | Pass 0 value |
| `getAllLogs_WithNegativeLimit_ShouldCallLogStorage` | Retrieve negative limit | Pass negative value |
| `getAllLogs_WhenStorageThrowsException_ShouldPropagateException` | Propagate retrieve all exception | Pass exception as is |
| `getAllLogs_WithLargeLimit_ShouldCallLogStorage` | Retrieve large limit | Handle large number |
| `service_ShouldDelegateAllCallsToLogStorage` | Delegate all calls | Complete delegation pattern |
| `service_ShouldHandleMultipleConsecutiveCalls` | Handle consecutive calls | Stateless processing |
| `service_ShouldMaintainLogStorageReference` | Maintain Storage reference | Preserve dependency reference |
| `service_ShouldHandleComplexLogEntries` | Handle complex logs | Logs with metadata |

---

### 6. gRPC Handler Tests (`GrpcLogHandlerTest.java`)

#### 🎯 Test Purpose
Verify gRPC log handler service layer, dependency injection, and business logic.

#### 📋 Test Cases

| Test Method | Description | Verification Item |
|-------------|-------------|-------------------|
| `grpcLogHandler_ShouldHaveCorrectAnnotations` | Handler annotations | @Service, @ConditionalOnGrpcProtocol |
| `constructor_WithLogStorage_ShouldCreateInstance` | Constructor dependency injection | Create normal instance |
| `constructor_WithNullLogStorage_ShouldThrowException` | Handle null dependency | IllegalArgumentException |
| `storeLog_WithValidLogEntry_ShouldCallLogStorage` | Delegate log storage | Call Storage method |
| `storeLog_WithNullLogEntry_ShouldCallLogStorageWithNull` | Delegate null log | Pass null value |
| `storeLog_WhenStorageThrowsException_ShouldPropagateException` | Propagate storage exception | Pass exception as is |
| `storeLogs_WithValidLogEntries_ShouldCallLogStorage` | Delegate batch storage | Call Batch Storage |
| `storeLogs_WithEmptyList_ShouldCallLogStorage` | Delegate empty batch | Pass empty list |
| `storeLogs_WithNullList_ShouldCallLogStorageWithNull` | Delegate null batch | Pass null value |
| `storeLogs_WhenStorageThrowsException_ShouldPropagateException` | Propagate batch exception | Pass exception as is |
| `getLogsForConsumer_WithValidParameters_ShouldCallLogStorage` | Delegate consumer retrieval | Call Storage retrieval |
| `getLogsForConsumer_WithNullChannel_ShouldCallLogStorage` | Retrieve null channel | Pass null value |
| `getLogsForConsumer_WithNullConsumerId_ShouldCallLogStorage` | Retrieve null consumer | Pass null value |
| `getLogsForConsumer_WithZeroLimit_ShouldCallLogStorage` | Retrieve zero limit | Pass 0 value |
| `getLogsForConsumer_WhenStorageThrowsException_ShouldPropagateException` | Propagate retrieval exception | Pass exception as is |
| `getAllLogs_WithValidLimit_ShouldCallLogStorage` | Delegate retrieve all | Storage retrieve all |
| `getAllLogs_WithZeroLimit_ShouldCallLogStorage` | Retrieve all zero limit | Pass 0 value |
| `getAllLogs_WithNegativeLimit_ShouldCallLogStorage` | Retrieve negative limit | Pass negative value |
| `getAllLogs_WhenStorageThrowsException_ShouldPropagateException` | Propagate retrieve all exception | Pass exception as is |
| `getAllLogs_WithLargeLimit_ShouldCallLogStorage` | Retrieve large limit | Handle large number |
| `handler_ShouldDelegateAllCallsToLogStorage` | Delegate all calls | Complete delegation pattern |
| `handler_ShouldHandleMultipleConsecutiveCalls` | Handle consecutive calls | Stateless processing |
| `handler_ShouldMaintainLogStorageReference` | Maintain Storage reference | Preserve dependency reference |
| `handler_ShouldHandleComplexLogEntries` | Handle complex logs | Logs with metadata |
| `handler_ShouldImplementLogServiceInterface` | Interface implementation | Implement LogService interface |
| `handler_ShouldHaveCorrectBeanName` | Check Bean name | Appropriate Spring Bean name |
| `handler_ShouldBeConditionalOnGrpcProtocol` | Conditional activation | Check gRPC protocol condition |

---

### 7. gRPC Service Tests (`LogPilotGrpcServiceTest.java`)

#### 🎯 Test Purpose
Verify gRPC protocol handling, message conversion, and StreamObserver pattern.

#### 📋 Test Cases

| Test Method | Description | Verification Item |
|-------------|-------------|-------------------|
| `grpcService_ShouldHaveCorrectAnnotations` | gRPC service annotations | @Service, @ConditionalOnGrpcProtocol |
| `sendLog_WithValidRequest_ShouldReturnSuccessResponse` | Single log gRPC send | Return SUCCESS response |
| `sendLog_WithMetadata_ShouldConvertCorrectly` | Send with metadata | Metadata conversion handling |
| `sendLog_WithEmptyMessage_ShouldHandleGracefully` | Handle empty message | Safe empty value handling |
| `sendLog_WhenServiceThrowsException_ShouldReturnErrorResponse` | Send exception handling | Return FAILED response |
| `sendLogs_WithValidRequests_ShouldReturnSuccessResponse` | Batch logs gRPC send | Batch SUCCESS response |
| `sendLogs_WithEmptyList_ShouldReturnSuccessResponse` | Send empty batch | Empty list SUCCESS handling |
| `sendLogs_WhenServiceThrowsException_ShouldReturnErrorResponse` | Batch exception handling | Return FAILED response |
| `listLogs_ShouldReturnProtoLogEntries` | Stream log list | StreamObserver response |
| `listLogs_WithNoLogs_ShouldReturnEmptyResponse` | Empty log list | Stream empty response |
| `listLogs_WhenServiceThrowsException_ShouldCallOnError` | List retrieval exception | onError called |
| `fetchLogs_WithChannel_ShouldCallGetLogsForConsumer` | Retrieve by channel | Retrieve specific channel |
| `fetchLogs_WithoutChannel_ShouldCallGetAllLogs` | Retrieve all logs | Retrieve all without channel |
| `fetchLogs_WithEmptyChannel_ShouldCallGetAllLogs` | Retrieve empty channel | Treat empty channel as retrieve all |
| `fetchLogs_WhenServiceThrowsException_ShouldCallOnError` | Retrieval exception handling | onError called |
| `convertLogRequestToLogEntry_ShouldMapAllFields` | Convert gRPC request | Map all fields |
| `convertLogRequestToLogEntry_WithMetadata_ShouldConvertMap` | Convert metadata map | gRPC Map → Java Map |
| `convertToProtoLogEntry_ShouldMapAllFields` | Convert Java → Proto | Reverse map all fields |
| `convertStringToLogLevel_WithValidLevel_ShouldConvert` | Convert LogLevel string | Convert valid level |
| `convertStringToLogLevel_WithInvalidLevel_ShouldDefaultToInfo` | Invalid LogLevel | Use default INFO |

---

### 8. Integration Tests (`LogPilotServerIntegrationTest.java`)

#### 🎯 Test Purpose
Verify end-to-end workflow of the entire server module, multi-protocol, and concurrency.

#### 📋 Test Cases

| Test Method | Description | Verification Item |
|-------------|-------------|-------------------|
| `contextLoads_WithAllProfiles_ShouldStartSuccessfully` | Full profile context | Normal loading of all components |
| `restEndpoints_ShouldWorkEndToEnd_WithFileStorage` | REST API Full Workflow | Complete Save->Retrieve cycle |
| `restAndGrpc_ShouldWorkTogether_SameStorage` | REST-gRPC Integrated Operation | Share same storage |
| `multipleClients_ShouldAccessConcurrently` | Concurrent Multi-client Access | Concurrency safety |
| `largeBatchRequests_ShouldProcessCorrectly` | Large Batch Processing | 500 logs processing performance |
| `invalidRequests_ShouldReturnAppropriateErrors` | Handle Invalid Requests | Appropriate error response |
| `crossProtocolDataConsistency_ShouldMaintain` | Data Consistency across protocols | Data identity between REST-gRPC |
| `applicationShutdown_ShouldCloseResourcesProperly` | Application Shutdown Handling | Resource cleanup |
| `storagePerformance_ShouldMeetBasicRequirements` | Storage Performance Requirements | Meet basic performance standards |
| `errorHandling_ShouldBeConsistent` | Consistent Error Handling | Same error handling across layers |
| `healthCheck_ShouldIndicateSystemStatus` | Health Check Functionality | Accurately reflect system status |
| `dataIntegrity_ShouldBePreserved` | Data Integrity | Match stored/retrieved data |
| `concurrentReadWrite_ShouldMaintainConsistency` | Concurrent Read/Write Consistency | Data integrity during concurrent ops |

---

### 9. Server Configuration Tests (`ServerConfigurationTest.java`)

#### 🎯 Test Purpose
Verify various environment configurations, protocol-specific bean activation, and storage type settings.

#### 📋 Test Cases

| Test Method | Description | Verification Item |
|-------------|-------------|-------------------|
| `RestOnlyConfigurationTest` | REST-only configuration | Activate REST beans only, deactivate gRPC beans |
| `GrpcOnlyConfigurationTest` | gRPC-only configuration | Activate gRPC beans only, deactivate REST beans |
| `AllProtocolsConfigurationTest` | All protocols configuration | Activate all beans |
| `FileStorageConfigurationTest` | File storage configuration | Create FileLogStorage bean |
| `SqliteStorageConfigurationTest` | SQLite storage configuration | Create SqliteLogStorage bean |
| `CustomPortsConfigurationTest` | Custom port configuration | User-defined port binding |
| `DefaultPropertiesConfigurationTest` | Default configuration values | Normal operation with defaults |
| `LoggingLevelConfigurationTest` | Logging level configuration | Apply custom logging settings |
| `ActuatorConfigurationTest` | Actuator configuration | Activate monitoring endpoints |
| `ProfileSwitchingConfigurationTest` | Profile switching configuration | Apply different settings per profile |
| `ResourceCleanupConfigurationTest` | Resource cleanup configuration | Appropriate resource management |

---

### 10. Performance Tests (`PerformanceTest.java`)

#### 🎯 Test Purpose
Verify throughput, response time, memory usage, and concurrency performance.

#### 📋 Test Cases

| Test Method | Description | Verification Item |
|-------------|-------------|-------------------|
| `restApi_ShouldHandleHighThroughput` | REST API High Throughput | 100 requests, >10 per second |
| `concurrentClients_ShouldMaintainPerformance` | Concurrent Client Performance | 10 simultaneous clients |
| `largeBatches_ShouldProcessWithinTimeout` | Large Batch Performance | 500 batch within 10 seconds |
| `memoryUsage_ShouldStayWithinLimits` | Memory Usage Limits | Memory increase within 50MB |
| `storagePerformance_ShouldMeetRequirements` | Storage Performance | Write <500ms, Read <200ms |
| `responseTime_ShouldMeetSLA` | Response Time SLA | Avg 1s, 95% 2s, Max 5s |
| `errorRate_ShouldStayBelowThreshold` | Error Rate Threshold | Maintain error rate <1% |

#### 🔍 Performance Criteria

**Throughput:**
- REST API: >10 req/sec
- Concurrent Clients: 10 clients, >15 req/sec
- Large Batch: 500 logs within 10s

**Response Time:**
- Avg: <1s
- 95th Percentile: <2s
- Max: <5s

**Resource Usage:**
- Memory Increase: <50MB
- Error Rate: <1%

---

## 🔧 How to Run Server Module Tests

### Run All Tests
```bash
./gradlew :logpilot-server:test
```

### Run by Category
```bash
# Integration Tests Only
./gradlew :logpilot-server:test --tests "*IntegrationTest"

# Performance Tests Only
./gradlew :logpilot-server:test --tests "*PerformanceTest"

# Configuration Tests Only
./gradlew :logpilot-server:test --tests "*ConfigurationTest"
```

### Run Specific Test Class
```bash
./gradlew :logpilot-server:test --tests "com.logpilot.server.rest.LogControllerTest"
```

### Check Test Report
```bash
open logpilot-server/build/reports/tests/test/index.html
```

---

## 🔧 How to Run Core Module Tests

### Run All Tests
```bash
./gradlew :logpilot-core:test
```

### Run Specific Test Class
```bash
./gradlew :logpilot-core:test --tests "com.logpilot.core.model.LogEntryTest"
```

### Run Specific Test Method
```bash
./gradlew :logpilot-core:test --tests "com.logpilot.core.model.LogEntryTest.constructor_ShouldCreateLogEntryWithRequiredFields"
```

### Check Test Report
```bash
open logpilot-core/build/reports/tests/test/index.html
```

---

# LogPilot Client Module Test Cases

## 🧪 Client Module Detailed Test Cases by File

### 1. Client SDK Tests (`LogPilotClientTest.java`)

#### 🎯 Test Purpose
Verify `LogPilotClient` functionality, including synchronous logging, async batching, and graceful shutdown.

#### 📋 Test Cases

| Test Method | Description | Verification Item |
|-------------|-------------|-------------------|
| `testBatchingTriggersFlush` | Async batching behavior | Queueing logs and auto-flushing when batch size reached |
| `testFlushOnClose` | Graceful shutdown | Flushing pending logs on client close |

## 🔧 How to Run Client Module Tests

### Run All Tests
```bash
./gradlew :logpilot-client:test
```
