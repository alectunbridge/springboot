package hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.PreDestroy;

@Controller
@EnableAutoConfiguration
@SpringBootApplication
public class SampleController implements ApplicationListener<ContextClosedEvent>{

    private int requestCount;

    private Object lock =  new Object();

    public int getRequestCount() {
        synchronized (lock) {
            return requestCount;
        }
    }

    public void incrementRequestCount() {
        synchronized (lock) {
            requestCount++;
        }
    }

    public void decrementRequestCount() {
        synchronized (lock) {
            requestCount--;
            lock.notifyAll();
        }
    }

    @RequestMapping("/add")
    @ResponseBody
    String add() {
        incrementRequestCount();
        System.out.println(getRequestCount());
        return "added "+getRequestCount();
    }

    @RequestMapping("/subtract")
    @ResponseBody
    String subtract() {
        decrementRequestCount();
        System.out.println(getRequestCount());
        return "subtracted "+getRequestCount();
    }


    public static void main(String[] args) throws Exception {
        SpringApplication.run(SampleController.class, args);
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        while(getRequestCount()>0){
            try {
                synchronized(lock) {
                    lock.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}