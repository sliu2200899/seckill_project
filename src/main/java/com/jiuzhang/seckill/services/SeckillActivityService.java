package com.jiuzhang.seckill.services;

import com.alibaba.fastjson.JSON;
import com.jiuzhang.seckill.db.dao.OrderDao;
import com.jiuzhang.seckill.db.dao.SeckillActivityDao;
import com.jiuzhang.seckill.db.po.Order;
import com.jiuzhang.seckill.db.po.SeckillActivity;
import com.jiuzhang.seckill.mq.RocketMQService;
import com.jiuzhang.seckill.util.RedisService;
import com.jiuzhang.seckill.util.SnowFlake;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

@Service
public class SeckillActivityService {

    @Resource
    private OrderDao orderDao;

    @Resource
    private RedisService redisService;

    @Resource
    private SeckillActivityDao seckillActivityDao;

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
        SeckillActivity activity = seckillActivityDao.querySeckillActivityById(seckillActivityId);
        Order order = new Order();

        // 采用雪花算法生成订单ID
        order.setOrderNo(String.valueOf(snowFlake.nextId()));
        order.setSeckillActivityId(activity.getId());
        order.setUserId(userId);
        order.setOrderAmount(activity.getSeckillPrice().longValue());

        // 发送创建订单消息
        rocketMQService.sendMessage("seckill_order", JSON.toJSONString(order));

        return order;
    }

    public void payOrderProcess(String orderNo) {
        Order order = orderDao.queryOrder(orderNo);
        boolean deductStockResult = seckillActivityDao.deductStock(order.getSeckillActivityId());

        if (deductStockResult) {
            order.setPayTime(new Date());
            // 0  没有可用库存，无效订单
            // 1 以创建等待支付
            // 2 完成支付
            order.setOrderStatus(2);
            orderDao.updateOrder(order);
        }
    }
}
