package com.jiuzhang.seckill.mq;

import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class RocketMQService {

    @Resource
    private RocketMQTemplate template;

    public void sendMessage(String topic, String body) throws Exception {
        Message message = new Message(topic, body.getBytes());
        template.getProducer().send(message);
    }

    public void sendDelayMessage(String topic, String body, int delayTimeLevel) throws Exception{
        Message message = new Message(topic, body.getBytes());
        message.setDelayTimeLevel(delayTimeLevel);
        template.getProducer().send(message);
    }
}
