/*
 * logger.h
 *
 *  Created on: 02.12.2021
 *      Author: mur1
 */
#pragma once

#include "mutex.h"

#include <stdbool.h>

struct logger {
	struct mutex lock;

	int level;
};

const int LOG_LEVEL_VERBOSE = 3;
const int LOG_LEVEL_DEBUG = 2;
const int LOG_LEVEL_WARNING = 1;
const int LOG_LEVEL_ERROR = 0;

void logger_init(struct logger *l);
void logger_destroy(struct logger *l);
void logger_level_set(struct logger *l, int level);
void logger_write(struct logger *l, int level, const char *prefix, const char *msg);

