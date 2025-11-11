# Product Service

正常的 Product Service，用于电商系统的产品管理。

## 功能
- 接收 POST 请求创建产品
- 正常返回 201 Created

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

**响应**: 201 Created

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

服务将在 `http://localhost:8080` 启动

### 使用 Maven
```bash
mvn spring-boot:run
```

## 测试
```bash
curl -X POST http://localhost:8080/api/product \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "sku": "TEST123",
    "manufacturer": "Test Corp",
    "categoryId": 1,
    "weight": 100,
    "someOtherId": 1
  }'
```

