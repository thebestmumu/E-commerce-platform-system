package com.rabbiter.em.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rabbiter.em.entity.CategoryIcon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 商品分类图标 Mapper
 */
@Mapper
public interface CategoryIconMapper extends BaseMapper<CategoryIcon> {
    
    /**
     * 根据分类名称查询图标
     */
    CategoryIcon selectByCategoryName(@Param("categoryName") String categoryName);
    
    /**
     * 根据关键词模糊匹配图标
     */
    CategoryIcon selectByKeyword(@Param("keyword") String keyword);
}
