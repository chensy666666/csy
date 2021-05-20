package com.ruoyi.fangyuanapi.mapper;

import com.ruoyi.system.domain.DbComment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评论Mapper接口
 * 
 * @author fangyuan
 * @date 2020-09-01
 */
public interface DbCommentMapper 
{
    /**
     * 查询评论
     * 
     * @param id 评论ID
     * @return 评论
     */
    public DbComment selectDbCommentById(Long id);

    /**
     * 查询评论列表
     * 
     * @param dbComment 评论
     * @return 评论集合
     */
    public List<DbComment> selectDbCommentList(DbComment dbComment);

    /**
     * 新增评论
     * 
     * @param dbComment 评论
     * @return 结果
     */
    public int insertDbComment(DbComment dbComment);

    /**
     * 修改评论
     * 
     * @param dbComment 评论
     * @return 结果
     */
    public int updateDbComment(DbComment dbComment);

    /**
     * 删除评论
     * 
     * @param id 评论ID
     * @return 结果
     */
    public int deleteDbCommentById(Long id);

    /**
     * 批量删除评论
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteDbCommentByIds(String[] ids);

    /**
     * 通过动态id 查询评论对应的数量
     * @return
     */
    Integer selectDbCommentSumByDynamicId();

    List<DbComment> getCommentList(@Param("dynamicId") Long dynamicId,@Param("currPage") Integer currPage,@Param("pageSize") Integer pageSize);

    /**
     * 查询一级评论对应的二级评论
     * @param id 一级评论id
     * @param currPage
     * @param pageSize
     * @return
     */
    List<DbComment> selectDbCommentByParentCommentId(@Param("id") Long id,@Param("currPage") Integer currPage,@Param("pageSize") Integer pageSize);

}
