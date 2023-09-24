## 欢迎使用 获取ID3标签 小工具

### [English](README_ENG.md)

### 本工具主要用来配合 [椒盐歌单转换助手](https://github.com/Winnie0408/SaltPlayerConverter) 使用，当然也可独立使用。

## 使用方法

1. 打开**获取ID3标签**应用程序。
2. 点击**选择目录**按钮。
3. 按照软件提示授予权限：
    * Android 11及以上：授予 **所有文件管理权限** `android.permission.MANAGE_EXTERNAL_STORAGE`
    * Android 10及以下：授予 **读写外置存储权限** `android.permission.READ_EXTERNAL_STORAGE`
      与`android.permission.WRITE_EXTERNAL_STORAGE`
4. 按照软件提示，选择您设备中存放音乐文件的目录，选择方法如下图所示：<br>
   **注意图片中第一行显示的当前父目录**

| 音乐文件存放类型 | 在同一个目录中                                |
|----------|----------------------------------------|
| 目录树示例    | ![img.png](markdownResources/img1.png) | 
| 操作方法     | 直接选择到父目录`Music`即可                      | 

| 音乐文件存放类型 | 在同一目录的子目录中                             |
|----------|----------------------------------------|
| 目录树示例    | ![img.png](markdownResources/img2.png) |
| 操作方法     | 直接选择到父目录`Music`即可                      |

| 音乐文件存放类型 | 在不同目录中                                                               |
|----------|----------------------------------------------------------------------|
| 目录树示例    | ![img.png](markdownResources/img3.png)                               |
| 操作方法     | 需要进行多次选择操作，每次选择**一个**存放音乐的目录(如图中的`中文歌曲`、`temp`)，并在接下来的冲突对话框中选择**追加** |

5. 等待音乐ID3信息导出完成。
6. 前往`/storage/emulated/0/Download`(`即/sdcard/Download`)目录，查看**本地音乐导出.txt**文件。
7. 使用**本地音乐导出.txt**文件。

## 支持的音乐格式

- OGG
- MP3
- FLAC
- MP4
- M4A
- M4P
- WMA
- WAV
- RA
- RM
- M4B
- AIF
- AIFF
- AIFC
- DSF
- DFF

## 需要使用的权限说明

- Android 11 (API 30) 及以上：
    - **所有文件管理权限** (`android.permission.MANAGE_EXTERNAL_STORAGE`)：从外置存储(`/storage/emulated/0`)及其子目录中读取音乐文件，写入结果文件
- Android 10 (API 29) 及以下：
    - **读外置存储权限** (`android.permission.READ_EXTERNAL_STORAGE`)：从外置存储(`/storage/emulated/0`)及其子目录中读取音乐文件
    - **写外置存储权限** (`android.permission.WRITE_EXTERNAL_STORAGE`)：向下载目录(`/storage/emulated/0/Download`)中写入结果文件

## 赞助与支持

如果这个项目对您有所帮助，您可以给我一颗⭐，或者请我喝杯咖啡！<br>
非常感谢您的支持！ <br>
<a  href="markdownResources/Alipay WeChatPay.jpg">
<img src="markdownResources/Sponsorship.png" width = "150" height = "90" alt="alipay.jpg" align=center />
</a>


