package com.uniticket.service.impl;

import com.uniticket.entity.CampusPost;
import com.uniticket.mapper.CampusPostMapper;
import com.uniticket.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * CampusPost Service Implementation - 校园动态服务实现类
 * Legacy name (BlogServiceImpl) retained for compatibility
 * Maps to CampusPost entity and CampusPostMapper
 * </p>
 *
 * @author UniTicket Team
 * @since 2024-12-26
 */
@Service
public class BlogServiceImpl extends ServiceImpl<CampusPostMapper, CampusPost> implements IBlogService {

}
