# ServiceKit 简介

ServiceKit 是一个用来简化构建 Java 服务类应用程序的库。ServiceKit 包含一组工具类，它们实现了一系列常见的基础功能，包括：
- 程序生命周期管理
- 分页日志
- 全局线程池
- 原子性的文件读写
- 异步后台服务
- 周期性后台任务
- 事件发布-订阅机制

### “KISS” 原则（Keep it Simple and Stupid）

ServiceKit 提供的功能全部使用最简单、直接、有效的方式实现，没有复杂和多余设计。这一特点使 ServiceKit 的行为非常明了，代码量非常小，易于修改和定制。与其它第三方实现相比，ServiceKit 更适合做为一个白盒的**迭代的基础**，而不是一个黑盒的外部依赖。

### ServiceKit 为何包含这些功能？

实践出真知。ServiceKit 集中实现了服务类应用程序中最需要的一组功能，并使用最简单的方式实现了它们。这些功能甚至对于最简单的服务类程序也是必要的，即便有时它们看起来似乎并无直接联系。

你可以试想实现一个驻留后台的小服务程序，譬如：定期从某网站下载特定数据并保存下来。一旦你开始思考这个程序的具体实现，你就会发现 ServiceKit 提供的大部分功能你都会用到。这样一来，你需要关注的东西就是核心业务逻辑了。

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

> 待补充

### 5. 异步后台服务

> 待补充

### 6. 周期性后台任务

> 待补充

### 7. 事件发布-订阅机制

> 待补充

## 安装

你可以直接从 GitHub 下载 ServiceKit 的源代码，也可以通过依赖管理工具获取 ServiceKit：

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
