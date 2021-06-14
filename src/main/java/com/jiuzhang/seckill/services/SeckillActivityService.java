package com.jiuzhang.seckill.services;

import com.alibaba.fastjson.JSON;
import com.jiuzhang.seckill.db.dao.OrderDao;
import com.jiuzhang.seckill.db.dao.SeckillActivityDao;
import com.jiuzhang.seckill.db.dao.SeckillCommodityDao;
import com.jiuzhang.seckill.db.po.Order;
import com.jiuzhang.seckill.db.po.SeckillActivity;
import com.jiuzhang.seckill.db.po.SeckillCommodity;
import com.jiuzhang.seckill.mq.RocketMQService;
import com.jiuzhang.seckill.util.RedisService;
import com.jiuzhang.seckill.util.SnowFlake;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

@Slf4j
@Service
public class SeckillActivityService {

    @Resource
    private OrderDao orderDao;

    @Resource
    private RedisService redisService;

    @Resource
    private SeckillActivityDao seckillActivityDao;

    @Resource
    private SeckillCommodityDao seckillCommodityDao;

    @Resource
    private RocketMQService rocketMQService;

    private SnowFlake snowFlake = new SnowFlake(1, 1);

    /**
     * 判断商品是否还有库存
     * @param activityId 商品ID
     * @return
     */
    public boolean seckillStockValidator(long activityId) {
        String key = "stock:" + activityId;
        return redisService.stockDeductValidator(key);
    }

    public Order createOrder(long seckillActivityId, long userId) throws Exception {
        /*
         *  1. create order
         */
        SeckillActivity activity = seckillActivityDao.querySeckillActivityById(seckillActivityId);
        Order order = new Order();

        // 采用雪花算法生成订单ID
        order.setOrderNo(String.valueOf(snowFlake.nextId()));
        order.setSeckillActivityId(activity.getId());
        order.setUserId(userId);
        order.setOrderAmount(activity.getSeckillPrice().longValue());

        /*
         * 2. 发送创建订单消息
         */
        rocketMQService.sendMessage("seckill_order", JSON.toJSONString(order));

        /*
         * 3. 发送订单付款状态校验信息
         * 开源RocketMQ 支持延迟消息，但是不支持秒级精度
         * messageDelayLevel = 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
         */
        rocketMQService.sendDelayMessage("pay_check", JSON.toJSONString(order), 3);

        return order;
    }

    /**
     * 将秒杀详情相关信息倒入redis
     * @param seckillActivityId
     */
    public void pushSeckillInfoToRedis(long seckillActivityId) {
        SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(seckillActivityId);
        redisService.setValue("seckillActivity:" + seckillActivityId, JSON.toJSONString(seckillActivity));

        SeckillCommodity seckillCommodity = seckillCommodityDao.querySeckillCommodityById(seckillActivity.getCommodityId());
        redisService.setValue("seckillCommodity:" + seckillActivity.getCommodityId(), JSON.toJSONString(seckillCommodity));
    }

    public void payOrderProcess(String orderNo) throws Exception {
        log.info("完成支付订单    订单号：" + orderNo);
        Order order = orderDao.queryOrder(orderNo);

        /*
         *  1. 判断订单是否存在
         *  2. 判断订单状态是否为未支付
         */
        if (order == null) {
            log.error("订单号对应订单不存在：" + orderNo);
            return;
        }
        else if(order.getOrderStatus() != 1) {
            log.error("订单状态无效: " + orderNo);
            return;
        }

        /*
         *  2. 订单支付完成
         */
        order.setPayTime(new Date());
        // 0  没有可用库存，无效订单
        // 1 以创建等待支付
        // 2 完成支付
        order.setOrderStatus(2);
        orderDao.updateOrder(order);

        /*
         *  3. 发送订单付款成功消息
         */
        rocketMQService.sendMessage("pay_done", JSON.toJSONString(order));
    }
}
