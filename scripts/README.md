# 项目脚本

- `dev/`：Windows 本地开发环境使用的 Kafka、Redis 启停与初始化脚本
- `test/`：缓存性能测试和白盒测试脚本

## 注意事项

- Kafka、Redis 脚本目前包含本机安装路径，运行前需要按实际环境修改 `KAFKA_HOME` 和 `REDIS_HOME`。
- 白盒测试脚本会自动定位项目根目录，并通过 Maven Wrapper 执行。
- 缓存性能测试要求 MySQL、Redis 和后端服务已经启动。
