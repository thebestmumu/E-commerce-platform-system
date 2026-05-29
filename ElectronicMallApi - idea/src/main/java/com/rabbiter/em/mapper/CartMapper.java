package com.rabbiter.em.mapper;

import com.rabbiter.em.entity.Cart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface CartMapper extends BaseMapper<Cart> {

    @MapKey("id")
    List<Map<String, Object>> selectByUserId(Long userId);
    
    @Select("SELECT value FROM good_standard WHERE good_id = #{goodId} LIMIT 1")
    String selectFirstStandard(@Param("goodId") Long goodId);
}
