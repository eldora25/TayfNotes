package com.eldora25.tayfnotes.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null

    fun startRecording(outputFile: File) {
        try {
            stopRecording() // Clean up existing
            
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            throw e
        }
    }

    fun stopRecording() {
        try {
            recorder?.stop()
        } catch (_: Exception) {
        } finally {
            recorder?.release()
            recorder = null
        }
    }

    fun startPlaying(file: File, onFinished: () -> Unit) {
        try {
            stopPlaying()
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    onFinished()
                    stopPlaying()
                }
            }
        } catch (e: Exception) {
            player?.release()
            player = null
            onFinished()
        }
    }

    fun stopPlaying() {
        try {
            player?.stop()
        } catch (_: Exception) {}
        player?.release()
        player = null
    }
}
