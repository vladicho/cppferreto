#include <jni.h>
#include <string>
#include <android/log.h>
#include <sstream>
#include <cstdio>
#include <vector>

#define LOG_TAG "M3U8_DOWNLOADER"

void updateStatus(JNIEnv* env, jobject instance, const std::string& msg) {
    jclass clazz = env->GetObjectClass(instance);
    jmethodID methodId = env->GetMethodID(clazz, "atualizarStatus", "(Ljava/lang/String;)V");
    if (methodId != nullptr) {
        jstring jmsg = env->NewStringUTF(msg.c_str());
        env->CallVoidMethod(instance, methodId, jmsg);
        env->DeleteLocalRef(jmsg);
    }
}

void updateProgress(JNIEnv* env, jobject instance, int current, int total) {
    jclass clazz = env->GetObjectClass(instance);
    jmethodID methodId = env->GetMethodID(clazz, "atualizarProgresso", "(II)V");
    if (methodId != nullptr) {
        env->CallVoidMethod(instance, methodId, current, total);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_myapplication_MainActivity_iniciarDownloadNativo(
        JNIEnv* env, jobject instance, jstring url, jstring path) {

    jclass clazz = env->GetObjectClass(instance);
    const char* nativePath = env->GetStringUTFChars(path, 0);
    const char* masterUrl = env->GetStringUTFChars(url, 0);

    updateStatus(env, instance, "Iniciando...");
    updateProgress(env, instance, 0, 100);

    jmethodID idBaixarTexto = env->GetMethodID(clazz, "baixarTextoDaUrl", "(Ljava/lang/String;)Ljava/lang/String;");
    jstring conteudoJStr = (jstring)env->CallObjectMethod(instance, idBaixarTexto, url);
    const char* conteudo = env->GetStringUTFChars(conteudoJStr, 0);

    if (conteudo == nullptr || std::string(conteudo).find("Erro") == 0) {
        updateStatus(env, instance, "Erro no link");
        env->ReleaseStringUTFChars(url, masterUrl);
        env->ReleaseStringUTFChars(path, nativePath);
        return;
    }

    std::string baseUrl = std::string(masterUrl);
    baseUrl = baseUrl.substr(0, baseUrl.find_last_of('/') + 1);

    std::string line;
    std::stringstream ss(conteudo);
    std::string realPlaylistUrl = "";
    while (std::getline(ss, line)) {
        if (line.find("#EXT-X-STREAM-INF") != std::string::npos && std::getline(ss, line)) {
            realPlaylistUrl = (line.find("http") == 0) ? line : baseUrl + line;
            break;
        }
    }

    const char* finalConteudo = conteudo;
    jstring finalConteudoJStr = conteudoJStr;
    if (!realPlaylistUrl.empty()) {
        jstring jRealUrl = env->NewStringUTF(realPlaylistUrl.c_str());
        finalConteudoJStr = (jstring)env->CallObjectMethod(instance, idBaixarTexto, jRealUrl);
        finalConteudo = env->GetStringUTFChars(finalConteudoJStr, 0);
        baseUrl = realPlaylistUrl.substr(0, realPlaylistUrl.find_last_of('/') + 1);
    }

    std::string fullPath = std::string(nativePath) + "/video_baixado.ts";
    FILE* file = fopen(fullPath.c_str(), "wb");
    if (!file) {
        updateStatus(env, instance, "Erro ao criar arquivo");
        env->ReleaseStringUTFChars(url, masterUrl);
        env->ReleaseStringUTFChars(path, nativePath);
        return;
    }

    std::vector<std::string> segments;
    std::stringstream ss2(finalConteudo);
    while (std::getline(ss2, line)) {
        if (!line.empty() && line[0] != '#') {
            segments.push_back((line.find("http") == 0) ? line : baseUrl + line);
        }
    }

    jmethodID idBaixarBytes = env->GetMethodID(clazz, "baixarBytesDaUrl", "(Ljava/lang/String;)[B");
    int total = segments.size();
    for (int i = 0; i < total; ++i) {
        char statusMsg[50];
        sprintf(statusMsg, "Baixando: %d / %d", i + 1, total);
        updateStatus(env, instance, statusMsg);
        updateProgress(env, instance, i + 1, total);

        jstring jSegUrl = env->NewStringUTF(segments[i].c_str());
        jbyteArray byteArray = (jbyteArray)env->CallObjectMethod(instance, idBaixarBytes, jSegUrl);
        if (byteArray != nullptr) {
            jbyte* bytes = env->GetByteArrayElements(byteArray, nullptr);
            fwrite(bytes, 1, env->GetArrayLength(byteArray), file);
            env->ReleaseByteArrayElements(byteArray, bytes, JNI_ABORT);
        }
        env->DeleteLocalRef(jSegUrl);
    }

    fclose(file);
    updateStatus(env, instance, "Concluído!");
    updateProgress(env, instance, 100, 100);

    env->ReleaseStringUTFChars(url, masterUrl);
    env->ReleaseStringUTFChars(path, nativePath);
    if (finalConteudo != conteudo) env->ReleaseStringUTFChars(finalConteudoJStr, finalConteudo);
    env->ReleaseStringUTFChars(conteudoJStr, conteudo);
}