package com.example.logger;

import com.example.logger.aop.LoggerAop;
import com.example.logger.aop.LoggingMethodInterceptor;
import com.example.logger.config.AppLoggerProperties;
import com.example.logger.service.MessageService;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * AppLogger自動構成クラス
 *
 * このクラスはSpring Bootの起動時に自動的に読み込まれ、
 * 必要なBeanを自動的に登録します。
 *
 * 無効化方法:
 * @SpringBootApplication(exclude = AppLoggerAutoConfiguration.class)
 */
@AutoConfiguration
@EnableConfigurationProperties(AppLoggerProperties.class)
public class AppLoggerAutoConfiguration {
    /**
     * AOP設定
     *
     * AspectJがクラスパスに存在する場合のみ有効
     */
    @AutoConfiguration
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    @ConditionalOnProperty(
            prefix = "app.logger.aop",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    @EnableAspectJAutoProxy
    public static class AopAutoConfiguration {

        /**
         * LoggerAopを自動登録
         *
         * ユーザーが独自のLoggerAopを定義していない場合のみ登録
         */
        @Bean
        @ConditionalOnMissingBean(LoggerAop.class)
        public LoggerAop loggerAop(AppLoggerProperties properties) {
            return new LoggerAop(properties);
        }

        @Bean
        @ConditionalOnMissingBean(name = "appLoggerAdvisor")
        public Advisor appLoggerAdvisor(AppLoggerProperties properties) {
            AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
            pointcut.setExpression(properties.getAop().getPointcut());
            return new DefaultPointcutAdvisor(pointcut, new LoggingMethodInterceptor(properties));
        }
    }

    /**
     * MessageService自動構成
     *
     * MessageSourceが存在する場合のみ有効
     */
    @Bean
    @ConditionalOnClass(MessageSource.class)
    @ConditionalOnMissingBean(MessageService.class)
    @ConditionalOnProperty(
            prefix = "app.logger.message",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public MessageService messageService(MessageSource messageSource) {
        return new MessageService(messageSource);
    }
}
