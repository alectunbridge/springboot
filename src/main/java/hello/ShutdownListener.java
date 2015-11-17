package hello;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import java.sql.DriverManager;
import java.sql.SQLException;

@Component
public class ShutdownListener implements ApplicationListener<ContextClosedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShutdownListener.class);

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        try {
            DriverManager.getConnection("jdbc:derby:cs;shutdown=true");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}