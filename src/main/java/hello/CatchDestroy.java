package hello;

import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;

@Configuration
public class CatchDestroy {


    @PreDestroy
    public void cleanup() {
        System.out.println("tidying up this mess");
    }

    public CommonAnnotationBeanPostProcessor commonAnnotationBeanPostProcessor(){
        return new CommonAnnotationBeanPostProcessor();
    }
}
