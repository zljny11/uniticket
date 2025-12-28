-- ============================================================
-- UniTicket Campus Ticketing System - Test Data Script
-- 项目: 校园票务与信息发布平台
-- Database: uniticket
-- Author: UniTicket Team
-- Date: 2024-12-28
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- Step 1: 清空现有数据 (保留表结构)
-- ============================================================

-- 清空场馆类型数据
TRUNCATE TABLE `tb_shop_type`;

-- 清空场馆数据
TRUNCATE TABLE `tb_shop`;

-- 清空票券数据 (先清空关联表)
TRUNCATE TABLE `tb_seckill_voucher`;
TRUNCATE TABLE `tb_voucher_order`;
TRUNCATE TABLE `tb_voucher`;

-- 清空帖子相关数据
TRUNCATE TABLE `tb_blog_comments`;
TRUNCATE TABLE `tb_blog`;

-- 清空关注数据
TRUNCATE TABLE `tb_follow`;

-- ============================================================
-- Step 2: 插入场馆类型数据 (4大类别)
-- ============================================================

INSERT INTO `tb_shop_type` ([id](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/dto/UserDTO.java#L6-L6), [name](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/entity/Venue.java#L41-L41), [icon](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/dto/UserDTO.java#L8-L8), [sort](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/entity/VenueCategory.java#L51-L51), `create_time`, `update_time`) VALUES
(1, '音乐会/演唱会', '/types/concert.png', 1, NOW(), NOW()),
(2, '学术讲座', '/types/lecture.png', 2, NOW(), NOW()),
(3, '体育赛事', '/types/sports.png', 3, NOW(), NOW()),
(4, '社团活动', '/types/club.png', 4, NOW(), NOW());

-- ============================================================
-- Step 3: 插入场馆数据 (4个校园场馆)
-- ============================================================

INSERT INTO `tb_shop` (
    [id](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/dto/UserDTO.java#L6-L6), [name](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/entity/Venue.java#L41-L41), `type_id`, [images](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/entity/Venue.java#L51-L51), [area](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/entity/Venue.java#L56-L56), [address](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/entity/Venue.java#L61-L61), 
    [x](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/entity/Venue.java#L66-L66), [y](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/entity/Venue.java#L71-L71), `avg_price`, [sold](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/entity/Venue.java#L81-L81), [comments](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/entity/Venue.java#L86-L86), [score](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/entity/Venue.java#L91-L91), 
    `open_hours`, `create_time`, `update_time`
) VALUES
-- 场馆1: University Cultural Centre (音乐会场馆)
(1, 'University Cultural Centre (UCC)', 1, 
    '/imgs/venues/ucc-main.jpg,/imgs/venues/ucc-interior.jpg,/imgs/venues/ucc-stage.jpg',
    'Kent Ridge Campus', 
    '50 Kent Ridge Crescent, Singapore 119279',
    103.773416, 1.303396,
    2500,  -- 平均票价 SGD 25.00
    1250, 350, 45,  -- 已售1250张, 350条评论, 评分4.5
    '09:00-22:00',
    NOW(), NOW()
),

-- 场馆2: School of Computing Auditorium (学术讲座)
(2, 'School of Computing Auditorium', 2,
    '/imgs/venues/soc-aud-main.jpg,/imgs/venues/soc-aud-seats.jpg',
    'Computing Campus',
    'COM1, 13 Computing Drive, Singapore 117417',
    103.773908, 1.294774,
    500,   -- 平均票价 SGD 5.00 (大部分免费讲座)
    3200, 580, 48,  -- 已售3200张, 580条评论, 评分4.8
    '08:00-20:00',
    NOW(), NOW()
),

-- 场馆3: Central Sports Hall (体育场馆)
(3, 'Central Sports Hall', 3,
    '/imgs/venues/sports-hall-main.jpg,/imgs/venues/sports-court.jpg,/imgs/venues/sports-crowd.jpg',
    'University Town',
    '2 Sports Drive 2, Singapore 117548',
    103.772585, 1.304107,
    1000,  -- 平均票价 SGD 10.00
    890, 125, 42,   -- 已售890张, 125条评论, 评分4.2
    '07:00-23:00',
    NOW(), NOW()
),

-- 场馆4: Campus Open Square (社团活动/露天活动)
(4, 'Campus Open Square', 4,
    '/imgs/venues/open-square-day.jpg,/imgs/venues/open-square-night.jpg,/imgs/venues/open-square-event.jpg',
    'Central Campus',
    'University Hall Road, Singapore 119077',
    103.772842, 1.297163,
    800,   -- 平均票价 SGD 8.00
    2100, 450, 44,  -- 已售2100张, 450条评论, 评分4.4
    '00:00-24:00',  -- 全天开放
    NOW(), NOW()
);

-- ============================================================
-- Step 4: 插入票券数据 (混合场景测试)
-- ============================================================

-- 普通票券插入
INSERT INTO `tb_voucher` (
    [id](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/dto/UserDTO.java#L6-L6), `shop_id`, [title](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/entity/Ticket.java#L46-L46), `sub_title`, [rules](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/entity/Ticket.java#L56-L56), 
    `pay_value`, `actual_value`, [type](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/entity/Ticket.java#L71-L71), [status](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/entity/Ticket.java#L76-L76), 
    `create_time`, `update_time`
) VALUES
-- Scenario A: 秒杀票 - 2025 Annual Grand Concert
(1, 1, '2025 Annual Grand Concert', 
    'Featuring: Singapore Symphony Orchestra',
    '仅限在校学生及校友购买 | 需携带学生证/校友卡入场 | 一人限购2张',
    3000, 5000,  -- 秒杀价 SGD 30.00, 原价 SGD 50.00
    1, 1,  -- type=1 (秒杀票), status=1 (可用)
    NOW(), NOW()
),

-- Scenario B: 普通票 - AI Frontiers Lecture
(2, 2, 'AI Frontiers Lecture by Industry Expert',
    'Speaker: Dr. Sarah Chen - Former Google AI Lead',
    '对全校师生开放 | 免费入场，需提前领票 | 凭学生证或工作证入场',
    0, 0,  -- 免费活动
    0, 1,  -- type=0 (普通票), status=1 (可用)
    NOW(), NOW()
),

-- Scenario C: 售罄票 - Basketball Finals
(3, 3, 'NUS vs NTU Final Basketball Match',
    'Annual Inter-University Championship Finals',
    '本场比赛门票已售罄 | 关注我们获取下一场比赛信息',
    1500, 1500,  -- SGD 15.00
    0, 1,  -- type=0 (普通票), status=1 (可用但库存为0)
    NOW(), NOW()
),

-- 额外票券: 社团周末活动
(4, 4, 'Campus Culture Festival - Weekend Pass',
    'Access to 20+ Club Booths & Performances',
    '周末两日通票 | 包含所有表演及工作坊 | 含餐券一张',
    1200, 1800,  -- SGD 12.00, 原价 SGD 18.00
    0, 1,  -- 普通票
    NOW(), NOW()
),

-- 额外票券: 即将开始的音乐会
(5, 1, 'Jazz Night at UCC',
    'Featuring: Student Jazz Ensemble',
    '凭学生证享8折优惠 | 现场提供免费饮料',
    800, 1000,  -- SGD 8.00, 原价 SGD 10.00
    0, 1,
    NOW(), NOW()
);

-- ============================================================
-- Step 5: 插入秒杀票配置 (Scenario A - Flash Sale)
-- ============================================================

INSERT INTO `tb_seckill_voucher` (
    `voucher_id`, [stock](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/entity/Ticket.java#L81-L82), `begin_time`, `end_time`, 
    `create_time`, `update_time`
) VALUES
-- 秒杀票ID=1: 2025 Annual Grand Concert
-- 开始时间: 当前时间+1小时, 结束时间: 当前时间+7天
(1, 100, 
    DATE_ADD(NOW(), INTERVAL 1 HOUR),   -- 1小时后开始抢票
    DATE_ADD(NOW(), INTERVAL 7 DAY),    -- 7天内有效
    NOW(), NOW()
);

-- ============================================================
-- Step 6: 插入校园帖子数据 (5+帖子，包含热门帖)
-- ============================================================

INSERT INTO `tb_blog` (
    [id](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/dto/UserDTO.java#L6-L6), `shop_id`, `user_id`, [title](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/entity/Ticket.java#L46-L46), [images](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/entity/Venue.java#L51-L51), [content](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/entity/CampusPost.java#L85-L85), 
    [liked](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/entity/CampusPost.java#L91-L91), [comments](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/entity/Venue.java#L86-L86), `create_time`, `update_time`
) VALUES
-- 帖子1: 热门帖 - 音乐会评价 (150+ likes)
(1, 1, 1001, 
    '🎵 Last Night\'s Concert Was AMAZING! 🎉',
    '/imgs/blogs/concert-crowd.jpg,/imgs/blogs/concert-stage.jpg,/imgs/blogs/concert-lights.jpg',
    'Just attended the Symphony Orchestra performance at UCC last night and I\'m still blown away! 🤩<br/><br/>
    The acoustics were incredible, and the musicians were top-notch. Special shoutout to the violin solo in Vivaldi\'s Four Seasons - absolutely breathtaking! 🎻<br/><br/>
    Pro tips for future concert-goers:<br/>
    ✅ Arrive 30 mins early to get good seats<br/>
    ✅ The center seats in rows 5-10 have the best sound quality<br/>
    ✅ Dress code is smart casual<br/>
    ✅ FREE parking available at car park A<br/><br/>
    Already bought tickets for next month\'s Jazz Night! Can\'t wait! 🎷',
    156, 23,  -- 156个赞, 23条评论
    DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()
),

-- 帖子2: 讲座分享
(2, 2, 1002,
    '📚 Must-Attend: Dr. Chen\'s AI Lecture This Friday!',
    '/imgs/blogs/ai-lecture-poster.jpg,/imgs/blogs/soc-auditorium.jpg',
    'Reminder: The AI Frontiers lecture is happening THIS FRIDAY at 6 PM! 🚀<br/><br/>
    Dr. Sarah Chen will be sharing insights from her 10 years at Google AI. Topics include:<br/>
    🔹 Latest trends in Large Language Models<br/>
    🔹 Career advice for aspiring AI engineers<br/>
    🔹 Q&A session (bring your questions!)<br/><br/>
    It\'s FREE but tickets are running out fast! Only 50 seats left. Grab yours now! 🎟️<br/><br/>
    See you there! 💡',
    89, 12,
    DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()
),

-- 帖子3: 热门帖 - 篮球赛回顾 (120+ likes)
(3, 3, 1003,
    '🏀 What a Match! NUS vs NTU Finals Recap',
    '/imgs/blogs/basketball-action.jpg,/imgs/blogs/basketball-crowd.jpg,/imgs/blogs/basketball-trophy.jpg',
    'WE WON! NUS takes the championship trophy! 🏆🎉<br/><br/>
    Final score: 78-72. What an intense match! The atmosphere was ELECTRIC! ⚡<br/><br/>
    Highlights:<br/>
    🔥 John Lee\'s incredible 3-pointer in the last minute<br/>
    🔥 Sarah Tan\'s defensive plays were game-changing<br/>
    🔥 The crowd support was insane - thank you all! 📣<br/><br/>
    Unfortunately missed this match? Don\'t worry, semifinals are next month!<br/>
    Follow @nus_sports for updates! 🏀',
    128, 34,  -- 128个赞, 34条评论
    DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()
),

-- 帖子4: 文化节预告
(4, 4, 1004,
    '🎭 Culture Festival is BACK! Weekend Pass Now Available',
    '/imgs/blogs/culture-fest-main.jpg,/imgs/blogs/culture-booth.jpg',
    'Mark your calendars! 📅 Campus Culture Festival returns next weekend! 🎉<br/><br/>
    What to expect:<br/>
    🎪 20+ club booths (Photography, Dance, Music, Anime, etc.)<br/>
    🎤 Live performances every hour<br/>
    🍜 International food stalls (Korean, Japanese, Indian, Western!)<br/>
    🎨 Art workshops (Calligraphy, Painting, Origami)<br/>
    🎮 Gaming zone with PS5 & VR headsets<br/><br/>
    Weekend Pass (2 days) = Only $12! Includes 1 meal voucher! 🍔<br/>
    Single day = $8<br/><br/>
    Trust me, you DON\'T want to miss this! Last year was legendary! 🔥',
    95, 18,
    DATE_SUB(NOW(), INTERVAL 3 HOUR), NOW()
),

-- 帖子5: 热门帖 - 场馆推荐 (110+ likes)
(5, 1, 1005,
    '🎪 Complete Guide to UCC - Best Event Venue on Campus!',
    '/imgs/blogs/ucc-exterior.jpg,/imgs/blogs/ucc-seats.jpg,/imgs/blogs/ucc-night.jpg',
    'As someone who\'s attended 15+ events at UCC, here\'s my ultimate insider guide! 📝<br/><br/>
    🎯 BEST SEATS:<br/>
    - Orchestra (Rows 5-12): Best sound & view, worth the premium<br/>
    - Balcony Front: Great view, slightly cheaper<br/>
    - Avoid: Last 3 rows (sound quality drops significantly)<br/><br/>
    💰 MONEY-SAVING TIPS:<br/>
    - Student discounts available (show matriculation card)<br/>
    - Early bird tickets = 30% off!<br/>
    - Group bookings (5+) get extra 15% off<br/><br/>
    📍 FACILITIES:<br/>
    ✅ Free WiFi (surprisingly fast!)<br/>
    ✅ Wheelchair accessible<br/>
    ✅ Cafe inside (overpriced though 😅)<br/>
    ✅ Clean restrooms<br/>
    ✅ Air-conditioned (bring a light jacket!)<br/><br/>
    🚗 PARKING:<br/>
    Car Park A = FREE after 6 PM on weekends!<br/><br/>
    Overall rating: ⭐⭐⭐⭐⭐ 5/5<br/>
    My favorite venue on campus! 💙',
    112, 28,  -- 112个赞, 28条评论
    DATE_SUB(NOW(), INTERVAL 7 DAY), NOW()
),

-- 帖子6: 普通帖 - 寻找同伴
(6, 2, 1006,
    'Looking for Study Buddy for AI Lecture 🤝',
    '/imgs/blogs/study-group.jpg',
    'Hi! Is anyone else attending the AI lecture on Friday? 🙋‍♀️<br/><br/>
    I\'m a Year 2 CS student interested in machine learning. Would love to find a study buddy to:<br/>
    - Discuss lecture content after the session<br/>
    - Share notes and insights<br/>
    - Maybe form a study group for AI modules?<br/><br/>
    Comment below or DM me if interested! Let\'s learn together! 📚💪',
    15, 7,
    DATE_SUB(NOW(), INTERVAL 6 HOUR), NOW()
),

-- 帖子7: 转票帖子
(7, 1, 1007,
    '🎫 Selling 2x Concert Tickets (Can\'t Attend 😢)',
    '/imgs/blogs/concert-tickets.jpg',
    'Sad news - I have to return home urgently and can\'t attend the Grand Concert on 5th Jan 😭<br/><br/>
    Selling 2 tickets at COST PRICE (paid $30 each, selling at $30)<br/>
    Seats: Row 8, Center section (EXCELLENT view!)<br/><br/>
    Meet-up: SOC or Central Library<br/>
    Payment: PayNow or cash<br/><br/>
    First come first served! DM me ASAP! 🏃‍♂️<br/>
    Please help me out, tickets are non-refundable 🙏',
    42, 15,
    DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()
);

-- ============================================================
-- Step 7: 插入测试订单数据 (可选，用于测试订单系统)
-- ============================================================

-- 示例订单 (可根据需要添加更多)
INSERT INTO `tb_voucher_order` (
    [id](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/dto/UserDTO.java#L6-L6), `user_id`, `voucher_id`, `pay_type`, [status](file:///Users/zljny/Desktop/uniticket/项目/src/main/java/com/uniticket/entity/Ticket.java#L76-L76),
    `create_time`, `pay_time`, `use_time`, `refund_time`, `update_time`
) VALUES
-- 已支付订单
(1001, 1001, 2, 1, 2, 
    DATE_SUB(NOW(), INTERVAL 2 DAY),  -- 2天前下单
    DATE_SUB(NOW(), INTERVAL 2 DAY),  -- 立即支付
    NULL, NULL, NOW()
),
-- 已核销订单
(1002, 1002, 5, 2, 3,
    DATE_SUB(NOW(), INTERVAL 5 DAY),
    DATE_SUB(NOW(), INTERVAL 5 DAY),
    DATE_SUB(NOW(), INTERVAL 1 DAY),  -- 1天前核销
    NULL, NOW()
);

-- ============================================================
-- Step 8: 数据统计输出
-- ============================================================

SELECT '✅ Data Import Summary:' AS '';
SELECT CONCAT('场馆类型: ', COUNT(*), ' 条') AS 'Venue Categories' FROM `tb_shop_type`;
SELECT CONCAT('场馆: ', COUNT(*), ' 条') AS 'Venues' FROM `tb_shop`;
SELECT CONCAT('票券: ', COUNT(*), ' 条') AS 'Tickets' FROM `tb_voucher`;
SELECT CONCAT('秒杀票配置: ', COUNT(*), ' 条') AS 'Flash Sale Configs' FROM `tb_seckill_voucher`;
SELECT CONCAT('校园帖子: ', COUNT(*), ' 条') AS 'Campus Posts' FROM `tb_blog`;
SELECT CONCAT('测试订单: ', COUNT(*), ' 条') AS 'Test Orders' FROM `tb_voucher_order`;

-- ============================================================
-- 完成提示
-- ============================================================

SELECT '🎉 UniTicket Test Data Import Complete!' AS '';
SELECT '📌 Next Steps:' AS '';
SELECT '1️⃣ 重启Spring Boot应用' AS 'Step 1';
SELECT '2️⃣ 测试场馆列表接口: GET /venue/list' AS 'Step 2';
SELECT '3️⃣ 测试秒杀功能: POST /voucher-order/seckill/{id}' AS 'Step 3';
SELECT '4️⃣ 测试热门帖子: GET /blog/hot' AS 'Step 4';
SELECT '' AS '';
SELECT '💡 Tip: 秒杀活动将在1小时后开始，可修改 tb_seckill_voucher.begin_time 提前测试' AS '';

SET FOREIGN_KEY_CHECKS = 1;
