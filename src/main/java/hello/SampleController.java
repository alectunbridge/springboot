package hello;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.jooq.UpdatableRecord;
import org.jooq.example.gradle.db.app.tables.Jam;
import org.jooq.example.gradle.db.app.tables.records.JamRecord;
import org.jooq.impl.TableImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import static java.lang.Class.forName;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Controller
@EnableAutoConfiguration
@SpringBootApplication
public class SampleController implements ApplicationListener<ContextClosedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SampleController.class);

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
    public ResponseEntity<Resource> postJson(@PathVariable String clazz, HttpEntity<String> httpEntity) {
        String json = httpEntity.getBody();
        try {
            Class entityClass = forName("org.jooq.example.gradle.db.app.tables.pojos." + clazz);

            ObjectMapper mapper = new ObjectMapper();
            Object thing = mapper.readValue(json,entityClass);

            return new ResponseEntity<>(hal2db(entityClass, entityClass.cast(thing)), HttpStatus.OK);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @RequestMapping(value = "/db2hal/{clazz}/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<String> delete(@PathVariable String clazz, @PathVariable int id){
        LOGGER.debug("VARS:"+clazz+","+id);
        try {
            TableImpl tableClass = (TableImpl)forName("org.jooq.example.gradle.db.app.tables." + clazz).newInstance();
            int response = connection.delete(tableClass).where(tableClass.getIdentity().getField().equal(id)).execute();
            return new ResponseEntity<String>(""+response, HttpStatus.OK);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
    }

    private <T> TypeReference<T> weirdHelper(Class<T> clazz) {
        return new TypeReference<T>() {
            @Override
            public Type getType() {
                return super.getType();
            }
        };
    }

    private Resource hal2db(Class clazz, Object object) {
        try {
            int rowsUpdated = ((UpdatableRecord) connection.newRecord((TableImpl)forName("org.jooq.example.gradle.db.app.tables." + clazz.getSimpleName()).newInstance(), object)).store();
            if( rowsUpdated == 1 ) {
                return new Resource(object);
            } else return null;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
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
