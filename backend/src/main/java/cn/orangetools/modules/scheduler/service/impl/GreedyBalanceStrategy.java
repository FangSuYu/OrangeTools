package cn.orangetools.modules.scheduler.service.impl;

import cn.orangetools.modules.scheduler.dto.ScheduleRequirement;
import cn.orangetools.modules.scheduler.dto.ScheduleResultDTO;
import cn.orangetools.modules.scheduler.dto.StudentCandidate;
import cn.orangetools.modules.scheduler.service.ScheduleStrategy;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author YuHeng
 * @project backend
 * @file GreedyBalanceStrategy
 * @date 2025/12/16 22:05
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Component("GREEDY_BALANCE") // 这里的名字对应前端传来的 strategy 值
public class GreedyBalanceStrategy implements ScheduleStrategy {

    @Override
    public ScheduleResultDTO execute(List<StudentCandidate> students, List<ScheduleRequirement> requirements, int maxPerWeek) {
        ScheduleResultDTO result = new ScheduleResultDTO();
        Map<String, List<StudentCandidate>> solution = new HashMap<>();
        List<String> warnings = new ArrayList<>();

        // 1. 初始化负载计数器 (记录每个人已经排了几个班)
        Map<String, Integer> currentLoad = new HashMap<>();
        for (StudentCandidate s : students) {
            currentLoad.put(s.getId(), 0);
        }

        // 2. 预处理：为了公平，可以将需求列表随机打乱，或者按“难易度”排序
        // 这里暂时按默认顺序处理

        int totalNeeds = 0;

        // 3. 核心循环：填坑
        for (ScheduleRequirement req : requirements) {
            String slotKey = req.getKey();
            int needCount = req.getCount();
            totalNeeds += needCount;

            // 3.1 筛选可用人员
            List<StudentCandidate> available = students.stream()
                    .filter(s -> {
                        // 规则A: 这个时间段他不忙
                        if (s.getBusySlots().contains(slotKey)) return false;
                        // 规则B: 他没超过周最大负载
                        if (currentLoad.get(s.getId()) >= maxPerWeek) return false;
                        return true;
                    })
                    .collect(Collectors.toList());

            // 3.2 排序 (负载均衡的核心！)
            // 策略：优先选负载最少的。如果负载一样，随机选一个(防止死盯着某个人)
            Collections.shuffle(available); // 先打乱，解决同分情况下的公平性
            available.sort(Comparator.comparingInt(s -> currentLoad.get(s.getId())));

            // 3.3 录取 Top N
            List<StudentCandidate> selected = new ArrayList<>();
            if (available.size() < needCount) {
                // 人不够
                warnings.add(String.format("周%d-第%s节 需求%d人，实排%d人 (可用人力不足)",
                        req.getDay(), parseSectionName(req.getSection()), needCount, available.size()));
                selected.addAll(available); // 能排几个是几个
            } else {
                // 人够了，取前 N 个
                selected.addAll(available.subList(0, needCount));
            }

            // 3.4 更新负载和结果
            for (StudentCandidate s : selected) {
                currentLoad.put(s.getId(), currentLoad.get(s.getId()) + 1);
            }
            solution.put(slotKey, selected);
        }

        result.setSolution(solution);
        result.setWarnings(warnings);
        result.setTotalNeeds(totalNeeds);
        return result;
    }

    // 辅助方法：转换节次名称用于显示警告
    private String parseSectionName(int section) {
        if (section == 0) return "早自习";
        if (section == 11) return "午休";
        if (section == 12) return "傍晚";
        if (section == 13) return "晚间";
        return String.valueOf(section);
    }
}
