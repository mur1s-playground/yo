/*
 * irc_client.c
 *
 *  Created on: 03.12.2021
 *      Author: mur1
 */

#include "irc_client.h"

#include "main.h"
#include "logger.h"
#include "util.h"

#include <sstream>
#include <string.h>

void irc_client_init(struct irc_client *irc_c, const char *nickname, const char *token, const char *hostname, unsigned int port, const char *cert_hash) {
	util_chararray_from_const(nickname, &irc_c->nickname);
	util_chararray_from_const(token, &irc_c->token);

	util_chararray_from_const(hostname, &irc_c->hostname);
	util_chararray_from_const(cert_hash, &irc_c->cert_hash);
	irc_c->port = port;

	irc_c->buffer_ = (char *) malloc(TLS_MAXDATASIZE);
	irc_c->ring_size_ = 2000;
	irc_c->ring_buffer_ = (struct irc_message *) malloc(irc_c->ring_size_ * sizeof(struct irc_message));
	irc_c->ring_position_ = 0;
	irc_c->ring_position_r_ = 0;
	mutex_init(&irc_c->ring_position_lock_);

	irc_c->irc_parse_separators = 0;
	irc_c->irc_parse_write_head = nullptr;
	irc_c->current_msg = nullptr;
	irc_c->irc_receive_last_incomplete = false;

	irc_c->channels = new vector<string>();

	irc_c->tls_c.connected = false;

	thread_pool_init(&irc_c->tp, 1);
}

bool irc_client_connect(struct irc_client *irc_c) {
	bool s_i = tls_client_init(&irc_c->tls_c, irc_c->hostname, irc_c->port, irc_c->cert_hash);
	if (!s_i) {
		logger_write(&main_logger, LOG_LEVEL_ERROR, "IRC_CLIENT", "socket_init failed");
	} else {
		logger_write(&main_logger, LOG_LEVEL_DEBUG, "IRC_CLIENT", "socket_init successful");
		return true;
	}
	return false;
}

bool irc_client_send_msg(struct irc_client *irc_c, std::string data) {
    data += "\n";
    if (tls_client_send(&irc_c->tls_c, data.c_str())) {
        return true;
    }
    tls_client_disconnect(&irc_c->tls_c);
    return false;
}

bool irc_client_login(struct irc_client *irc_c) {
    bool auth_set = false;
    if (strlen(irc_c->token) > 0) {
        auth_set = true;

        std::stringstream data_pass_ss;
        data_pass_ss << "PASS oauth:" << irc_c->token;
        std::string data_pass = data_pass_ss.str();
        if (irc_client_send_msg(irc_c, data_pass)) {
        	logger_write(&main_logger, LOG_LEVEL_DEBUG, "IRC_CLIENT", "SENT PASS");
        } else {
            return false;
        }
    }

    std::stringstream data_nick_ss;
    data_nick_ss << "NICK " << irc_c->nickname;

    std::string data_nick = data_nick_ss.str();
    if (irc_client_send_msg(irc_c, data_nick)) {
    	logger_write(&main_logger, LOG_LEVEL_DEBUG, "IRC_CLIENT", "SENT NICK");
        std::string data_user = "USER yo 8 * :client";
        if (irc_client_send_msg(irc_c, data_user)) {
        	logger_write(&main_logger, LOG_LEVEL_DEBUG, "IRC_CLIENT", "SENT USER");
        	logger_write(&main_logger, LOG_LEVEL_DEBUG, "IRC_CLIENT", "logged in");
            return true;
        }
    }
    return false;
}

bool irc_client_enable_twitch_tags(struct irc_client *irc_c) {
    std::string data_ext = "CAP REQ :twitch.tv/tags twitch.tv/commands";
    if (irc_client_send_msg(irc_c, data_ext)) {
    	logger_write(&main_logger, LOG_LEVEL_DEBUG, "IRC_CLIENT", "enabled twitch tags");
        return true;
    }
    return false;
}

bool irc_client_join(struct irc_client *irc_c, std::string channel) {
    std::string join_chan = "join #" + channel;
    if (irc_client_send_msg(irc_c, join_chan)) {
    	logger_write(&main_logger, LOG_LEVEL_DEBUG, "IRC_CLIENT", join_chan.c_str());
        return true;
    }
    return false;
}

int irc_client_parse_append_till_separator(struct irc_client *irc_c, const char *line, const int line_pos, const int line_len, const char *separator, bool incomplete) {
    const char *append_end = strstr(&line[line_pos], separator);
    int bytes = 0;
    if (append_end == nullptr) {
        bytes = line_len - line_pos;
    } else {
        bytes = append_end - &line[line_pos];
    }
    memcpy(irc_c->irc_parse_write_head, &line[line_pos], bytes);
    irc_c->irc_parse_write_head += bytes;
    if (append_end != nullptr || (irc_c->irc_parse_separators == 3 && !incomplete)) {
        irc_c->irc_parse_write_head[0] = '\0';
        bytes++;
        switch (irc_c->irc_parse_separators) {
            case 0: irc_c->irc_parse_write_head = irc_c->current_msg->name; break;
            case 1: irc_c->irc_parse_write_head = irc_c->current_msg->command; break;
            case 2: irc_c->irc_parse_write_head = irc_c->current_msg->channel; break;
            case 3: irc_c->irc_parse_write_head = irc_c->current_msg->msg; break;
            default: break;
        }
        irc_c->irc_parse_separators++;
    }
    return bytes;
}

void ring_position_inc(struct irc_client *irc_c) {
    mutex_wait_for(&irc_c->ring_position_lock_);
    irc_c->ring_position_ = (irc_c->ring_position_ + 1) % irc_c->ring_size_;
    mutex_release(&irc_c->ring_position_lock_);
}

void irc_client_parse(struct irc_client *irc_c, char* line, int len, bool irc_parse_incomplete) {
	logger_write(&main_logger, LOG_LEVEL_DEBUG, "IRC_CLIENT", line);

    if (irc_c->irc_parse_write_head == nullptr) {
        irc_c->current_msg = &irc_c->ring_buffer_[irc_c->ring_position_];
        memset(irc_c->current_msg, 0, sizeof(struct irc_message));
        irc_c->irc_parse_write_head = irc_c->current_msg->prefix;
    }

    int line_pos = 0;

    do {
        if (irc_c->irc_parse_separators < 4) {
            line_pos += irc_client_parse_append_till_separator(irc_c, line, line_pos, len, " ", irc_parse_incomplete);
        } else {
            line_pos += irc_client_parse_append_till_separator(irc_c, line, line_pos, len, "\n", irc_parse_incomplete);
        }
    } while (line_pos < len);

    logger_write(&main_logger, LOG_LEVEL_DEBUG, "irc_client msg_prefix", irc_c->current_msg->prefix);
    logger_write(&main_logger, LOG_LEVEL_DEBUG, "irc_client msg_name", irc_c->current_msg->name);
    logger_write(&main_logger, LOG_LEVEL_DEBUG, "irc_client msg_command", irc_c->current_msg->command);
    logger_write(&main_logger, LOG_LEVEL_DEBUG, "irc_client msg_channel", irc_c->current_msg->channel);
    logger_write(&main_logger, LOG_LEVEL_DEBUG, "irc_client msg_msg", irc_c->current_msg->msg);

    if (!irc_parse_incomplete) {
        irc_c->irc_parse_separators = 0;
        irc_c->irc_parse_write_head = nullptr;
        if (strstr(irc_c->current_msg->command, "PRIVMSG") != nullptr || strstr(irc_c->current_msg->command, "CLEARCHAT") != nullptr) {
            ring_position_inc(irc_c);
        } else if (strstr(irc_c->current_msg->prefix, "PING") != nullptr) {
            char response_buf[256];
            snprintf(response_buf, 255, "PONG %s", irc_c->current_msg->name);
            if (irc_client_send_msg(irc_c, response_buf)) {
                logger_write(&main_logger, LOG_LEVEL_DEBUG, "irc_client", response_buf);
            }
        }
    }
}

void irc_client_receive_loop(void *param) {
    struct irc_client *irc_c = (struct irc_client *) param;
    irc_c->tls_c.running = true;
    logger_write(&main_logger, LOG_LEVEL_DEBUG, "receive thread", "start!");
    while (irc_c->tls_c.connected) {
        int len = tls_client_read(&irc_c->tls_c, irc_c->buffer_);
        if (len > 0) {
            char *line_start = irc_c->buffer_;
            char *line_end = nullptr;
            while ((line_end = strstr(line_start, "\n")) != nullptr) {
                int eol_o = 1;
                if (strstr(line_start, "\r") == line_end - 1) {
                    eol_o++;
                    line_end--;
                }
                if (!irc_c->irc_receive_last_incomplete) {
                    while (line_start[0] == ' ') {
                        line_start++;
                        if (line_start[0] == '\0') {
                            line_start = nullptr;
                            break;
                        }
                    }
                }
                logger_write(&main_logger, LOG_LEVEL_DEBUG, "irc_client line", "complete!");
                irc_c->irc_receive_last_incomplete = false;
                irc_client_parse(irc_c, line_start, line_end-line_start, false);
                line_start = line_end + eol_o;
            }
            if (line_start - irc_c->buffer_ < len && line_start != nullptr) {
            	logger_write(&main_logger, LOG_LEVEL_DEBUG, "irc_client line", "incomplete!");
                irc_c->irc_receive_last_incomplete = true;
                irc_client_parse(irc_c, line_start, len - (line_start - irc_c->buffer_), true);
            }
        }
    }
    logger_write(&main_logger, LOG_LEVEL_DEBUG, "receive thread", "exit!");
    irc_c->tls_c.running = false;
}

void irc_client_channel_add(struct irc_client *irc_c, string channel) {
	irc_c->channels->push_back(channel);
	if (irc_c->tls_c.connected) irc_client_join(irc_c, channel);
}

bool irc_client_connection_establish(struct irc_client *irc_c) {
	if (irc_client_connect(irc_c)) {
		if (irc_client_login(irc_c)) {
			thread_create(&irc_c->tp, (void*) &irc_client_receive_loop, (void*) irc_c);
			if (irc_client_enable_twitch_tags(irc_c)) {
				for (int c = 0; c < irc_c->channels->size(); c++) {
					irc_client_join(irc_c, (*irc_c->channels)[c]);
				}
				return 1;
			}
		}
	}
	return 0;
}

bool irc_client_message_next(struct irc_client *irc_c, struct irc_message *out_msg) {
	bool result = false;
	mutex_wait_for(&irc_c->ring_position_lock_);
	if (irc_c->ring_position_r_ != irc_c->ring_position_) {
		memcpy(out_msg, &irc_c->ring_buffer_[irc_c->ring_position_r_], sizeof(struct irc_message));
		irc_c->ring_position_r_ = (irc_c->ring_position_r_ + 1) % irc_c->ring_size_;
		result = true;
	}
	mutex_release(&irc_c->ring_position_lock_);
	return result;
}
