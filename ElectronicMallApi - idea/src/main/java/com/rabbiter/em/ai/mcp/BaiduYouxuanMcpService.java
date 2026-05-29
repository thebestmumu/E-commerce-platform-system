package com.rabbiter.em.ai.mcp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 百度优选MCP服务 - 真实商品购买
 * 
 * 百度优选MCP是国内首个支持完整交易闭环的电商平台MCP
 * 支持商品检索、知识查询、交易下单等完整购买流程
 * 
 * 接入文档：https://openai.baidu.com/
 * MCP端点：https://mcp-youxuan.baidu.com/mcp/sse?key={连接密钥}
 */
@Component
public class BaiduYouxuanMcpService {

    private static final Logger log = LoggerFactory.getLogger(BaiduYouxuanMcpService.class);

    @Value("${ai.mcp.baidu-youxuan.api-key:}")
    private String apiKey;

    @Value("${ai.mcp.baidu-youxuan.enabled:false}")
    private boolean enabled;

    // MCP SSE 端点
    private static final String MCP_SSE_ENDPOINT = "https://mcp-youxuan.baidu.com/mcp/sse";
    
    // 保存消息端点URL（从第一次SSE连接获取）
    private String messageEndpointUrl;
    
    // 保存SSE连接的BufferedReader（必须在整个连接生命周期内复用）
    private BufferedReader sseReader;
    
    // 保存SSE连接对象
    private HttpURLConnection activeSseConnection;

    /**
     * 搜索百度优选商品
     * 
     * @param keyword 搜索关键词
     * @return 商品列表JSON
     */
    @Tool("搜索百度优选真实商品，根据关键词查找商品列表。用户想买什么东西时调用此工具。返回真实商品ID、名称、价格、图片等信息")
    public String searchYouxuanProducts(
            @P("搜索关键词，比如商品名称或类型，如'手机'、'运动鞋'、'笔记本电脑'") String keyword
    ) {
        if (!enabled) {
            return "{\"error\":\"百度优选MCP未启用，请在application.yml中配置ai.mcp.baidu-youxuan.enabled=true\"}";
        }

        HttpURLConnection sseConn = null;
        try {
            log.info("调用百度优选MCP搜索商品：{}", keyword);

            // 步骤1：建立 SSE 连接（这个连接会一直保持，用于接收所有响应）
            sseConn = createSseConnection();
            if (sseConn == null) {
                return "{\"error\":\"无法连接到百度优选MCP服务器\"}";
            }
            log.info("SSE 连接已建立，消息端点：{}", messageEndpointUrl);

            // 步骤2：初始化 MCP 会话
            if (!initializeMcpSession()) {
                sseConn.disconnect();
                return "{\"error\":\"MCP会话初始化失败\"}";
            }
            log.info("MCP 会话已初始化");

            // 步骤2.5：获取工具列表
            String toolsList = listAvailableTools(sseConn);
            log.info("百度优选MCP可用工具列表：{}", toolsList);

            // 步骤3：调用搜索工具（使用正确的工具名 spu_list）
            String searchResult = callMcpTool(sseConn, "spu_list", keyword);
            
            log.info("百度优选MCP搜索结果：{}", searchResult.substring(0, Math.min(200, searchResult.length())));
            return searchResult;

        } catch (Exception e) {
            log.error("百度优选MCP搜索失败", e);
            return "{\"error\":\"搜索失败：" + e.getMessage() + "\"}";
        } finally {
            if (sseConn != null) {
                sseConn.disconnect();
            }
        }
    }

    /**
     * 步骤1：创建 SSE 连接，获取消息端点
     */
    private HttpURLConnection createSseConnection() {
        try {
            String urlStr = MCP_SSE_ENDPOINT + "?key=" + apiKey;
            log.info("尝试连接 MCP SSE 端点：{}", urlStr);
            
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "text/event-stream");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(120000);

            int responseCode = conn.getResponseCode();
            log.info("MCP SSE 连接响应码：{}", responseCode);

            if (responseCode == 200) {
                // 保存Reader供后续复用
                sseReader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                activeSseConnection = conn;
                
                String line;
                String currentEvent = null;
                long startTime = System.currentTimeMillis();
                
                while ((line = sseReader.readLine()) != null) {
                    log.info("SSE 行：{}", line);
                    
                    if (line.startsWith("event:")) {
                        currentEvent = line.substring(6).trim();
                        log.info("SSE event：{}", currentEvent);
                    } else if (line.startsWith("data:")) {
                        String data = line.substring(5).trim();
                        log.info("SSE data：{}", data);
                        
                        if ("endpoint".equals(currentEvent) && data.startsWith("/")) {
                            URL baseUrl = new URL(MCP_SSE_ENDPOINT);
                            messageEndpointUrl = baseUrl.getProtocol() + "://" + baseUrl.getHost() + data;
                            log.info("获取到 MCP 消息端点：{}", messageEndpointUrl);
                            return conn;
                        }
                    } else if (line.isEmpty()) {
                        currentEvent = null;
                    }
                    
                    if (System.currentTimeMillis() - startTime > 60000) {
                        log.error("等待 endpoint 超时");
                        cleanupSseConnection();
                        return null;
                    }
                }
                cleanupSseConnection();
            } else {
                log.error("MCP SSE 连接失败，状态码：{}", responseCode);
            }
        } catch (Exception e) {
            log.error("连接 MCP 服务器失败", e);
        }
        return null;
    }

    /**
     * 清理SSE连接资源
     */
    private void cleanupSseConnection() {
        try {
            if (sseReader != null) {
                sseReader.close();
                sseReader = null;
            }
            if (activeSseConnection != null) {
                activeSseConnection.disconnect();
                activeSseConnection = null;
            }
        } catch (Exception e) {
            log.error("清理SSE连接异常", e);
        }
    }

    /**
     * 步骤2：初始化 MCP 会话
     */
    private boolean initializeMcpSession() {
        try {
            JSONObject request = new JSONObject();
            request.put("jsonrpc", "2.0");
            request.put("id", UUID.randomUUID().toString());
            request.put("method", "initialize");
            
            JSONObject params = new JSONObject();
            params.put("protocolVersion", "2024-11-05");
            params.put("capabilities", new JSONObject());
            
            JSONObject clientInfo = new JSONObject();
            clientInfo.put("name", "ElectronicMall");
            clientInfo.put("version", "1.0.0");
            params.put("clientInfo", clientInfo);
            
            request.put("params", params);

            return sendMcpRequest(request);
        } catch (Exception e) {
            log.error("初始化 MCP 会话失败", e);
            return false;
        }
    }

    /**
     * 步骤2.5：获取可用工具列表
     */
    private String listAvailableTools(HttpURLConnection sseConn) {
        try {
            JSONObject request = new JSONObject();
            request.put("jsonrpc", "2.0");
            String requestId = UUID.randomUUID().toString();
            request.put("id", requestId);
            request.put("method", "tools/list");
            request.put("params", new JSONObject());

            // POST 请求到消息端点
            URL messageUrl = new URL(messageEndpointUrl);
            HttpURLConnection messageConn = (HttpURLConnection) messageUrl.openConnection();
            messageConn.setRequestMethod("POST");
            messageConn.setRequestProperty("Content-Type", "application/json");
            messageConn.setDoOutput(true);
            messageConn.setConnectTimeout(10000);
            messageConn.setReadTimeout(30000);

            try (java.io.OutputStream os = messageConn.getOutputStream()) {
                os.write(request.toJSONString().getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int messageResponseCode = messageConn.getResponseCode();
            log.info("MCP 工具列表请求响应码：{}", messageResponseCode);
            messageConn.disconnect();
            
            if (messageResponseCode != 200 && messageResponseCode != 202) {
                return "{\"error\":\"MCP 工具列表请求失败，状态码：" + messageResponseCode + "\"}";
            }
            
            // 从已建立的SSE连接读取响应
            return readSseResponse(sseConn, requestId);
            
        } catch (Exception e) {
            log.error("获取工具列表失败", e);
            return "{\"error\":\"获取工具列表失败：" + e.getMessage() + "\"}";
        }
    }

    /**
     * 步骤3：调用 MCP 工具（使用已建立的SSE连接）
     */
    private String callMcpTool(HttpURLConnection sseConn, String toolName, String keyword) {
        try {
            if (messageEndpointUrl == null) {
                return "{\"error\":\"消息端点未初始化\"}";
            }

            // 发送工具调用请求
            JSONObject request = new JSONObject();
            request.put("jsonrpc", "2.0");
            String requestId = UUID.randomUUID().toString();
            request.put("id", requestId);
            request.put("method", "tools/call");
            
            JSONObject params = new JSONObject();
            params.put("name", toolName);
            
            JSONObject arguments = new JSONObject();
            arguments.put("query", keyword);
            params.put("arguments", arguments);
            
            request.put("params", params);

            // POST 请求到消息端点
            URL messageUrl = new URL(messageEndpointUrl);
            HttpURLConnection messageConn = (HttpURLConnection) messageUrl.openConnection();
            messageConn.setRequestMethod("POST");
            messageConn.setRequestProperty("Content-Type", "application/json");
            messageConn.setDoOutput(true);
            messageConn.setConnectTimeout(10000);
            messageConn.setReadTimeout(30000);

            try (OutputStream os = messageConn.getOutputStream()) {
                os.write(request.toJSONString().getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int messageResponseCode = messageConn.getResponseCode();
            log.info("MCP 工具调用请求响应码：{}", messageResponseCode);
            messageConn.disconnect();
            
            if (messageResponseCode != 200 && messageResponseCode != 202) {
                return "{\"error\":\"MCP 工具调用请求失败，状态码：" + messageResponseCode + "\"}";
            }
            
            // 从已建立的SSE连接读取响应
            return readSseResponse(sseConn, requestId);
            
        } catch (Exception e) {
            log.error("调用 MCP 工具失败", e);
            return "{\"error\":\"工具调用失败：" + e.getMessage() + "\"}";
        }
    }

    /**
     * 从SSE连接读取响应（复用已创建的sseReader）
     */
    private String readSseResponse(HttpURLConnection sseConn, String requestId) {
        try {
            if (sseReader == null) {
                return "{\"error\":\"SSE读取器未初始化\"}";
            }
            
            String line;
            String currentEvent = null;
            long startTime = System.currentTimeMillis();
            long timeout = 60000;
            
            while ((line = sseReader.readLine()) != null) {
                if (System.currentTimeMillis() - startTime > timeout) {
                    log.warn("MCP 工具调用超时");
                    break;
                }
                
                log.info("SSE 响应行：{}", line);
                
                if (line.startsWith("event:")) {
                    currentEvent = line.substring(6).trim();
                } else if (line.startsWith("data:")) {
                    String data = line.substring(5).trim();
                    log.info("SSE 响应数据：{}", data);
                    
                    try {
                        JSONObject dataObj = JSON.parseObject(data);
                        
                        // 检查是否是我们的请求的响应
                        if (dataObj.containsKey("id") && requestId.equals(dataObj.getString("id"))) {
                            if (dataObj.containsKey("result")) {
                                JSONObject toolResult = dataObj.getJSONObject("result");
                                if (toolResult.containsKey("content")) {
                                    JSONArray content = toolResult.getJSONArray("content");
                                    if (content != null && !content.isEmpty()) {
                                        JSONObject firstContent = content.getJSONObject(0);
                                        if (firstContent.containsKey("text")) {
                                            return firstContent.getString("text");
                                        }
                                    }
                                }
                                return toolResult.toJSONString();
                            } else if (dataObj.containsKey("error")) {
                                String errorMsg = dataObj.getJSONObject("error").getString("message");
                                return "{\"error\":\"MCP工具调用失败：" + errorMsg + "\"}";
                            }
                        }
                    } catch (Exception e) {
                        log.debug("解析 SSE 数据失败：{}", data);
                    }
                } else if (line.isEmpty()) {
                    currentEvent = null;
                }
            }
            
            return "{\"error\":\"未获取到搜索结果\"}";
        } catch (Exception e) {
            log.error("读取 SSE 响应失败", e);
            return "{\"error\":\"读取响应失败：" + e.getMessage() + "\"}";
        }
    }

    /**
     * 发送 MCP 请求
     */
    private boolean sendMcpRequest(JSONObject request) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(messageEndpointUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(request.toJSONString().getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            log.debug("MCP 请求响应码：{}", responseCode);
            
            return responseCode == 200 || responseCode == 202;
        } catch (Exception e) {
            log.error("发送 MCP 请求失败", e);
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
