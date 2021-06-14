package com.jiuzhang.seckill.demo;

import com.jiuzhang.seckill.services.ActivityHtmlPageService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class ThymeleafServiceTest {

    @Resource
    private ActivityHtmlPageService activityHtmlPageService;

    @Test
    public void createHtmlTest() {
        activityHtmlPageService.createActivityHtml(19);
    }
}
