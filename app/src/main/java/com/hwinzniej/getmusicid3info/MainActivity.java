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
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
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
import java.util.concurrent.Semaphore;

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
    AlertDialog.Builder alertDialog;
    int userAction = 0;
    Semaphore userActionSema = new Semaphore(0);
    private ActivityResultLauncher<Intent> activityResultLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
//                    Toast.makeText(this, getResources().getString(R.string.toastAlreadyHaveManageAllFileAccessPermission), Toast.LENGTH_SHORT).show();
                    Toast.makeText(this, getResources().getString(R.string.toastChooseDirectory), Toast.LENGTH_SHORT).show();
                    openDirectory();
                } else
                    Toast.makeText(this, getResources().getString(R.string.toastFailedGetManageAllFilesAccessPermission), Toast.LENGTH_SHORT).show();
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
//        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setTitle(getResources().getString(R.string.scanSongsDialogTitle));
        progressDialog.setMessage(getResources().getString(R.string.scanSongsDialogMessage));

        alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle(getResources().getString(R.string.fileConflictAlertDialogTitle));
        alertDialog.setMessage(getResources().getString(R.string.fileConflictAlertDialogMessage).replace("#n", "\n"));
        alertDialog.setCancelable(false);
        alertDialog.setPositiveButton(getResources().getString(R.string.fileConflictAlertDialogOptions1), (dialogInterface, i) -> {
            userAction = 1;
            userActionSema.release();
        });
        alertDialog.setNegativeButton(getResources().getString(R.string.fileConflictAlertDialogOptions2), (dialogInterface, i) -> {
            userAction = 2;
            userActionSema.release();
        });

        getId3Btn.setOnClickListener(v -> {
            progressDialog.setTitle(getResources().getString(R.string.scanSongsDialogTitle));
            progressDialog.setMessage(getResources().getString(R.string.scanSongsDialogMessage));
            logPrint.setText(getResources().getString(R.string.initTextView));
            progressPercent = 0;
            executorPool = Executors.newFixedThreadPool(core);
            manageAllFiles();
        });
    }

    ActivityResultLauncher<Intent> openDirectoryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                if (data != null) {
                    Uri uri = data.getData();
                    logPrint.append("\n\n" + getResources().getString(R.string.youHaveSelected) + uri.getPathSegments().get(uri.getPathSegments().size() - 1).replace("primary:", "/storage/emulated/0/") + getResources().getString(R.string.directory) + "\n");
                    executorService.execute(() -> checkFileExists());
                    progressDialog.show();
                    executorService.execute(() -> listFilesInTree(uri));
                }
            }
        }
    });

    private void checkFileExists() {
        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), getResources().getString(R.string.outputFileName));
            if (!file.exists()) {
                file.createNewFile();
            } else {
                if (progressPercent == 0) {
                    runOnUiThread(() -> alertDialog.show());

                    while (true) {
                        try {
                            userActionSema.acquire();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        if (userAction == 1) {
                            runOnUiThread(() -> logPrint.append("\n" + getResources().getString(R.string.appendLogPring) + "\n"));
                            break;
                        }
                        if (userAction == 2) {
                            new FileWriter(file, false).close();
                            runOnUiThread(() -> logPrint.append("\n" + getResources().getString(R.string.overwriteLogPrint) + "\n"));
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void manageAllFiles() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11及以上版本
            if (Environment.isExternalStorageManager()) {
//                Toast.makeText(this, getResources().getString(R.string.toastAlreadyHaveManageAllFileAccessPermission), Toast.LENGTH_SHORT).show();
                openDirectory();
            } else {
                Toast.makeText(this, getResources().getString(R.string.toastGrantManageAllFileAccessPermission), Toast.LENGTH_SHORT).show();
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
//                Toast.makeText(this, getResources().getString(R.string.toastAlreadyHaveReadOrWriteExternalStoragePermissions), Toast.LENGTH_SHORT).show();
                Toast.makeText(this, getResources().getString(R.string.toastChooseDirectory), Toast.LENGTH_SHORT).show();
                openDirectory();
            } else {
                Toast.makeText(MainActivity.this, getResources().getString(R.string.toastFailedGetReadOrWriteExternalStoragePermissions), Toast.LENGTH_SHORT).show();
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
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, getResources().getString(R.string.toastThisDirectoryDoNotHaveMusicFile), Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            });
            return;
        }
        runOnUiThread(() -> progressDialog.setTitle(getResources().getString(R.string.exportSongsDialogTitle)));
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
                    executorPool.execute(() -> handleFile(file.getUri().getPathSegments().get(file.getUri().getPathSegments().size() - 1).replace("primary:", "/storage/emulated/0/")));
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
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, getResources().getString(R.string.toastExportComplete1) + progressPercent + getResources().getString(R.string.toastExportComplete2).replace("。", ""), Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
                logPrint.append("\n" + getResources().getString(R.string.toastExportComplete1) + progressPercent + getResources().getString(R.string.toastExportComplete2) + "\n" + getResources().getString(R.string.exportCompleteLogPrint));
            });
            executorPool.shutdown();
            max = 0;
            userAction = 0;
        }
    }

    private synchronized void writeToFile(Tag tag, String dfile) {
        String fileName = getResources().getString(R.string.outputFileName);
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
        try {
            FileWriter fileWriter = new FileWriter(file, true);
            fileWriter.write((tag.getFirst(FieldKey.TITLE) + "#*#" + tag.getFirst(FieldKey.ARTIST) + "#*#" + tag.getFirst(FieldKey.ALBUM) + "#*#" + dfile + "#*#" + progressPercent + "\n"));
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (Exception e) {
            logPrint.append("\n" + dfile + getResources().getString(R.string.failReadTag) + e + "\n");
            return;
        }

        increment();
        runOnUiThread(() -> progressDialog.setMessage(getResources().getString(R.string.progress) + progressPercent + " / " + max));
    }

    private synchronized void increment() {
        ++progressPercent;
    }

}