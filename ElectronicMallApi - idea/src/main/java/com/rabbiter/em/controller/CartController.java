package com.rabbiter.em.controller;

import cn.hutool.core.date.DateUtil;
import com.rabbiter.em.annotation.Authority;
import com.rabbiter.em.common.Result;
import com.rabbiter.em.constants.Constants;
import com.rabbiter.em.entity.AuthorityType;
import com.rabbiter.em.entity.Cart;
import com.rabbiter.em.service.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Authority(AuthorityType.requireLogin)
@RestController
@RequestMapping("/api/cart")
public class CartController {
    private static final Logger log = LoggerFactory.getLogger(CartController.class);
    
    @Resource
    private CartService cartService;

    /*
    查询
    */
    //根据购物车 id 查询
    @GetMapping("/{id}")
    public Result selectById(@PathVariable Long id) {
        return Result.success(cartService.getById(id));
    }
    //查找所有用户的购物车
    @GetMapping
    public Result findAll() {
        List<Cart> list = cartService.list();
        return Result.success(list);
    }
    //查找某个用户的购物车
    @GetMapping("/userid/{userId}")
    public Result selectByUserId(@PathVariable Long userId) {
        return Result.success(cartService.selectByUserId(userId)) ;
    }

    /*
    保存
    */
    @PostMapping
    public Result save(@RequestBody Cart cart) {
        log.info("接收到购物车数据：{}", cart);
        
        // 如果规格为"默认"或空，查询商品的第一个规格
        if (cart.getStandard() == null || "默认".equals(cart.getStandard()) || "".equals(cart.getStandard().trim())) {
            // 查询商品的第一个规格
            String firstStandard = cartService.getFirstStandard(cart.getGoodId());
            if (firstStandard != null && !"".equals(firstStandard)) {
                cart.setStandard(firstStandard);
                log.info("使用商品第一个规格：{}", firstStandard);
            }
        }
        
        cart.setCreateTime(DateUtil.now());
        boolean success = cartService.save(cart);
        log.info("保存结果：{}", success ? "成功" : "失败");
        if (success) {
            return Result.success();
        } else {
            return Result.error(Constants.CODE_500, "保存失败");
        }
    }

    @PutMapping
    public Result update(@RequestBody Cart cart) {
        cartService.updateById(cart);
        return Result.success();
    }

    /*
    删除
    */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        cartService.removeById(id);
        return Result.success();
    }





}
