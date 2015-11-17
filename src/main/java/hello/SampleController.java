package hello;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.lang.reflect.InvocationTargetException;

import static java.lang.Class.forName;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;

@Controller
@EnableAutoConfiguration
@SpringBootApplication
public class SampleController implements ApplicationListener<ContextClosedEvent>{

    private int requestCount;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        notificationService.send("message");
        return "added "+getRequestCount();
    }

    @RequestMapping("/subtract")
    @ResponseBody
    String subtract() {
        decrementRequestCount();
        System.out.println(getRequestCount());
        return "subtracted "+getRequestCount();
    }


    @RequestMapping("/queue")
    @ResponseBody
    String queue() {
        int rowCount = this.jdbcTemplate.queryForObject("select count(*) from INT_CHANNEL_MESSAGE", Integer.class);
        return ""+rowCount;
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

    @RequestMapping("/db2hal/{clazz}")
    @ResponseBody
    public Resource getJson(@PathVariable String clazz){
        try {
            Class<?> entityClass = forName("hello."+clazz);
            return db2hal(entityClass);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private <T> Resource<T> db2hal(Class<T> clazz) {
        Resource<T> resource  = null;
        try {
            resource = new Resource(clazz.getConstructor().newInstance());
            resource.add(linkTo(methodOn(SampleController.class).getJson(clazz.getSimpleName())).withSelfRel());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return resource;
    }


}