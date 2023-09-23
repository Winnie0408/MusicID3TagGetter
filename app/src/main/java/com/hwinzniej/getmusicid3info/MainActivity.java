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
import android.view.View;
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
    ScrollView scrollView;
    int max = 0;
    ExecutorService executorPool;
    ExecutorService executorService;
    ProgressDialog progressDialog;
    private ActivityResultLauncher<Intent> activityResultLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
//                            Toast.makeText(this, "已授予所有文件管理权限", Toast.LENGTH_SHORT).show();
                    Toast.makeText(this, "请选择存放音乐文件的目录", Toast.LENGTH_SHORT).show();
                    openDirectory();
                } else Toast.makeText(this, "未授予所有文件管理权限", Toast.LENGTH_SHORT).show();
            }

        });

        getId3Btn = findViewById(R.id.getId3Button);
        logPrint = findViewById(R.id.logPrint);
        scrollView = findViewById(R.id.scrollView);

        int core = Runtime.getRuntime().availableProcessors();
        executorService = Executors.newSingleThreadExecutor();

        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setCancelable(true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setTitle("正在扫描歌曲");
        progressDialog.setMessage("请稍等...");

        getId3Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logPrint.setText(R.string.initTextView);
                progressPercent = 0;
                executorPool = Executors.newFixedThreadPool(core);
                manageAllFiles();
            }
        });
    }

    ActivityResultLauncher<Intent> openDirectoryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11及以上版本
            if (Environment.isExternalStorageManager()) {
                Toast.makeText(this, "已拥有所有文件管理权限", Toast.LENGTH_SHORT).show();
                openDirectory();
            } else {
                Toast.makeText(this, "请授予所有文件管理权限", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                activityResultLauncher.launch(intent);
            }
        } else { // Android 10及以下版本
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
            } else openDirectory();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
//                Toast.makeText(MainActivity.this, "已授予内部存储读写权限", Toast.LENGTH_SHORT).show();
                Toast.makeText(this, "请选择存放音乐文件的目录", Toast.LENGTH_SHORT).show();
                openDirectory();
            } else {
                Toast.makeText(MainActivity.this, "未授予内部存储读写权限", Toast.LENGTH_SHORT).show();
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.setTitle("正在导出");
            }
        });
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
                    max++;
                }
            }
        }
    }

    private void listFilesInTree(DocumentFile root) {
        if (root != null) {
            for (DocumentFile file : root.listFiles()) {
                if (file.isDirectory()) {
                    // 如果是目录，则递归遍历
                    listFilesInTree(file);
                } else {
                    File file1 = new File(file.getUri().getPathSegments().get(file.getUri().getPathSegments().size() - 1).replace("primary:", "/storage/emulated/0/"));
                    try {
                        AudioFileIO.read(file1);
                    } catch (CannotReadException | IOException | TagException |
                             ReadOnlyFileException | InvalidAudioFrameException e) {
                        continue;
                    }
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
        AudioFile audioFile;
        try {
            audioFile = AudioFileIO.read(new File(dfile));
        } catch (Exception e) {
            return;
        }

        Tag tag = audioFile.getTag();

        writeToFile(tag, dfile);

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

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.setMessage("已完成: " + progressPercent + " / " + max);
            }
        });
    }

}