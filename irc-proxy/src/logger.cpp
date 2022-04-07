/*
 * logger.c
 *
 *  Created on: 02.12.2021
 *      Author: mur1
 */

#include "logger.h"

#include <stdio.h>

struct mutex logger_lock;
bool logger_lock_init = false;

void logger_init(struct logger *l) {
	if (!logger_lock_init) {
		mutex_init(&logger_lock);
	}
	mutex_init(&l->lock);
	l->level = 0;
}

void logger_destroy(struct logger *l) {

}

void logger_level_set(struct logger *l, int level) {
	l->level = level;
}

void logger_write(struct logger *l, int level, const char *prefix, const char *msg) {
	if (level <= l->level) {
		mutex_wait_for(&l->lock);
		mutex_wait_for(&logger_lock);
		printf("%s: %s\n", prefix, msg);
		mutex_release(&logger_lock);
		mutex_release(&l->lock);
	}
}
