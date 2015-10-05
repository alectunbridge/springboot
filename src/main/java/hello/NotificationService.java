package hello;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.Publisher;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

    @Publisher(channel = "notificationQueue")
    public void send(@Payload String notification) {
        LOGGER.debug("Pushing notification to queue. " + notification);
    }
}
