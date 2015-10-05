package hello;

import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;

import javax.transaction.Transactional;
import java.io.IOException;

@MessageEndpoint
public class Poller {


    @Transactional
    @ServiceActivator(inputChannel = "notificationQueue", poller = @org.springframework.integration.annotation.Poller(fixedDelay = "1000"), adviceChain = "retryAdvice")
    public void notifyDatafeedsProcessor(String notification) throws IOException {
        //do stuff

    }
}

