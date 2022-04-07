/*
 * tls_client.c
 *
 *  Created on: 02.12.2021
 *      Author: mur1
 */

#include "tls_client.h"

#include "main.h"
#include "crypto.h"
#include "util.h"
#include "logger.h"

#include <sstream>
#include <string.h>

using namespace std;

SSL *tls_bio_get_ssl(BIO *bio) {
    SSL *ssl = nullptr;
    BIO_get_ssl(bio, &ssl);
    if (ssl == nullptr) {
    	logger_write(&main_logger, LOG_LEVEL_ERROR, "TLS", "error getting tls_bio_get_ssl");
    }
    return ssl;
}

void tls_cert_hash_set(struct tls_client *tls_c, const char *cert_hash) {
	util_chararray_from_const(cert_hash, &tls_c->cert_hash_);
}

bool tls_cert_verify(struct tls_client *tls_c, SSL *ssl, const char *hostname) {
    X509 *cert = SSL_get_peer_certificate(ssl);
    if (cert == nullptr) {
    	logger_write(&main_logger, LOG_LEVEL_ERROR, "TLS", "error no cert");
        return false;
    }

    BIO *cert_b = BIO_new(BIO_s_mem());
    PEM_write_bio_X509(cert_b, cert);
    BUF_MEM *mem = NULL;
    BIO_get_mem_ptr(cert_b, &mem);

    string pem(mem->data, mem->length);

    stringstream pem_len;
    pem_len << mem->length;

    logger_write(&main_logger, LOG_LEVEL_DEBUG, "TLS", pem_len.str().c_str());
    logger_write(&main_logger, LOG_LEVEL_DEBUG, "TLS", pem.c_str());

    unsigned char *cert_hash = crypto_hash_sha256((unsigned char*) mem->data, mem->length);
    char *cert_hash_b64 = crypto_base64_encode(cert_hash, SHA256_DIGEST_LENGTH);
    BIO_free_all(cert_b);
    free(cert_hash);

    logger_write(&main_logger, LOG_LEVEL_DEBUG, "TLS", cert_hash_b64);
    logger_write(&main_logger, LOG_LEVEL_DEBUG, "TLS", tls_c->cert_hash_);

    bool equal = true;
                              //has line ending before \0;
    if (strlen(tls_c->cert_hash_) != strlen(cert_hash_b64)-1) {
        equal = false;
    } else {
        for (int c = 0; c < strlen(tls_c->cert_hash_); c++) {
            if (tls_c->cert_hash_[c] != cert_hash_b64[c]) {
                equal = false;
            }
        }
    }
    free(cert_hash_b64);

    X509_free(cert);

    if (!equal) {
    	logger_write(&main_logger, LOG_LEVEL_ERROR, "TLS", "invalid cert");
        return false;
    }

    if (X509_check_host(cert, hostname, strlen(hostname), 0, nullptr) != 1) {
    	logger_write(&main_logger, LOG_LEVEL_ERROR, "TLS", "error cert hostname mismatch");
        return false;
    }
    return true;
}

bool tls_client_init(struct tls_client *tls_c, const char *hostname, unsigned int port, const char *cert_hash) {
#ifdef _WIN32
#else
    SSL_library_init();
#endif

    tls_cert_hash_set(tls_c, cert_hash);

    logger_write(&main_logger, LOG_LEVEL_DEBUG, "TLS", "client_init");

    tls_c->ctx = SSL_CTX_new(TLS_client_method());
    SSL_CTX_set_min_proto_version(tls_c->ctx, TLS1_2_VERSION);

    if (SSL_CTX_set_default_verify_paths(tls_c->ctx) != 1) {
    	logger_write(&main_logger, LOG_LEVEL_DEBUG, "TLS", "error loading trust store");
        return false;
    }

    stringstream ip_port;
    ip_port << hostname << ":" << port;

    tls_c->bio = BIO_new_connect(ip_port.str().c_str());
    if (BIO_do_connect(tls_c->bio) <= 0) {
    	logger_write(&main_logger, LOG_LEVEL_ERROR, "TLS", "error connecting to host");
        return false;
    }

    tls_c->ssl_bio = BIO_new_ssl(tls_c->ctx, 1);
    BIO_push(tls_c->ssl_bio, tls_c->bio);
    if (tls_c->ssl_bio == NULL) {
    	logger_write(&main_logger, LOG_LEVEL_ERROR, "TLS", "error creating ssl filter");
        return false;
    }

    SSL_set_tlsext_host_name(tls_bio_get_ssl(tls_c->ssl_bio), hostname);
    SSL_set1_host(tls_bio_get_ssl(tls_c->ssl_bio), hostname);
    int err = BIO_do_handshake(tls_c->ssl_bio);
    if (err <= 0) {
        stringstream err_code;
        err_code << "error ssl handshake: " << err;
        logger_write(&main_logger, LOG_LEVEL_ERROR, "TLS", err_code.str().c_str());
        return false;
    }

    if (!tls_cert_verify(tls_c, tls_bio_get_ssl(tls_c->ssl_bio), hostname)) {
        return false;
    }

    tls_c->connected = true;
    return true;
}

void tls_client_disconnect(struct tls_client *tls_c) {
    tls_c->connected = false;
}

int tls_client_read(struct tls_client *tls_c, char* data_out) {
    memset(data_out, 0, TLS_MAXDATASIZE);

    int bytes = BIO_read(tls_c->ssl_bio, data_out, TLS_MAXDATASIZE - 1);

    if (bytes > 0) {
        return bytes;
    } else {
    	logger_write(&main_logger, LOG_LEVEL_ERROR, "TLS client", "read failed");
        tls_client_disconnect(tls_c);
    }
    return 0;
}

bool tls_client_send(struct tls_client *tls_c, const char* data) {
    int len = strlen(data);
    if (BIO_write(tls_c->ssl_bio, data, len) != len) {
    	logger_write(&main_logger, LOG_LEVEL_ERROR, "TLS client", "write failed/incomplete");
        tls_client_disconnect(tls_c);
        return false;
    }
    if (BIO_flush(tls_c->ssl_bio) <= 0) {
    	logger_write(&main_logger, LOG_LEVEL_ERROR, "TLS client", "flush failed");
        tls_client_disconnect(tls_c);
        return false;
    }
    return true;
}
