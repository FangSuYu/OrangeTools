package cn.orangetools.modules.scheduler.service.impl;

import cn.orangetools.modules.scheduler.dto.ScheduleRequirement;
import cn.orangetools.modules.scheduler.dto.ScheduleResultDTO;
import cn.orangetools.modules.scheduler.dto.StudentCandidate;
import cn.orangetools.modules.scheduler.service.ScheduleStrategy;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author YuHeng
 * @project backend
 * @file GlobalOptimalStrategy
 * @date 2025/12/16 22:29
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Component("HUNGARIAN") // 对应前端的 value="HUNGARIAN"
public class GlobalOptimalStrategy implements ScheduleStrategy {

    // 迭代次数：次数越多越精确，但越慢。5000次通常在 100ms 内
    private static final int ITERATIONS = 5000;

    @Override
    public ScheduleResultDTO execute(List<StudentCandidate> students, List<ScheduleRequirement> requirements, int maxPerWeek) {

        ScheduleResultDTO bestResult = null;
        double minVariance = Double.MAX_VALUE;

        // 疯狂迭代 N 次，寻找最公平的解
        for (int i = 0; i < ITERATIONS; i++) {
            // 每次迭代必须深拷贝一份数据，或者重新计算
            ScheduleResultDTO currentResult = runRandomizedGreedy(students, requirements, maxPerWeek);

            // 计算这个结果的“不公平度” (方差)
            double variance = calculateVariance(currentResult.getSolution(), students);

            // 擂台赛：谁方差小，谁就是当前的“最优解”
            if (variance < minVariance) {
                minVariance = variance;
                bestResult = currentResult;
                // 如果方差已经为0（绝对公平），直接提前结束
                if (minVariance == 0.0) break;
            }
        }

        return bestResult;
    }

    /**
     * 执行一次随机化的分配
     * (类似贪心，但每次人员顺序是随机的，避免因为 ID 排序导致 ID 靠前的人总是先被选中)
     */
    private ScheduleResultDTO runRandomizedGreedy(List<StudentCandidate> students, List<ScheduleRequirement> reqs, int maxPerWeek) {
        ScheduleResultDTO result = new ScheduleResultDTO();
        Map<String, List<StudentCandidate>> solution = new HashMap<>();
        List<String> warnings = new ArrayList<>();
        Map<String, Integer> currentLoad = new HashMap<>();
        for (StudentCandidate s : students) currentLoad.put(s.getId(), 0);

        int totalNeeds = 0;

        // 随机打乱需求处理顺序 (防止前面的日子总是抢占资源)
        List<ScheduleRequirement> shuffledReqs = new ArrayList<>(reqs);
        Collections.shuffle(shuffledReqs);

        for (ScheduleRequirement req : shuffledReqs) {
            String slotKey = req.getKey();
            int needCount = req.getCount();
            totalNeeds += needCount;

            List<StudentCandidate> available = new ArrayList<>();
            for(StudentCandidate s : students) {
                if (!s.getBusySlots().contains(slotKey) && currentLoad.get(s.getId()) < maxPerWeek) {
                    available.add(s);
                }
            }

            // 关键：完全随机选取，不看负载，纯靠大数定律去撞最优解
            // 或者：也可以在这里加入一点贪心（优先选负载低的），结合随机性，效果更好
            Collections.shuffle(available);

            // 这里我们采用“随机贪心”：先打乱，再稍微排个序(稍微增加点权重)，这样收敛更快
            available.sort(Comparator.comparingInt(s -> currentLoad.get(s.getId())));

            List<StudentCandidate> selected = new ArrayList<>();
            if (available.size() < needCount) {
                warnings.add(String.format("周%d-第%d节 人力不足", req.getDay(), req.getSection()));
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
        return result;
    }

    // 计算排班结果的方差 (越低越公平)
    private double calculateVariance(Map<String, List<StudentCandidate>> solution, List<StudentCandidate> allStudents) {
        Map<String, Integer> counts = new HashMap<>();
        for (StudentCandidate s : allStudents) counts.put(s.getId(), 0);

        for (List<StudentCandidate> list : solution.values()) {
            for (StudentCandidate s : list) {
                counts.put(s.getId(), counts.get(s.getId()) + 1);
            }
        }

        double sum = 0;
        for (int c : counts.values()) sum += c;
        double mean = sum / allStudents.size();

        double temp = 0;
        for (int c : counts.values()) {
            temp += (c - mean) * (c - mean);
        }
        return temp / allStudents.size();
    }
}
