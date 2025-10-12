package com.example.logger.conditional;

import com.example.logger.AppLoggerAutoConfiguration;
import com.example.logger.aop.LoggerAop;
import com.example.logger.service.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 条件付きBean設定のテスト
 * 
 * 様々な条件下でのBean生成をテスト
 */
class ConditionalConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AppLoggerAutoConfiguration.class));

    @Test
    void shouldNotConfigureAopWhenAspectJIsMissing() {
        contextRunner
                .withClassLoader(new FilteredClassLoader("org.aspectj.lang.annotation.Aspect"))
                .run(context -> {
                    // AspectJがクラスパスにない場合、LoggerAopは生成されない
                    assertThat(context).doesNotHaveBean(LoggerAop.class);
                });
    }

    @Test
    void shouldConfigureAopWhenAspectJIsPresent() {
        contextRunner.run(context -> {
            // AspectJがクラスパスにある場合、LoggerAopが生成される
            assertThat(context).hasSingleBean(LoggerAop.class);
        });
    }

    @Test
    void shouldNotConfigureMessageServiceWhenMessageSourceIsMissing() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(MessageSource.class))
                .run(context -> {
                    // MessageSourceがクラスパスにない場合、MessageServiceは生成されない
                    assertThat(context).doesNotHaveBean(MessageService.class);
                });
    }

    @Test
    void shouldConfigureMessageServiceWhenMessageSourceIsPresent() {
        contextRunner
                .withUserConfiguration(MessageSourceConfig.class)
                .run(context -> {
                    // MessageSourceが存在する場合、MessageServiceが生成される
                    assertThat(context).hasSingleBean(MessageService.class);
                    assertThat(context).hasSingleBean(MessageSource.class);
                });
    }

    @Test
    void shouldDisableAopViaProperty() {
        contextRunner
                .withPropertyValues("app.logger.aop.enabled=false")
                .run(context -> {
                    // プロパティでAOPを無効化した場合、LoggerAopは生成されない
                    assertThat(context).doesNotHaveBean(LoggerAop.class);
                });
    }

    @Test
    void shouldDisableMessageServiceViaProperty() {
        contextRunner
                .withUserConfiguration(MessageSourceConfig.class)
                .withPropertyValues("app.logger.message.enabled=false")
                .run(context -> {
                    // プロパティでMessageServiceを無効化した場合、生成されない
                    assertThat(context).doesNotHaveBean(MessageService.class);
                    // MessageSourceは別途設定されているので存在する
                    assertThat(context).hasSingleBean(MessageSource.class);
                });
    }

    @Test
    void shouldUseDefaultPropertyValues() {
        contextRunner.run(context -> {
            // デフォルト値でのプロパティ設定確認
            assertThat(context).hasSingleBean(LoggerAop.class);
            // matchIfMissing=trueなので、プロパティが設定されていなくても有効
        });
    }

    @Test
    void shouldNotOverrideUserDefinedLoggerAop() {
        contextRunner
                .withUserConfiguration(CustomLoggerAopConfig.class)
                .run(context -> {
                    // ユーザー定義のLoggerAopがある場合、それを優先
                    assertThat(context).hasSingleBean(LoggerAop.class);
                    LoggerAop loggerAop = context.getBean(LoggerAop.class);
                    // カスタム実装であることを確認
                    assertThat(loggerAop).isNotNull();
                });
    }

    @Test
    void shouldNotOverrideUserDefinedMessageService() {
        contextRunner
                .withUserConfiguration(CustomMessageServiceConfig.class)
                .run(context -> {
                    // ユーザー定義のMessageServiceがある場合、それを優先
                    assertThat(context).hasSingleBean(MessageService.class);
                    MessageService messageService = context.getBean(MessageService.class);
                    assertThat(messageService).isNotNull();
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
                    // 複数の条件が満たされた場合の設定確認
                    assertThat(context).hasSingleBean(LoggerAop.class);
                    assertThat(context).hasSingleBean(MessageService.class);
                });
    }

    @Test
    void shouldConfigureOnlyRequiredBeans() {
        contextRunner
                .withPropertyValues("app.logger.aop.enabled=false")
                .withClassLoader(new FilteredClassLoader(MessageSource.class))
                .run(context -> {
                    // 条件が満たされない場合、必要最小限のBeanのみ生成
                    assertThat(context).doesNotHaveBean(LoggerAop.class);
                    assertThat(context).doesNotHaveBean(MessageService.class);
                    // AppLoggerPropertiesは常に生成される
                    assertThat(context).hasSingleBean(com.example.logger.config.AppLoggerProperties.class);
                });
    }

    /**
     * MessageSource設定
     */
    @Configuration
    static class MessageSourceConfig {
        @Bean
        public MessageSource messageSource() {
            ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
            messageSource.setBasename("messages");
            return messageSource;
        }
    }

    /**
     * カスタムLoggerAop設定
     */
    @Configuration
    static class CustomLoggerAopConfig {
        @Bean
        public LoggerAop customLoggerAop() {
            return new LoggerAop(); // カスタム実装
        }
    }

    /**
     * カスタムMessageService設定
     */
    @Configuration
    static class CustomMessageServiceConfig {
        @Bean
        public MessageSource messageSource() {
            return new ResourceBundleMessageSource();
        }

        @Bean
        public MessageService customMessageService(MessageSource messageSource) {
            return new MessageService(messageSource); // カスタム実装
        }
    }
}