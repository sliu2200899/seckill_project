package com.jiuzhang.seckill.demo;

import com.jiuzhang.seckill.services.SeckillActivityService;
import com.jiuzhang.seckill.util.RedisService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class RedisDemoTest {
    @Resource
    private RedisService redisService;

    @Resource
    SeckillActivityService seckillActivityService;

    @Test
    public void setTest() {
        redisService.setValue("age", 100L);
    }

    @Test
    public void getTest() {
        String age = redisService.getValue("age");
        System.out.println(age);
    }

    @Test
    public void stockTest() {
        redisService.setValue("stock:19", 9L);
    }

    @Test
    public void getStockTest() {
        String stock = redisService.getValue("stock:19");
        System.out.println(stock);
    }


    @Test
    public void stockDeductValidatorTest() {
        boolean result = redisService.stockDeductValidator("stock:31");
        System.out.println(result);
        String stock = redisService.getValue("stock:31");
        System.out.println("stock:" + stock);
    }

    @Test
    public void pushSeckillInfoToRedisTest() {
        seckillActivityService.pushSeckillInfoToRedis(19);
    }
}
