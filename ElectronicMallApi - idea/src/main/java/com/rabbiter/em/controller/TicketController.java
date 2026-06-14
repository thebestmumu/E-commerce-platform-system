package com.rabbiter.em.controller;

import com.rabbiter.em.common.Result;
import com.rabbiter.em.entity.Ticket;
import com.rabbiter.em.entity.TicketHistory;
import com.rabbiter.em.service.TicketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工单管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ticket")
public class TicketController {

    @Resource
    private TicketService ticketService;

    /**
     * 创建工单
     */
    @PostMapping("/create")
    public Result createTicket(@RequestBody Map<String, String> request, @RequestHeader("userId") Long userId) {
        try {
            String category = request.get("category");
            String subject = request.get("subject");
            String description = request.get("description");

            if (category == null || subject == null || description == null) {
                return Result.error("缺少必要参数");
            }

            String ticketNo = ticketService.createTicket(userId, category, subject, description);
            Map<String, String> data = new HashMap<>();
            data.put("ticketNo", ticketNo);
            return Result.ok(data, "工单创建成功");
        } catch (Exception e) {
            log.error("创建工单失败", e);
            return Result.error("创建工单失败：" + e.getMessage());
        }
    }

    /**
     * 查询用户的工单列表
     */
    @GetMapping("/list")
    public Result getUserTickets(@RequestHeader("userId") Long userId, 
                           @RequestParam(required = false) String status) {
        try {
            List<Ticket> tickets = ticketService.getUserTickets(userId, status);
            return Result.ok(tickets);
        } catch (Exception e) {
            log.error("查询工单列表失败", e);
            return Result.error("查询工单列表失败：" + e.getMessage());
        }
    }

    /**
     * 查询工单详情
     */
    @GetMapping("/detail/{ticketId}")
    public Result getTicketDetail(@PathVariable Long ticketId) {
        try {
            Ticket ticket = ticketService.getTicketDetail(ticketId);
            if (ticket == null) {
                return Result.error("工单不存在");
            }

            // 查询流转历史
            List<TicketHistory> history = ticketService.getTicketHistory(ticketId);

            Map<String, Object> data = new HashMap<>();
            data.put("ticket", ticket);
            data.put("history", history);
            return Result.ok(data);
        } catch (Exception e) {
            log.error("查询工单详情失败", e);
            return Result.error("查询工单详情失败：" + e.getMessage());
        }
    }

    /**
     * 分配工单给客服（管理员接口）
     */
    @PostMapping("/assign")
    public Result assignTicket(@RequestBody Map<String, Long> request, 
                         @RequestHeader("adminId") Long adminId) {
        try {
            Long ticketId = request.get("ticketId");
            Long serviceId = request.get("serviceId");

            if (ticketId == null || serviceId == null) {
                return Result.error("缺少必要参数");
            }

            ticketService.assignTicket(ticketId, serviceId, adminId);
            return Result.ok("工单分配成功");
        } catch (Exception e) {
            log.error("分配工单失败", e);
            return Result.error("分配工单失败：" + e.getMessage());
        }
    }

    /**
     * 解决工单（客服接口）
     */
    @PostMapping("/resolve")
    public Result resolveTicket(@RequestBody Map<String, Object> request, 
                          @RequestHeader("serviceId") Long serviceId) {
        try {
            Long ticketId = Long.parseLong(request.get("ticketId").toString());
            String remark = (String) request.get("remark");

            ticketService.resolveTicket(ticketId, serviceId, remark);
            return Result.ok("工单已解决");
        } catch (Exception e) {
            log.error("解决工单失败", e);
            return Result.error("解决工单失败：" + e.getMessage());
        }
    }

    /**
     * 关闭工单
     */
    @PostMapping("/close/{ticketId}")
    public Result closeTicket(@PathVariable Long ticketId, @RequestHeader("userId") Long userId) {
        try {
            ticketService.closeTicket(ticketId, userId);
            return Result.ok("工单已关闭");
        } catch (Exception e) {
            log.error("关闭工单失败", e);
            return Result.error("关闭工单失败：" + e.getMessage());
        }
    }

    /**
     * 评价工单
     */
    @PostMapping("/rate")
    public Result rateTicket(@RequestBody Map<String, Object> request, 
                       @RequestHeader("userId") Long userId) {
        try {
            Long ticketId = Long.parseLong(request.get("ticketId").toString());
            Integer score = Integer.parseInt(request.get("score").toString());
            String comment = (String) request.get("comment");

            ticketService.rateTicket(ticketId, score, comment);
            return Result.ok("评价成功");
        } catch (Exception e) {
            log.error("评价工单失败", e);
            return Result.error("评价工单失败：" + e.getMessage());
        }
    }
}
