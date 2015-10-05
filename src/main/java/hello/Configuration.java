package hello;

import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.aop.PublisherAnnotationBeanPostProcessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.jdbc.store.channel.DerbyChannelMessageStoreQueryProvider;
import org.springframework.integration.store.MessageGroupQueue;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.messaging.MessageChannel;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.AlwaysRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import javax.sql.DataSource;

@org.springframework.context.annotation.Configuration
public class Configuration {

    @Bean
    public JdbcChannelMessageStore jdbcChannelMessageStore() {
        JdbcChannelMessageStore jdbcChannelMessageStore = new JdbcChannelMessageStore(dataSource());
        jdbcChannelMessageStore.setChannelMessageStoreQueryProvider(new DerbyChannelMessageStoreQueryProvider());
        return jdbcChannelMessageStore;
    }

    @Bean
    public DataSource dataSource() {
//        EmbeddedDatabaseBuilder embeddedDatabaseBuilder = new EmbeddedDatabaseBuilder();
//        return embeddedDatabaseBuilder
//                .setType(EmbeddedDatabaseType.DERBY)
//                .addScript("classpath:org/springframework/integration/jdbc/store/channel/schema-derby.sql")
//                .setType()
//                .build();
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.apache.derby.jdbc.EmbeddedDriver");
        dataSource.setUrl("jdbc:derby:directory:alec;");
        //TODO this needs to be conditional in some way
        //DatabasePopulatorUtils.execute(databasePopulator(), dataSource);
        return dataSource;
    }

//    @Bean
//    public DataSourceInitializer dataSourceInitializer(final DataSource dataSource) {
//        final DataSourceInitializer initializer = new DataSourceInitializer();
//        initializer.setDataSource(dataSource);
//        initializer.setDatabasePopulator(databasePopulator());
//        return initializer;
//    }

    private DatabasePopulator databasePopulator() {
        final ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("org/springframework/integration/jdbc/store/channel/schema-derby.sql"));
        return populator;
    }


    @Bean
    public MessageChannel notificationQueue() {
        QueueChannel queueChannel = new QueueChannel(new MessageGroupQueue(jdbcChannelMessageStore(),"notificationStore"));
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
}
