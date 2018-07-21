package com.icarusrises.caseyellowimageanalysis.config;

import com.icarusrises.caseyellowimageanalysis.configuration.ConfigurationManager;
import com.icarusrises.caseyellowimageanalysis.queues.model.MessageType;
import com.icarusrises.caseyellowimageanalysis.queues.services.MessageProducerService;
import com.icarusrises.caseyellowimageanalysis.services.storage.StorageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import javax.jms.JMSException;
import java.io.File;

@org.springframework.context.annotation.Configuration
public class Configuration {

    @Bean
    @Profile("dev")
    public StorageService storageService() {

        return new StorageService() {

            @Override
            public File getFile(String identifier) {
                return null;
            }
        };
    }

    @Bean
    @Profile("dev")
    public ConfigurationManager configurationManager() {
        return new ConfigurationManager() {
            @Override
            public String awsAccessKeyID() {
                return null;
            }

            @Override
            public String awsSecretAccessKey() {
                return null;
            }

            @Override
            public String googleVisionKey() {
                return null;
            }
        };
    }

    @Bean
    @Profile("dev")
    public MessageProducerService messageProducerService() {
        return new MessageProducerService() {
            @Override
            public <T> void send(MessageType type, T payload) throws JMSException {

            }
        };
    }
}
