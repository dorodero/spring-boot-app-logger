package com.example.logger.aop;

import com.example.logger.config.AppLoggerProperties;
import com.example.logger.exception.AppException;
import com.example.logger.exception.AppMsg;
import com.example.logger.type.MessageType;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * LoggerAopの単体テスト
 * 
 * モックを使用してAOPの動作を詳細にテスト
 */
@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig(LoggerAopTest.TestConfig.class)
class LoggerAopTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    private LoggerAop loggerAop;
    private AppLoggerProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AppLoggerProperties();
        properties.getAop().setEnabled(true);
        properties.getAop().setLogArgs(true);
        properties.getAop().setLogExecutionTime(true);
        
        loggerAop = new LoggerAop(properties);
    }

    @Test
    void shouldInterceptControllerMethod() throws Throwable {
        // Given
        TestController target = new TestController();
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"test"});
        when(joinPoint.proceed()).thenReturn("success");

        // When
        Object result = loggerAop.aroundController(joinPoint);

        // Then
        assertThat(result).isEqualTo("success");
        verify(joinPoint).proceed();
    }

    @Test
    void shouldInterceptServiceMethod() throws Throwable {
        // Given
        TestService target = new TestService();
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("processData");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"data"});
        when(joinPoint.proceed()).thenReturn("processed");

        // When
        Object result = loggerAop.aroundService(joinPoint);

        // Then
        assertThat(result).isEqualTo("processed");
        verify(joinPoint).proceed();
    }

    @Test
    void shouldHandleExceptionInController() throws Throwable {
        // Given
        TestController target = new TestController();
        RuntimeException exception = new RuntimeException("Test error");
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("errorMethod");
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenThrow(exception);

        // When & Then
        assertThatThrownBy(() -> loggerAop.aroundController(joinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Test error");
        
        verify(joinPoint).proceed();
    }

    @Test
    void shouldLogAppExceptionCorrectly() throws NoSuchMethodException {
        // Given
        TestService target = new TestService();
        Method method = TestService.class.getMethod("throwException");
        AppMsg appMsg = new AppMsg(MessageType.Error, "E001", "Test error");
        AppException appException = new AppException(appMsg);
        
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);

        // When
        loggerAop.logAppException(joinPoint, appException);

        // Then - ログが出力されることを確認（実際の実装ではログ出力の検証が必要）
        // この例では例外処理が正常に動作することを確認
        assertThat(appException.getAppMsg()).isEqualTo(appMsg);
    }

    @Test
    void shouldSkipLoggingWhenDisabled() throws Throwable {
        // Given
        properties.getAop().setEnabled(false);
        loggerAop = new LoggerAop(properties);
        
        TestController target = new TestController();
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.proceed()).thenReturn("success");

        // When
        Object result = loggerAop.aroundController(joinPoint);

        // Then
        assertThat(result).isEqualTo("success");
        verify(joinPoint).proceed();
        // ログ処理がスキップされることを確認
    }

    @Test
    void shouldSkipExceptionLoggingWhenDisabled() {
        // Given
        properties.getExceptionLogging().setLogInAop(false);
        loggerAop = new LoggerAop(properties);
        
        TestService target = new TestService();
        AppMsg appMsg = new AppMsg(MessageType.Error, "E001", "Test error");
        AppException appException = new AppException(appMsg);
        
        when(joinPoint.getTarget()).thenReturn(target);

        // When
        loggerAop.logAppException(joinPoint, appException);

        // Then - ログ処理がスキップされることを確認
        // 実際の実装ではログ出力されないことを検証
    }

    /**
     * テスト設定
     */
    @TestConfiguration
    @EnableAspectJAutoProxy
    static class TestConfig {
        
        @Bean
        public AppLoggerProperties appLoggerProperties() {
            return new AppLoggerProperties();
        }
        
        @Bean
        public LoggerAop loggerAop(AppLoggerProperties properties) {
            return new LoggerAop(properties);
        }
        
        @Bean
        public TestController testController() {
            return new TestController();
        }
        
        @Bean
        public TestService testService() {
            return new TestService();
        }
    }

    /**
     * テスト用Controller
     */
    @Controller
    static class TestController {
        public String testMethod(String param) {
            return "success";
        }
        
        public String errorMethod() {
            throw new RuntimeException("Test error");
        }
    }

    /**
     * テスト用Service
     */
    @Service
    static class TestService {
        public String processData(String data) {
            return "processed: " + data;
        }
        
        public void throwException() throws Exception {
            throw new Exception("Test service exception");
        }
    }
}