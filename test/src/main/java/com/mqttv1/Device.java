package com.mqttv1;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

@SpringBootApplication
@IntegrationComponentScan
public class Device {
    public static void main(String[] args) {
        new SpringApplicationBuilder(Device.class).web(false).run(args);
    }

    @Bean
    public MessageChannel mqttInputChannel(){
        return new DirectChannel();
    }

    @Bean
    public MqttPahoClientFactory mqttClientFactory(){
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setServerURIs("tcp://localhost:1883");
        factory.setUserName("admin");
        factory.setPassword("password");
        return factory;
    }

    @Bean
    public MessageProducer inbound(){
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter("device", mqttClientFactory(), "notice");
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler handler(){
        return new MessageHandler() {
            @Override
            public void handleMessage(Message<?> message) throws MessagingException {
                System.out.println(message.getPayload());
            }
        };
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutbound(){
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler("device", mqttClientFactory());
        messageHandler.setAsync(true);
        messageHandler.setDefaultTopic("notice");
        return messageHandler;
    }

    @Bean
    public MessageChannel mqttOutboundChannel(){
        return new DirectChannel();
    }

    @MessagingGateway(defaultRequestChannel = "mqttOutboundChannel")
    public interface MyGateway{
        void sendToMqtt(String data);
    }
}
