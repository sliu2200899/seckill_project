package com.jiuzhang.seckill.mq;

import com.alibaba.fastjson.JSON;
import com.jiuzhang.seckill.db.dao.OrderDao;
import com.jiuzhang.seckill.db.dao.SeckillActivityDao;
import com.jiuzhang.seckill.db.po.Order;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
@RocketMQMessageListener(topic = "seckill_order", consumerGroup = "seckill_order_group")
public class OrderConsumer implements RocketMQListener<MessageExt> {

    @Resource
    private OrderDao orderDao;

    @Resource
    private SeckillActivityDao seckillActivityDao;

    @Override
    @Transactional
    public void onMessage(MessageExt messageExt) {
        // 解析创建订单的请求消息
        String message = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        log.info("receviced the request of order creation" + message);

        Order order = JSON.parseObject(message, Order.class);
        order.setCreateTime(new Date());

        // 2. 扣减库存
        boolean lockStockResult = seckillActivityDao.lockStock(order.getSeckillActivityId());
        if(lockStockResult) {
            // 锁定成功
            // 1 = 以创建，等待付款
            order.setOrderStatus(1);
        } else {
            // 0 = 没有可用库存，无效订单
            order.setOrderStatus(0);
        }
        // 3. 插入订单
        orderDao.insertOrder(order);
    }
}
