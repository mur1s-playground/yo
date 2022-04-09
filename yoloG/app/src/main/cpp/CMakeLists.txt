cmake_minimum_required(VERSION 3.10.2)

include_directories(/home/mur1/Downloads/openssl-1.1.1m-android/openssl-1.1.1m/include)

project("yoloG")

add_library(lib_ssl STATIC IMPORTED)
set_target_properties(lib_ssl PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libssl.a)

add_library(lib_crypto STATIC IMPORTED)
set_target_properties(lib_crypto PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libcrypto.a)

add_library(
             native-lib
             SHARED
             main.cpp
             crypto.cpp
             ecdh.cpp
             irc_client.cpp
             logger.cpp
             mutex.cpp
             network.cpp
             thread.cpp
             tls_client.cpp
             util.cpp )

find_library(
              log-lib
              log )

target_link_libraries(
                        native-lib

                        lib_ssl
                        lib_crypto

                       ${log-lib} )
