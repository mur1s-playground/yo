#include "main.h"

#include "irc_client.h"
#include "util.h"
#include "writer.h"

#include <sstream>
#include <cstring>
#include <vector>
#include <ctime>

using namespace std;

struct logger main_logger;

int main(int argc, char *argv[]) {
	logger_init(&main_logger);
	logger_level_set(&main_logger, LOG_LEVEL_VERBOSE);
	logger_write(&main_logger, LOG_LEVEL_VERBOSE, "MAIN", "start");


	vector<string> cfg = util_file_read("./config");

	string 	channel;
	int	file_interval_seconds = 1;
	string	cert_hash;
	int	delete_older_than_seconds = 120;
	string  pass = "";
	string  nick = "justinfan1337";

	for (int c = 0; c < cfg.size(); c++) {
		if (cfg[c].length() > 0) {
			vector<string> k_v = util_split(cfg[c], ";");
			if (k_v.size() == 2) {
				if (strstr(k_v[0].c_str(), "channel") != nullptr) {
					channel = string(k_v[1].c_str());
				} else if (strstr(k_v[0].c_str(), "file_interval_seconds") != nullptr) {
					file_interval_seconds = stoi(k_v[1].c_str());
				} else if (strstr(k_v[0].c_str(), "certificate_hash") != nullptr) {
					cert_hash = string(k_v[1].c_str());
				} else if (strstr(k_v[0].c_str(), "delete_older_than_seconds") != nullptr) {
					delete_older_than_seconds = stoi(k_v[1].c_str());
				} else if (strstr(k_v[0].c_str(), "pass") != nullptr) {
					pass = string(k_v[1].c_str());
				} else if (strstr(k_v[0].c_str(), "nick") != nullptr) {
					nick = string(k_v[1].c_str());
				}
			}
		}
	}

	struct irc_client irc_c;
	irc_client_init(&irc_c, nick.c_str(), pass.c_str(), "irc.chat.twitch.tv", 6697, cert_hash.c_str());
	irc_client_channel_add(&irc_c, channel);
	irc_client_connection_establish(&irc_c);

	int sleep_ms = 4;
	int nothing_new_c = 0;

	struct irc_message message_current;

	int sleep_ms_ct = 0;

	std::time_t result = std::time(nullptr);

	std::stringstream name_ss;
	name_ss << "./data/" << result;

	struct writer w;
	writer_init(&w, name_ss.str());

	while (true) {
		std::time_t result_ = std::time(nullptr);

		if (result_ - result >= file_interval_seconds) {
                                result = result_;
                                writer_close(&w);

				vector<string> dir_list = util_directory_list("./data/");
				for (int d = 0; d < dir_list.size(); d++) {
					long t = stol(dir_list[d]);
					stringstream fname;
					fname << "./data/" << dir_list[d];
//					logger_write(&main_logger, LOG_LEVEL_VERBOSE, "MAIN", fname.str().c_str());
					if (result - t > delete_older_than_seconds) util_file_delete(fname.str().c_str());
				}

                                std::stringstream name_ss_;
                                name_ss_ << "./data/" << result;
                                writer_init(&w, name_ss_.str());
                }

		if (irc_client_message_next(&irc_c, &message_current)) {
			writer_write(&w, &message_current);
			nothing_new_c = 0;
		} else {
			nothing_new_c++;
		}

		if (nothing_new_c > 0) util_sleep(4);
		sleep_ms_ct += 4;
	}

	logger_write(&main_logger, LOG_LEVEL_VERBOSE, "MAIN", "end");
}
