#ifndef UTIL_H
#define UTIL_H

#include <string>
#include <vector>

enum util_file_type {
    UTIL_FILE_TYPE_UNKNOWN,
    UTIL_FILE_TYPE_PNG,
    UTIL_FILE_TYPE_GIF
};

int util_system_command(const char *command, char **out);

char* util_issue_command(const char* cmd);
std::string& util_ltrim(std::string& str, const std::string& chars);
std::string& util_rtrim(std::string& str, const std::string& chars);
std::string& util_trim(std::string& str, const std::string& chars);

void util_ipv6_address_to_normalform(char* pbar);

std::vector<std::string>  util_split(const std::string& str, const std::string& separator);

void util_sleep(const unsigned int milliseconds);

void util_chararray_from_const(const char *str, char **out);

bool util_str_equals(const char *str1, const char *str2);

bool util_file_exists(const char *file);
void util_file_delete(const char *file);

std::vector<std::string> util_directory_list(const char *path);

std::vector<std::string> util_file_read(const char *file);

bool util_mkdir(const char *dir);

enum util_file_type util_file_type(const char *file);

void util_download_file_to(const char *url, const char *destination);

#endif
