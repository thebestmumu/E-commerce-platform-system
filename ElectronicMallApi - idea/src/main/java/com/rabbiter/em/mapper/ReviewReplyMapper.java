package com.rabbiter.em.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rabbiter.em.entity.ReviewReply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 评论回复 Mapper 接口
 */
@Mapper
public interface ReviewReplyMapper extends BaseMapper<ReviewReply> {
    
    /**
     * 查询评论的回复列表（带用户信息）
     */
    List<ReviewReply> selectReplyListByReviewId(@Param("reviewId") Long reviewId,
                                                 @Param("status") Integer status,
                                                 @Param("offset") Integer offset,
                                                 @Param("limit") Integer limit);
    
    /**
     * 查询楼中楼回复（父子结构）
     */
    List<ReviewReply> selectNestedReplies(@Param("reviewId") Long reviewId,
                                          @Param("parentId") Long parentId,
                                          @Param("status") Integer status);
    
    /**
     * 统计评论回复数
     */
    Integer countByReviewId(@Param("reviewId") Long reviewId, @Param("status") Integer status);
    
    /**
     * 批量插入回复
     */
    Integer insertBatch(@Param("list") List<ReviewReply> replies);
}
