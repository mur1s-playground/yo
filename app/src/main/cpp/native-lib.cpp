#include <jni.h>
#include <string>

#include <sys/socket.h>
#include <netinet/in.h>
#include <fcntl.h>
#include <netdb.h>
#include <unistd.h>

#include <pthread.h>

#include <android/log.h>

#include <sstream>

#ifdef __cplusplus
extern "C" {
#endif

#define MAXPREFIXSIZE 1024
#define MAXNAMESIZE 256
#define MAXCOMMANDSIZE 50

#define MAXDATASIZE 8192

#define MAXMSGSIZE (MAXDATASIZE - MAXCOMMANDSIZE -MAXNAMESIZE - MAXPREFIXSIZE)

struct irc_message {
    char prefix[MAXPREFIXSIZE];
    char name[MAXNAMESIZE];
    char command[MAXCOMMANDSIZE];
    char msg[MAXMSGSIZE];
};

int socket_ = -1;
bool connected_ = false;

int ring_position_r_ = 0;
int ring_position_ = 0;
pthread_mutex_t ring_position_lock_;

int ring_size_ = 250;
struct irc_message *ring_buffer_ = nullptr;

char* buffer_ = nullptr;

pthread_t receive_thread = -1L;

void ring_position_inc() {
    pthread_mutex_lock(&ring_position_lock_);
    ring_position_ = (ring_position_ + 1) % ring_size_;
    pthread_mutex_unlock(&ring_position_lock_);
}

bool socket_init() {
    if ((socket_ = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP)) == -1) {
        return false;
    }

    int on = 1;
    if (setsockopt(socket_, SOL_SOCKET, SO_REUSEADDR, (char const*) &on, sizeof(on)) == -1) {
        return false;
    }

    fcntl(socket_, F_SETFL, O_NONBLOCK);
    fcntl(socket_, F_SETFL, O_ASYNC);

    return true;
}

bool socket_connect() {
    hostent* he;

    if (!(he = gethostbyname("irc.chat.twitch.tv"))) {
        return false;
    }

    sockaddr_in addr;
    addr.sin_family = AF_INET;
    addr.sin_port = htons(6667);
    addr.sin_addr = *((const in_addr*)he->h_addr);
    //memset(&(addr.sin_zero), '\0', 8);

    if (connect(socket_, (sockaddr*)&addr, sizeof(addr)) == -1) {
        close(socket_);
        return false;
    }

    connected_ = true;
    return true;
}

void socket_disconnect() {
    __android_log_write(ANDROID_LOG_DEBUG, "irc_client", "disconnecting");
    if (connected_) {
        close(socket_);
        connected_ = false;
    }
}

bool socket_send_data(const char* data) {
    if (connected_) {
        if (send(socket_, data, strlen(data), 0) == -1) {
            return false;
        }
        return true;
    }
    return false;
}

int socket_recv_data(char *data_out) {
    memset(data_out, 0, MAXDATASIZE);

    int bytes = recv(socket_, data_out, MAXDATASIZE - 1, 0);

    if (bytes > 0) {
        return bytes;
    } else {
        __android_log_write(ANDROID_LOG_ERROR, "irc_client", "socket_recv_data failed");
        socket_disconnect();
    }
    return 0;
}

bool irc_connect() {
    bool s_i = socket_init();
    if (!s_i) {
        __android_log_write(ANDROID_LOG_ERROR, "irc_client", "socket_init failed");
    } else {
        __android_log_write(ANDROID_LOG_DEBUG, "irc_client", "socket_init successful");
        bool s_c = socket_connect();
        if (!s_c) {
            __android_log_write(ANDROID_LOG_DEBUG, "irc_client", "socket_connect failed");
        } else {
            __android_log_write(ANDROID_LOG_DEBUG, "irc_client", "socket_connect successful");
            return true;
        }
    }
    return false;
}

bool irc_send_msg(std::string data) {
    data += "\n";
    if (socket_send_data(data.c_str())) {
        return true;
    }
    socket_disconnect();
    return false;
}

bool irc_login() {
    std::string data_nick = "NICK justinfan7331";
    if (irc_send_msg(data_nick.c_str())) {
        __android_log_write(ANDROID_LOG_DEBUG, "irc_client", "sent NICK");
        std::string data_user = "USER yo 8 * :client";
        if (irc_send_msg(data_user.c_str())) {
            __android_log_write(ANDROID_LOG_DEBUG, "irc_client", "sent USER");
            __android_log_write(ANDROID_LOG_DEBUG, "irc_client", "logged in");
            return true;
        }
    }
    return false;
}

bool irc_enable_twitch_tags() {
    std::string data_ext = "CAP REQ :twitch.tv/tags";
    if (irc_send_msg(data_ext.c_str())) {
        __android_log_write(ANDROID_LOG_DEBUG, "irc_client", "enabled twitch tags");
        return true;
    }
    return false;
}

bool irc_join(std::string channel) {
    std::string join_chan = "join #" + channel;
    if (irc_send_msg(join_chan.c_str())) {
        __android_log_write(ANDROID_LOG_DEBUG, "irc_client", join_chan.c_str());
        return true;
    }
    return false;
}


int irc_parse_separators = 0;
char *irc_parse_write_head = nullptr;
struct irc_message *current_msg = nullptr;

int irc_parse_append_till_separator(const char *line, const int line_pos, const int line_len, const char *separator, bool incomplete) {
    const char *append_end = strstr(&line[line_pos], separator);
    int bytes = 0;
    if (append_end == nullptr) {
        bytes = line_len - line_pos;
    } else {
        bytes = append_end - &line[line_pos];
    }
    memcpy(irc_parse_write_head, &line[line_pos], bytes);
    irc_parse_write_head += bytes;
    if (append_end != nullptr || (irc_parse_separators == 3 && !incomplete)) {
        irc_parse_write_head[0] = '\0';
        bytes++;
        switch (irc_parse_separators) {
            case 0: irc_parse_write_head = current_msg->name; break;
            case 1: irc_parse_write_head = current_msg->command; break;
            case 2: irc_parse_write_head = current_msg->msg; break;
            case 3: irc_parse_write_head = current_msg->msg; break;
            default: break;
        }
        irc_parse_separators++;
    }
    return bytes;
}

void irc_parse(char* line, int len, bool irc_parse_incomplete) {
    __android_log_write(ANDROID_LOG_DEBUG, "irc_client", line);

    if (irc_parse_write_head == nullptr) {
        current_msg = &ring_buffer_[ring_position_];
        memset(current_msg, 0, sizeof(struct irc_message));
        irc_parse_write_head = current_msg->prefix;
    }

    int line_pos = 0;

    do {
        if (irc_parse_separators < 4) {
            line_pos += irc_parse_append_till_separator(line, line_pos, len, " ", irc_parse_incomplete);
        } else {
            line_pos += irc_parse_append_till_separator(line, line_pos, len, "\n", irc_parse_incomplete);
        }
    } while (line_pos < len);

    __android_log_write(ANDROID_LOG_DEBUG, "irc_client msg_prefix", current_msg->prefix);
    __android_log_write(ANDROID_LOG_DEBUG, "irc_client msg_name", current_msg->name);
    __android_log_write(ANDROID_LOG_DEBUG, "irc_client msg_command", current_msg->command);
    __android_log_write(ANDROID_LOG_DEBUG, "irc_client msg_msg", current_msg->msg);

    if (!irc_parse_incomplete) {
        irc_parse_separators = 0;
        irc_parse_write_head = nullptr;
        if (strstr(current_msg->command, "PRIVMSG") != nullptr) {
            ring_position_inc();
        } else if (strstr(current_msg->prefix, "PING") != nullptr) {
            char response_buf[256];
            snprintf(response_buf, 255, "PONG %s", current_msg->name);
            if (irc_send_msg(response_buf)) {
                __android_log_write(ANDROID_LOG_DEBUG, "irc_client", response_buf);
            }
        }
    }
}

bool irc_receive_last_incomplete = false;

void irc_receive_loop() {
    while (connected_) {
        int len = socket_recv_data(buffer_);
        if (len > 0) {
            char *line_start = buffer_;
            char *line_end = nullptr;
            while ((line_end = strstr(line_start, "\n")) != nullptr) {
                int eol_o = 1;
                if (strstr(line_start, "\r") == line_end - 1) {
                    eol_o++;
                    line_end--;
                }
                if (!irc_receive_last_incomplete) {
                    while (line_start[0] == ' ') {
                        line_start++;
                        if (line_start[0] == '\0') {
                            line_start = nullptr;
                            break;
                        }
                    }
                }
                __android_log_write(ANDROID_LOG_DEBUG, "irc_client line", "complete!");
                irc_receive_last_incomplete = false;
                irc_parse(line_start, line_end-line_start, false);
                line_start = line_end + eol_o;
            }
            if (line_start - buffer_ < len && line_start != nullptr) {
                __android_log_write(ANDROID_LOG_DEBUG, "irc_client line", "incomplete!");
                irc_receive_last_incomplete = true;
                irc_parse(line_start, len - (line_start - buffer_), true);
            }
        }
    }
}

JNIEXPORT jstring JNICALL Java_de_mur1_yo_MainActivity_stringFromJNI(JNIEnv *env, jobject /* this */) {
    buffer_ = (char *) malloc(MAXDATASIZE);
    ring_buffer_ = (struct irc_message *) malloc(ring_size_ * sizeof(struct irc_message));
    pthread_mutex_init(&ring_position_lock_, nullptr);

    std::string status = "";

    if (irc_connect()) {
        if (irc_login()) {
            pthread_create(&receive_thread, nullptr, (void *(*)(void*))&irc_receive_loop, nullptr);
            if (irc_enable_twitch_tags()) {
                irc_join("xqcow");
            }
        }
    }

    return env->NewStringUTF(status.c_str());
}

JNIEXPORT jstring JNICALL Java_de_mur1_yo_UpdateData_updateMessage(JNIEnv *env, jobject /* this */) {
    std::stringstream result_ss;
    pthread_mutex_lock(&ring_position_lock_);
    if (ring_position_r_ != ring_position_) {
        result_ss << ring_buffer_[ring_position_r_].prefix << '\n';
        result_ss << ring_buffer_[ring_position_r_].name << '\n';
        result_ss << ring_buffer_[ring_position_r_].command << '\n';
        result_ss << ring_buffer_[ring_position_r_].msg;
        ring_position_r_ = (ring_position_r_ + 1) % ring_size_;
    }
    pthread_mutex_unlock(&ring_position_lock_);
    return env->NewStringUTF(result_ss.str().c_str());

    //return env->NewStringUTF("");
}

#ifdef __cplusplus
}
#endif