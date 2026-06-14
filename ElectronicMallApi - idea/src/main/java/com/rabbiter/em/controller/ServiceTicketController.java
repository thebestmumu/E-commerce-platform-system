package com.rabbiter.em.controller;

import com.rabbiter.em.common.Result;
import com.rabbiter.em.dto.TicketStatsDTO;
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
 * 客服工单管理控制器
 * 用于客服后台管理系统
 */
@Slf4j
@RestController
@RequestMapping("/api/service/ticket")
public class ServiceTicketController {

    @Resource
    private TicketService ticketService;

    /**
     * 客服登录后查看待处理工单
     */
    @GetMapping("/pending")
    public Result getPendingTickets(@RequestHeader("serviceId") Long serviceId) {
        try {
            // 查询所有待处理工单（未分配或分配给当前客服）
            List<Ticket> tickets = ticketService.getUserTickets(null, "pending");
            return Result.ok(tickets);
        } catch (Exception e) {
            log.error("查询待处理工单失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 客服查看已分配给自己的工单
     */
    @GetMapping("/assigned")
    public Result getAssignedTickets(@RequestHeader("serviceId") Long serviceId) {
        try {
            // 这里应该查询分配给当前客服的工单
            // 简化处理：查询所有处理中的工单
            List<Ticket> tickets = ticketService.getUserTickets(null, "processing");
            return Result.ok(tickets);
        } catch (Exception e) {
            log.error("查询已分配工单失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 客服抢单/接单
     */
    @PostMapping("/claim/{ticketId}")
    public Result claimTicket(@PathVariable Long ticketId, @RequestHeader("serviceId") Long serviceId) {
        try {
            ticketService.assignTicket(ticketId, serviceId, serviceId);
            return Result.ok("接单成功");
        } catch (Exception e) {
            log.error("接单失败", e);
            return Result.error("接单失败：" + e.getMessage());
        }
    }

    /**
     * 客服回复工单
     */
    @PostMapping("/reply")
    public Result replyTicket(@RequestBody Map<String, Object> request, 
                        @RequestHeader("serviceId") Long serviceId) {
        try {
            Long ticketId = Long.parseLong(request.get("ticketId").toString());
            String content = (String) request.get("content");
            Boolean isInternal = (Boolean) request.getOrDefault("isInternal", false);

            ticketService.replyTicket(ticketId, serviceId, "service", content, isInternal);
            return Result.ok("回复成功");
        } catch (Exception e) {
            log.error("回复工单失败", e);
            return Result.error("回复失败：" + e.getMessage());
        }
    }

    /**
     * 客服解决工单
     */
    @PostMapping("/resolve")
    public Result resolveTicket(@RequestBody Map<String, Object> request, 
                          @RequestHeader("serviceId") Long serviceId) {
        try {
            Long ticketId = Long.parseLong(request.get("ticketId").toString());
            String remark = (String) request.get("remark");

            ticketService.resolveTicket(ticketId, serviceId, remark);
            return Result.ok("工单已标记为已解决");
        } catch (Exception e) {
            log.error("解决工单失败", e);
            return Result.error("解决失败：" + e.getMessage());
        }
    }

    /**
     * 查看工单详情（含历史记录）
     */
    @GetMapping("/detail/{ticketId}")
    public Result getTicketDetail(@PathVariable Long ticketId) {
        try {
            Ticket ticket = ticketService.getTicketDetail(ticketId);
            if (ticket == null) {
                return Result.error("工单不存在");
            }

            List<TicketHistory> history = ticketService.getTicketHistory(ticketId);

            Map<String, Object> data = new HashMap<>();
            data.put("ticket", ticket);
            data.put("history", history);
            return Result.ok(data);
        } catch (Exception e) {
            log.error("查询工单详情失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 客服查看工单统计
     */
    @GetMapping("/stats")
    public Result getStats(@RequestHeader("serviceId") Long serviceId) {
        try {
            TicketStatsDTO stats = new TicketStatsDTO();
            
            // 查询各状态工单数
            List<Ticket> allTickets = ticketService.list();
            
            int pending = 0, processing = 0, resolved = 0, closed = 0;
            double totalSatisfaction = 0;
            int ratedCount = 0;
            
            for (Ticket ticket : allTickets) {
                switch (ticket.getStatus()) {
                    case "pending":
                        pending++;
                        break;
                    case "processing":
                        processing++;
                        break;
                    case "resolved":
                        resolved++;
                        break;
                    case "closed":
                        closed++;
                        break;
                }
                
                // 统计满意度评分
                if (ticket.getSatisfactionScore() != null) {
                    totalSatisfaction += ticket.getSatisfactionScore();
                    ratedCount++;
                }
            }

            stats.setPendingCount(pending);
            stats.setProcessingCount(processing);
            stats.setResolvedCount(resolved);
            stats.setTotalCount(allTickets.size());
            
            // 实际计算平均满意度
            if (ratedCount > 0) {
                stats.setAvgSatisfactionScore(Math.round(totalSatisfaction / ratedCount * 10.0) / 10.0);
            } else {
                stats.setAvgSatisfactionScore(0.0);
            }

            return Result.ok(stats);
        } catch (Exception e) {
            log.error("查询统计数据失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 客服查看历史工单（已完成的）
     */
    @GetMapping("/history")
    public Result getHistoryTickets(@RequestHeader("serviceId") Long serviceId) {
        try {
            // 查询所有已完成的工单
            List<Ticket> tickets = ticketService.getUserTickets(null, "completed");
            return Result.ok(tickets);
        } catch (Exception e) {
            log.error("查询历史工单失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 客服开始处理工单
     */
    @PostMapping("/process/{ticketId}")
    public Result processTicket(@PathVariable Long ticketId, @RequestHeader("serviceId") Long serviceId) {
        try {
            ticketService.startProcessing(ticketId, serviceId);
            return Result.ok("开始处理");
        } catch (Exception e) {
            log.error("开始处理失败", e);
            return Result.error("处理失败：" + e.getMessage());
        }
    }

    /**
     * 客服完成工单
     */
    @PostMapping("/complete/{ticketId}")
    public Result completeTicket(@PathVariable Long ticketId, @RequestHeader("serviceId") Long serviceId) {
        try {
            ticketService.completeTicket(ticketId, serviceId);
            return Result.ok("工单已完成");
        } catch (Exception e) {
            log.error("完成工单失败", e);
            return Result.error("完成失败：" + e.getMessage());
        }
    }
}
