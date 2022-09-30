package cn.itcast.hotel.mq;

import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cn.itcast.hotel.constants.HotelMqConstants;
import cn.itcast.hotel.service.IHotelService;

@Component
public class HotelListener {

    @Autowired
    private IHotelService hotelService;

    @RabbitListener(bindings = @QueueBinding(value = @Queue(name = HotelMqConstants.HOTLE_INSERT_QUEUE), exchange = @Exchange(name = HotelMqConstants.HOTEL_EXCHANGE, type = ExchangeTypes.TOPIC), key = HotelMqConstants.HOTLE_INSERT_KEY))
    public void listenHotelInsert(Long hotelId) {
        // 新增
        hotelService.saveById(hotelId);
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(name = HotelMqConstants.HOTLE_DELETE_QUEUE), exchange = @Exchange(name = HotelMqConstants.HOTEL_EXCHANGE, type = ExchangeTypes.TOPIC), key = HotelMqConstants.HOTLE_DELETE_KEY))
    public void listenHotelDelete(Long hotelId) {
        // 删除
        hotelService.deleteById(hotelId);
    }
}
