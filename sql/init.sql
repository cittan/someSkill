-- =============================================================
-- OA 员工花名册模块初始化脚本（PostgreSQL）
-- 演示密码统一为 123456（MD5 摘要，生产环境应替换为 BCrypt）
-- =============================================================

CREATE TABLE IF NOT EXISTS department (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE
);

-- RBAC 简化模型：角色集合稳定，直接在用户表存角色，
-- 行级/字段级权限规则收敛在后端 RoleEnum 枚举中统一维护
CREATE TABLE IF NOT EXISTS sys_user (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(64)  NOT NULL,
    role          VARCHAR(32)  NOT NULL,   -- HR / EXECUTIVE / DEPT_ADMIN / ATTENDANCE / EMPLOYEE
    dept_id       BIGINT       NOT NULL
);

CREATE TABLE IF NOT EXISTS employee (
    id            BIGSERIAL PRIMARY KEY,
    emp_no        VARCHAR(32)    NOT NULL UNIQUE,      -- 工号
    name          VARCHAR(64)    NOT NULL,
    dept_id       BIGINT         NOT NULL,
    position      VARCHAR(64),
    email         VARCHAR(128),
    phone         VARCHAR(20),
    id_card       VARCHAR(18),
    bank_card     VARCHAR(32),
    salary        NUMERIC(12, 2),
    attendance_no VARCHAR(32),                         -- 考勤号
    status        VARCHAR(16)    NOT NULL,             -- ACTIVE / RESIGNED / PROBATION
    hire_date     DATE,
    created_at    TIMESTAMP      DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_employee_dept_id ON employee (dept_id);

-- ---------------- 测试数据 ----------------
INSERT INTO department (id, name) VALUES
    (1, '技术部'), (2, '产品部'), (3, '市场部')
ON CONFLICT DO NOTHING;

-- 5 个角色各一个账号，dept_id 用于验证行级权限与跨部门降级
INSERT INTO sys_user (username, password_hash, role, dept_id) VALUES
    ('hr',     'e10adc3949ba59abbe56e057f20f883e', 'HR',          1),
    ('ceo',    'e10adc3949ba59abbe56e057f20f883e', 'EXECUTIVE',   1),
    ('admin1', 'e10adc3949ba59abbe56e057f20f883e', 'DEPT_ADMIN',  1),
    ('attn1',  'e10adc3949ba59abbe56e057f20f883e', 'ATTENDANCE',  1),
    ('emp2',   'e10adc3949ba59abbe56e057f20f883e', 'EMPLOYEE',    2)
ON CONFLICT DO NOTHING;

INSERT INTO employee (emp_no, name, dept_id, position, email, phone, id_card, bank_card, salary, attendance_no, status, hire_date) VALUES
    ('E001', '张伟',   1, 'Java 工程师',   'zhangwei@oa.com',  '13812345678', '110101199001011234', '6222020000123456789', 18000.00, 'ATT-0001', 'ACTIVE',    DATE '2021-03-15'),
    ('E002', '李娜',   1, '前端工程师',    'lina@oa.com',      '15698765432', '310101199202023456', '6222020000987654321', 15000.00, 'ATT-0002', 'ACTIVE',    DATE '2022-07-01'),
    ('E003', '王强',   1, '测试工程师',    'wangqiang@oa.com', '18911223344', '440101198805054321', '6222020000555544443', 12000.00, 'ATT-0003', 'PROBATION', DATE '2024-11-20'),
    ('E004', '赵敏',   2, '产品经理',      'zhaomin@oa.com',   '13755667788', '510101199505056789', '6222020000111122223', 16000.00, 'ATT-0004', 'ACTIVE',    DATE '2023-01-10'),
    ('E005', '陈晨',   2, '产品助理',      'chenchen@oa.com',  '18633445566', '120101200001017890', '6222020000333344445',  9000.00, 'ATT-0005', 'RESIGNED',  DATE '2020-06-08')
ON CONFLICT DO NOTHING;
