# epubparser
本工程提供了一种安卓中epub电子书的解析方案，利用WebView加载epub.js读取书籍内容，通过AndServer向客户端回传结果
# 项目说明
工程分Android和JS两部分，JS工程位于js_project目录，其中client目录为react项目，负责epub解析核心逻辑及调试页面，server目录用于启动本地服务调试
## Android工程
### Kiter.kt 入口，主要方法如下
- openBook 打开书籍

| 参数 | 说明 | 备注|
|  ----  | ----  | ----|
|context|||
|bookKey|书籍唯一标识|Android端和JS端均使用它做业务处理|
|bookPath|书籍相对路径|相对于getExternalFilesDir所在目录，例：/book/书名.epub|
|bookUnzipPath|书籍解压后相对路径|例：/book/unzip/[bookKey]/|
|receiver|用于接收回调||

- loadChapter 加载章节内容

| 参数 | 说明 | 备注|
|  ----  | ----  | ----|
|context|||
|bookKey|||
|chapterIndex|章节编号||
|receiver|用于接收回调||

### book 书籍相关实体类目录 包含如下类
- Container.kt 对应META-INF/container.xml文件内容 

| 字段 | 说明 | 备注 |
| ---- | ---- | ---- |
| directory|container.xml文件所在目录|相对于解压目录|
|encoding|编码格式||
|pakcagePath|opf文件所在相对路径|相对于解压目录|

- MetaData.kt 对应opf文件中metadata标签内容
- ResourceItem.kt 资源文件实体
- SpineItem.kt 目录项
- OpfPackage.kt 对应整个图书实体 包括基本图书信息、章节目录、资源列表等

