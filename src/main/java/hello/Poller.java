package hello;

import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

//@MessageEndpoint
public class Poller {

    //@Transactional(propagation = Propagation.REQUIRES_NEW)
    //@ServiceActivator(inputChannel = "notificationQueue", poller = @org.springframework.integration.annotation.Poller(fixedDelay = "10000"), adviceChain = {"txAdvice", "retryAdvice"})
    public void notifyDatafeedsProcessor(String notification) throws IOException {
        //do stuff
        System.out.println(String.format("Got a message '%s' from the queue!",notification));
        throw new RuntimeException("la la nothing is coming off of this queue");
    }
}

