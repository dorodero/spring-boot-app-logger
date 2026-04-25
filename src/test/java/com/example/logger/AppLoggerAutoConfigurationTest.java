package com.example.logger;

import com.example.logger.aop.AppExceptionLoggingAspect;
import com.example.logger.config.AppLoggerProperties;
import com.example.logger.service.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.aop.Advisor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

import static org.assertj.core.api.Assertions.assertThat;

class AppLoggerAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AppLoggerAutoConfiguration.class));

    @Test
    void shouldAutoConfigureAppLoggerProperties() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AppLoggerProperties.class);

            AppLoggerProperties properties = context.getBean(AppLoggerProperties.class);
            assertThat(properties.getAop().isEnabled()).isTrue();
            assertThat(properties.getMessage().isEnabled()).isTrue();
        });
    }

    @Test
    void shouldAutoConfigureAopBeansWhenAspectJIsPresent() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AppExceptionLoggingAspect.class);
            assertThat(context).hasBean("appLoggerAdvisor");
        });
    }

    @Test
    void shouldAutoConfigureMessageServiceWhenMessageSourceIsPresent() {
        contextRunner
                .withUserConfiguration(MessageSourceConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(MessageService.class);
                    assertThat(context).hasSingleBean(MessageSource.class);
                });
    }

    @Test
    void shouldNotConfigureMessageServiceWhenDisabled() {
        contextRunner
                .withPropertyValues("app.logger.message.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MessageService.class);
                });
    }

    @Test
    void shouldDisableAopWhenPropertyIsSet() {
        contextRunner
                .withPropertyValues("app.logger.aop.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AppExceptionLoggingAspect.class);
                    assertThat(context).doesNotHaveBean(Advisor.class);
                });
    }

    @Test
    void shouldConfigureWithCustomProperties() {
        contextRunner
                .withPropertyValues(
                        "app.logger.aop.log-args=false",
                        "app.logger.aop.log-result=true",
                        "app.logger.aop.log-execution-time=false"
                )
                .run(context -> {
                    AppLoggerProperties properties = context.getBean(AppLoggerProperties.class);
                    assertThat(properties.getAop().isLogArgs()).isFalse();
                    assertThat(properties.getAop().isLogResult()).isTrue();
                    assertThat(properties.getAop().isLogExecutionTime()).isFalse();
                });
    }

    @Test
    void shouldNotOverrideUserDefinedAppExceptionLoggingAspect() {
        contextRunner
                .withUserConfiguration(CustomAspectConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(AppExceptionLoggingAspect.class);
                    assertThat(context.getBean(AppExceptionLoggingAspect.class)).isNotNull();
                });
    }

    @Configuration
    static class MessageSourceConfiguration {
        @Bean
        public MessageSource messageSource() {
            ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
            messageSource.setBasename("messages");
            return messageSource;
        }
    }

    @Configuration
    static class CustomAspectConfiguration {
        @Bean
        public AppExceptionLoggingAspect customAspect() {
            return new AppExceptionLoggingAspect();
        }
    }
}
