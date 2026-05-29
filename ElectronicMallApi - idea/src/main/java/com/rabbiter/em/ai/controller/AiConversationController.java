package com.rabbiter.em.ai.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.rabbiter.em.ai.entity.ChatRequest;
import com.rabbiter.em.common.Result;
import com.rabbiter.em.constants.Constants;
import com.rabbiter.em.entity.AiConversation;
import com.rabbiter.em.service.AiConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/**
 * AI 对话历史管理 Controller
 */
@RestController
@RequestMapping("/api/ai/conversations")
public class AiConversationController {

    private static final Logger log = LoggerFactory.getLogger(AiConversationController.class);

    @Resource
    private AiConversationService aiConversationService;

    /**
     * 获取用户的对话列表
     */
    @GetMapping("/list")
    public Result getConversations(@RequestParam Long userId) {
        log.info("获取用户对话列表，用户 ID: {}", userId);
        List<AiConversation> conversations = aiConversationService.getUserConversations(userId);
        return Result.success(conversations);
    }

    /**
     * 创建新对话
     */
    @PostMapping("/create")
    public Result createConversation(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        String title = (String) params.get("title");
        String type = (String) params.get("type");

        log.info("创建新对话，用户 ID: {}, 标题：{}, 类型：{}", userId, title, type);
        AiConversation conversation = aiConversationService.createConversation(userId, title, type);
        return Result.success(conversation);
    }

    /**
     * 获取对话详情
     */
    @GetMapping("/{id}")
    public Result getConversation(@PathVariable Long id) {
        log.info("获取对话详情，对话 ID: {}", id);
        AiConversation conversation = aiConversationService.getConversationById(id);
        if (conversation == null) {
            return Result.error(Constants.CODE_500, "对话不存在");
        }
        return Result.success(conversation);
    }

    /**
     * 更新对话消息
     */
    @PutMapping("/{id}/messages")
    public Result updateMessages(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        log.info("更新对话消息，对话 ID: {}", id);
        
        // 解析消息列表
        Object messagesObj = params.get("messages");
        List<Map<String, Object>> messages;
        if (messagesObj instanceof List) {
            messages = (List<Map<String, Object>>) messagesObj;
        } else if (messagesObj instanceof String) {
            JSONArray jsonArray = JSONUtil.parseArray((String) messagesObj);
            messages = new ArrayList<>();
            for (int i = 0; i < jsonArray.size(); i++) {
                messages.add(jsonMapToObject(jsonArray.getJSONObject(i)));
            }
        } else {
            return Result.error(Constants.CODE_500, "消息格式错误");
        }

        boolean success = aiConversationService.updateConversationMessages(id, messages);
        return success ? Result.success(true) : Result.error(Constants.CODE_500, "更新失败");
    }
    
    // 辅助方法：将 JSONObject 转换为 Map
    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonMapToObject(cn.hutool.json.JSONObject jsonObject) {
        Map<String, Object> map = new HashMap<>();
        for (String key : jsonObject.keySet()) {
            map.put(key, jsonObject.get(key));
        }
        return map;
    }

    /**
     * 添加消息到对话
     */
    @PostMapping("/{id}/messages")
    public Result addMessage(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        log.info("添加消息到对话，对话 ID: {}", id);
        
        String role = (String) params.get("role");
        String content = (String) params.get("content");
        String action = (String) params.get("action");
        Object actionData = params.get("actionData");

        boolean success = aiConversationService.addMessage(id, role, content, action, actionData);
        return success ? Result.success(true) : Result.error(Constants.CODE_500, "添加失败");
    }

    /**
     * 更新对话标题
     */
    @PutMapping("/{id}/title")
    public Result updateTitle(@PathVariable Long id, @RequestBody Map<String, String> params) {
        String title = params.get("title");
        log.info("更新对话标题，对话 ID: {}, 标题：{}", id, title);
        
        boolean success = aiConversationService.updateConversationTitle(id, title);
        return success ? Result.success(true) : Result.error(Constants.CODE_500, "更新失败");
    }

    /**
     * 删除对话
     */
    @DeleteMapping("/{id}")
    public Result deleteConversation(@PathVariable Long id) {
        log.info("删除对话，对话 ID: {}", id);
        boolean success = aiConversationService.deleteConversation(id);
        return success ? Result.success(true) : Result.error(Constants.CODE_500, "删除失败");
    }

    /**
     * 批量删除对话
     */
    @PostMapping("/batch-delete")
    public Result deleteConversations(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        List<Long> ids = (List<Long>) params.get("ids");
        
        log.info("批量删除对话，用户 ID: {}, 对话 IDs: {}", userId, ids);
        boolean success = aiConversationService.deleteConversations(ids, userId);
        return success ? Result.success(true) : Result.error(Constants.CODE_500, "删除失败");
    }

    /**
     * 清空用户的所有对话
     */
    @DeleteMapping("/clear")
    public Result clearConversations(@RequestParam Long userId) {
        log.info("清空用户对话，用户 ID: {}", userId);
        boolean success = aiConversationService.clearUserConversations(userId);
        return success ? Result.success(true) : Result.error(Constants.CODE_500, "清空失败");
    }
}
