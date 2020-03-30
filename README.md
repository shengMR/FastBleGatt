# FastBleGatt

## 版本更新信息

### v1.0.0

- 正式版提交

## 具体使用：

### 1，引入库

添加JitPack仓库到项目中

项目根 build.gradle

```groovy
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```

添加依赖，app模块中

```groovy
dependencies {
	implementation 'com.github.shengMR:FastBleGatt:v1.0.0'
}
```

### 2，使用

1. 初始化蓝牙管理器

   ```java
   FastBleManager.getInstance().init(this);
   ```

2. 获取一个蓝牙关联器 FastBleGatt，之后所有操作都是对这个蓝牙关联器进行操作

   ```java
   // 参数：device，扫描到的蓝牙BluetoothDevice对象
   FastBleGatt fastBleGatt = FastBleManager.getInstance().with(device);
   ```

3. 通过蓝牙关联器设置回调并开始连接

    ```java
    FastBleManager.getInstance()
      .with(device)
      .setBleCallback(new BleCallback() {
          @Override
          public void onConnecting(BluetoothDevice device) {
             // 设备连接中
          }

          @Override
          public void onConnected(BluetoothDevice device) {
            // 设备连接成功
          }

          @Override
          public void onDeviceReady(BluetoothDevice device) {
            // 设备准备完毕，此时可以发送命令了
          }

          @Override
          public void onDisconnecting(BluetoothDevice device) {
           // 设备断开中
          }

          @Override
          public void onDisconnectByUser(BluetoothDevice device) {
            // 设备主动断开
          }

          @Override
          public void onDisconnected(BluetoothDevice device) {
            // 设备被动断开
          }

          @Override
          public void onConnectTimeout(BluetoothDevice device) {
            // 设备连接超时
          }
        })
      .connect();
    ```

4. 通过蓝牙关联器对指定蓝牙设备进行操作

  - 开启通知

    ```java
    // 创建一条命令
    Request request = Request.newEnableNotifyRequest(
      serviceUUID,  // 服务UUID
      characteristicUuid, // 特征UUID
      new RequestCallback() { // 请求命令回调
        @Override
        public void success(FastBleGatt fastBleGatt, Request request, Object data) {
    			// 发送成功
        }
    
        @Override
        public void error(FastBleGatt fastBleGatt, Request request, String errorMsg) {
    			// 发送失败
        }
    
        @Override
        public boolean timeout(FastBleGatt fastBleGatt, Request request) {
          // 发送超时，如果返回false，则不再继续发送，返回true则重新发送此命令
          return false;
        }
      });
    
    // 对指定设备做操作
    FastBleManager.getInstance()
      .with(device)
      .sendRequest(request);
    ```

  - 写操作

    ```java
    // 创建一条命令
    Request request = Request.newReadRequest(
      serviceUUID,  // 服务UUID
      characteristicUuid, // 特征UUID
      new RequestCallback() { // 请求命令回调
         @Override
         public void success(FastBleGatt fastBleGatt, Request request, Object data) {
    				// 发送成功
         }
    
        @Override
        public void error(FastBleGatt fastBleGatt, Request request, String errorMsg) {
    				// 发送失败
        }
    
        @Override
        public boolean timeout(FastBleGatt fastBleGatt, Request request) {
          // 发送超时，如果返回false，则不再继续发送，返回true则重新发送此命令
          return false;
        }
      });
      
    // 对指定设备做操作
    FastBleManager.getInstance()
      .with(device).sendRequest(request);
    ```

  - 读操作

    ```java
    // 创建一条命令
    Request request = Request.newReadRequest(
      serviceUUID,  // 服务UUID
      characteristicUuid, // 特征UUID
      data, // 需要写的数据
      new RequestCallback() { // 请求命令回调
         @Override
         public void success(FastBleGatt fastBleGatt, Request request, Object data) {
    				// 发送成功
         }
    
        @Override
        public void error(FastBleGatt fastBleGatt, Request request, String errorMsg) {
    				// 发送失败
        }
    
        @Override
        public boolean timeout(FastBleGatt fastBleGatt, Request request) {
          // 发送超时，如果返回false，则不再继续发送，返回true则重新发送此命令
          return false;
        }
      });
      
    // 对指定设备做操作
    FastBleManager.getInstance()
      .with(device).sendRequest(request);
    ```

## 基本概念

### 1，Request消息对象

​	我们要发送一个消息，需要创建一个请求对象，即Request，可用类方法直接构造出来，然后通过蓝牙关联器对指定设备进行发送

```java
Request.newWriteRequest(); // 写操作
Request.newReadRequest(); // 读操作
Request.newMtuRequest(); // 申请MTU操作
Request.newEnableNotifyRequest(); // 开启通知
Request.newDisableNotifyRequest(); // 关闭通知
```

每个Request消息都会要求输入一个RequestCallback对象，用来反馈消息的发送成功与否

#### 消息特性：

* 延时发送

  每一个Request对象都可以设置延时的时间，不过延时的时间相对于上一条命令

  如：request(1)延时200ms，request(2)延时300ms，然后两条一起发送，命令执行如下：

  sendRequest ---- 间隔200ms ---- request(1)发送 ---- 成功/失败 ---- 延时300ms --- request(2)发送

* 消息Tag

  每一个Request对象都可以携带一个Tag变量，用来发送成功失败判断是哪一个包的失败与发送

