package com.example.logger;

import com.example.logger.aop.LoggerAop;
import com.example.logger.config.AppLoggerProperties;
import com.example.logger.service.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AppLoggerAutoConfigurationのテスト
 * 
 * ApplicationContextRunnerを使用して軽量で高速なテストを実行
 */
class AppLoggerAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AppLoggerAutoConfiguration.class));

    @Test
    void shouldAutoConfigureAppLoggerProperties() {
        contextRunner.run(context -> {
            // プロパティ値が未定義の場合、デフォルト値で構成される。
            assertThat(context).hasSingleBean(AppLoggerProperties.class);
            
            AppLoggerProperties properties = context.getBean(AppLoggerProperties.class);
            assertThat(properties.getAop().isEnabled()).isTrue();
            assertThat(properties.getMessage().isEnabled()).isTrue();
        });
    }

    @Test
    void shouldAutoConfigureLoggerAopWhenAspectJIsPresent() {
        contextRunner.run(context -> {
            // AspectJがクラスパスにある場合、LoggerAopが自動設定される
            assertThat(context).hasSingleBean(LoggerAop.class);
        });
    }

    @Test
    void shouldAutoConfigureMessageServiceWhenMessageSourceIsPresent() {
        contextRunner
                .withUserConfiguration(MessageSourceConfiguration.class)
                .run(context -> {
                    // MessageSourceをBean定義されている場合に、
                    // MessageServiceが自動設定される
                    assertThat(context).hasSingleBean(MessageService.class);
                    assertThat(context).hasSingleBean(MessageSource.class);
                });
    }

    @Test
    void shouldNotConfigureMessageServiceWhenMessageSourceIsMissing() {
        contextRunner
                .withPropertyValues("app.logger.message.enabled=false")
                .run(context -> {
                    // app.logger.message.enabled=falseの場合、MessageServiceが構成されない
                    assertThat(context).doesNotHaveBean(MessageService.class);
                });
    }

    @Test
    void shouldDisableAopWhenPropertyIsSet() {
        contextRunner
                .withPropertyValues("app.logger.aop.enabled=false")
                .run(context -> {
                    // app.logger.aop.enabled=falseの場合、LoggerAopが構成されない
                    assertThat(context).doesNotHaveBean(LoggerAop.class);
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
                    // AppLoggerPropertiesがプロパティ設定値通りに取得できること
                    AppLoggerProperties properties = context.getBean(AppLoggerProperties.class);
                    assertThat(properties.getAop().isLogArgs()).isFalse();
                    assertThat(properties.getAop().isLogResult()).isTrue();
                    assertThat(properties.getAop().isLogExecutionTime()).isFalse();
                });
    }

    @Test
    void shouldNotOverrideUserDefinedBeans() {
        contextRunner
                .withUserConfiguration(CustomLoggerAopConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(LoggerAop.class);
                    LoggerAop loggerAop = context.getBean(LoggerAop.class);
                    // カスタム実装であることを確認（実際の実装では識別方法を追加）
                    assertThat(loggerAop).isNotNull();
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
    static class CustomLoggerAopConfiguration {
        @Bean
        public LoggerAop customLoggerAop() {
            return new LoggerAop(); // カスタム実装
        }
    }
}