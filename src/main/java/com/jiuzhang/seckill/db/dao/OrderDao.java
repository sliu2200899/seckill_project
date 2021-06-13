package com.jiuzhang.seckill.db.dao;

import com.jiuzhang.seckill.db.po.Order;

public interface OrderDao {
    void insertOrder(Order order);

    Order queryOrder(String OrderNo);

    void updateOrder(Order order);
}
