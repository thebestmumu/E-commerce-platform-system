package com.rabbiter.em.service;

import com.rabbiter.em.entity.Cart;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rabbiter.em.mapper.CartMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
public class CartService extends ServiceImpl<CartMapper, Cart> {

    @Resource
    private CartMapper cartMapper;

    public List<Map<String,Object>> selectByUserId(Long userId) {
         return cartMapper.selectByUserId(userId);
    }
    
    /**
     * 获取商品的第一个规格
     * @param goodId 商品 ID
     * @return 第一个规格值
     */
    public String getFirstStandard(Long goodId) {
        return cartMapper.selectFirstStandard(goodId);
    }
}
