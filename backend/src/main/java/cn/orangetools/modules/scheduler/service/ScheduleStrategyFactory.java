package cn.orangetools.modules.scheduler.service;

import cn.orangetools.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author YuHeng
 * @project backend
 * @file ScheduleStrategyFactory
 * @date 2025/12/16 22:07
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Service
public class ScheduleStrategyFactory {

    // Spring 会自动把所有实现了 ScheduleStrategy 的 Bean注入到这个 Map 里
    // Key 是 Bean 的名字 (即 @Component("GREEDY_BALANCE") 里的名字)
    @Autowired
    private Map<String, ScheduleStrategy> strategyMap = new ConcurrentHashMap<>();

    public ScheduleStrategy getStrategy(String strategyName) {
        ScheduleStrategy strategy = strategyMap.get(strategyName);
        if (strategy == null) {
            // 默认回退到负载均衡，或者抛异常
//            return strategyMap.get("GREEDY_BALANCE");
            new ServiceException("无对应算法实现智能排班，请检查上传的配置！");
        }
        return strategy;
    }
}
