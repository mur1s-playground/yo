#include "util.h"

#include <stdio.h>
#include <cstdlib>
#include <cstring>
#include <sstream>
#ifdef _WIN32
#else
#include <unistd.h>
#include <dirent.h>
#endif
#include <fstream>

#include "main.h"
#include "logger.h"

#ifdef _WIN32

#else
#include <sys/stat.h>
#endif

int util_system_command(const char *command, char **out) {
#ifdef _WIN32
#else
	FILE *p = popen(command, "r");
	if (p == NULL) return 0;

	char buf[2048];

	char *buffer = (char*) malloc(2048);
	char *buffer_ = buffer;
	int pos = 0;
	int ct = 1;
	while (!feof(p)) {
		if (fgets(buf, 2048, p) != NULL) {
			int len = strlen(buf);
			while (pos + len + 1 > 2048 * ct) {
				buffer = (char *) realloc(buffer, (ct+1) * 2048);
				ct++;
				if (buffer == NULL) {
                                	free(buffer_);
	                                return 0;
        	                }
				buffer_ = buffer;
			}
			memcpy(&buffer_[pos], buf, len);
			pos += len;
		}
	}
	buffer_[pos] = '\0';

	pclose(p);

	*out = buffer_;
#endif
	return 1;
}

/* TMP */
char* util_issue_command(const char* cmd) {
        char* result;
        char buffer[256];
        int i;
        std::stringstream ss;
#ifdef _WIN32
        FILE* pipe = _popen(cmd, "r");
        if (pipe != nullptr) {
                while (fgets(buffer, sizeof buffer, pipe) != NULL) {
                        ss << buffer;
                }
                _pclose(pipe);
        }
#else
        FILE* pipe;
        pipe = (FILE*)popen(cmd, "r");
        if (!pipe) return NULL;
        while (!feof(pipe)) {
                if (fgets(buffer, 256, pipe) != NULL) {
                        for (i = 0; i < 256; i++) {
                                if (buffer[i] == '\0') break;
                                ss << buffer[i];
                        }
                }
        }
        pclose(pipe);
#endif
        result = (char*)malloc(ss.str().length() + 1);
        memcpy(result, ss.str().data(), ss.str().length());
        result[ss.str().length()] = '\0';
        return result;
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

std::vector<std::string>  util_split(const std::string& str, const std::string& separator) {
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

void util_sleep(const unsigned int milliseconds) {
#ifdef _WIN32
        Sleep(milliseconds);
#else   
        usleep(milliseconds * 1000);
#endif
}

void util_ipv6_address_to_normalform(char* pbar) {
        char part_2[46];
        //memset(&part_2, 'x', 46);
        part_2[45] = '\0';

        int part_2_pos = 44;

        char* end = strchr(pbar, '\0');
        end--;
        int segment_ct = 0;
        int segs = 0;

        bool dbl = false;
        while (end >= pbar) {
                if (*end == ':') {
                        if (*(end - 1) == ':') {
                                dbl = true;
                        }
                        else if (segment_ct > 0) {
                                for (int s = segment_ct; s < 4; s++) {
                                        part_2[part_2_pos] = '0';
                                        part_2_pos--;
                                }
                        }
                        part_2[part_2_pos] = *end;
                        part_2_pos--;
                        segment_ct = 0;
                        segs++;
                }
                else {
                        part_2[part_2_pos] = *end;
                        part_2_pos--;
                        segment_ct = (segment_ct + 1) % 4;
                }
                end--;
                if (dbl) break;
        }
        //printf("part_2: %s\n", part_2);

        part_2_pos++;
        if (dbl) {
                char part_1[46];
                //memset(&part_1, 'x', 46);
                int part_1_pos = 44;
                part_1[45] = '\0';
                while (end >= pbar) {
                        if (*end == ':') {
                                if (segment_ct > 0) {
                                        for (int s = segment_ct; s < 4; s++) {
                                                part_1[part_1_pos] = '0';
                                                part_1_pos--;
                                        }
                                }
                                part_1[part_1_pos] = *end;
                                part_1_pos--;
                                segment_ct = 0;
                                segs++;
                        }
                        else {
                                part_1[part_1_pos] = *end;
                                part_1_pos--;
                                segment_ct = (segment_ct + 1) % 4;
                        }
                        end--;
                }
                //printf("part_1: %s\n", part_1);
                part_1_pos++;

                int total_pos = 0;
                memcpy(pbar, &part_1[part_1_pos], 45 - part_1_pos);
                total_pos = 45 - part_1_pos;
                for (int s = segs; s < 8; s++) {
                        pbar[total_pos] = '0';
                        total_pos++;
                        pbar[total_pos] = '0';
                        total_pos++;
                        pbar[total_pos] = '0';
                        total_pos++;
                        pbar[total_pos] = '0';
                        total_pos++;
                        if (s < 7) {
                                pbar[total_pos] = ':';
                                total_pos++;
                        }
                }
                memcpy(&pbar[total_pos], &part_2[part_2_pos], 45 - part_2_pos);
                total_pos += (45 - part_2_pos);
                pbar[total_pos] = '\0';
        }
        else {
                memcpy(pbar, &part_2[part_2_pos], 45 - part_2_pos);
                pbar[45 - part_2_pos] = '\0';
        }
}

void util_chararray_from_const(const char *str, char **out) {
	char *o = (char *)malloc(strlen(str) + 1);
	memcpy(o, str, strlen(str));
	o[strlen(str)] = '\0';

	*out = o;
}

bool util_str_equals(const char *str1, const char *str2) {
    if (strlen(str1) != strlen(str2)) return false;
    if (strstr(str1, str2) == nullptr) return false;
    return true;
}

bool util_file_exists(const char *file) {
    FILE *fp = fopen(file, "r");
    if (fp != nullptr) {
        fclose(fp);
        return true;
    }
    return false;
}

void util_file_delete(const char *file) {
    if (util_file_exists(file)) {
        remove(file);
    }
}

std::vector<std::string> util_directory_list(const char *path) {
    std::vector<std::string> result = std::vector<std::string>();

    DIR *dir = opendir(path);
    if (dir != NULL) {
	    struct dirent *entity = readdir(dir);

	    while(entity != NULL) {
		if(entity->d_type == DT_REG) {
			result.push_back(std::string(entity->d_name));
        	}
	        entity = readdir(dir);
	    }

	    closedir(dir);
    }

    return result;
}

std::vector<std::string> util_file_read(const char *file) {
    std::vector<std::string> result = std::vector<std::string>();

    std::ifstream t;
    t.open(file);
    std::string line;
    while(t){
        std::getline(t, line);
        result.push_back(line);
        logger_write(&main_logger, LOG_LEVEL_DEBUG, "UTIL_FILE_READ", line.c_str());
    }
    t.close();

    return result;
}

bool util_mkdir(const char *dir) {
#ifdef _WIN32

#else
    const int dir_err = mkdir(dir, S_IRWXU | S_IRWXG | S_IROTH | S_IXOTH);
    if (dir_err == -1) {
        return false;
    }
    return true;
#endif
}

enum util_file_type util_file_type(const char *file) {
    enum util_file_type type = UTIL_FILE_TYPE_UNKNOWN;
#ifdef _WIN32

#else
    std::stringstream command_str;
    command_str << "file " << file;

    char *output = nullptr;
    util_system_command(command_str.str().c_str(), &output);

    if (strstr(output, "PNG image data,") != nullptr) {
        type = UTIL_FILE_TYPE_PNG;
    }
    free(output);
#endif
    return type;
}

void util_download_file_to(const char *url, const char *destination) {
#ifdef _WIN32

#else
    std::stringstream command_str;
    command_str << "wget -O " << destination << " " << url;

    char *output = nullptr;
    util_system_command(command_str.str().c_str(), &output);
    free(output);
#endif
}
