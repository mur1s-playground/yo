#pragma once

#include <string>
#include <iostream>
#include <fstream>

using namespace std;

struct writer {
	string name;

	bool open;
	ofstream file;
};

void writer_init(struct writer *w, string name);
void writer_write(struct writer *w, struct irc_message *irc_m);
void writer_close(struct writer *w);
