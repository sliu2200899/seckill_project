package com.jiuzhang.seckill.web;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
public class TestController {

    @ResponseBody
    @RequestMapping("hello")
    public String hello() {
        String result;

        try(Entry entry = SphU.entry("HelloResource")) {
            // 被保护业务的逻辑
            result = "Hello Sentinel";
            return result;
        } catch(BlockException ex) {
            // 资源访问禁止，被限流或被降级
            // 在此处进行相应的处理操作
            log.error(ex.toString());
            result = "系统繁忙稍后再试";
            return result;
        }
    }

    /**
     * 定义限流规则
     * 1.创建存放限流规则的集合
     * 2.创建限流规则
     * 3.将限流规则放到集合中
     * 4.加载限流规则
     * @PostConstruct 当前类的构造函数执行完之后执行
     */
    @PostConstruct
    public void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        FlowRule rule = new FlowRule();
        rule.setResource("seckills");
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(1);

        FlowRule rule2 = new FlowRule();
        rule2.setResource("HelloResource");
        rule2.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(2);

        rules.add(rule);
        rules.add(rule2);

        FlowRuleManager.loadRules(rules);
    }
}
