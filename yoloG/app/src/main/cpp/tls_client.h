/*
 * tls_client.h
 *
 *  Created on: 02.12.2021
 *      Author: mur1
 */

#ifndef SRC_TLS_CLIENT_H_
#define SRC_TLS_CLIENT_H_

#include <stdbool.h>

#include <openssl/bio.h>
#include <openssl/ssl.h>
#include <openssl/err.h>
#include <openssl/x509v3.h>
#include <openssl/pem.h>
#include <openssl/sha.h>

struct tls_client {
    SSL_CTX *ctx;
    BIO *bio;
    BIO *ssl_bio;
    bool connected;
    bool running;

    char *hostname;
    int port;
    char *cert_hash_;
};

#define TLS_MAXDATASIZE 8192*2

bool tls_client_init(struct tls_client *tls_c, const char *hostname, unsigned int port, const char *cert_hash);
void tls_client_disconnect(struct tls_client *tls_c);
int tls_client_read(struct tls_client *tls_c, char* data_out);
bool tls_client_send(struct tls_client *tls_c, const char* data);



#endif /* SRC_TLS_CLIENT_H_ */
