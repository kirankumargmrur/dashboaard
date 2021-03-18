package com.capitalone.dashboard;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * Application configuration and bootstrap
 */
@SpringBootApplication
public class Application {

	private static final Log LOG = LogFactory.getLog(Application.class);
    public static void main(String[] args) {
    	try {
        SpringApplication.run(Application.class, args);
    	}
    	catch(Exception e) {
    		LOG.info("Test report Application startup exception: " + e);
    	}
    }
}
