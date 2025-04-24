# Apate

![License](https://img.shields.io/badge/license-Apache%202.0-blue)  
*A high-efficiency data generation tool for QA testing*

---

## 🚀 Introduction
Apate is designed to **rapidly generate large-scale test data** and **database schema operations** with controllable proportions. It empowers QA teams to:
- Achieve **near 100% test case utilization**
- Reduce manual effort by **1 day per iteration**
- Support diverse scenarios (e-commerce, messaging, etc.) without script modifications

---

## ✨ Key Features
### 🏎️ High-Performance Generation
- Produce **100,000+ DDL operations** in <15 seconds  
- Concurrent data writing with configurable strategies  

### 📊 Real-Time Monitoring
- Live progress tracking via performance dashboards  
- Detailed logging for audit and debugging  

### ⚙️ Flexible Configuration
```yaml
# Example config (config/global.yml)
ddl_ratio: 0.3      # 30% schema operations
batch_size: 5000    # Records per transaction
scenarios:
  - ecommerce
  - social_media
