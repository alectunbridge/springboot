package hello;

import org.jooq.DSLContext;
import org.jooq.impl.TableImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

import static java.lang.Class.forName;
import static org.jooq.example.gradle.db.app.tables.IntChannelMessage.INT_CHANNEL_MESSAGE;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Controller
@EnableAutoConfiguration
@SpringBootApplication
public class SampleController implements ApplicationListener<ContextClosedEvent> {

    private int requestCount;

    @Autowired
    private DSLContext connection;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Object lock = new Object();

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
        return "added " + getRequestCount();
    }

    @RequestMapping("/subtract")
    @ResponseBody
    String subtract() {
        decrementRequestCount();
        System.out.println(getRequestCount());
        return "subtracted " + getRequestCount();
    }


    @RequestMapping("/queue")
    @ResponseBody
    String queue() {
        int rowCount = this.jdbcTemplate.queryForObject("select count(*) from INT_CHANNEL_MESSAGE", Integer.class);
        return "" + rowCount;
    }


    public static void main(String[] args) throws Exception {
        SpringApplication.run(SampleController.class, args);
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        while (getRequestCount() > 0) {
            try {
                synchronized (lock) {
                    lock.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @RequestMapping(value = "/db2hal/{clazz}")
    @ResponseBody
    public ResponseEntity<Resources> getJson(@PathVariable String clazz) {
        try {
            Class entityClass = forName("org.jooq.example.gradle.db.app.tables.pojos." + clazz);
            return new ResponseEntity<>(db2hal(entityClass), HttpStatus.OK);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @RequestMapping(value = "/db2hal/{clazz}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Resource> postJson(@PathVariable String clazz, @RequestBody Resource resource) {
        try {
            Class entityClass = forName("org.jooq.example.gradle.db.app.tables.pojos." + clazz);
            return new ResponseEntity<>(hal2db(entityClass), HttpStatus.OK);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Resource hal2db(Class entityClass) {
        //connection.newRecord(DERIVATIVE, derivative).store();
        return null;
    }

    private Resources<Resource> db2hal(Class clazz) {
        try {
            return new Resources<>((List<Resource>) connection.select().from((TableImpl)
                    forName("org.jooq.example.gradle.db.app.tables." + clazz.getSimpleName()).newInstance()
            ).fetchInto(clazz),linkTo(methodOn(SampleController.class).getJson(clazz.getSimpleName())).withSelfRel());
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }


}
