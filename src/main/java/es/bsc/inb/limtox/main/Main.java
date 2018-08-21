package es.bsc.inb.limtox.main;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import es.bsc.inb.limtox.config.AppConfig;
import es.bsc.inb.limtox.services.ClassifierService;

class Main {

    public static void main(String[] args) {
    	AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(AppConfig.class);
        ctx.refresh();
        ClassifierService mainService = (ClassifierService)ctx.getBean("classifierServiceImpl");
        String properties_parameters_path = args[0];
        mainService.classify(properties_parameters_path);
    }      
}
