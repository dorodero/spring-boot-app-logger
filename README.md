# spring-boot-app-logger

> **Note:** This project is for learning purposes only. The package name (`com.example`) and group ID are placeholders and not intended for production use.

A Spring Boot auto-configuration library focused on recording the start and end of application endpoints. Provides AOP-based method logging, structured exception handling with `AppException`, and i18n message support.

## Features

- **Endpoint logging** — Automatically logs START/END/ERROR of configured methods (default: Controller) without touching your code
- **Structured exceptions** — `AppException` carries an error code + message (`AppMsg`); automatically logged by `AppExceptionLoggingAspect`
- **i18n message support** — Resolves log messages from Spring's `MessageSource` for multilingual apps
- **Spring Boot Auto Configuration** — Zero-config setup; just add the dependency and go
- **Configurable pointcut** — Change the target methods via `application.yml`; SQL/Repository/Service logging is left to the framework or the application itself

## Requirements

- Java 17+
- Spring Boot 3.x

## Installation

Add the following to your `pom.xml`:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>spring-boot-app-logger</artifactId>
    <version>1.0.0</version>
</dependency>
```

For AOP logging, `spring-boot-starter-aop` must also be on the classpath:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

## Quick Start

No configuration is required. Once the dependency is added, the library auto-configures itself.

### Basic logging

Use SLF4J (or Lombok's `@Slf4j`) directly in your application code:

```java
@Slf4j
public class UserService {

    public User findUser(Long userId) {
        log.info("Finding user: {}", userId);
        // ...
    }
}
```

### Structured exception handling

```java
// Throw a structured exception with an error code
AppMsg errorMsg = new AppMsg(MessageType.Error, "E001", "User not found");
throw new AppException(errorMsg);
```

The AOP advice automatically catches `AppException` and logs it with the appropriate level:

```
[ERROR] [E001] User not found
```

### i18n messages via MessageService

Define messages in `messages.properties`:

```properties
user.notfound=User not found: {0}
```

Resolve and log them with `MessageService`:

```java
@Autowired
private MessageService messageService;

AppMsg msg = messageService.getError("user.notfound", userId);
logger.msg(msg);
```

## AOP Auto Logging

When AspectJ is on the classpath, the library automatically intercepts method calls and produces structured logs.

**Example output:**

```
[INFO ] START: UserController.getUser | PathVariable(userId)=123
[DEBUG] START: UserService.findUser
[DEBUG] END:   UserService.findUser | 15ms
[INFO ] END:   UserController.getUser | 45ms | result: User(id=123, name=John)
```

**On exception:**

```
[ERROR] ERROR: UserController.getUser | 12ms | exception: AppException
[ERROR] [E001] User not found
```

## Configuration

All settings are optional. Below is the full reference with defaults:

```yaml
app:
  logger:
    aop:
      enabled: true                                         # Enable/disable AOP logging
      log-args: true                                        # Log method arguments
      log-result: false                                     # Log return values
      log-execution-time: true                              # Log method execution time
    message:
      enabled: true                                         # Enable/disable MessageService
    exception-logging:
      log-in-aop: true                                      # Log AppException in AOP advice
```

### Recommended profiles

**Production** (minimal output, security-conscious):

```yaml
app:
  logger:
    aop:
      log-args: false
      log-result: false
```

**Development** (verbose output for debugging):

```yaml
app:
  logger:
    aop:
      log-args: true
      log-result: true
```

### Disabling auto-configuration

To opt out entirely:

```java
@SpringBootApplication(exclude = AppLoggerAutoConfiguration.class)
```

## Architecture

```
AppLoggerAutoConfiguration
├── AppLoggerProperties         — Binds app.logger.* properties
├── appLoggerAdvisor            — Logs START/END/ERROR of configured pointcut (requires AspectJ)
│   └── LoggingMethodInterceptor
├── AppExceptionLoggingAspect   — Catches AppException and logs it (requires AspectJ)
└── MessageService              — i18n message resolver (requires MessageSource)

AppException → AppMsg (code + message + MessageType)
     └── AppExceptionLoggingAspect catches and logs automatically
```

## License

MIT
