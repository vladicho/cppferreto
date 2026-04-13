package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.example.myapplication.databinding.ActivityMainBinding
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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

        // Example of a call to a native method
        binding.sampleText.text = stringFromJNI()

        // Configuração completa do WebView para suportar logins e sites modernos
        val webSettings = binding.webview.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true // ESSENCIAL para logins modernos
        webSettings.databaseEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        webSettings.setSupportZoom(true)
        
        // Define um User-Agent de um navegador real (Chrome no Android)
        webSettings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Mobile Safari/537.36"

        // Habilitar Cookies
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webview, true)

        binding.webview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                view?.loadUrl(request?.url.toString())
                return true
            }
            // Para compatibilidade com versões antigas
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let { view?.loadUrl(it) }
                return true
            }
        }

        binding.btnOpenSite.setOnClickListener {
            val url = binding.editUrl.text.toString()
            if (url.isNotEmpty()) {
                binding.webview.loadUrl(url)
            } else {
                binding.sampleText.text = "Insira uma URL primeiro"
            }
        }

        binding.btnDownload.setOnClickListener {
            val user = binding.editUser.text.toString()
            val pass = binding.editPass.text.toString()
            val urlM3u8 = binding.editUrl.text.toString()
            
            if (urlM3u8.isEmpty()) {
                binding.sampleText.text = "Por favor, insira a URL do M3U8"
                return@setOnClickListener
            }

            // Criar o cabeçalho de autenticação (Basic Auth)
            if (user.isNotEmpty() && pass.isNotEmpty()) {
                val credentials = "$user:$pass"
                authHeader = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
            } else {
                authHeader = null
            }

            val pastaDestino = getExternalFilesDir(null)?.absolutePath ?: filesDir.absolutePath
            thread {
                iniciarDownloadNativo(urlM3u8, pastaDestino)
            }
            binding.sampleText.text = "Baixando via C++: $urlM3u8"
        }
    }

    /**
     * Função que o C++ vai chamar para obter os dados da internet (Texto)
     */
    fun baixarTextoDaUrl(url: String): String {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            authHeader?.let { connection.setRequestProperty("Authorization", it) }
            connection.inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            "Erro ao baixar texto: ${e.message}"
        }
    }

    /**
     * Função que o C++ vai chamar para baixar os segmentos do vídeo (Bytes)
     */
    fun baixarBytesDaUrl(url: String): ByteArray? {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            authHeader?.let { connection.setRequestProperty("Authorization", it) }
            connection.inputStream.readBytes()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * A native method that is implemented by the 'myapplication' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    external fun iniciarDownloadNativo(url: String, path: String)

    override fun onBackPressed() {
        if (binding.webview.canGoBack()) {
            binding.webview.goBack()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        // Used to load the 'myapplication' library on application startup.
        init {
            System.loadLibrary("myapplication")
        }
    }
}