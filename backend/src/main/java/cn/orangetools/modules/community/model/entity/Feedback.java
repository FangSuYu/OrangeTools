package cn.orangetools.modules.community.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author YuHeng
 * @project backend
 * @file Feedback
 * @date 2025/12/7 14:02
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Data
@TableName("sys_feedback")
public class Feedback {
    private Long id;
    private String type; // suggestion, bug
    private String title;
    private String content;
    private String contact;
    private Boolean isPublicCheck; // 用户是否申请公开
    private Integer status; // 0=待审核, 1=公开...
    private Integer likeCount;
    private Integer dislikeCount;
    private String replyContent;
    private LocalDateTime replyTime;
    private LocalDateTime createTime;
}
