package hello;

import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.aop.PublisherAnnotationBeanPostProcessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.jdbc.store.channel.DerbyChannelMessageStoreQueryProvider;
import org.springframework.integration.store.MessageGroupQueue;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.messaging.MessageChannel;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.AlwaysRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import javax.sql.DataSource;

@org.springframework.context.annotation.Configuration
@EnableTransactionManagement
public class Configuration implements TransactionManagementConfigurer {

    @Bean
    public JdbcChannelMessageStore jdbcChannelMessageStore() {
        JdbcChannelMessageStore jdbcChannelMessageStore = new JdbcChannelMessageStore(dataSource());
        jdbcChannelMessageStore.setChannelMessageStoreQueryProvider(new DerbyChannelMessageStoreQueryProvider());
        return jdbcChannelMessageStore;
    }

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.apache.derby.jdbc.EmbeddedDriver");
        dataSource.setUrl("jdbc:derby:directory:alec;create=true");
        return dataSource;
    }

    @Bean(initMethod = "migrate")
    Flyway flyway() {
        Flyway flyway = new Flyway();
        flyway.setBaselineOnMigrate(true);
        flyway.setDataSource(dataSource());
        return flyway;
    }

    @Bean
    public MessageChannel notificationQueue() {
        QueueChannel queueChannel = new QueueChannel(
                new MessageGroupQueue(jdbcChannelMessageStore(), "notificationStore"));
        return queueChannel;
    }

    @Bean
    public PublisherAnnotationBeanPostProcessor publisherAnnotationBeanPostProcessor() {
        return new PublisherAnnotationBeanPostProcessor();
    }

    @Bean
    public RequestHandlerRetryAdvice retryAdvice() {
        RetryTemplate retryTemplate = new RetryTemplate();
        RequestHandlerRetryAdvice retryAdvice = new RequestHandlerRetryAdvice();
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        RetryPolicy retryPolicy = new AlwaysRetryPolicy();
        backOffPolicy.setInitialInterval(1000);
        backOffPolicy.setMaxInterval(300000);
        backOffPolicy.setMultiplier(2);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.setRetryPolicy(retryPolicy);
        retryAdvice.setRetryTemplate(retryTemplate);
        return retryAdvice;
    }

    @Bean
    public PlatformTransactionManager txManager() {
        return new DataSourceTransactionManager(dataSource());
    }

    @Override
    public PlatformTransactionManager annotationDrivenTransactionManager() {
        return txManager();
    }

    @Bean
    public TransactionInterceptor txAdvice() {
        MatchAlwaysTransactionAttributeSource attributeSource = new MatchAlwaysTransactionAttributeSource();
        attributeSource.setTransactionAttribute(new DefaultTransactionAttribute());
        TransactionInterceptor interceptor = new TransactionInterceptor(txManager(), attributeSource);
        return interceptor;
    }

    @Bean
    public NotificationService notificationService() {
        return new NotificationService();
    }

    @Bean
    JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource());
    }

    @Bean
    public DSLContext dsl() {
        return new DefaultDSLContext(dataSource(), SQLDialect.DERBY);
    }
}
