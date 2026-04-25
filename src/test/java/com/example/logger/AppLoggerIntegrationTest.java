package com.example.logger;

import com.example.logger.aop.AppExceptionLoggingAspect;
import com.example.logger.config.AppLoggerProperties;
import com.example.logger.exception.AppException;
import com.example.logger.exception.AppMsg;
import com.example.logger.service.MessageService;
import com.example.logger.type.MessageType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
    classes = {
        AppLoggerAutoConfiguration.class,
        AppLoggerIntegrationTest.TestApp.class
    }
)
@TestPropertySource(properties = {
    "app.logger.aop.enabled=true",
    "app.logger.aop.log-args=true",
    "app.logger.message.enabled=true"
})
class AppLoggerIntegrationTest {

    @Autowired
    private AppLoggerProperties properties;

    @Autowired
    private AppExceptionLoggingAspect appExceptionLoggingAspect;

    @Autowired
    private MessageService messageService;

    @Autowired
    private TestController testController;

    @Autowired
    private TestService testService;

    @Test
    void shouldAutoConfigureAllBeans() {
        assertThat(properties).isNotNull();
        assertThat(appExceptionLoggingAspect).isNotNull();
        assertThat(messageService).isNotNull();
    }

    @Test
    void shouldConfigurePropertiesCorrectly() {
        assertThat(properties.getAop().isEnabled()).isTrue();
        assertThat(properties.getAop().isLogArgs()).isTrue();
        assertThat(properties.getMessage().isEnabled()).isTrue();
    }

    @Test
    void shouldInterceptControllerMethods() {
        String result = testController.testMethod("parameter");
        assertThat(result).isEqualTo("success");
    }

    @Test
    void shouldInterceptServiceMethods() {
        String result = testService.processData("data");
        assertThat(result).isEqualTo("processed: data");
    }

    @Test
    void shouldHandleAppExceptionInAop() {
        assertThatThrownBy(() -> testService.throwAppException())
                .isInstanceOf(AppException.class)
                .hasMessageContaining("[E001]");
    }

    @Test
    void shouldCreateMessagesViaMessageService() {
        AppMsg errorMsg = messageService.getError("test.error", "param1");
        assertThat(errorMsg.getType()).isEqualTo(MessageType.Error);
        assertThat(errorMsg.getCode()).isEqualTo("test.error");
    }

    @TestConfiguration
    static class TestApp {

        @Bean
        public MessageSource messageSource() {
            ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
            messageSource.setBasename("test-messages");
            messageSource.setDefaultEncoding("UTF-8");
            return messageSource;
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

    @Controller
    static class TestController {
        public String testMethod(String parameter) {
            return "success";
        }
    }

    @Service
    static class TestService {
        public String processData(String data) {
            return "processed: " + data;
        }

        public void throwAppException() {
            AppMsg msg = new AppMsg(MessageType.Error, "E001", "Test error message");
            throw new AppException(msg);
        }
    }
}
