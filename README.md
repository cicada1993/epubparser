# epubparser
本工程提供了一种安卓中epub电子书的解析方案：在子进程中利用WebView加载epub.js读取书籍内容，通过[AndServer](https://github.com/yanzhenjie/AndServer)向客户端回传结果
# 效果演示
目前自己动手实现了如下两种翻页效果，更多翻页效果参考[此项目](https://github.com/GarrettLance/Reader)，该项目的翻页效果可很容易集成到本工程中
## 滑动翻页
![image](https://github.com/cicada1993/epubparser/blob/master/slide.gif)
## 覆盖翻页
![image](https://github.com/cicada1993/epubparser/blob/master/cover.gif)
# 功能演示
![image](https://github.com/cicada1993/epubparser/blob/master/Screenrecorder-2019-11-18-13-36.gif)
# 整体业务流程
![image](https://github.com/cicada1993/epubparser/blob/master/Untitled%20Diagram.png?raw=true)
# 工程说明
工程分Android和JS两部分，JS工程位于js_project目录，其中client目录为react项目，负责epub解析核心逻辑及调试页面，server目录用于启动本地服务调试
## Android工程核心依赖
- [AndServer](https://github.com/yanzhenjie/AndServer) 主要用于提供WebServer服务：图书资源文件服务、图书接口服务
## JS工程核心依赖
- [epub.js ](https://github.com/futurepress/epub.js) 主要用于解析epub电子书
- [postcss](https://github.com/postcss/postcss) 主要用于解析书中的css文件
- [react-html-parser](https://github.com/wrakky/react-html-parser) 主要用于将书中的html文件解析成由ReactNode元素构成的树形解构
## Android部分
### Kiter - 客户端业务入口
因为项目中用到了[AndServer](https://github.com/yanzhenjie/AndServer)这个框架，Android本地图书资源也可以看作远程服务器资源，所以可以统一使用url的方式打开epub书籍
- 方法 **openBook** 打开书籍

| 参数 | 说明 | 备注|
|  ----  | ----  | ----|
|context|||
|bookKey|书籍唯一标识|Android端和JS端均使用它做业务处理|
|bookUrl|书籍地址|本地资源或远程服务器资源地址|
|zipped|是否压缩文件方式打开|epub.js支持以解压目录或压缩文件形式打开书籍|
|receiver|书籍回调||

- 方法 **loadChapter** 加载章节内容

| 参数 | 说明 | 备注|
|  ----  | ----  | ----|
|context|||
|bookKey||
|bookUrl||
|zipped|||
|chapterIndex|章节编号从0开始||
|receiver|章节回调||

- 方法 **initServer** 启动WebServer 端口默认是9696，ip视网络环境而定

## JS部分
Android部分的几个主要目录和JS的基本一致，这里也不详情说明了，需要知道的是，图书的解析及章节内容的加载均在js端进行，图书相关的实体类两端基本一致

### 其他说明
由于整体架构设计比较简单 各个类的功能就不一一说明了，大家可通过代码及下面详细流程去了解

# 解析业务流
![image](https://github.com/cicada1993/epubparser/blob/master/Program.png?raw=true)

# 渲染业务流
Android端从JS端得到的只是非线性html元素，需要经过转换后得到线性页面元素，从而排列渲染
## 实体类转化
### JS端的实体转化
![image](https://github.com/cicada1993/epubparser/blob/master/JS%20trans.png?raw=true)

也就是说Android端得到的是上图转化后的末端数据
### Android端的实体转化
![image](https://github.com/cicada1993/epubparser/blob/master/Android%20trans.png?raw=true)

Android端对html元素转化程页面元素，得到页面数据进而渲染

