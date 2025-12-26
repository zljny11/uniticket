package com.hmdp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.CampusPost;
import com.hmdp.entity.User;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * CampusPost Controller - 校园动态/帖子前端控制器
 * Business Context: Social feature where students share event experiences,
 * trade tickets, review venues, and interact with campus community
 * </p>
 *
 * @author UniTicket Team
 * @since 2024-12-26
 */
@RestController
@RequestMapping("/campus-post")  // New RESTful route
public class CampusPostController {

    @Resource
    private IBlogService campusPostService;  // Will be renamed to ICampusPostService later
    
    @Resource
    private IUserService userService;

    /**
     * 发布校园动态
     * POST /campus-post
     * 
     * @param post 动态内容（标题、图片、正文）
     * @return 动态ID
     */
    @PostMapping
    public Result savePost(@RequestBody CampusPost post) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        post.setUserId(user.getId());
        
        // 保存动态
        campusPostService.save(post);
        
        // 推送到粉丝的 Feed 流 (uniticket:feed:{userId})
        // TODO: Implement feed push logic
        
        return Result.ok(post.getId());
    }

    /**
     * 点赞/取消点赞动态
     * PUT /campus-post/like/{postId}
     * 
     * Uses Redis Set to track likes: uniticket:post:liked:{postId}
     * 
     * @param id 动态ID
     * @return 成功/失败
     */
    @PutMapping("/like/{id}")
    public Result likePost(@PathVariable("id") Long id) {
        // TODO: Implement Redis Set-based like logic
        // 1. Get userId from UserHolder
        // 2. Check if userId exists in Set (SISMEMBER)
        // 3. If exists: remove (SREM) and decrement count
        // 4. If not exists: add (SADD) and increment count
        
        // Temporary implementation (直接修改数据库)
        campusPostService.update()
                .setSql("liked = liked + 1")
                .eq("id", id)
                .update();
        return Result.ok();
    }

    /**
     * 查询我的动态列表
     * GET /campus-post/my?current=1
     * 
     * @param current 页码
     * @return 动态列表
     */
    @GetMapping("/my")
    public Result queryMyPosts(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        
        // 分页查询
        Page<CampusPost> page = campusPostService.query()
                .eq("user_id", user.getId())
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        
        return Result.ok(page.getRecords());
    }

    /**
     * 查询热门动态（按点赞数降序）
     * GET /campus-post/hot?current=1
     * 
     * @param current 页码
     * @return 热门动态列表
     */
    @GetMapping("/hot")
    public Result queryHotPosts(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 分页查询热门动态
        Page<CampusPost> page = campusPostService.query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        
        // 填充用户信息（头像、昵称）
        List<CampusPost> records = page.getRecords();
        records.forEach(post -> {
            Long userId = post.getUserId();
            User user = userService.getById(userId);
            if (user != null) {
                post.setName(user.getNickName());
                post.setIcon(user.getIcon());
            }
        });
        
        return Result.ok(records);
    }
}
