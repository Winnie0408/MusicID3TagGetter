package com.hwinzniej.getmusicid3info;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.color.DynamicColors;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_CODE = 123;
    public int progressPercent;
    Button getId3Btn;
    TextView logPrint;
    //    ProgressBar progressBar;
//    TextView current;
//    TextView total;
    ViewGroup parentView;
    ScrollView scrollView;
    boolean threadStop = false;
    int max = 0;
    ExecutorService executorPool;
    ExecutorService executorService;
    ProgressDialog progressDialog;
    private ActivityResultLauncher<Intent> activityResultLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (Environment.isExternalStorageManager()) {
                            Toast.makeText(this, "已授予所有文件管理权限", Toast.LENGTH_SHORT).show();
                            Toast.makeText(this, "请选择存放音乐文件的目录", Toast.LENGTH_SHORT).show();
                            openDirectory();
                        } else
                            Toast.makeText(this, "未能获取所有文件管理权限，请重试", Toast.LENGTH_SHORT).show();
                    }

                }
        );

        getId3Btn = findViewById(R.id.getId3Button);
        logPrint = findViewById(R.id.logPrint);
//        progressBar = findViewById(R.id.loading);
//        current = findViewById(R.id.current);
//        total = findViewById(R.id.total);
//        parentView = (ViewGroup) progressBar.getParent();
        scrollView = findViewById(R.id.scrollView);

        int core = Runtime.getRuntime().availableProcessors();
//        executorPool = Executors.newFixedThreadPool(core);
        executorService = Executors.newSingleThreadExecutor();

        Log.i("HWinZnieJ", "创建线程数: " + core);
        Log.i("HWinZnieJ", "缓存位置：" + getCacheDir());

        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setCancelable(true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setTitle("正在导出");
        progressDialog.setMessage("请稍等...");

        getId3Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getId3Btn.getText().equals("选择目录")) {
                    logPrint.setText(R.string.initTextView);
//                    current.setText("0");
//                    total.setText("0");
                    progressPercent = 0;
                    executorPool = Executors.newFixedThreadPool(core);
                    manageAllFiles();
                } else if (getId3Btn.getText().equals("停止导出")) {
                    threadStop = true;
                    getId3Btn.setText("选择目录");
//                    progressBar.setVisibility(View.GONE);
                    executorPool.shutdown();
                    Toast.makeText(MainActivity.this, "导出已停止", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    ActivityResultLauncher<Intent> openDirectoryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Uri uri = data.getData();
                            logPrint.append("\n\n您已选择：" + uri.getPathSegments().get(uri.getPathSegments().size() - 1).replace("primary:", "/storage/emulated/0/") + "目录\n");
                            progressDialog.show();
                            executorService.execute(new Runnable() {
                                @Override
                                public void run() {
                                    listFilesInTree(uri);
                                }
                            });
                        }
                    }
                }
            });

    private void manageAllFiles() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Toast.makeText(this, "Android 11+", Toast.LENGTH_SHORT).show();
            // Android 11及以上版本
            if (Environment.isExternalStorageManager()) {
                Toast.makeText(this, "已拥有所有文件管理权限", Toast.LENGTH_SHORT).show();
                openDirectory();
                // Your app has the MANAGE_EXTERNAL_STORAGE permission.
            } else {
                Toast.makeText(this, "请授予所有文件管理权限", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                activityResultLauncher.launch(intent);
                // Your app does not have the MANAGE_EXTERNAL_STORAGE permission.
            }
        } else {
            Toast.makeText(this, "Android 10-", Toast.LENGTH_SHORT).show();
            // Android 10及以下版本
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
            } else
                openDirectory();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "已授予内部存储读写权限", Toast.LENGTH_SHORT).show();
                Toast.makeText(this, "请选择存放音乐文件的目录", Toast.LENGTH_SHORT).show();
                openDirectory();
            } else {
                Toast.makeText(MainActivity.this, "未授予内部存储读写权限", Toast.LENGTH_SHORT).show();
                // Permissions denied.
            }
        }
    }

    private void openDirectory() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        openDirectoryLauncher.launch(intent);
    }

    private void listFilesInTree(Uri treeUri) {
        DocumentFile root = DocumentFile.fromTreeUri(this, treeUri);
        musicCounter(root);
        Log.i("HWinZnieJ", "音乐文件总数：" + max);
        if (max == 0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "该目录下没有音乐文件", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
            });
            return;
        }
        listFilesInTree(root);
    }

    private void musicCounter(DocumentFile root) {
        if (root != null) {
            for (DocumentFile file : root.listFiles()) {
                if (file.isDirectory()) {
                    // 如果是目录，递归遍历
                    musicCounter(file);
                } else {
                    File file1 = new File(file.getUri().getPathSegments().get(file.getUri().getPathSegments().size() - 1).replace("primary:", "/storage/emulated/0/"));
                    // 如果是音乐文件，计数器加1
                    try {
                        AudioFileIO.read(file1);
                    } catch (CannotReadException | IOException | TagException |
                             ReadOnlyFileException | InvalidAudioFrameException e) {
                        continue;
                    }
                    Log.i("HWinZnieJ", "当前文件名" + file.getName());
                    max++;
                }
            }
        }
    }

    private void listFilesInTree(DocumentFile root) {
        if (root != null) {
//            max = root.listFiles().length;
            for (DocumentFile file : root.listFiles()) {
                if (threadStop) {
                    threadStop = false;
                    executorPool.shutdown();
                    return;
                }
                if (file.isDirectory()) {
                    // 如果是目录，则递归遍历
//                    progressPercent = 0;
                    listFilesInTree(file);
                } else {
                    File file1 = new File(file.getUri().getPathSegments().get(file.getUri().getPathSegments().size() - 1).replace("primary:", "/storage/emulated/0/"));
                    try {
                        AudioFileIO.read(file1);
                    } catch (CannotReadException | IOException | TagException |
                             ReadOnlyFileException | InvalidAudioFrameException e) {
                        continue;
                    }
                    // 如果是文件，则处理文件
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
////                            progressBar.setMax(root.listFiles().length - 1);
////                            parentView.addView(progressBar);
//                            total.setText(String.valueOf(root.listFiles().length));
//                        }
//                    });
//                    Log.i("HWinZnieJ", "线程池可用数：" + ((ThreadPoolExecutor) executorPool).getQueue().remainingCapacity());
                    executorPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            handleFile(file.getUri().getPathSegments().get(file.getUri().getPathSegments().size() - 1).replace("primary:", "/storage/emulated/0/"));
                        }
                    });
                }
            }
        }
    }

    private void handleFile(String dfile) {

        // 读取文件内容
        AudioFile audioFile = null;
//        Log.i("HWinZnieJ", "线程ID：" + Thread.currentThread().getId() + "，状态：" + Thread.currentThread().getState());
        //获取文件的输入流
//            InputStream inputStream = getContentResolver().openInputStream(dfile.getUri());

        //将输入流写入临时文件
//            FileOutputStream outputStream = new FileOutputStream(musicFile);
//            FileChannel outChannel = outputStream.getChannel();
//            ReadableByteChannel inChannel = Channels.newChannel(inputStream);
//            outChannel.transferFrom(inChannel, 0, 2097152);
//            byte[] buffer = new byte[1024];
//            int bytesRead;
////            while ((bytesRead = inputStream.read(buffer)) != -1) {
////                outputStream.write(buffer, 0, bytesRead);
////            }
//            int temp = 0;
//            while ((bytesRead = inputStream.read(buffer)) != -1) {
//                if (temp >= 2097152)
//                    break;
//                outputStream.write(buffer, 0, bytesRead);
//                temp += 1024;
//            }
//            outputStream.close();
        try {
            audioFile = AudioFileIO.read(new File(dfile));
        } catch (Exception e) {
//                musicFile.delete();
            return;
        }

//            if (musicFile.getName().endsWith(".mp3")) {
//                MP3FileReader mp3FileReader = new MP3FileReader();
//                audioFile = mp3FileReader.read(musicFile);
//            } else if (musicFile.getName().endsWith(".flac")) {
//                FlacFileReader flacFileReader = new FlacFileReader();
//                audioFile = flacFileReader.read(musicFile);
//            } else {
//                musicFile.delete();
//                return;
//            }
        Tag tag = audioFile.getTag();
        increment();

//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
////                            logPrint(tag, dfile.getUri().getPathSegments().get(3).replace("primary:", "/storage/emulated/0/"));
//                            logPrint.append("\n歌名：" + tag.getFirst(FieldKey.TITLE));
//                            logPrint.append("\n歌手：" + tag.getFirst(FieldKey.ARTIST));
//                            logPrint.append("\n专辑：" + tag.getFirst(FieldKey.ALBUM));
//                            logPrint.append("\n路径：" + dfile.getUri().getPathSegments().get(3).replace("primary:", "/storage/emulated/0/") + "\n");
//                            current.setText(String.valueOf(++progressPercent));
////      current.setText(String.valueOf(++progressPercent));
//                            scrollView.fullScroll(ScrollView.FOCUS_DOWN);
//
//                            if (progressPercent == 1) {
//                                progressBar.setVisibility(View.VISIBLE);
//                                getId3Btn.setText("停止导出");
//                            }
//
//                            if (progressPercent == max) {
//                                Toast.makeText(MainActivity.this, "导出完成", Toast.LENGTH_SHORT).show();
//                                progressBar.setVisibility(View.INVISIBLE);
//                                getId3Btn.setText("选择目录");
//                            }
//                        }
//                    });
//
//            Log.i("HWinZnieJ", Thread.currentThread().getId() + "，歌名：" + tag.getFirst(FieldKey.TITLE));
//            Log.i("HWinZnieJ", Thread.currentThread().getId() + "，歌手：" + tag.getFirst(FieldKey.ARTIST));
//            Log.i("HWinZnieJ", Thread.currentThread().getId() + "，专辑：" + tag.getFirst(FieldKey.ALBUM));
//            Log.i("HWinZnieJ", Thread.currentThread().getId() + "，路径：" + dfile.getUri().getPathSegments().get(3).replace("primary:", "/storage/emulated/0/") + "\n");
//            Log.i("HWinZnieJ", Thread.currentThread().getId() + "，进度: " + increment() + "/" + max);

        writeToFile(tag, dfile);

//            String fileName = "example.txt";
//            String selection = MediaStore.MediaColumns.DISPLAY_NAME + "=?";
//            String[] selectionArgs = new String[]{fileName};
//            Uri contentUri = MediaStore.Files.getContentUri("external");
//            Uri fileUri = null;
//
//            try (Cursor cursor = getContentResolver().query(contentUri, null, selection, selectionArgs, null)) {
//                if (cursor != null && cursor.getCount() > 0) {
//                    // 文件存在
//                    // 获取文件的Uri
//                    cursor.moveToFirst();
//                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
//                    long id = cursor.getLong(columnIndex);
//                    fileUri = ContentUris.withAppendedId(contentUri, id);
//
//                    if (progressPercent == 1) {
//                        getContentResolver().delete(fileUri, null, null);
//                        fileUri = null;
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                Toast.makeText(MainActivity.this, "已删除旧文件", Toast.LENGTH_SHORT).show();
//                            }
//                        });
//                    }
//                } else {
//                    // 文件不存在，创建新文件
//                    ContentValues values = new ContentValues();
//                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
//                    values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
//                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
//                    fileUri = getContentResolver().insert(contentUri, values);
//                }
//            }
//
//            if (fileUri != null) {
//                OutputStream outputStream1 = getContentResolver().openOutputStream(fileUri, "wa");
//                // 在这里向outputStream中写入数据
//                outputStream1.write((tag.getFirst(FieldKey.TITLE) + "#*#" + tag.getFirst(FieldKey.ARTIST) + "#*#" + tag.getFirst(FieldKey.ALBUM) + "#*#" + dfile.getUri().getPathSegments().get(3).replace("primary:", "/storage/emulated/0/") + "\n").getBytes());
//                outputStream1.close();
//            }


        if (progressPercent == max) {
            max = 0;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "导出完成，共" + progressPercent + "首歌", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                    logPrint.append("\n导出完成！\n请前往Download目录查看结果：本地音乐导出.txt");
                }
            });
            executorPool.shutdown();
        }
//            musicFile.delete();

    }

    private synchronized void writeToFile(Tag tag, String dfile) {
        String fileName = "本地音乐导出.txt";
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
        try {
            if (!file.exists()) {
                file.createNewFile();
            } else {
                if (progressPercent == 1) {
                    new FileWriter(file, false).close();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            logPrint.append("\n已删除旧的“本地音乐导出.txt”文件\n");
                        }
                    });
                }
            }
            FileWriter fileWriter = new FileWriter(file, true);
            fileWriter.write((tag.getFirst(FieldKey.TITLE) + "#*#" + tag.getFirst(FieldKey.ARTIST) + "#*#" + tag.getFirst(FieldKey.ALBUM) + "#*#" + dfile + "\n"));
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }


//        String selection = MediaStore.MediaColumns.DISPLAY_NAME + "=?";
//        String[] selectionArgs = new String[]{fileName};
//        Uri contentUri = MediaStore.Files.getContentUri("external");
//        Uri fileUri = null;
//
//        try (Cursor cursor = getContentResolver().query(contentUri, null, selection, selectionArgs, null)) {
//            if (cursor != null && cursor.getCount() > 0) {
//                // 文件存在
//                // 获取文件的Uri
//                cursor.moveToFirst();
//                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
//                long id = cursor.getLong(columnIndex);
//                fileUri = ContentUris.withAppendedId(contentUri, id);
//
//                if (progressPercent == 1) {
//                    getContentResolver().delete(fileUri, null, null);
//                    ContentValues values = new ContentValues();
//                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
//                    values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
//                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
//                    fileUri = getContentResolver().insert(contentUri, values);
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(MainActivity.this, "已删除旧的音乐列表", Toast.LENGTH_SHORT).show();
//                        }
//                    });
//                }
//            } else {
//                // 文件不存在，创建新文件
//                ContentValues values = new ContentValues();
//                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
//                values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
//                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
//                fileUri = getContentResolver().insert(contentUri, values);
//            }
//        }
//        try {
//            if (fileUri != null) {
//                OutputStream outputStream1 = getContentResolver().openOutputStream(fileUri, "wa");
//                // 在这里向outputStream中写入数据
//                outputStream1.write((tag.getFirst(FieldKey.TITLE) + "#*#" + tag.getFirst(FieldKey.ARTIST) + "#*#" + tag.getFirst(FieldKey.ALBUM) + "#*#" + dfile + "\n").getBytes());
//                outputStream1.close();
//            }
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.setMessage("已完成: " + progressPercent + " / " + max);
            }
        });
    }

//    private synchronized void modifyUI(){
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
////                            logPrint(tag, dfile.getUri().getPathSegments().get(3).replace("primary:", "/storage/emulated/0/"));
//                logPrint.append("\n歌名：" + tag.getFirst(FieldKey.TITLE));
//                logPrint.append("\n歌手：" + tag.getFirst(FieldKey.ARTIST));
//                logPrint.append("\n专辑：" + tag.getFirst(FieldKey.ALBUM));
//                logPrint.append("\n路径：" + dfile.getUri().getPathSegments().get(3).replace("primary:", "/storage/emulated/0/") + "\n");
//                current.setText(String.valueOf(++progressPercent));
////      current.setText(String.valueOf(++progressPercent));
//                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
//
//                if (progressPercent == 1) {
//                    progressBar.setVisibility(View.VISIBLE);
//                    getId3Btn.setText("停止导出");
//                }
//
//                if (progressPercent == max) {
//                    Toast.makeText(MainActivity.this, "导出完成", Toast.LENGTH_SHORT).show();
//                    progressBar.setVisibility(View.INVISIBLE);
//                    getId3Btn.setText("选择目录");
//                }
//            }
//        });
//
//        Log.i("HWinZnieJ", Thread.currentThread().getId() + "，进度: " + progressPercent + "/" + max);
//    }

    private synchronized int increment() {
        Log.i("HWinZnieJ", Thread.currentThread().getId() + "，进度: " + ++progressPercent + "/" + max);
        return progressPercent;
    }

//    private synchronized void logPrint(Tag tag, String url) {
//        logPrint.append("\n歌名：" + tag.getFirst(FieldKey.TITLE));
//        logPrint.append("\n歌手：" + tag.getFirst(FieldKey.ARTIST));
//        logPrint.append("\n专辑：" + tag.getFirst(FieldKey.ALBUM));
//        logPrint.append("\n路径：" + url + "\n");
//        current.setText(String.valueOf(increment()));
////      current.setText(String.valueOf(++progressPercent));
//        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
//    }

}