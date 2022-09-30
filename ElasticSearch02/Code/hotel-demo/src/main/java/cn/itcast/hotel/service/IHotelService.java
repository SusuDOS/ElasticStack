package cn.itcast.hotel.service;

import com.baomidou.mybatisplus.extension.service.IService;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;

public interface IHotelService extends IService<Hotel> {
    PageResult search(RequestParams params);
}
