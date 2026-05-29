package com.rabbiter.em.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rabbiter.em.constants.Constants;
import com.rabbiter.em.entity.Good;
import com.rabbiter.em.entity.GoodStandard;
import com.rabbiter.em.entity.dto.GoodDTO;
import com.rabbiter.em.exception.ServiceException;
import com.rabbiter.em.mapper.GoodMapper;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.rabbiter.em.constants.RedisConstants.GOOD_TOKEN_KEY;
import static com.rabbiter.em.constants.RedisConstants.GOOD_TOKEN_TTL;

@Service
public class GoodService extends ServiceImpl<GoodMapper, Good> {

    @Resource
    private GoodMapper goodMapper;
    @Resource
    private RedisTemplate<String, Good> redisTemplate;

    //查询一个商品的信息
    public Good getGoodById(Long id) {
        String redisKey = GOOD_TOKEN_KEY + id;
        //从 redis 中查，若有则返回
        ValueOperations<String, Good> valueOperations = redisTemplate.opsForValue();
        Good redisGood = valueOperations.get(redisKey);
        if(redisGood!=null){
            redisTemplate.expire(redisKey,GOOD_TOKEN_TTL, TimeUnit.MINUTES);
            return redisGood;
        }
        //若 redis 中没有则去数据库查
        LambdaQueryWrapper<Good> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Good::getIsDelete,false);
        queryWrapper.eq(Good::getId,id);
        Good dbGood = getOne(queryWrapper);
        if(dbGood!=null){
            //查询商品的规格列表
            List<GoodStandard> standards = goodMapper.getStandardById(id.intValue());
            if(standards != null && !standards.isEmpty()){
                List<String> standardList = standards.stream()
                    .map(GoodStandard::getValue)
                    .collect(java.util.stream.Collectors.toList());
                dbGood.setStandardList(standardList);
                //设置最低价格
                BigDecimal minPrice = getMinPrice(id);
                dbGood.setPrice(minPrice);
            }
            //将商品信息存入 redis
            valueOperations.set(redisKey,dbGood);
            redisTemplate.expire(redisKey,GOOD_TOKEN_TTL, TimeUnit.MINUTES);
            return dbGood;
        }
        //数据库中没有则返回异常
        throw new ServiceException(Constants.NO_RESULT,"无结果");

    }
    //查询商品的规格
    public String getStandard(int id){
        List<GoodStandard> standards = goodMapper.getStandardById(id);
        if(standards.size()==0){
            throw new ServiceException(Constants.NO_RESULT,"无结果");
        }
        return JSON.toJSONString(standards);
    }
    //查询某商品的最低规格价
    public BigDecimal getMinPrice(Long id){
        return goodMapper.getMinPrice(id);
    }
    //查询全部（首页推荐商品）
    public List<GoodDTO> findFrontGoods() {
        return goodMapper.findFrontGoods();
    }


    //假删除
    public void deleteGood(Long id) {
        redisTemplate.delete(GOOD_TOKEN_KEY+id);
        goodMapper.fakeDelete(id);
    }
    //保存商品信息
    public Long saveOrUpdateGood(Good good) {
        System.out.println(good);
        if(good.getId()==null){
            DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            good.setCreateTime(df.format(LocalDateTime.now()));
            goodMapper.insertGood(good);
        }else{
            saveOrUpdate(good);
            redisTemplate.delete(GOOD_TOKEN_KEY + good.getId());
        }
        return good.getId();
    }

    public boolean setRecommend(Long id,Boolean isRecommend) {
        LambdaUpdateWrapper<Good> goodsLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        goodsLambdaUpdateWrapper.eq(Good::getId,id)
                .set(Good::getRecommend,isRecommend);
        return update(goodsLambdaUpdateWrapper);
    }

    public List<Good> getSaleRank(int num) {
        List<Good> goods = goodMapper.getSaleRank(num);
        //为每个商品计算价格
        for (Good good : goods) {
            BigDecimal minPrice = getMinPrice(good.getId());
            good.setPrice(minPrice);
        }
        return goods;
    }


    public void update(Good good) {
        updateById(good);
        redisTemplate.delete(GOOD_TOKEN_KEY + good.getId());
    }
    //分页查询
    public IPage<GoodDTO> findPage(Integer pageNum, Integer pageSize, String searchText, Integer categoryId, String sortBy) {
        LambdaQueryWrapper<Good> query = Wrappers.<Good>lambdaQuery();
        
        //对名称和描述进行模糊查询
        if (StrUtil.isNotBlank(searchText)) {
            query.like(Good::getName, searchText).or().like(Good::getDescription,searchText).or().eq(Good::getId,searchText);
        }
        if(categoryId != null){
            query.eq(Good::getCategoryId,categoryId);
        }
        //筛除掉已被删除的商品
        query.eq(Good::getIsDelete,false);
        
        //根据排序类型添加排序规则
        if ("sales".equals(sortBy)) {
            query.orderByDesc(Good::getSales);
        } else if ("price".equals(sortBy)) {
            //价格不是数据库字段，需要用子查询从good_standard表获取最低价格
            query.last("ORDER BY (SELECT MIN(price) FROM good_standard WHERE good_standard.good_id = good.id) ASC");
        } else if ("rating".equals(sortBy)) {
            //好评榜：评论平均分占70%，评论数目占30%
            query.last("ORDER BY (COALESCE(good_rating, 0) * 0.7 + COALESCE(review_count, 0) * 0.3) DESC");
        } else {
            //综合排序：销量、好评、价格各占三分之一
            //销量越高越好，好评越高越好，价格越低越好
            query.last("ORDER BY (COALESCE(sales, 0) * 0.333 + (COALESCE(good_rating, 0) * 0.7 + COALESCE(review_count, 0) * 0.3) * 0.333 + (10000 - COALESCE((SELECT MIN(price) FROM good_standard WHERE good_standard.good_id = good.id), 0)) * 0.334) DESC");
        }
        
        IPage<Good> page = this.page(new Page<>(pageNum, pageSize), query);
        //把good转为dto
        IPage<GoodDTO> goodDTOPage = page.convert(good -> {
            GoodDTO goodDTO = new GoodDTO();
            BeanUtil.copyProperties(good, goodDTO);
            return goodDTO;
        });
        for (GoodDTO good : goodDTOPage.getRecords()) {
            //附上最低价格
            BigDecimal minPrice = getMinPrice(good.getId());
            good.setPrice(minPrice);
        }
        return goodDTOPage;
    }
    public IPage<Good> findFullPage(Integer pageNum, Integer pageSize, String searchText, Integer categoryId, String sortBy) {
        LambdaQueryWrapper<Good> query = Wrappers.<Good>lambdaQuery();
        
        //对名称和描述进行模糊查询
        if (StrUtil.isNotBlank(searchText)) {
            query.like(Good::getName, searchText).or().like(Good::getDescription,searchText).or().eq(Good::getId,searchText);
        }
        if(categoryId != null){
            query.eq(Good::getCategoryId,categoryId);
        }
        //筛除掉已被删除的商品
        query.eq(Good::getIsDelete,false);
        
        //根据排序类型添加排序规则
        if ("sales".equals(sortBy)) {
            query.orderByDesc(Good::getSales);
        } else if ("price".equals(sortBy)) {
            //价格不是数据库字段，需要用子查询从good_standard表获取最低价格
            query.last("ORDER BY (SELECT MIN(price) FROM good_standard WHERE good_standard.good_id = good.id) ASC");
        } else if ("rating".equals(sortBy)) {
            //好评榜：评论平均分占70%，评论数目占30%
            query.last("ORDER BY (COALESCE(good_rating, 0) * 0.7 + COALESCE(review_count, 0) * 0.3) DESC");
        } else {
            //综合排序：销量、好评、价格各占三分之一
            //销量越高越好，好评越高越好，价格越低越好
            query.last("ORDER BY (COALESCE(sales, 0) * 0.333 + (COALESCE(good_rating, 0) * 0.7 + COALESCE(review_count, 0) * 0.3) * 0.333 + (10000 - COALESCE((SELECT MIN(price) FROM good_standard WHERE good_standard.good_id = good.id), 0)) * 0.334) DESC");
        }
        
        IPage<Good> page = this.page(new Page<>(pageNum, pageSize), query);
        for (Good good : page.getRecords()) {
            //附上最低价格
            BigDecimal minPrice = getMinPrice(good.getId());
            good.setPrice(minPrice);
        }
        return page;
    }

    /**
     * 获取所有商品的发货地址数据，用于全球地图展示
     * @return 发货地址列表，包含城市、坐标和商品数量
     */
    public List<Map<String, Object>> getDeliveryAddresses() {
        LambdaQueryWrapper<Good> query = Wrappers.<Good>lambdaQuery();
        query.eq(Good::getIsDelete, false)
             .isNotNull(Good::getDeliveryAddress)
             .ne(Good::getDeliveryAddress, "");
        
        List<Good> goods = list(query);
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 城市坐标映射（简化版，实际应使用地理编码API）
        Map<String, double[]> cityCoordinates = new HashMap<>();
        cityCoordinates.put("北京", new double[]{116.4074, 39.9042});
        cityCoordinates.put("上海", new double[]{121.4737, 31.2304});
        cityCoordinates.put("广州", new double[]{113.2644, 23.1291});
        cityCoordinates.put("深圳", new double[]{114.0579, 22.5431});
        cityCoordinates.put("杭州", new double[]{120.1551, 30.2741});
        cityCoordinates.put("成都", new double[]{104.0665, 30.5728});
        cityCoordinates.put("武汉", new double[]{114.3054, 30.5931});
        cityCoordinates.put("西安", new double[]{108.9398, 34.3416});
        cityCoordinates.put("南京", new double[]{118.7969, 32.0603});
        cityCoordinates.put("重庆", new double[]{106.5516, 29.5630});
        cityCoordinates.put("天津", new double[]{117.2009, 39.0842});
        cityCoordinates.put("苏州", new double[]{120.5853, 31.2989});
        cityCoordinates.put("长沙", new double[]{112.9388, 28.2282});
        cityCoordinates.put("郑州", new double[]{113.6253, 34.7466});
        cityCoordinates.put("济南", new double[]{117.0249, 36.6827});
        cityCoordinates.put("青岛", new double[]{120.3826, 36.0671});
        cityCoordinates.put("大连", new double[]{121.6147, 38.9140});
        cityCoordinates.put("厦门", new double[]{118.0894, 24.4798});
        cityCoordinates.put("福州", new double[]{119.2965, 26.0745});
        cityCoordinates.put("合肥", new double[]{117.2272, 31.8206});
        cityCoordinates.put("南昌", new double[]{115.8581, 28.6829});
        cityCoordinates.put("昆明", new double[]{102.8329, 25.0406});
        cityCoordinates.put("贵阳", new double[]{106.6302, 26.6470});
        cityCoordinates.put("南宁", new double[]{108.3665, 22.8170});
        cityCoordinates.put("哈尔滨", new double[]{126.5349, 45.8038});
        cityCoordinates.put("长春", new double[]{125.3235, 43.8171});
        cityCoordinates.put("沈阳", new double[]{123.4315, 41.8057});
        cityCoordinates.put("石家庄", new double[]{114.5149, 38.0428});
        cityCoordinates.put("太原", new double[]{112.5489, 37.8706});
        cityCoordinates.put("兰州", new double[]{103.8343, 36.0611});
        cityCoordinates.put("银川", new double[]{106.2309, 38.4872});
        cityCoordinates.put("西宁", new double[]{101.7782, 36.6171});
        cityCoordinates.put("乌鲁木齐", new double[]{87.6168, 43.8256});
        cityCoordinates.put("拉萨", new double[]{91.1409, 29.6456});
        cityCoordinates.put("呼和浩特", new double[]{111.7491, 40.8424});
        
        // 城市到省份的映射
        Map<String, String> cityProvinceMap = new HashMap<>();
        cityProvinceMap.put("北京", "北京市");
        cityProvinceMap.put("上海", "上海市");
        cityProvinceMap.put("天津", "天津市");
        cityProvinceMap.put("重庆", "重庆市");
        cityProvinceMap.put("广州", "广东省");
        cityProvinceMap.put("深圳", "广东省");
        cityProvinceMap.put("东莞", "广东省");
        cityProvinceMap.put("佛山", "广东省");
        cityProvinceMap.put("杭州", "浙江省");
        cityProvinceMap.put("宁波", "浙江省");
        cityProvinceMap.put("温州", "浙江省");
        cityProvinceMap.put("南京", "江苏省");
        cityProvinceMap.put("苏州", "江苏省");
        cityProvinceMap.put("无锡", "江苏省");
        cityProvinceMap.put("成都", "四川省");
        cityProvinceMap.put("武汉", "湖北省");
        cityProvinceMap.put("西安", "陕西省");
        cityProvinceMap.put("长沙", "湖南省");
        cityProvinceMap.put("郑州", "河南省");
        cityProvinceMap.put("济南", "山东省");
        cityProvinceMap.put("青岛", "山东省");
        cityProvinceMap.put("大连", "辽宁省");
        cityProvinceMap.put("沈阳", "辽宁省");
        cityProvinceMap.put("厦门", "福建省");
        cityProvinceMap.put("福州", "福建省");
        cityProvinceMap.put("合肥", "安徽省");
        cityProvinceMap.put("南昌", "江西省");
        cityProvinceMap.put("昆明", "云南省");
        cityProvinceMap.put("贵阳", "贵州省");
        cityProvinceMap.put("南宁", "广西壮族自治区");
        cityProvinceMap.put("哈尔滨", "黑龙江省");
        cityProvinceMap.put("长春", "吉林省");
        cityProvinceMap.put("石家庄", "河北省");
        cityProvinceMap.put("太原", "山西省");
        cityProvinceMap.put("兰州", "甘肃省");
        cityProvinceMap.put("银川", "宁夏回族自治区");
        cityProvinceMap.put("西宁", "青海省");
        cityProvinceMap.put("乌鲁木齐", "新疆维吾尔自治区");
        cityProvinceMap.put("拉萨", "西藏自治区");
        cityProvinceMap.put("呼和浩特", "内蒙古自治区");
        
        for (Good good : goods) {
            String address = good.getDeliveryAddress();
            if (address != null && !address.isEmpty()) {
                // 尝试从地址中提取城市
                String city = extractCity(address);
                if (city != null && cityCoordinates.containsKey(city)) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("city", city);
                    item.put("province", cityProvinceMap.get(city));
                    item.put("address", address);
                    item.put("count", 1);
                    item.put("coordinates", cityCoordinates.get(city));
                    result.add(item);
                }
            }
        }
        
        return result;
    }

    /**
     * 从地址字符串中提取城市名
     * @param address 地址字符串
     * @return 城市名
     */
    private String extractCity(String address) {
        if (address == null || address.isEmpty()) {
            return null;
        }
        
        // 常见城市列表
        String[] cities = {
            "北京", "上海", "广州", "深圳", "杭州", "成都", "武汉", "西安", "南京", "重庆",
            "天津", "苏州", "长沙", "郑州", "济南", "青岛", "大连", "厦门", "福州", "合肥",
            "南昌", "昆明", "贵阳", "南宁", "哈尔滨", "长春", "沈阳", "石家庄", "太原",
            "兰州", "银川", "西宁", "乌鲁木齐", "拉萨", "呼和浩特"
        };
        
        for (String city : cities) {
            if (address.contains(city)) {
                return city;
            }
        }
        
        // 尝试匹配 "XX省XX市" 格式
        Pattern pattern = Pattern.compile("([^省]+省|.+自治区)?([^市]+市)");
        Matcher matcher = pattern.matcher(address);
        if (matcher.find()) {
            return matcher.group(2).replace("市", "");
        }
        
        return null;
    }
}
