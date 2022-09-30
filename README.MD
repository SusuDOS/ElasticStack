# ElasticStack

<font color=red> 版本有极其严格的对应，使用8.4.2版本容器无法启动,7.17.6相对于代码演示的7.12.1，API有巨大改动，且没有在hotel项目中work，回退到7.12.1使用！ </font>

## ElasticSeach

### 安装部署

#### 方式一: 部署单节点es

```bash
# 创建一个docker网络环境
docker network create es-net

# 移除
docker network remove es-net

# 下载ik插件解压内容放在ik目录下，再将ik文件夹整体放到plugins下面.

# 单节点运行es,默认一个g,资源占用有点多.
docker run -d --name es --net es-net \
-p 9200:9200 -p 9300:9300 \
-e "ES_JAVA_OPTS=-Xms4096m -Xmx4096m" \
-e "discovery.type=single-node" \
-v /root/WorkSpace/ElasticStack/config/es/plugins:/usr/share/elasticsearch/plugins \
elasticsearch:7.17.6

# 9200为服务端口.9300为集群之间数据交换端口.
# 版本8.4.2运行，容器正常但是端口打不开服务.
```

- 部署kibana

```bash
docker run -d \
--name kibana \
-e ELASTICSEARCH_HOSTS=http://es:9200 \
--network=es-net \
-p 5601:5601  \
kibana:7.17.6
```

#### 方式二:  `Docker-compose` 部署

- 启动前置服务

```bash
docker-compose up -d
```

- 用hotel演示

```bash
mysql -h 127.0.0.1 -u root -P 3306 -pabc123456
source tb_hotel.sql
```

- 手动起rabbitmq

```bash
docker run --rm  \
 -e RABBITMQ_DEFAULT_USER=itcast \
 -e RABBITMQ_DEFAULT_PASS=abc123456 \
 --name mq \
 --hostname rabbitmq \
 -p 15672:15672 \
 -p 5672:5672 \
 -d rabbitmq:3.10-management
```

## rabbit逻辑

定义rabbitmq的交换机,队列在hotel-demo中,因此需要先启动rabbitmq后,启动hotel-demo,再启动hotel-admin

```bash
# hotel-admin 对数据库修改后,将操作的id发送到mq消息;

# hotel-demo,接受到消息后,对insert/delete队列的消息进行执行会重新调用mysql+es的api更新es中的数据;
```









