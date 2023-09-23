## Welcome to the Get ID3 Tag Tool APP

### [中文](README.md)

### This tool is mainly used in conjunction with the [SaltPlayer Playlist Converter](https://github.com/Winnie0408/SaltPlayerConverter), but it can also be used independently.

## How to Use

1. Open the **Get ID3 Tag** application.
2. Click the **Select Directory** button.
3. Grant permissions as prompted by the software:
    * Android 11 and above: Grant **Manage All Files Access Permission** `android.permission.MANAGE_EXTERNAL_STORAGE`
    * Android 10 and below: Grant **Read/Write External Storage Permissions** `android.permission.READ_EXTERNAL_STORAGE`与`android.permission.WRITE_EXTERNAL_STORAGE`
4. Follow the software prompts to select the directory where your music files are stored on your
   device. The selection method is shown in the figure below:<br>
   **Note the current parent directory displayed in the first line of the picture**

| Type of Music File Storage | In the Same Directory                        | In Subdirectories of the Same Directory      | In Different Directories                                                                                                                                                                             |
|----------------------------|----------------------------------------------|----------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Directory Tree Example     | ![img.png](markdownResources/img1.png)       | ![img.png](markdownResources/img2.png)       | ![img.png](markdownResources/img3.png)                                                                                                                                                               |
| Operation Method           | Directly select the parent directory `Music` | Directly select the parent directory `Music` | Multiple selection operations are required, **each time select one** directory that stores music (such as `中文歌曲`, `temp` in the picture), and choose **Append** in the following conflict dialog box |

5. Wait for the music ID3 information to be exported.
6. Go to the `/storage/emulated/0/Download`(i.e., `/sdcard/Download`)directory and check the **Local
   Music Export.txt** file.
7. Use the **Local Music Export.txt** file.
