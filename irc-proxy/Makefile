all:
	mkdir data
	g++ src/crypto.cpp  src/writer.cpp src/ecdh.cpp  src/irc_client.cpp  src/logger.cpp  src/main.cpp  src/mutex.cpp  src/network.cpp  src/thread.cpp  src/tls_client.cpp src/util.cpp -lcrypto -lssl -lpthread -o irc-proxy
