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
 * @file RandomStrategy
 * @date 2025/12/16 22:27
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Component("RANDOM")
public class RandomStrategy implements ScheduleStrategy {

    @Override
    public ScheduleResultDTO execute(List<StudentCandidate> students, List<ScheduleRequirement> requirements, int maxPerWeek) {
        ScheduleResultDTO result = new ScheduleResultDTO();
        Map<String, List<StudentCandidate>> solution = new HashMap<>();
        List<String> warnings = new ArrayList<>();

        // 记录负载，防止超标
        Map<String, Integer> currentLoad = new HashMap<>();
        for (StudentCandidate s : students) currentLoad.put(s.getId(), 0);

        int totalNeeds = 0;

        for (ScheduleRequirement req : requirements) {
            String slotKey = req.getKey();
            int needCount = req.getCount();
            totalNeeds += needCount;

            // 1. 筛选可用人员
            List<StudentCandidate> available = students.stream()
                    .filter(s -> {
                        if (s.getBusySlots().contains(slotKey)) return false;
                        if (currentLoad.get(s.getId()) >= maxPerWeek) return false;
                        return true;
                    })
                    .collect(Collectors.toList());

            // 2. 核心逻辑：彻底洗牌
            Collections.shuffle(available);

            List<StudentCandidate> selected = new ArrayList<>();
            if (available.size() < needCount) {
                warnings.add(String.format("周%d-第%d节 人力不足", req.getDay(), req.getSection()));
                selected.addAll(available);
            } else {
                selected.addAll(available.subList(0, needCount));
            }

            // 更新负载
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
}
