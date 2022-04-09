/*
 * irc_client.h
 *
 *  Created on: 03.12.2021
 *      Author: mur1
 */

#ifndef SRC_IRC_CLIENT_H_
#define SRC_IRC_CLIENT_H_

#include "tls_client.h"
#include "mutex.h"
#include "thread.h"

#include <vector>
#include <string>

using namespace std;

#define MAXPREFIXSIZE 1024*2
#define MAXNAMESIZE 256*2
#define MAXCOMMANDSIZE 50*2
#define MAXCHANNELSIZE 128*2

#define MAXMSGSIZE (TLS_MAXDATASIZE - MAXCHANNELSIZE - MAXCOMMANDSIZE - MAXNAMESIZE - MAXPREFIXSIZE)

struct irc_message {
    char prefix[MAXPREFIXSIZE];
    char name[MAXNAMESIZE];
    char command[MAXCOMMANDSIZE];
    char channel[MAXCHANNELSIZE];
    char msg[MAXMSGSIZE];
};

struct irc_client {
	char *nickname;
	char *token;

	vector<string>	*channels;

	char *hostname;
	unsigned int port;
	char *cert_hash;

	struct tls_client tls_c;

	char *buffer_;
	int irc_parse_separators;
	char *irc_parse_write_head;
	struct irc_message *current_msg;
	bool irc_receive_last_incomplete;

	struct irc_message *ring_buffer_;
	unsigned int ring_size_;
	unsigned int ring_position_;
	unsigned int ring_position_r_;
	struct mutex ring_position_lock_;

	struct ThreadPool tp;
};

void irc_client_init(struct irc_client *irc_c, const char *nickname, const char *token, const char *hostname, unsigned int port, const char *cert_hash);
void irc_client_destroy(struct irc_client *irc_c);
void irc_client_channel_add(struct irc_client *irc_c, string channel);
void irc_client_channel_remove(struct irc_client *irc_c, string channel);
bool irc_client_join(struct irc_client *irc_c, std::string channel);
bool irc_client_part(struct irc_client *irc_c, std::string channel);
bool irc_client_connection_establish(struct irc_client *irc_c);
bool irc_client_send_privmsg(struct irc_client *irc_c, std::string target, std::string msg);
bool irc_client_message_next(struct irc_client *irc_c, struct irc_message *out_msg);


#endif /* SRC_IRC_CLIENT_H_ */
