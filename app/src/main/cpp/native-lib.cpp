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
    std::string data_nick = "NICK justinfan733";
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

void irc_parse(char* line, int len) {
    __android_log_write(ANDROID_LOG_DEBUG, "irc_client", line);

    struct irc_message *current_msg = &ring_buffer_[ring_position_];
    memset(current_msg, 0, sizeof(struct irc_message));

    char *first_space = strstr(line, " ");
    if (first_space != nullptr) {
        int first_space_offset = first_space - line;
        if (first_space[1] == ':') {
            memcpy(current_msg->prefix, line, first_space_offset);
            current_msg->prefix[first_space_offset] = '\0';

            __android_log_write(ANDROID_LOG_DEBUG, "irc_client msg_prefix", current_msg->prefix);

            char *second_space = strstr(&first_space[1], " ");
            if (second_space == nullptr) {
                if (strstr(current_msg->prefix, "PING") != nullptr) {
                    char response_buf[256];
                    snprintf(response_buf, 255, "PONG %s", &first_space[1]);
                    if (irc_send_msg(response_buf)) {
                        __android_log_write(ANDROID_LOG_DEBUG, "irc_client", response_buf);
                    }
                }
            } else {
                int second_space_offset = second_space - 2 - (first_space - 1);
                memcpy(current_msg->name, &first_space[1], second_space_offset);
                current_msg->name[second_space_offset] = '\0';

                __android_log_write(ANDROID_LOG_DEBUG, "irc_client msg_name", current_msg->name);

                char *third_space = strstr(&second_space[1], " ");
                if (third_space != nullptr) {
                    int third_space_offset = third_space - 2 - (second_space - 1);
                    memcpy(current_msg->command, &second_space[1], third_space_offset);
                    current_msg->command[third_space_offset] = '\0';

                    __android_log_write(ANDROID_LOG_DEBUG, "irc_client msg_command", current_msg->command);

                    char *msg = strstr(&third_space[1], ":");
                    if (msg != nullptr) {
                        int msg_offset = msg - (third_space - 1);
                        int msg_len = len - (first_space_offset + 1 + second_space_offset + 1 + third_space_offset + msg_offset);
                        memcpy(current_msg->msg, &msg[1], msg_len);
                        current_msg->msg[msg_len] = '\0';

                        __android_log_write(ANDROID_LOG_DEBUG, "irc_client msg_msg", current_msg->msg);

                        ring_position_inc();
                    }
                }
            }
        }
    }
}

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
                while (line_start[0] == ' ') {
                    line_start++;
                    if (line_start[0] == '\0') {
                        line_start = nullptr;
                        break;
                    }
                }
                irc_parse(line_start, line_end-line_start);
                line_start = line_end + eol_o;
            }
            if (line_start != nullptr) {
                irc_parse(line_start, len - (line_start - buffer_));
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
                irc_join("summit1g");
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
}

#ifdef __cplusplus
}
#endif