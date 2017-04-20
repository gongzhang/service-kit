# ServiceKit 简介

ServiceKit 是一个用来简化构建 Java 服务类应用程序的库。ServiceKit 包含一组工具类，它们能够完成一系列常见的功能，如：
- 程序生命周期管理
- 日志
- 全局线程池
- 原子性的单文件读写
- 异步服务
- 周期性任务
- 事件发布-订阅机制


## 代码样例

### 1. 程序生命周期管理

在主函数中使用 `App` 和 `AppDelegate` 来构造一个持续运行的服务程序：

```java
public static void main(String[] args) {
    App.main(new AppDelegate() {

        @Override
        public void onStart() {
            // your code goes here:
            System.out.println("onStart");
        }

        @Override
        public void onStop() {
            // app is shutting down...
            System.out.println("onStop");
        }

        @Override
        public void onCommand(@NotNull String cmd) {
            // handle arbitrary user command from stdin
            switch (cmd) {
                case "exit":
                    Runtime.getRuntime().exit(0);
                    break;

                default:
                    System.err.println("Unknown command: " + cmd);
                    break;
            }
        }

    });
}
```

ServiceKit 会从标准输入流 `stdin` 读取用户命令，并持续运行，直到输入流结束（`EOF`）或应用程序收到 `SIGINT` (interruption signal) 信号——比如用户在终端中按下了 `Ctrl-C`。

### 2. 日志

ServiceKit 提供一个非阻塞的日志工具类 `Log`。可以使用静态方法来输出程序日志：

```java
Log.i("Main", "Hello!");
Log.w("Main", "This is a warning.");
Log.e("Main", "And this is an error.");
```

日志效果：

```
2017-04-13T16:38:02.392  i  Main  Hello!
2017-04-13T16:38:02.398  w  Main  This is a warning.
2017-04-13T16:38:02.398  e  Main  And this is an error.
```

日志内容将在标准输出流 `stdout` 和错误输出流 `stderr` 打印，同时也会输出到名为 `.log` 的日志文件上。可以通过覆盖 `AppDelegate` 的 `getLogFile()` 方法来自行指定日志文件。

### 3. 全局线程池

ServiceKit 管理了一个全局线程池，可以使用 `ThreadPool` 类来方便地执行异步操作。

```java
ThreadPool.execute(() -> {
    // async code here...
});
```

`ThreadPool` 只是 Java 标准库中 `Executor` 的一个简单封装，但 ServiceKit 会负责在程序结束时销毁它，因此你不需要手动执行销毁操作。

### 4. 原子性的单文件读写



- 异步服务
- 周期性任务
- 事件发布-订阅机制


## 安装

### Maven

```xml
<dependency>
    <groupId>co.gongzh.servicekit</groupId>
    <artifactId>servicekit</artifactId>
    <version>1.0</version>
</dependency>
```

### Gradle

```gradle
compile 'co.gongzh.servicekit:servicekit:1.0'
```
