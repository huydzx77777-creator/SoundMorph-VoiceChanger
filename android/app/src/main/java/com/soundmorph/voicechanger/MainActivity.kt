package com.soundmorph.voicechanger

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.soundmorph.voicechanger.adapter.VoiceEffectAdapter
import com.soundmorph.voicechanger.audio.AudioRecorder
import com.soundmorph.voicechanger.audio.VoiceEffectEngine
import com.soundmorph.voicechanger.audio.WavUtils
import com.soundmorph.voicechanger.databinding.ActivityMainBinding
import com.soundmorph.voicechanger.model.VoiceEffect
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var audioRecorder: AudioRecorder? = null
    private var rawRecordedFile: File? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingEffect: VoiceEffect? = null
    private lateinit var effectAdapter: VoiceEffectAdapter

    private var recordSeconds = 0
    private var timerJob: Job? = null

    // Request Audio Permission Launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startRecording()
        } else {
            Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_SHORT).show()
        }
    }

    // Pick Audio File from Storage Launcher
    private val pickAudioLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleImportedAudioUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
    }

    private fun setupRecyclerView() {
        val effects = VoiceEffect.values().toList()
        effectAdapter = VoiceEffectAdapter(
            effects = effects,
            onPlayClicked = { effect -> handlePlayEffect(effect) },
            onSaveClicked = { effect -> handleSaveEffect(effect) },
            onShareClicked = { effect -> handleShareEffect(effect) }
        )
        binding.rvVoiceEffects.layoutManager = LinearLayoutManager(this)
        binding.rvVoiceEffects.adapter = effectAdapter
    }

    private fun setupListeners() {
        // Record Mic Button
        binding.btnRecordMic.setOnClickListener {
            if (audioRecorder?.isCurrentlyRecording() == true) {
                stopRecording()
            } else {
                checkPermissionAndStartRecording()
            }
        }

        // Import Audio File Button
        binding.btnImportAudio.setOnClickListener {
            pickAudioLauncher.launch("audio/*")
        }

        // Record Again Button
        binding.btnRecordAgain.setOnClickListener {
            stopPlayback()
            binding.panelEffects.visibility = View.GONE
            binding.panelRecording.visibility = View.VISIBLE
            binding.tvRecordStatus.text = getString(R.string.tap_to_record)
            binding.tvTimer.text = "00:00"
            recordSeconds = 0
        }
    }

    private fun checkPermissionAndStartRecording() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startRecording()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startRecording() {
        stopPlayback()
        val cacheDir = File(cacheDir, "recordings")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        rawRecordedFile = File(cacheDir, "voice_record.wav")
        audioRecorder = AudioRecorder(rawRecordedFile!!)
        audioRecorder?.startRecording(lifecycleScope)

        binding.tvRecordStatus.text = getString(R.string.recording)
        binding.btnRecordMic.setImageResource(android.R.drawable.ic_media_pause)

        // Start timer
        recordSeconds = 0
        timerJob?.cancel()
        timerJob = lifecycleScope.launch {
            while (isActive && audioRecorder?.isCurrentlyRecording() == true) {
                val mins = recordSeconds / 60
                val secs = recordSeconds % 60
                binding.tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
                delay(1000)
                recordSeconds++
            }
        }
    }

    private fun stopRecording() {
        timerJob?.cancel()
        audioRecorder?.stopRecording()
        binding.btnRecordMic.setImageResource(android.R.drawable.ic_btn_speak_now)

        // Give a short delay to finalize WAV header writing
        lifecycleScope.launch {
            delay(350)
            binding.panelRecording.visibility = View.GONE
            binding.panelEffects.visibility = View.VISIBLE
        }
    }

    private fun handleImportedAudioUri(uri: Uri) {
        val cacheDir = File(cacheDir, "recordings")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val importedFile = File(cacheDir, "voice_imported.wav")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    importedFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                rawRecordedFile = importedFile
                withContext(Dispatchers.Main) {
                    binding.panelRecording.visibility = View.GONE
                    binding.panelEffects.visibility = View.VISIBLE
                    Toast.makeText(this@MainActivity, "Đã tải file âm thanh thành công!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Không thể đọc file âm thanh này", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handlePlayEffect(effect: VoiceEffect) {
        if (currentPlayingEffect == effect && mediaPlayer?.isPlaying == true) {
            stopPlayback()
            return
        }

        stopPlayback()
        val inputFile = rawRecordedFile ?: return

        lifecycleScope.launch {
            val effectFile = getOrCreateEffectFile(inputFile, effect)
            if (effectFile != null && effectFile.exists()) {
                playAudioFile(effectFile, effect)
            } else {
                Toast.makeText(this@MainActivity, "Lỗi xử lý âm thanh", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun getOrCreateEffectFile(sourceWav: File, effect: VoiceEffect): File? =
        withContext(Dispatchers.IO) {
            try {
                val effectDir = File(cacheDir, "effects")
                if (!effectDir.exists()) effectDir.mkdirs()

                val effectFile = File(effectDir, "effect_${effect.name.lowercase()}.wav")
                if (effectFile.exists() && effectFile.lastModified() > sourceWav.lastModified()) {
                    return@withContext effectFile
                }

                val (samples, sampleRate) = WavUtils.readWavFile(sourceWav)
                if (samples.isEmpty()) return@withContext null

                val processedSamples = VoiceEffectEngine.applyEffect(samples, sampleRate, effect)
                WavUtils.writeWavFile(processedSamples, sampleRate, effectFile)
                return@withContext effectFile
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }

    private fun playAudioFile(file: File, effect: VoiceEffect) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    stopPlayback()
                }
            }
            currentPlayingEffect = effect
            effectAdapter.setPlayingEffect(effect)
        } catch (e: Exception) {
            Toast.makeText(this, "Không thể phát âm thanh: ${e.message}", Toast.LENGTH_SHORT).show()
            stopPlayback()
        }
    }

    private fun stopPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        currentPlayingEffect = null
        effectAdapter.setPlayingEffect(null)
    }

    private fun handleSaveEffect(effect: VoiceEffect) {
        val inputFile = rawRecordedFile ?: return
        lifecycleScope.launch {
            val effectFile = getOrCreateEffectFile(inputFile, effect) ?: return@launch
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "SoundMorph_${effect.name.lowercase()}_$timestamp.wav"

            val success = saveFileToMusicFolder(effectFile, filename)
            if (success) {
                Toast.makeText(this@MainActivity, "Đã lưu vào thư mục Âm nhạc: $filename", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@MainActivity, getString(R.string.save_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun saveFileToMusicFolder(sourceFile: File, displayName: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
                        put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                        put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/SoundMorph")
                    }
                    val uri = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                        ?: return@withContext false

                    contentResolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(sourceFile).use { input ->
                            input.copyTo(out)
                        }
                    }
                    true
                } else {
                    val musicDir = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                        "SoundMorph"
                    )
                    if (!musicDir.exists()) musicDir.mkdirs()
                    val destFile = File(musicDir, displayName)
                    sourceFile.copyTo(destFile, overwrite = true)
                    true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    private fun handleShareEffect(effect: VoiceEffect) {
        val inputFile = rawRecordedFile ?: return
        lifecycleScope.launch {
            val effectFile = getOrCreateEffectFile(inputFile, effect) ?: return@launch
            try {
                val uri = FileProvider.getUriForFile(
                    this@MainActivity,
                    "${packageName}.fileprovider",
                    effectFile
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "audio/wav"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Giọng đổi: ${effect.title}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_title)))
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Lỗi chia sẻ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        stopPlayback()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlayback()
        audioRecorder?.stopRecording()
    }
}
