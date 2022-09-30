package cn.itcast.hotel.constants;

public class HotelMqConstants {
    // 定义队列和交换机
    public static final String HOTEL_EXCHANGE = "hotel.topic";
    public static final String HOTLE_INSERT_QUEUE = "hotel.insert.queue";
    public static final String HOTLE_DELETE_QUEUE = "hotel.delete.queue";
    public static final String HOTLE_INSERT_KEY = "hotel.insert";
    public static final String HOTLE_DELETE_KEY = "hotel.delete";
}
