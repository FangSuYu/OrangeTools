package cn.orangetools.modules.scheduler.service.impl;

import cn.orangetools.modules.scheduler.dto.ScheduleRequirement;
import cn.orangetools.modules.scheduler.dto.ScheduleResultDTO;
import cn.orangetools.modules.scheduler.dto.StudentCandidate;
import cn.orangetools.modules.scheduler.service.ScheduleStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author YuHeng
 * @project backend
 * @file ConsecutiveStrategy
 * @date 2025/12/16 22:28
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Component("CONSECUTIVE")
@Slf4j
public class ConsecutiveStrategy implements ScheduleStrategy {

    @Override
    public ScheduleResultDTO execute(List<StudentCandidate> students, List<ScheduleRequirement> requirements, int maxPerWeek) {
        log.info("策略执行开始：CONSECUTIVE，学生人数：{}，需求项：{}", students.size(), requirements.size());
        ScheduleResultDTO result = new ScheduleResultDTO();
        Map<String, List<StudentCandidate>> solution = new HashMap<>();
        List<String> warnings = new ArrayList<>();
        Map<String, Integer> currentLoad = new HashMap<>();
        for (StudentCandidate s : students) currentLoad.put(s.getId(), 0);

        // 1. 关键步骤：必须按时间顺序排序需求！
        // 这样处理第2节时，第1节的结果一定已经出来了
        requirements.sort((a, b) -> {
            if (!a.getDay().equals(b.getDay())) return a.getDay() - b.getDay();
            return a.getSection() - b.getSection();
        });

        int totalNeeds = 0;

        for (ScheduleRequirement req : requirements) {
            String slotKey = req.getKey();
            int needCount = req.getCount();
            totalNeeds += needCount;

            // 获取上一节课的 key (简单逻辑：同一天，section - 1)
            // 这里可以根据你的 0, 11, 12, 13 做更复杂的映射，这里简化处理 ID 连续的情况
            String prevSlotKey = req.getDay() + "_" + (req.getSection() - 1);
            List<StudentCandidate> prevWorkers = solution.getOrDefault(prevSlotKey, Collections.emptyList());
            Set<String> prevWorkerIds = prevWorkers.stream().map(StudentCandidate::getId).collect(Collectors.toSet());

            List<StudentCandidate> available = students.stream()
                    .filter(s -> !s.getBusySlots().contains(slotKey) && currentLoad.get(s.getId()) < maxPerWeek)
                    .collect(Collectors.toList());

            // 2. 核心逻辑：排序评分
            available.sort((s1, s2) -> {
                // 因子1：是否是连班 (权重最高)
                boolean s1Consecutive = prevWorkerIds.contains(s1.getId());
                boolean s2Consecutive = prevWorkerIds.contains(s2.getId());
                if (s1Consecutive && !s2Consecutive) return -1; // s1 优先
                if (!s1Consecutive && s2Consecutive) return 1;

                // 因子2：负载均衡 (次要权重)
                return currentLoad.get(s1.getId()) - currentLoad.get(s2.getId());
            });

            List<StudentCandidate> selected = new ArrayList<>();
            if (available.size() < needCount) {
                warnings.add("人力不足: " + slotKey);
                selected.addAll(available);
            } else {
                selected.addAll(available.subList(0, needCount));
            }

            for (StudentCandidate s : selected) {
                currentLoad.put(s.getId(), currentLoad.get(s.getId()) + 1);
            }
            solution.put(slotKey, selected);
        }

        result.setSolution(solution);
        result.setWarnings(warnings);
        result.setTotalNeeds(totalNeeds);
        log.info("策略执行结束：CONSECUTIVE，总需求：{}", totalNeeds);
        return result;
    }
}
