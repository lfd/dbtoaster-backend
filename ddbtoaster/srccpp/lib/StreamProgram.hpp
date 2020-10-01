/* -*- Mode: C; indent-tabs-mode: t; c-basic-offset: 4; tab-width: 4 -*- */

#ifndef STREAM_PROGRAM_HPP
#define STREAM_PROGRAM_HPP

namespace dbtoaster {
  class StreamProgram : public Program {
  private:
	  unsigned int iterations;
	  void estimate_tuple_count() {
		  size_t tuples = 0;

		  // We don't need a log buffer if logging is disabled
		  if (log_count_every == 0)
			  return;

		  auto it = stream_multiplexer.inputs.begin();
		  auto end = stream_multiplexer.inputs.end();

		  for (; it != end; ++it) {
			  std::shared_ptr<dbt_file_source> s = std::dynamic_pointer_cast<dbt_file_source> (*it);

			  if (s) {
				  char *buf = s->buffer;
				  size_t count;
				  for (count=0; buf[count]; buf[count]=='\n' ? count++ : *buf++);

				  // Add one for a potentially missing final EOL
				  tuples += (count+1);
			  }
		  }

		  // Add one to approximate ceil(...)
		  resize_log_buffer(iterations*(tuples/log_count_every + 1));
	  };

  public:
  StreamProgram(int argc=0, char *argv[] = nullptr): Program(argc, argv) {};
	  void init() {
		  table_multiplexer.init_source(run_opts->batch_size, run_opts->parallel, true);
		  iterations = run_opts->iterations;
		  process_tables();
		  data.on_system_ready_event();
	  };

	  void process_streams() {
		  estimate_tuple_count();

		  // TODO: How do we properly handle multiple input streams? Interleave 1:1?
		  // For now, the code assumes we have only one input stream
		  std::vector<std::shared_ptr<source> >::iterator it = stream_multiplexer.inputs.begin();
		  std::vector<std::shared_ptr<source> >::iterator end = stream_multiplexer.inputs.end();

		  for (; it != end; ++it) {
			  auto eventList = std::shared_ptr<std::list<event_t> >(new std::list<event_t>());
			  auto eventQue = std::shared_ptr<std::list<event_t> >(new std::list<event_t>());

			  std::shared_ptr<dbt_file_source> s = std::dynamic_pointer_cast<dbt_file_source> (*it);

			  if (!s || !s->buffer) {
				  // Not sure how this should happen, but DBToaster makes a
				  // corresponding checks
				  cerr << "Internal error: Empty file source?!" << endl;
				  exit(-1);
			  }

			  const char* delim = s->frame_info.delimiter.c_str();
			  size_t delim_size = s->frame_info.delimiter.size();

			  s->init_source(); // Technically, we know that this function is empty;
			  // no idea why DBToaster insists on calling it -- it's not even part of
			  // the base class

			  char* start_event_pos = s->buffer;
			  char* end_event_pos;

			  size_t iter = 0;
			  char c;
			  while(iter < iterations) {
				  end_event_pos = strstr(start_event_pos, delim);

				  if(!end_event_pos || end_event_pos == s->buffer + s->bufferLength) {
					  iter +=1;
                      start_event_pos = s->buffer;
					  continue;
				  }

				  c = end_event_pos[0];
				  *end_event_pos = '\0';
				  s->adaptor->read_adaptor_events(start_event_pos,eventList,eventQue);
				  end_event_pos[0] = c;

				  while (!eventList->empty()) {
					  process_stream_event(eventList->front());
					  eventList->pop_front();
				  }

				  while (!eventQue->empty()) {
					  process_stream_event(eventQue->front());
					  eventQue->pop_front();
				  }

				  start_event_pos = end_event_pos + delim_size;
			  }

#if defined(__rtems__)
			  delete[] s->buffer;
#else
			  munmap(s->buffer, s->bufferLength);
#endif
		  }
	  };
  };
}

#endif
