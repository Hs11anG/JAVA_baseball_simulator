-- 選擇或建立資料庫
CREATE DATABASE IF NOT EXISTS BASEBALLJAVAGAME;
USE BASEBALLJAVAGAME;

-- 1. PITCHER 表格 (重新插入內容)
-- 請注意，如果 PITCHER 表已存在且有數據，你可能需要 TRUNCATE TABLE PITCHER; 或 DELETE FROM PITCHER; 先清空舊數據
CREATE TABLE PITCHER (
    PID INT PRIMARY KEY,
    TID INT,
    PNAME VARCHAR(255),
    YEAR INT,
    LR VARCHAR(1),
    STUFF INT,
    VELOCITY INT,
    PTYPE INT
);

INSERT INTO PITCHER (TID, PID, PNAME, YEAR, LR, STUFF, VELOCITY, PTYPE) VALUES
(1, 1, 'SHOHEI OHTANI', 23, 'R', 80, 80, 36),
(1, 2, 'PAUL SKENES', 25, 'R', 80, 80, 22),
(1, 3, 'ZACK WHEELER', 25, 'R', 80, 80, 24),
(2, 4, 'TYLER ROGERS', 25, 'R', 80, 80, -60),
(2, 5, 'MAX FRIED', 25, 'L', 80, 80, 47),
(2, 6, 'TARIK SKUBAL', 25, 'L', 80, 80, 50);

-- 2. BALLTYPE 表格
CREATE TABLE BALLTYPE (
    BID INT PRIMARY KEY,
    BNAME VARCHAR(255)
);

INSERT INTO BALLTYPE (BID, BNAME) VALUES
(1, '4SEAMFAST'),
(2, 'SLIDER'),
(3, 'CURVE'),
(4, 'CHANGE'),
(5, 'SINKER'),
(6, 'SPLIT'),
(7, 'SWEEPER'),
(8, 'CUTTER');

-- 3. TRAJECTORY 表格 (請注意 PID 和 BID 可能是外鍵，但在此處我僅將其作為普通欄位創建)
CREATE TABLE TRAJECTORY (
    PID INT,
    BID INT,
    USEP DECIMAL(5,2),
    HMOV DECIMAL(5,2),
    VMOV DECIMAL(5,2),
    REX DECIMAL(5,2),
    REY DECIMAL(5,2),
    SPEED DECIMAL(5,2),
    PRIMARY KEY (PID, BID) -- 組合主鍵，因為 PID 和 BID 一起唯一標識一條記錄
);

INSERT INTO TRAJECTORY (PID, BID, USEP, HMOV, VMOV, REX, REY, SPEED) VALUES
(1, 7, 35.0, 8.6, -32.4, -2.4, 5.7, 83.8),
(1, 1, 33.0, -2.7, -13.5, -2.2, 5.8, 96.8),
(1, 8, 15.0, 2.0, -25.6, -2.4, 5.7, 88.6),
(1, 6, 6.0, -4.0, -28.0, -2.0, 6.1, 88.6),
(1, 5, 6.0, -8.7, -22.8, -2.3, 5.7, 94.3),
(2, 1, 37.0, -8.4, -15.4, -2.4, 5.7, 98.4),
(2, 5, 20.0, -8.6, -25.7, -2.4, 5.6, 95.1),
(2, 2, 24.0, 5.2, -28.6, -2.4, 5.6, 84.9),
(2, 4, 9.0, -10.1, -27.2, -2.6, 5.6, 88.4),
(3, 1, 41.0, -5.4, -13.5, -2.1, 5.3, 95.8),
(3, 5, 16.0, -9.9, -20.3, -2.1, 5.2, 95.1),
(3, 7, 14.0, 7.1, -31.9, -2.2, 5.2, 84.0),
(3, 6, 10.0, -7.5, -26.7, -2.1, 5.3, 86.9),
(3, 3, 9.0, 6.9, -46.9, -2.1, 5.2, 81.0),
(4, 5, 72.0, -1.0, -48.0, -3.8, 1.2, 83.1),
(4, 2, 28.0, 5.9, -32.6, -3.8, 1.3, 73.9),
(5, 1, 43.0, 1.7, -13.3, 1.3, 6.2, 95.3),
(5, 5, 18.0, 5.9, -23.7, 1.6, 6.0, 93.6),
(5, 3, 16.0, -6.5, -59.9, 1.5, 6.2, 75.4),
(5, 2, 11.0, -9.0, -42.8, 1.8, 5.9, 81.3),
(5, 4, 9.0, 7.5, -31.0, 1.9, 5.8, 84.3),
(6, 4, 30.0, 9.1, -25.5, 1.9, 6.2, 88.5),
(6, 5, 28.0, 7.8, -13.6, 1.7, 6.3, 97.5),
(6, 1, 27.0, 2.4, -10.4, 1.6, 6.4, 97.7),
(6, 3, 13.0, -1.9, -25.6, 1.9, 6.3, 89.9);

ALTER TABLE TRAJECTORY
ADD CONSTRAINT fk_trajectory_pitcher
FOREIGN KEY (PID) REFERENCES PITCHER(PID);

ALTER TABLE TRAJECTORY
ADD CONSTRAINT fk_trajectory_balltype
FOREIGN KEY (BID) REFERENCES BALLTYPE(BID);