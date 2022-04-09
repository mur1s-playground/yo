
#include "main.h"
#include "irc_client.h"
#include "mutex.h"
#include "util.h"
#include "thread.h"

#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <time.h>

struct logger main_logger;

bool            init = false;

struct mutex    lock;
ThreadPool      main_tp;

bool            networking_manage       = false;
bool            mgm_reconnect           = false;
bool            mgm_channels_updated    = false;
bool            mgm_connected           = false;

char                        *username_          = nullptr;
char                        *token_             = nullptr;
char                        *cert_hash_         = nullptr;
std::vector<std::string>    channels_;
int                         timeout_seconds_    = 1;

struct irc_client           irc_c;

void networking_management_loop(void *args) {
    while (true) {
        mutex_wait_for(&lock);
        int timeout_seconds = timeout_seconds_;

        if (!irc_c.tls_c.connected) {
            mgm_reconnect = true;
        }

        if (mgm_reconnect) {
            if (mgm_connected) {
                irc_client_destroy(&irc_c);
            }
            irc_client_init(&irc_c, username_, token_, "irc.chat.twitch.tv", 6697, cert_hash_);
            if (irc_client_connection_establish(&irc_c)) {
                mgm_connected = true;
                mgm_reconnect = false;
            } else {
                irc_client_destroy(&irc_c);
            }
        }

        if (mgm_connected && mgm_channels_updated) {
            for (int c = 0; c < channels_.size(); c++) {
                bool found = false;
                for (int c_ = 0; c_ < irc_c.channels->size(); c_++) {
                    if (util_str_equals(channels_[c].c_str(), (*irc_c.channels)[c_].c_str())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    irc_client_channel_add(&irc_c, channels_[c]);
                    irc_client_join(&irc_c, channels_[c]);
                }
            }
            vector<int> to_remove = vector<int>();
            for (int c_ = 0; c_ < irc_c.channels->size(); c_++) {
                bool found = false;
                for (int c = 0; c < channels_.size(); c++) {
                    if (util_str_equals(channels_[c].c_str(), (*irc_c.channels)[c_].c_str())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    irc_client_part(&irc_c, (*irc_c.channels)[c_]);
                    to_remove.push_back(c_);
                }
            }
            for (int t = to_remove.size() - 1; t >= 0; t--) {
                irc_client_channel_remove(&irc_c, (*irc_c.channels)[t]);
            }
        }

        if (!networking_manage) {
            if (mgm_connected) irc_client_destroy(&irc_c);
            break;
        }
        mutex_release(&lock);
        util_sleep(timeout_seconds * 1000);
    }
    mutex_release(&lock);
}

int copy_str_to_bytearray(const char *str, jchar *c, int m_pos) {
    int pos = 0;
    while (str[pos] != '\0') {
        c[m_pos] = str[pos];
        m_pos++;
        pos++;
    }
    return m_pos;
}

extern "C" {


JNIEXPORT void JNICALL Java_de_mur1_yolog_UpdateChatDirect_init(JNIEnv *env, jclass jclazz) {
    if (!init) {
        logger_init(&main_logger);
        logger_level_set(&main_logger, LOG_LEVEL_ERROR);
        logger_write(&main_logger, LOG_LEVEL_VERBOSE, "MAIN", "start");

        mutex_init(&lock);
        thread_pool_init(&main_tp, 10);
        channels_ = std::vector<std::string>();
        init = true;
    }
}

JNIEXPORT void JNICALL Java_de_mur1_yolog_UpdateChatDirect_setSettings(JNIEnv *env, jclass jclazz, jboolean connect, jstring channels, jstring username, jstring token, jstring cert_hash, jint timeout_seconds) {
    mutex_wait_for(&lock);

    jboolean is_copy;
    const char *channels_c = env->GetStringUTFChars(channels, &is_copy);
    std::string channels_s(channels_c);
    std::vector<std::string> channels_v = util_split(channels_s, ",");
    channels_.clear();
    for (int c = 0; c < channels_v.size(); c++) {
        channels_.push_back(channels_v[c]);
        logger_write(&main_logger, LOG_LEVEL_VERBOSE, "setSettings channel add", channels_v[c].c_str());
        mgm_channels_updated = true;
    }
    env->ReleaseStringUTFChars(channels, channels_c);

    const char *username_c = env->GetStringUTFChars(username, &is_copy);
    if (username_ != nullptr) {
        if (!util_str_equals(username_c, username_)) {
            mgm_reconnect = true;
            free(username_);
            util_chararray_from_const(username_c, &username_);
        }
    } else {
        mgm_reconnect = true;
        util_chararray_from_const(username_c, &username_);
    }
    env->ReleaseStringUTFChars(username, username_c);

    const char *token_c = env->GetStringUTFChars(token, &is_copy);
    if (token_ != nullptr) {
        if (!util_str_equals(token_c, token_)) {
            mgm_reconnect = true;
            free(token_);
            util_chararray_from_const(token_c, &token_);
        }
    } else {
        mgm_reconnect = true;
        util_chararray_from_const(token_c, &token_);
    }
    env->ReleaseStringUTFChars(token, token_c);

    const char *cert_hash_c = env->GetStringUTFChars(cert_hash, &is_copy);
    if (cert_hash_ != nullptr) {
        if (!util_str_equals(cert_hash_c, cert_hash_)) {
            mgm_reconnect = true;
            free(cert_hash_);
            util_chararray_from_const(cert_hash_c, &cert_hash_);
        }
    } else {
        mgm_reconnect = true;
        util_chararray_from_const(cert_hash_c, &cert_hash_);
    }
    env->ReleaseStringUTFChars(cert_hash, cert_hash_c);

    timeout_seconds_ = timeout_seconds;

    if (connect == JNI_TRUE) {
        if (!networking_manage) {
            networking_manage = true;
            thread_create(&main_tp, (void *) &networking_management_loop, nullptr);
        }
    } else if (connect == JNI_FALSE) {
        networking_manage = false;
    }

    mutex_release(&lock);
}

JNIEXPORT jcharArray JNICALL Java_de_mur1_yolog_UpdateChatDirect_updateMessage(JNIEnv *env, jclass jclazz) {
    mutex_wait_for(&lock);

    if (irc_c.tls_c.connected) {
        struct irc_message irc_m;
        if (irc_client_message_next(&irc_c, &irc_m)) {
            jboolean is_copy;
            jcharArray irc_mb = env->NewCharArray(TLS_MAXDATASIZE + 100);
            jchar *irc_mbc = env->GetCharArrayElements(irc_mb, &is_copy);
            int m_pos = 0;

            long t = time(nullptr);
            char time_str[24];
            snprintf(time_str, 24, "%lu", t);

            m_pos = copy_str_to_bytearray("time:", irc_mbc, m_pos);
            m_pos = copy_str_to_bytearray(time_str, irc_mbc, m_pos);
            m_pos = copy_str_to_bytearray("\n", irc_mbc, m_pos);

            m_pos = copy_str_to_bytearray("prefix:", irc_mbc, m_pos);
            m_pos = copy_str_to_bytearray(irc_m.prefix, irc_mbc, m_pos);
            m_pos = copy_str_to_bytearray("\n", irc_mbc, m_pos);

            m_pos = copy_str_to_bytearray("name:", irc_mbc, m_pos);
            m_pos = copy_str_to_bytearray(irc_m.name, irc_mbc, m_pos);
            m_pos = copy_str_to_bytearray("\n", irc_mbc, m_pos);

            m_pos = copy_str_to_bytearray("command:", irc_mbc, m_pos);
            m_pos = copy_str_to_bytearray(irc_m.command, irc_mbc, m_pos);
            m_pos = copy_str_to_bytearray("\n", irc_mbc, m_pos);

            m_pos = copy_str_to_bytearray("channel:", irc_mbc, m_pos);
            m_pos = copy_str_to_bytearray(irc_m.channel, irc_mbc, m_pos);
            m_pos = copy_str_to_bytearray("\n", irc_mbc, m_pos);

            m_pos = copy_str_to_bytearray("msg:", irc_mbc, m_pos);
            m_pos = copy_str_to_bytearray(irc_m.msg, irc_mbc, m_pos);
            m_pos = copy_str_to_bytearray("\n", irc_mbc, m_pos);
            memset(&irc_mbc[m_pos], 0, TLS_MAXDATASIZE + 100 - m_pos - 1);

            env->ReleaseCharArrayElements(irc_mb, irc_mbc, 0);
            mutex_release(&lock);
            return irc_mb;
        }
    }
    mutex_release(&lock);
    return nullptr;
}

JNIEXPORT jboolean JNICALL Java_de_mur1_yolog_UpdateChatDirect_send(JNIEnv *env, jclass jclazz, jstring channel, jstring message) {
    jboolean result = JNI_FALSE;
    mutex_wait_for(&lock);
    if (irc_c.tls_c.connected) {
        jboolean is_copy;
        const char *channel_c = env->GetStringUTFChars(channel, &is_copy);
        std::string channel_s(channel_c);
        const char *message_c = env->GetStringUTFChars(message, &is_copy);
        std::string message_s(message_c);
        if (irc_client_send_privmsg(&irc_c, channel_c, message_c)) {
            result = JNI_TRUE;
        }
        env->ReleaseStringUTFChars(channel, channel_c);
        env->ReleaseStringUTFChars(message, message_c);
    }
    mutex_release(&lock);
    return result;
}

}