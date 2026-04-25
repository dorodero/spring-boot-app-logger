package com.example.logger.aop;

import com.example.logger.config.AppLoggerProperties;
import com.example.logger.exception.AppException;
import com.example.logger.exception.AppMsg;
import com.example.logger.type.MessageType;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppExceptionLoggingAspectTest {

    @Mock
    private JoinPoint joinPoint;

    private AppLoggerProperties properties;
    private AppExceptionLoggingAspect aspect;

    @BeforeEach
    void setUp() {
        properties = new AppLoggerProperties();
        properties.getExceptionLogging().setLogInAop(true);
        aspect = new AppExceptionLoggingAspect(properties);
    }

    @Test
    void shouldLogAppException() {
        // Given
        AppMsg appMsg = new AppMsg(MessageType.Error, "E001", "Test error");
        AppException appException = new AppException(appMsg);
        when(joinPoint.getTarget()).thenReturn(new Object());

        // When
        aspect.logAppException(joinPoint, appException);

        // Then - 例外が再スローされず正常に処理されることを確認
        verify(joinPoint).getTarget();
    }

    @Test
    void shouldLogWarningAppException() {
        // Given
        AppMsg appMsg = new AppMsg(MessageType.Warning, "W001", "Test warning");
        AppException appException = new AppException(appMsg);
        when(joinPoint.getTarget()).thenReturn(new Object());

        // When
        aspect.logAppException(joinPoint, appException);

        // Then
        verify(joinPoint).getTarget();
    }

    @Test
    void shouldSkipNonAppException() {
        // Given
        RuntimeException runtimeException = new RuntimeException("Not an AppException");

        // When
        aspect.logAppException(joinPoint, runtimeException);

        // Then - AppException でない場合は JoinPoint にアクセスしない
        verify(joinPoint, never()).getTarget();
    }

    @Test
    void shouldSkipWhenExceptionLoggingDisabled() {
        // Given
        properties.getExceptionLogging().setLogInAop(false);
        aspect = new AppExceptionLoggingAspect(properties);

        AppMsg appMsg = new AppMsg(MessageType.Error, "E001", "Test error");
        AppException appException = new AppException(appMsg);

        // When
        aspect.logAppException(joinPoint, appException);

        // Then - 無効化されている場合は JoinPoint にアクセスしない
        verify(joinPoint, never()).getTarget();
    }

    @Test
    void shouldUseDefaultPropertiesWhenNullPassed() {
        // Given
        AppExceptionLoggingAspect aspectWithDefault = new AppExceptionLoggingAspect(null);
        AppMsg appMsg = new AppMsg(MessageType.Error, "E001", "Test error");
        AppException appException = new AppException(appMsg);
        when(joinPoint.getTarget()).thenReturn(new Object());

        // When - デフォルトプロパティでも正常動作することを確認
        aspectWithDefault.logAppException(joinPoint, appException);

        // Then
        verify(joinPoint).getTarget();
    }
}
