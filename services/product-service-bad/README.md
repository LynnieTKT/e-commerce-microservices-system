# Product Service Bad

用于测试的 Product Service，**50%概率返回503错误**。

## 功能
- 接收 POST 请求创建产品
- **50%概率返回 503 Service Unavailable**
- 50%概率正常返回 201 Created

## API 端点

### 创建产品
```
POST /api/product
Content-Type: application/json

{
  "productId": 1,
  "sku": "ABC123",
  "manufacturer": "Example Corp",
  "categoryId": 100,
  "weight": 500,
  "someOtherId": 999
}
```

**响应**: 
- 50%概率: 503 Service Unavailable
- 50%概率: 201 Created

### 健康检查
```
GET /api/health
```

**响应**: 200 OK

## 运行方式

### 使用 Docker Compose
```bash
docker-compose up --build
```

服务将在 `http://localhost:8081` 启动（注意端口是8081）

### 使用 Maven
```bash
mvn spring-boot:run
```

## 测试
```bash
# 多次运行查看不同的响应
for i in {1..10}; do
  echo "Request $i:"
  curl -X POST http://localhost:8081/api/product \
    -H "Content-Type: application/json" \
    -d '{
      "productId": 1,
      "sku": "TEST123",
      "manufacturer": "Test Corp",
      "categoryId": 1,
      "weight": 100,
      "someOtherId": 1
    }'
  echo ""
done
```

## 用途
此服务专门用于：
- 测试系统的错误处理能力
- 测试重试机制
- 负载测试时模拟不稳定的服务

