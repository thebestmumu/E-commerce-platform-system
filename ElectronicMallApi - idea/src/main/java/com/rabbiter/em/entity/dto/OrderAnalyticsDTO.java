package com.rabbiter.em.entity.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class OrderAnalyticsDTO {
    // 年度消费数据
    private Map<Integer, BigDecimal> yearlyConsumption;
    
    // 月度消费数据
    private Map<Integer, BigDecimal> monthlyConsumption;
    
    // 分类消费数据
    private List<CategoryConsumption> categoryConsumption;
    
    // 周消费数据
    private Map<Integer, BigDecimal> weeklyConsumption;
    
    public static class CategoryConsumption {
        private String categoryName;
        private BigDecimal amount;
        private Integer count;
        
        public CategoryConsumption() {}
        
        public CategoryConsumption(String categoryName, BigDecimal amount, Integer count) {
            this.categoryName = categoryName;
            this.amount = amount;
            this.count = count;
        }
        
        public String getCategoryName() {
            return categoryName;
        }
        
        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }
        
        public BigDecimal getAmount() {
            return amount;
        }
        
        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
        
        public Integer getCount() {
            return count;
        }
        
        public void setCount(Integer count) {
            this.count = count;
        }
    }
    
    public Map<Integer, BigDecimal> getYearlyConsumption() {
        return yearlyConsumption;
    }
    
    public void setYearlyConsumption(Map<Integer, BigDecimal> yearlyConsumption) {
        this.yearlyConsumption = yearlyConsumption;
    }
    
    public Map<Integer, BigDecimal> getMonthlyConsumption() {
        return monthlyConsumption;
    }
    
    public void setMonthlyConsumption(Map<Integer, BigDecimal> monthlyConsumption) {
        this.monthlyConsumption = monthlyConsumption;
    }
    
    public List<CategoryConsumption> getCategoryConsumption() {
        return categoryConsumption;
    }
    
    public void setCategoryConsumption(List<CategoryConsumption> categoryConsumption) {
        this.categoryConsumption = categoryConsumption;
    }
    
    public Map<Integer, BigDecimal> getWeeklyConsumption() {
        return weeklyConsumption;
    }
    
    public void setWeeklyConsumption(Map<Integer, BigDecimal> weeklyConsumption) {
        this.weeklyConsumption = weeklyConsumption;
    }
}