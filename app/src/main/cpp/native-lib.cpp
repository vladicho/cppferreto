#include <jni.h>
#include <string>
#include <android/log.h>
#include <sstream>
#include <cstdio>

#define LOG_TAG "M3U8_DOWNLOADER"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_myapplication_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_myapplication_MainActivity_iniciarDownloadNativo(
        JNIEnv* env, jobject instance, jstring url, jstring path) {

    jclass clazz = env->GetObjectClass(instance);
    const char* nativePath = env->GetStringUTFChars(path, 0);
    const char* masterUrl = env->GetStringUTFChars(url, 0);

    // 1. Pegar o conteúdo do M3U8 (Texto)
    jmethodID idBaixarTexto = env->GetMethodID(clazz, "baixarTextoDaUrl", "(Ljava/lang/String;)Ljava/lang/String;");
    jstring conteudoJStr = (jstring)env->CallObjectMethod(instance, idBaixarTexto, url);
    const char* conteudo = env->GetStringUTFChars(conteudoJStr, 0);

    // Criar o arquivo de saída no Android
    std::string fullPath = std::string(nativePath) + "/video_baixado.ts";
    FILE* file = fopen(fullPath.c_str(), "wb");

    if (!file) {
        LOGI("C++: Erro ao criar arquivo em %s", fullPath.c_str());
        env->ReleaseStringUTFChars(url, masterUrl);
        env->ReleaseStringUTFChars(path, nativePath);
        env->ReleaseStringUTFChars(conteudoJStr, conteudo);
        return;
    }

    // 2. Método para baixar os bytes
    jmethodID idBaixarBytes = env->GetMethodID(clazz, "baixarBytesDaUrl", "(Ljava/lang/String;)[B");

    // Lógica para pegar a URL base (ex: http://site.com/video/)
    std::string baseUrl = std::string(masterUrl);
    size_t lastSlash = baseUrl.find_last_of('/');
    baseUrl = baseUrl.substr(0, lastSlash + 1);

    std::string line;
    std::stringstream ss(conteudo);
    int baixados = 0;

    while (std::getline(ss, line)) {
        if (!line.empty() && line[0] != '#') {
            // Alguns M3U8 tem links relativos, outros absolutos
            std::string segmentUrl;
            if (line.find("http") == 0) {
                segmentUrl = line;
            } else {
                segmentUrl = baseUrl + line;
            }

            LOGI("C++: Baixando segmento [%d]: %s", baixados, segmentUrl.c_str());

            jstring jSegUrl = env->NewStringUTF(segmentUrl.c_str());
            jbyteArray byteArray = (jbyteArray)env->CallObjectMethod(instance, idBaixarBytes, jSegUrl);

            if (byteArray != nullptr) {
                jbyte* bytes = env->GetByteArrayElements(byteArray, nullptr);
                jsize len = env->GetArrayLength(byteArray);

                // ESCREVE NO ARQUIVO VIA C++
                fwrite(bytes, 1, len, file);

                env->ReleaseByteArrayElements(byteArray, bytes, JNI_ABORT);
                baixados++;
                LOGI("C++: Salvo segmento %d com %d bytes", baixados, (int)len);
            }

            // Para o teste, vamos baixar apenas os 10 primeiros pedaços (aprox 20-30 seg de vídeo)
            if (baixados >= 10) break;
        }
    }

    fclose(file);
    LOGI("C++: Download concluído! Arquivo final: %s", fullPath.c_str());

    env->ReleaseStringUTFChars(url, masterUrl);
    env->ReleaseStringUTFChars(path, nativePath);
    env->ReleaseStringUTFChars(conteudoJStr, conteudo);
}