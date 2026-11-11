# Android 语音聊天实现指南

## 📋 目录

1. [环境准备](#环境准备)
2. [项目配置](#项目配置)
3. [权限配置](#权限配置)
4. [步骤1: 建立 WebSocket 连接](#步骤1-建立-websocket-连接)
5. [步骤2: 录音并发送](#步骤2-录音并发送)
6. [步骤3: 接收并播放音频](#步骤3-接收并播放音频)
7. [完整代码示例](#完整代码示例)
8. [常见问题](#常见问题)

---

## 环境准备

### 1. Android Studio
- 安装 Android Studio（最新版本）
- 最低 SDK: API 21 (Android 5.0)
- 目标 SDK: API 33+ (Android 13+)

### 2. 项目类型
- Kotlin 或 Java 项目
- 推荐使用 Kotlin

---

## 项目配置

### 1. 添加依赖 (build.gradle.kts 或 build.gradle)

```kotlin
dependencies {
    // WebSocket 客户端
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // 协程支持（Kotlin）
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // 权限处理
    implementation("com.karumi:dexter:6.2.3")
    
    // 音频处理（可选）
    implementation("androidx.media3:media3-exoplayer:1.2.0")
}
```

**Java 项目 (build.gradle)**:
```gradle
dependencies {
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.karumi:dexter:6.2.3'
}
```

---

## 权限配置

### AndroidManifest.xml

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- 录音权限 -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    
    <!-- 网络权限 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <!-- Android 6.0+ 需要运行时权限 -->
    
    <application
        android:usesCleartextTraffic="true"  <!-- 允许 HTTP，开发环境 -->
        ...>
        ...
    </application>
</manifest>
```

---

## 步骤1: 建立 WebSocket 连接

### Kotlin 实现

```kotlin
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

class VoiceChatWebSocket {
    private var client: OkHttpClient? = null
    private var webSocket: WebSocket? = null
    private var listener: WebSocketListener? = null
    
    // WebSocket 地址
    private val wsUrl = "ws://192.168.0.103:8000/ws/voice-chat"
    
    fun connect() {
        client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
        
        val request = Request.Builder()
            .url(wsUrl)
            .build()
        
        listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                println("✅ WebSocket 连接成功")
                // 连接成功，可以开始录音
            }
            
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // 收到音频数据（二进制）
                println("收到音频数据: ${bytes.size()} bytes")
                playAudio(bytes.toByteArray())
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                // 收到文本消息
                println("收到文本消息: $text")
                try {
                    val json = JSONObject(text)
                    if (json.has("error")) {
                        println("❌ 服务器错误: ${json.getString("error")}")
                    }
                } catch (e: Exception) {
                    // 不是 JSON
                }
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                println("❌ WebSocket 连接失败: ${t.message}")
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                println("连接正在关闭: $code $reason")
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                println("连接已关闭: $code $reason")
            }
        }
        
        webSocket = client?.newWebSocket(request, listener!!)
    }
    
    // 发送音频数据
    fun sendAudio(audioData: ByteArray) {
        webSocket?.send(ByteString.of(*audioData))
    }
    
    // 发送文本消息
    fun sendText(text: String) {
        webSocket?.send(text)
    }
    
    // 关闭连接
    fun close() {
        webSocket?.close(1000, "正常关闭")
        client?.dispatcher?.executorService?.shutdown()
    }
}
```

### Java 实现

```java
import okhttp3.*;
import okio.ByteString;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;

public class VoiceChatWebSocket {
    private OkHttpClient client;
    private WebSocket webSocket;
    private String wsUrl = "ws://192.168.0.103:8000/ws/voice-chat";
    
    public void connect() {
        client = new OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build();
        
        Request request = new Request.Builder()
            .url(wsUrl)
            .build();
        
        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                System.out.println("✅ WebSocket 连接成功");
            }
            
            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                System.out.println("收到音频数据: " + bytes.size() + " bytes");
                playAudio(bytes.toByteArray());
            }
            
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                System.out.println("收到文本消息: " + text);
            }
            
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                System.out.println("❌ WebSocket 连接失败: " + t.getMessage());
            }
            
            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                System.out.println("连接已关闭: " + code + " " + reason);
            }
        };
        
        webSocket = client.newWebSocket(request, listener);
    }
    
    public void sendAudio(byte[] audioData) {
        if (webSocket != null) {
            webSocket.send(ByteString.of(audioData));
        }
    }
    
    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "正常关闭");
        }
    }
}
```

---

## 步骤2: 录音并发送

### Kotlin 实现

```kotlin
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.AudioFormat
import kotlinx.coroutines.*

class AudioRecorder(private val webSocket: VoiceChatWebSocket) {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingJob: Job? = null
    
    // 音频参数
    private val sampleRate = 16000  // 16kHz
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    
    fun startRecording() {
        if (isRecording) return
        
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize * 2
            )
            
            audioRecord?.startRecording()
            isRecording = true
            
            // 在协程中读取音频数据
            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                val buffer = ByteArray(bufferSize)
                
                while (isRecording && audioRecord != null) {
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    
                    if (bytesRead > 0) {
                        // 发送音频数据
                        val audioData = buffer.copyOf(bytesRead)
                        webSocket.sendAudio(audioData)
                    }
                }
            }
            
            println("✅ 开始录音...")
        } catch (e: Exception) {
            println("❌ 启动录音失败: ${e.message}")
            stopRecording()
        }
    }
    
    fun stopRecording() {
        isRecording = false
        recordingJob?.cancel()
        
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        
        println("⏹️ 停止录音")
    }
}
```

### Java 实现

```java
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.AudioFormat;

public class AudioRecorder {
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private Thread recordingThread;
    private VoiceChatWebSocket webSocket;
    
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferSize;
    
    public AudioRecorder(VoiceChatWebSocket webSocket) {
        this.webSocket = webSocket;
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    }
    
    public void startRecording() {
        if (isRecording) return;
        
        try {
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * 2
            );
            
            audioRecord.startRecording();
            isRecording = true;
            
            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[bufferSize];
                
                while (isRecording && audioRecord != null) {
                    int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                    
                    if (bytesRead > 0) {
                        byte[] audioData = new byte[bytesRead];
                        System.arraycopy(buffer, 0, audioData, 0, bytesRead);
                        webSocket.sendAudio(audioData);
                    }
                }
            });
            
            recordingThread.start();
            System.out.println("✅ 开始录音...");
        } catch (Exception e) {
            System.out.println("❌ 启动录音失败: " + e.getMessage());
            stopRecording();
        }
    }
    
    public void stopRecording() {
        isRecording = false;
        
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        
        if (recordingThread != null) {
            try {
                recordingThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        System.out.println("⏹️ 停止录音");
    }
}
```

---

## 步骤3: 接收并播放音频

### Kotlin 实现

```kotlin
import android.media.AudioTrack
import android.media.AudioFormat
import android.media.AudioManager

class AudioPlayer {
    private var audioTrack: AudioTrack? = null
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    
    fun playAudio(audioData: ByteArray) {
        try {
            // 如果 AudioTrack 不存在或状态不对，重新创建
            if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                audioTrack?.stop()
                audioTrack?.release()
                
                audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize * 2,
                    AudioTrack.MODE_STREAM
                )
                
                audioTrack?.play()
            }
            
            // 写入音频数据并播放
            audioTrack?.write(audioData, 0, audioData.size)
        } catch (e: Exception) {
            println("❌ 播放音频失败: ${e.message}")
        }
    }
    
    fun stop() {
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}
```

### Java 实现

```java
import android.media.AudioTrack;
import android.media.AudioFormat;
import android.media.AudioManager;

public class AudioPlayer {
    private AudioTrack audioTrack;
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferSize;
    
    public AudioPlayer() {
        bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    }
    
    public void playAudio(byte[] audioData) {
        try {
            if (audioTrack == null || audioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                if (audioTrack != null) {
                    audioTrack.stop();
                    audioTrack.release();
                }
                
                audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize * 2,
                    AudioTrack.MODE_STREAM
                );
                
                audioTrack.play();
            }
            
            audioTrack.write(audioData, 0, audioData.length);
        } catch (Exception e) {
            System.out.println("❌ 播放音频失败: " + e.getMessage());
        }
    }
    
    public void stop() {
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
    }
}
```

---

## 完整代码示例

### MainActivity.kt (Kotlin)

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var webSocket: VoiceChatWebSocket
    private lateinit var audioRecorder: AudioRecorder
    private lateinit var audioPlayer: AudioPlayer
    
    private lateinit var statusText: TextView
    private lateinit var connectBtn: Button
    private lateinit var startBtn: Button
    private lateinit var stopBtn: Button
    
    private val REQUEST_RECORD_AUDIO = 100
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // 初始化组件
        statusText = findViewById(R.id.statusText)
        connectBtn = findViewById(R.id.connectBtn)
        startBtn = findViewById(R.id.startBtn)
        stopBtn = findViewById(R.id.stopBtn)
        
        // 初始化 WebSocket 和音频组件
        webSocket = VoiceChatWebSocket()
        audioPlayer = AudioPlayer()
        audioRecorder = AudioRecorder(webSocket)
        
        // 设置按钮点击事件
        connectBtn.setOnClickListener { connect() }
        startBtn.setOnClickListener { startRecording() }
        stopBtn.setOnClickListener { stopRecording() }
        
        // 检查权限
        checkPermissions()
    }
    
    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO
            )
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateStatus("✅ 权限已授予")
            } else {
                updateStatus("❌ 需要录音权限")
            }
        }
    }
    
    private fun connect() {
        updateStatus("正在连接...")
        webSocket.connect()
        updateStatus("已连接")
    }
    
    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            == PackageManager.PERMISSION_GRANTED) {
            audioRecorder.startRecording()
            updateStatus("录音中...")
        } else {
            checkPermissions()
        }
    }
    
    private fun stopRecording() {
        audioRecorder.stopRecording()
        updateStatus("已停止")
    }
    
    private fun updateStatus(text: String) {
        runOnUiThread {
            statusText.text = text
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        audioRecorder.stopRecording()
        audioPlayer.stop()
        webSocket.close()
    }
}
```

### activity_main.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">
    
    <TextView
        android:id="@+id/statusText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="未连接"
        android:textSize="18sp"
        android:padding="16dp"/>
    
    <Button
        android:id="@+id/connectBtn"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="连接"
        android:layout_marginTop="16dp"/>
    
    <Button
        android:id="@+id/startBtn"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="开始录音"
        android:layout_marginTop="8dp"
        android:enabled="false"/>
    
    <Button
        android:id="@+id/stopBtn"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="停止录音"
        android:layout_marginTop="8dp"
        android:enabled="false"/>
    
</LinearLayout>
```

---

## 常见问题

### 1. 权限被拒绝
**解决**: 在设置中手动授予录音权限

### 2. WebSocket 连接失败
**解决**: 
- 检查网络权限
- 检查服务器地址
- 检查防火墙设置

### 3. 音频格式不匹配
**解决**: 确保采样率、声道、编码格式与后端要求一致

### 4. 内存泄漏
**解决**: 在 `onDestroy()` 中释放资源

---

## 📚 下一步

- 添加 UI 美化
- 添加音量指示器
- 添加连接重试机制
- 添加音频格式转换
- 添加错误处理优化


