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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 飞猪MCP服务 - 机票/酒店预订
 * 
 * 飞猪是阿里巴巴旗下的旅行平台，提供机票、酒店、火车票等旅行服务
 * 通过飞猪开放平台API，可以实现机票搜索、酒店预订等功能
 * 
 * 接入文档：https://open.fliggy.com/
 */
@Component
public class FliggyMcpService {

    private static final Logger log = LoggerFactory.getLogger(FliggyMcpService.class);

    @Value("${ai.mcp.fliggy.api-key:}")
    private String apiKey;

    @Value("${ai.mcp.fliggy.app-key:}")
    private String appKey;

    @Value("${ai.mcp.fliggy.enabled:false}")
    private boolean enabled;

    private static final String BASE_URL = "https://eco.taobao.com/router/rest";

    /**
     * 搜索飞猪机票
     * 
     * @param departureCity 出发城市
     * @param arrivalCity 到达城市
     * @param departureDate 出发日期（格式：yyyy-MM-dd）
     * @return 机票列表JSON
     */
    @Tool("搜索飞猪机票，查询两个城市之间的航班信息。用户想买机票时调用此工具。返回航班号、价格、时间等信息")
    public String searchFlights(
            @P("出发城市，如'北京'、'上海'、'广州'") String departureCity,
            @P("到达城市，如'深圳'、'成都'、'杭州'") String arrivalCity,
            @P("出发日期，格式为yyyy-MM-dd，如'2025-05-10'") String departureDate
    ) {
        if (!enabled) {
            return "{\"error\":\"飞猪MCP未启用，请在application.yml中配置ai.mcp.fliggy.enabled=true\"}";
        }

        try {
            log.info("调用飞猪MCP搜索机票：{} -> {}, 日期：{}", departureCity, arrivalCity, departureDate);

            // 构建请求参数
            String encodedDeparture = URLEncoder.encode(departureCity, StandardCharsets.UTF_8.name());
            String encodedArrival = URLEncoder.encode(arrivalCity, StandardCharsets.UTF_8.name());
            String encodedDate = URLEncoder.encode(departureDate, StandardCharsets.UTF_8.name());

            String urlStr = String.format(
                    "%s?method=fliggy.search.flight&app_key=%s&departure_city=%s&arrival_city=%s&departure_date=%s",
                    BASE_URL, appKey, encodedDeparture, encodedArrival, encodedDate
            );

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                // 解析响应
                JSONObject result = JSON.parseObject(response.toString());
                if (result.containsKey("fliggy_search_flight_response") &&
                    result.getJSONObject("fliggy_search_flight_response").containsKey("flights")) {
                    JSONArray flights = result.getJSONObject("fliggy_search_flight_response")
                            .getJSONArray("flights");
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("total", flights.size());
                    resultData.put("flights", flights);
                    resultData.put("source", "飞猪");
                    return JSON.toJSONString(resultData);
                }

                return "{\"error\":\"飞猪API返回格式异常\"}";
            } else {
                return "{\"error\":\"飞猪API请求失败，状态码：" + responseCode + "\"}";
            }
        } catch (Exception e) {
            log.error("飞猪机票搜索失败", e);
            return "{\"error\":\"搜索机票失败：" + e.getMessage() + "\"}";
        }
    }

    /**
     * 搜索飞猪酒店
     * 
     * @param city 城市名称
     * @param checkInDate 入住日期（格式：yyyy-MM-dd）
     * @param checkOutDate 退房日期（格式：yyyy-MM-dd）
     * @return 酒店列表JSON
     */
    @Tool("搜索飞猪酒店，查询某个城市的酒店列表。用户想预订酒店时调用此工具。返回酒店名称、价格、评分、地址等信息")
    public String searchHotels(
            @P("城市名称，如'北京'、'上海'、'三亚'") String city,
            @P("入住日期，格式为yyyy-MM-dd，如'2025-05-10'") String checkInDate,
            @P("退房日期，格式为yyyy-MM-dd，如'2025-05-12'") String checkOutDate
    ) {
        if (!enabled) {
            return "{\"error\":\"飞猪MCP未启用\"}";
        }

        try {
            log.info("调用飞猪MCP搜索酒店：{}, 入住：{}, 退房：{}", city, checkInDate, checkOutDate);

            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8.name());
            String encodedCheckIn = URLEncoder.encode(checkInDate, StandardCharsets.UTF_8.name());
            String encodedCheckOut = URLEncoder.encode(checkOutDate, StandardCharsets.UTF_8.name());

            String urlStr = String.format(
                    "%s?method=fliggy.search.hotel&app_key=%s&city=%s&check_in_date=%s&check_out_date=%s",
                    BASE_URL, appKey, encodedCity, encodedCheckIn, encodedCheckOut
            );

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JSONObject result = JSON.parseObject(response.toString());
                if (result.containsKey("fliggy_search_hotel_response") &&
                    result.getJSONObject("fliggy_search_hotel_response").containsKey("hotels")) {
                    JSONArray hotels = result.getJSONObject("fliggy_search_hotel_response")
                            .getJSONArray("hotels");
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("total", hotels.size());
                    resultData.put("hotels", hotels);
                    resultData.put("source", "飞猪");
                    return JSON.toJSONString(resultData);
                }

                return "{\"error\":\"飞猪API返回格式异常\"}";
            } else {
                return "{\"error\":\"飞猪API请求失败，状态码：" + responseCode + "\"}";
            }
        } catch (Exception e) {
            log.error("飞猪酒店搜索失败", e);
            return "{\"error\":\"搜索酒店失败：" + e.getMessage() + "\"}";
        }
    }

    /**
     * 查询飞猪订单
     * 
     * @param orderType 订单类型（flight/hotel）
     * @return 订单列表JSON
     */
    @Tool("查询飞猪订单，查看用户的机票或酒店预订订单。用户想查看自己的旅行订单时调用")
    public String queryFliggyOrders(
            @P("订单类型，'flight'表示机票订单，'hotel'表示酒店订单") String orderType
    ) {
        if (!enabled) {
            return "{\"error\":\"飞猪MCP未启用\"}";
        }

        try {
            log.info("调用飞猪MCP查询订单：{}", orderType);

            String urlStr = String.format(
                    "%s?method=fliggy.query.orders&app_key=%s&order_type=%s",
                    BASE_URL, appKey, orderType
            );

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                return response.toString();
            } else {
                return "{\"error\":\"飞猪API请求失败，状态码：" + responseCode + "\"}";
            }
        } catch (Exception e) {
            log.error("查询飞猪订单失败", e);
            return "{\"error\":\"查询订单失败：" + e.getMessage() + "\"}";
        }
    }

    /**
     * 预订飞猪机票
     * 
     * @param flightId 航班ID
     * @param passengerName 乘客姓名
     * @param passengerIdCard 乘客身份证号
     * @return 预订结果JSON
     */
    @Tool("预订飞猪机票，用户确认要购买机票时调用此工具。需要提供航班ID、乘客姓名和身份证号")
    public String bookFlight(
            @P("航班ID，从机票搜索结果中获取") String flightId,
            @P("乘客姓名") String passengerName,
            @P("乘客身份证号") String passengerIdCard
    ) {
        if (!enabled) {
            return "{\"error\":\"飞猪MCP未启用\"}";
        }

        try {
            log.info("调用飞猪MCP预订机票：航班ID={}, 乘客={}", flightId, passengerName);

            String urlStr = BASE_URL + "?method=fliggy.book.flight&app_key=" + appKey;
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            JSONObject requestBody = new JSONObject();
            requestBody.put("flight_id", flightId);
            requestBody.put("passenger_name", passengerName);
            requestBody.put("passenger_id_card", passengerIdCard);

            conn.getOutputStream().write(requestBody.toJSONString().getBytes(StandardCharsets.UTF_8));

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                return response.toString();
            } else {
                return "{\"error\":\"飞猪预订机票失败，状态码：" + responseCode + "\"}";
            }
        } catch (Exception e) {
            log.error("预订飞猪机票失败", e);
            return "{\"error\":\"预订机票失败：" + e.getMessage() + "\"}";
        }
    }
}
