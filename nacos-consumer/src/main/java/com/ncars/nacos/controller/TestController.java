package com.ncars.nacos.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.exception.NacosException;
import com.ncars.nacos.pojo.dto.UserDto;
import com.ncars.nacos.service.UserService;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Controller
@RequestMapping("/test")
public class TestController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private LoadBalancerClient loadBalancerClient;

    @RequestMapping(value = "/get", method = GET)
    @ResponseBody
    public String get(@RequestParam String serviceName) throws NacosException {
        Map<String, Object> paramMap = new HashMap<>(16);
        paramMap.put("serviceName", "service1");
        URI uri = loadBalancerClient.choose("nacos-demo-provider").getUri();

//        String forObject = restTemplate.getForObject("http://nacos-demo-provider/discovery/get", String.class);
        String forObject = restTemplate.getForObject(uri+"/discovery/get?serviceName="+serviceName, String.class);
        return forObject;
    }

    @DubboReference
    private UserService userService;

    @HystrixCommand(
            groupKey = "timeline-group-rcmd",
            fallbackMethod = "findNameFallback",
            commandProperties = {
                    @HystrixProperty(name="execution.isolation.strategy", value="SEMAPHORE"), // ??????????????????????????????????????????ThreadLocal
                    @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "100"), //????????????
                    @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value="50"),//??????????????????????????????
                    @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value="30"),//?????????????????????????????????
                    @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds", value="3000"),//?????????????????????
                    @HystrixProperty(name = "execution.isolation.semaphore.maxConcurrentRequests", value="300"),// ??????????????????
                    @HystrixProperty(name = "fallback.isolation.semaphore.maxConcurrentRequests", value="100")// fallback??????????????????
            }
    )
//    @HystrixCommand(fallbackMethod = "findNameFallback")
    @GetMapping("/dd")
    @ResponseBody
    public String findName(){
        String name = userService.findName();
        return name;
    }

    String findNameFallback(){
        return "?????????";
    }


    @Value("${rm.name}")
    private String rmName;

    //???????????????????????????
    @Autowired
    private ConfigurableApplicationContext config;

    @GetMapping("/config")
    @ResponseBody
    public String config(){
        String property = config.getEnvironment().getProperty("rm.name");
        return property;
    }


    @GetMapping("/findUser")
    @ResponseBody
    public String findUser(){
        UserDto user = userService.findById(1);
        return JSON.toJSONString(user);
    }
}
