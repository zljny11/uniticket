-- 幂等性修复：为订单表添加唯一索引，防止同一用户重复购买同一票务
-- 执行方式: mysql -u root -p uniticket < add_idempotent_index.sql

USE uniticket;

-- 添加唯一索引 (user_id, voucher_id)
-- 如果存在重复数据，需要先清理重复数据
ALTER TABLE tb_voucher_order
ADD UNIQUE INDEX uk_user_voucher (user_id, voucher_id);