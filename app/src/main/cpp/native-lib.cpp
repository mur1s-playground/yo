#include <jni.h>
#include <string>

#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <fcntl.h>
#include <netdb.h>
#include <unistd.h>

#include <pthread.h>

#include <android/log.h>

#include <sstream>
#include <vector>

#ifdef __cplusplus
extern "C" {
#endif

void jni_getUTFChars(JNIEnv *env, jstring jstr, char **out_char) {
    jboolean is_copy = false;
    const char *buf = env->GetStringUTFChars(jstr, &is_copy);
    int len = strlen(buf);

    char *target = (char *) malloc(len + 1);
    if (len > 0) {
        memcpy(target, buf, len);
    }
    target[len] = '\0';

    env->ReleaseStringUTFChars(jstr, buf);

    *out_char = target;
}

std::string& util_ltrim(std::string& str, const std::string& chars) {
    str.erase(0, str.find_first_not_of(chars));
    return str;
}

std::string& util_rtrim(std::string& str, const std::string& chars) {
    str.erase(str.find_last_not_of(chars) + 1);
    return str;
}

std::string& util_trim(std::string& str, const std::string& chars) {
    return util_ltrim(util_rtrim(str, chars), chars);
}

std::string http_response_date_now() {
    std::stringstream datenow;
    datenow << "Date: ";

    time_t now = time(nullptr);

    tm* gmtm = gmtime(&now);

    std::string gmt(asctime(gmtm));
    gmt = util_trim(gmt, "\r\n\t ");
    datenow << gmt << " UTC\n";

    std::string result = datenow.str();
    return result;
}

std::vector<std::string> util_split(const std::string& str, const std::string& separator) {
    std::vector<std::string> result;
    int start = 0;
    int end = str.find_first_of(separator, start);
    while (end != std::string::npos) {
        result.push_back(str.substr(start, end - start));
        start = end + 1;
        end = str.find_first_of(separator, start);
    }
    result.push_back(str.substr(start));
    return result;
}

std::string http_request_get_param(std::vector<std::string>* params, std::string param) {
    std::string result = "";
    for (int p = 0; p < params->size(); p++) {
        std::vector<std::string> splt = util_split((*params)[p].c_str(), "=");
        const char* sparam = splt[0].c_str();
        if (strstr(sparam, param.c_str()) == sparam && splt.size() > 1) {
            std::stringstream res_ss;
            for (int s = 1; s < splt.size(); s++) {
                res_ss << splt[s];
                if (s + 1 < splt.size()) res_ss << "=";
            }
            result = res_ss.str();
            break;
        }
    }
    return result;
}

struct token_receiver_client_params {
    pthread_t thread;
    int client_socket;
};

pthread_t token_receiver_thread = -1L;
bool token_receiver_listen = true;
int token_receiver_socket = -1;

char *token_ = nullptr;
bool token_ready = false;
char *token_receiver_code_ = nullptr;

bool token_receiver_init() {
    if ((token_receiver_socket = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP)) == -1) {
        return false;
    }

    int on = 1;
    if (setsockopt(token_receiver_socket, SOL_SOCKET, SO_REUSEADDR, (char const*) &on, sizeof(on)) == -1) {
        return false;
    }

    sockaddr_in token_receiver_sockaddr;
    token_receiver_sockaddr.sin_family = AF_INET;
    token_receiver_sockaddr.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    token_receiver_sockaddr.sin_port = htons(8765);

    if (bind(token_receiver_socket, (const sockaddr *)&token_receiver_sockaddr, sizeof(token_receiver_sockaddr)) == -1) {
        __android_log_write(ANDROID_LOG_ERROR, "token_receiver", "bind failed");
        return false;
    }

    if (listen(token_receiver_socket, 1) == -1) {
        __android_log_write(ANDROID_LOG_ERROR, "token_receiver", "listen failed");
        return false;
    }
    return true;
}

void token_receiver_process_request(void *param) {
    struct token_receiver_client_params *trcp = (struct token_receiver_client_params *) param;
    bool error = false;

    char linebuffer[2048];

    int last_char = 0;
    int line_len = INT_MAX;
    int char_ct = 0;

    char buffer = 0;

    std::vector<std::string> request = std::vector<std::string>();

    while (!error) {
        int bytes = recv(trcp->client_socket, &buffer, 1, 0);
        if (bytes > 0) {
            if (char_ct + 1 < 2048) {
                linebuffer[char_ct] = buffer;
                char_ct++;

                last_char = buffer;

                if (last_char == 13) {
                    line_len = char_ct;

                    if (line_len == 2) {
                        std::string request_url("/");
                        std::vector<std::string> request_path = std::vector<std::string>();
                        std::vector<std::string> params = std::vector<std::string>();

                        std::stringstream response;

                        if (request.size() > 0) {
                            std::vector<std::string> req_0_splt = util_split(request[0], " ");

                            if (req_0_splt.size() > 1) {
                                request_url = req_0_splt[1];

                                std::vector<std::string> url_param_split = util_split(request_url, "?");
                                request_path = util_split(url_param_split[0].c_str(), "/");
                                request_path.erase(request_path.begin());

                                if (url_param_split.size() > 1) {
                                    params = util_split(url_param_split[1], "&");
                                }
                            }

                            std::string content;

                            const char *req_0 = request[0].c_str();
                            if (strstr(req_0, "GET") == req_0) {
                                std::string token = http_request_get_param(&params, "access_token");
                                std::string code = http_request_get_param(&params, "state");

                                response << "HTTP/1.1 200 OK\n";
                                if ((token.length() == 0 || code.length() == 0) && request_path[0].length() == 0) {
                                    content = "<html><body><script>window.location.href = document.location.href.replace(\"#\",\"?\");</script></body></html>";
                                } else {
                                    const char* code_c = code.c_str();
                                    if (strstr(code_c, token_receiver_code_) == code_c) {
                                        if (token_ != nullptr) {
                                            free(token_);
                                        }
                                        token_ = (char *)malloc(token.length()+1);
                                        memcpy(token_, token.c_str(), token.length());
                                        token_[token.length()] = '\0';
                                        token_ready = true;
                                        token_receiver_listen = false;
                                        close(token_receiver_socket);
                                        content = "{ \"thanks\": true }";
                                    } else {
                                        content = "{ \"state\": error }";
                                    }
                                }
                            } else {
                                response << "HTTP/1.1 400 Bad Request\n";
                                content = "{ \"error\": true }";
                            }
                            response << http_response_date_now();
                            response << "Content-Type: text/html\n";
                            response << "Content-Length: " << content.length() << "\n";
                            response << "Connection: closed\n";
                            response << "\n";
                            response << content;

                            if (send(trcp->client_socket, response.str().c_str(), response.str().length(), 0) == -1) {
                                __android_log_write(ANDROID_LOG_ERROR, "token_receiver", "process response send failed");
                            } else {
                                __android_log_write(ANDROID_LOG_DEBUG, "token_receiver response", response.str().c_str());
                            }
                            close(trcp->client_socket);
                            break;
                        }
                    } else{
                        linebuffer[char_ct] = '\0';
                        std::string tmp(linebuffer);
                        request.push_back(tmp);
                        memset(linebuffer, 0, char_ct);
                        __android_log_write(ANDROID_LOG_DEBUG, "token_receiver line", tmp.c_str());
                    }
                    char_ct = 0;
                }
            } else {
                error = true;
            }
        } else if (bytes == 0) {

        } else {
            error = true;
        }
    }
    free(trcp);
}

void token_receiver_listen_loop() {
    while (token_receiver_listen) {
        sockaddr client_sockaddr;
        socklen_t client_len = sizeof(client_sockaddr);
        memset(&client_sockaddr, 0, sizeof(client_sockaddr));
        struct token_receiver_client_params *trcp = (struct token_receiver_client_params *) malloc(sizeof(struct token_receiver_client_params));
        __android_log_write(ANDROID_LOG_DEBUG, "token_receiver", "waiting for connection");
        trcp->client_socket = accept(token_receiver_socket, (sockaddr *) &client_sockaddr, &client_len);

        if (trcp->client_socket >= 0) {
            pthread_create(&trcp->thread, nullptr, (void *(*)(void *)) &token_receiver_process_request, (void *) trcp);
            __android_log_write(ANDROID_LOG_DEBUG, "token_receiver", "client thread started");
        } else {
            __android_log_write(ANDROID_LOG_ERROR, "token_receiver", "client thread error accepting connection");
            free(trcp);
        }
    }
    token_receiver_thread = -1L;
}

JNIEXPORT jstring JNICALL Java_de_mur1_yo_UpdateToken_getTokenIfReady(JNIEnv *env, jobject) {
    if (token_ready) {
        token_ready = false;
        return env->NewStringUTF(token_);
    }
    return env->NewStringUTF("");
}

JNIEXPORT jboolean JNICALL Java_de_mur1_yo_MainActivity_prepareTokenReceive(JNIEnv *env, jobject, jstring code) {
    if (token_receiver_socket > -1) {
        token_receiver_listen = false;
        close(token_receiver_socket);
        token_receiver_socket = -1;
    }

    while (token_receiver_thread != -1L) {
        usleep(10);
    }

    if (token_receiver_code_ != nullptr) {
        free(token_receiver_code_);
    }

    jni_getUTFChars(env, code, &token_receiver_code_);

    if (!token_receiver_init()) {
        __android_log_write(ANDROID_LOG_ERROR, "token_receiver", "init failed");
        return JNI_FALSE;
    }
    pthread_create(&token_receiver_thread, nullptr, (void *(*)(void*))&token_receiver_listen_loop, nullptr);
    __android_log_write(ANDROID_LOG_ERROR, "token_receiver", "thread started");
    return JNI_TRUE;
}

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

char *channel_ = nullptr;
char *username_ = nullptr;

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
    bool auth_set = false;
    if (strlen(token_) > 0) {
        auth_set = true;

        std::stringstream data_pass_ss;
        data_pass_ss << "PASS oauth:" << token_;
        std::string data_pass = data_pass_ss.str();
        if (irc_send_msg(data_pass.c_str())) {
            __android_log_write(ANDROID_LOG_DEBUG, "irc_client", "sent PASS");
        } else {
            return false;
        }
    }

    std::stringstream data_nick_ss;
    data_nick_ss << "NICK ";
    if (auth_set) {
        data_nick_ss << username_;
    } else {
        data_nick_ss << "justinfan1337";
    }

    std::string data_nick = data_nick_ss.str();
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
    __android_log_write(ANDROID_LOG_DEBUG, "receive thread", "start!");
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
    receive_thread = -1L;
    __android_log_write(ANDROID_LOG_DEBUG, "receive thread", "exit!");
}

JNIEXPORT void JNICALL Java_de_mur1_yo_MainActivity_setSettings(JNIEnv *env, jobject, jstring channel, jstring username, jstring token) {
    if (receive_thread > -1L) {
        socket_disconnect();
    }
    if (channel_ != nullptr) {
        free(channel_);
    }
    if (username_ != nullptr) {
        free(username_);
    }
    if (token != nullptr) {
        free(token_);
    }

    jni_getUTFChars(env, channel, &channel_);
    jni_getUTFChars(env, username, &username_);
    jni_getUTFChars(env, token, &token_);
}

JNIEXPORT jstring JNICALL Java_de_mur1_yo_MainActivity_connect(JNIEnv *env, jobject /* this */) {
    socket_disconnect();

    while (receive_thread > -1L) {
        usleep(10);
    }

    buffer_ = (char *) malloc(MAXDATASIZE);
    ring_buffer_ = (struct irc_message *) malloc(ring_size_ * sizeof(struct irc_message));
    pthread_mutex_init(&ring_position_lock_, nullptr);

    std::string status = "";

    if (irc_connect()) {
        if (irc_login()) {
            pthread_create(&receive_thread, nullptr, (void *(*)(void*))&irc_receive_loop, nullptr);
            if (irc_enable_twitch_tags()) {
                irc_join(channel_);
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