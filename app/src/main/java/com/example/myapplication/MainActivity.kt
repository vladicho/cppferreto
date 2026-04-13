package com.example.myapplication

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.myapplication.databinding.ActivityMainBinding
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var authHeader: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configuração do WebView
        val webSettings = binding.webview.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Mobile Safari/537.36"

        binding.webview.webViewClient = object : WebViewClient() {
            override fun onLoadResource(view: WebView?, url: String?) {
                super.onLoadResource(view, url)
                if (url != null && (url.contains(".m3u8") || url.contains(".m3u"))) {
                    runOnUiThread {
                        binding.editUrl.setText(url)
                        binding.sampleText.text = "VÍDEO DETECTADO!"
                        binding.sampleText.setTextColor(android.graphics.Color.RED)
                    }
                }
            }
        }

        binding.btnOpenSite.setOnClickListener {
            val url = binding.editUrl.text.toString()
            if (url.isNotEmpty()) binding.webview.loadUrl(url)
        }

        binding.btnDownload.setOnClickListener {
            val urlM3u8 = binding.editUrl.text.toString()
            if (urlM3u8.isEmpty()) return@setOnClickListener
            
            val user = binding.editUser.text.toString()
            val pass = binding.editPass.text.toString()
            if (user.isNotEmpty() && pass.isNotEmpty()) {
                val credentials = "$user:$pass"
                authHeader = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
            }

            val pastaPrivada = getExternalFilesDir(null)?.absolutePath ?: filesDir.absolutePath
            thread {
                iniciarDownloadNativo(urlM3u8, pastaPrivada)
                // Após o C++ terminar, movemos para a pasta Downloads
                runOnUiThread { exportarParaDownloads() }
            }
        }

        binding.btnPlay.setOnClickListener {
            val file = File(getExternalFilesDir(null), "video_baixado.ts")
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(uri, "video/*")
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Baixe o vídeo primeiro!", Toast.LENGTH_SHORT).show()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webview.canGoBack()) binding.webview.goBack() else finish()
            }
        })
    }

    private fun exportarParaDownloads() {
        try {
            val arquivoOrigem = File(getExternalFilesDir(null), "video_baixado.ts")
            if (!arquivoOrigem.exists()) return

            val nomeFinal = "Video_Baixado_${System.currentTimeMillis()}.ts"
            
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, nomeFinal)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp2t")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }

            val contentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val uri = contentResolver.insert(contentUri, contentValues)
            uri?.let {
                contentResolver.openOutputStream(it)?.use { output ->
                    arquivoOrigem.inputStream().use { input -> input.copyTo(output) }
                }
                atualizarStatus("SALVO NA PASTA DOWNLOADS!")
                Toast.makeText(this, "Vídeo salvo em Downloads!", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            atualizarStatus("Erro ao exportar: ${e.message}")
        }
    }

    fun atualizarStatus(mensagem: String) {
        runOnUiThread { binding.statusText.text = mensagem }
    }

    /**
     * Atualiza a barra de progresso (0 a 100)
     */
    fun atualizarProgresso(atual: Int, total: Int) {
        runOnUiThread {
            if (total > 0) {
                val porcentagem = (atual * 100) / total
                binding.progressBar.progress = porcentagem
            }
        }
    }

    fun baixarTextoDaUrl(url: String): String {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            val cookies = CookieManager.getInstance().getCookie(url)
            if (cookies != null) conn.setRequestProperty("Cookie", cookies)
            authHeader?.let { conn.setRequestProperty("Authorization", it) }
            conn.inputStream.bufferedReader().readText()
        } catch (e: Exception) { "Erro" }
    }

    fun baixarBytesDaUrl(url: String): ByteArray? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            val cookies = CookieManager.getInstance().getCookie(url)
            if (cookies != null) conn.setRequestProperty("Cookie", cookies)
            authHeader?.let { conn.setRequestProperty("Authorization", it) }
            conn.inputStream.readBytes()
        } catch (e: Exception) { null }
    }

    external fun stringFromJNI(): String
    external fun iniciarDownloadNativo(url: String, path: String)

    companion object {
        init { System.loadLibrary("myapplication") }
    }
}