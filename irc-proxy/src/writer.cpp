#include "writer.h"

#include "irc_client.h"

#include <ctime>


void writer_init(struct writer *w, string name) {
	w->name = name;
	w->open = false;
}

void writer_write(struct writer *w, struct irc_message *irc_m) {
	if (!w->open) {
		w->file.open(w->name);
		w->open = true;
	}

	std::time_t t = std::time(nullptr);

	w->file << "time:" 	<< t 			<< "\n";
	w->file << "prefix:" 	<< irc_m->prefix 	<< "\n";
	w->file << "name:"	<< irc_m->name		<< "\n";
	w->file << "command:"	<< irc_m->command	<< "\n";
	w->file << "channel:"	<< irc_m->channel	<< "\n";
	w->file << "msg:"	<< irc_m->msg		<< "\n";
}

void writer_close(struct writer *w) {
	if (w->open) w->file.close();
}



