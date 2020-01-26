package com.volcano.dictation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.iflytek.cloud.SpeechConstant
import com.iflytek.cloud.SpeechSynthesizer
import com.iflytek.cloud.SpeechUtility
import com.iflytek.cloud.util.ResourceUtil
import kotlinx.android.synthetic.main.activity_main.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URLDecoder
import java.util.*


class MainActivity : AppCompatActivity() {
    private val appId = "5a54bf87"
    private val voicerXtts = "xiaoyan"
    private val chooseFileCode = 0x1001
    private var tts: SpeechSynthesizer? = null
    private var txt: LinkedList<String> = LinkedList()
    private var txtId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initialize()
        initializeUI()
        initializeTts()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            chooseFileCode -> {
                if (resultCode == RESULT_OK) {
                    val uri = data?.data
                    if (uri != null) {
                        val result = loadResources(uri)
                        if (result) {
                            resetUI(getFilename(uri))
                        }
                    }
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun initialize() {
        button_load.setOnClickListener { chooseResources() }
        button_prev.setOnClickListener {
            if (txtId > 0) {
                speak(txt[--txtId])
                refreshUI()
            }
        }
        button_next.setOnClickListener {
            if (txtId < txt.size - 1) {
                speak(txt[++txtId])
                refreshUI()
            }
        }
        button_play.setOnClickListener { speak(txt[txtId]) }
    }

    private fun initializeTts() {
        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=" + appId)
        tts = SpeechSynthesizer.createSynthesizer(this, null)
        if (tts == null) {
            Toast.makeText(this, R.string.msg_initialize_fail, Toast.LENGTH_SHORT).show()
            finish()
        }

        tts?.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_XTTS)
        tts?.setParameter(ResourceUtil.TTS_RES_PATH, getResourcePath(voicerXtts))
        tts?.setParameter(SpeechConstant.VOICE_NAME, voicerXtts)
        tts?.setParameter(SpeechConstant.SPEED, "40")
        speak("你好,贝贝!准备好了吗?请选择听写文件")
    }

    private fun initializeUI() {
        textView_filename.text = getString(R.string.button_load)
        button_prev.isEnabled = false
        button_next.isEnabled = false
        button_play.isEnabled = false
    }

    private fun resetUI(filename: String?) {
        textView_filename.text = filename
        txtId = 0
        refreshUI()

        if (txt.size > 0) {
            speak("我们要开始听写了,第一题:${txt[txtId]}")
        }
    }

    private fun refreshUI() {
        if (txt.size == 0) {
            button_next.isEnabled = false
            button_play.isEnabled = false
            textView_content.text = getString(R.string.msg_content_empty)
            return
        } else {
            button_play.isEnabled = true
            textView_content.text = "第${txtId + 1}题"
        }

        when (txtId) {
            0 -> {
                button_prev.isEnabled = false
                button_next.isEnabled = true
            }
            txt.size - 1 -> {
                button_prev.isEnabled = true
                button_next.isEnabled = false
            }
            else -> {
                button_prev.isEnabled = true
                button_next.isEnabled = true
            }
        }
    }

    private fun getResourcePath(voiceName: String): String {
        val tempBuffer = StringBuffer()
        val type = "xtts"

        // 合成通用资源
        tempBuffer.append(
            ResourceUtil.generateResourcePath(
                this, ResourceUtil.RESOURCE_TYPE.assets,
                "$type/common.jet"
            )
        )
        tempBuffer.append(";")
        // 发音人资源
        tempBuffer.append(
            ResourceUtil.generateResourcePath(
                this,
                ResourceUtil.RESOURCE_TYPE.assets,
                "$type/$voiceName.jet"
            )
        )

        return tempBuffer.toString()
    }

    private fun speak(content: String) {
        val code = tts?.startSpeaking(content, null)
        Log.d(getString(applicationInfo.labelRes), "speak result: $code")
    }

    private fun chooseResources() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "text/csv"
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        try {
            startActivityForResult(
                Intent.createChooser(
                    intent,
                    getString(R.string.msg_choose_file)
                ), chooseFileCode
            )
        } catch (e: Exception) {
            Toast.makeText(this, R.string.msg_need_install_file_explorer, Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadResources(uri: Uri): Boolean {
        val stream = contentResolver.openInputStream(uri)
        if (stream == null) {
            Toast.makeText(this, R.string.msg_open_file_fail, Toast.LENGTH_SHORT).show()
            return false
        }

        var reader: BufferedReader? = null
        txt.clear()

        try {
            reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
            var line: String? = null
            while (true) {
                line = reader.readLine() ?: break
                Log.d(getString(applicationInfo.labelRes), "read line: $line")
                txt.addAll(line.split(","))
            }

            return true
        } catch (e: Exception) {
            Toast.makeText(this, R.string.msg_read_file_fail, Toast.LENGTH_SHORT).show()
            return false
        } finally {
            reader?.close()
            stream.close()
        }
    }

    private fun getFilename(uri: Uri): String {
        val path = URLDecoder.decode(uri.toString(), Charsets.UTF_8.name())
        return path.substring(path.lastIndexOf('/') + 1)
    }
}
