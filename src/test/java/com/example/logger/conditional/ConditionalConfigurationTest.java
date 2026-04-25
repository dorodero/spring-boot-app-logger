package com.example.logger.conditional;

import com.example.logger.AppLoggerAutoConfiguration;
import com.example.logger.aop.AppExceptionLoggingAspect;
import com.example.logger.config.AppLoggerProperties;
import com.example.logger.service.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.aop.Advisor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionalConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AppLoggerAutoConfiguration.class));

    @Test
    void shouldNotConfigureAopWhenAspectJIsMissing() {
        contextRunner
                .withClassLoader(new FilteredClassLoader("org.aspectj.lang.annotation.Aspect"))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AppExceptionLoggingAspect.class);
                    assertThat(context).doesNotHaveBean(Advisor.class);
                });
    }

    @Test
    void shouldConfigureAopWhenAspectJIsPresent() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AppExceptionLoggingAspect.class);
            assertThat(context).hasBean("appLoggerAdvisor");
        });
    }

    @Test
    void shouldNotConfigureMessageServiceWhenMessageSourceIsMissing() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(MessageSource.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MessageService.class);
                });
    }

    @Test
    void shouldConfigureMessageServiceWhenMessageSourceIsPresent() {
        contextRunner
                .withUserConfiguration(MessageSourceConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(MessageService.class);
                    assertThat(context).hasSingleBean(MessageSource.class);
                });
    }

    @Test
    void shouldDisableAopViaProperty() {
        contextRunner
                .withPropertyValues("app.logger.aop.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AppExceptionLoggingAspect.class);
                    assertThat(context).doesNotHaveBean(Advisor.class);
                });
    }

    @Test
    void shouldDisableMessageServiceViaProperty() {
        contextRunner
                .withUserConfiguration(MessageSourceConfig.class)
                .withPropertyValues("app.logger.message.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MessageService.class);
                    assertThat(context).hasSingleBean(MessageSource.class);
                });
    }

    @Test
    void shouldUseDefaultPropertyValues() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AppExceptionLoggingAspect.class);
            assertThat(context).hasBean("appLoggerAdvisor");
        });
    }

    @Test
    void shouldNotOverrideUserDefinedAppExceptionLoggingAspect() {
        contextRunner
                .withUserConfiguration(CustomAspectConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(AppExceptionLoggingAspect.class);
                    assertThat(context.getBean(AppExceptionLoggingAspect.class)).isNotNull();
                });
    }

    @Test
    void shouldNotOverrideUserDefinedMessageService() {
        contextRunner
                .withUserConfiguration(CustomMessageServiceConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(MessageService.class);
                    assertThat(context.getBean(MessageService.class)).isNotNull();
                });
    }

    @Test
    void shouldConfigureWithMultipleConditions() {
        contextRunner
                .withUserConfiguration(MessageSourceConfig.class)
                .withPropertyValues(
                        "app.logger.aop.enabled=true",
                        "app.logger.message.enabled=true",
                        "app.logger.aop.log-args=false"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(AppExceptionLoggingAspect.class);
                    assertThat(context).hasSingleBean(MessageService.class);
                });
    }

    @Test
    void shouldConfigureOnlyRequiredBeans() {
        contextRunner
                .withPropertyValues("app.logger.aop.enabled=false")
                .withClassLoader(new FilteredClassLoader(MessageSource.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AppExceptionLoggingAspect.class);
                    assertThat(context).doesNotHaveBean(MessageService.class);
                    assertThat(context).hasSingleBean(AppLoggerProperties.class);
                });
    }

    @Configuration
    static class MessageSourceConfig {
        @Bean
        public MessageSource messageSource() {
            ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
            messageSource.setBasename("messages");
            return messageSource;
        }
    }

    @Configuration
    static class CustomAspectConfig {
        @Bean
        public AppExceptionLoggingAspect customAspect() {
            return new AppExceptionLoggingAspect();
        }
    }

    @Configuration
    static class CustomMessageServiceConfig {
        @Bean
        public MessageSource messageSource() {
            return new ResourceBundleMessageSource();
        }

        @Bean
        public MessageService customMessageService(MessageSource messageSource) {
            return new MessageService(messageSource);
        }
    }
}
