# RestClient操作ES

<font size=4 color=red>mysql-connect-java使用8.0.28无法work，使用8.0.23运行OK。</font>

- 参数抽取

ctl+alt+m将选中代码部分抽取为一个方法。

## 查询

- testMatchAll

- testMatch

- testBool(多条件复合型)

## 排序&分页

- testSortAndPage

## 高亮

match到数据将name内容中为搜索值的部分高亮，貌似默认高亮为红色.

- testHighlight


## 启动服务

http://localhost:8089



## REST API

```json
GET 	/device-management/devices       : Get all devices
POST 	/device-management/devices       : Create a new device

GET 	/device-management/devices/{id}   : Get the device information identified by "id"
PUT 	/device-management/devices/{id}   : Update the device information identified by "id"
DELETE	/device-management/devices/{id}   : Delete device by "id"
```