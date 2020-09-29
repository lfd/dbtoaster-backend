/* -*- Mode: C; indent-tabs-mode: t; c-basic-offset: 4; tab-width: 4 -*- */

#ifndef RTEMS_STREAM_HPP
#define RTEMS_STREAM_HPP

namespace dbtoaster {
  class RTEMSStreamProgram : public Program {
  private:
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
		  resize_log_buffer(tuples/log_count_every + 1);
	  };

  public:
  RTEMSStreamProgram(int argc=0, char *argv[] = nullptr): Program(argc, argv) {};
	  void init() {
		  table_multiplexer.init_source(run_opts->batch_size, run_opts->parallel, true);
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

			  if (!s) {
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
			
			  while(start_event_pos) {
				  end_event_pos = strstr(start_event_pos, delim);
				  if(!end_event_pos || end_event_pos == s->buffer + s->bufferLength) break;
				  *end_event_pos = '\0';
				  s->adaptor->read_adaptor_events(start_event_pos,eventList,eventQue);

				  // There can only be either zero or one event in the list
				  if (eventList->empty() && eventQue->empty()) {
					  cerr << "Internal error: No events" << endl;
					  exit(-1);
				  }

				  if (!eventList->empty()) {
					  process_stream_event(eventList->front());
					  eventList->pop_front();

					  if (!eventList->empty())
						  cout << "Internal error: Event list is not empty: "
							   << eventList->size() << endl;
				  }

				  if (!eventQue->empty()) {
					  process_stream_event(eventQue->front());
					  eventQue->pop_front();

					  if (!eventQue->empty())
						  cout << "Internal error: Event list is not empty: "
							   << eventQue->size() << endl;
				  }
				
				  start_event_pos = end_event_pos + delim_size;
			  }

			  delete[] s->buffer;
		  }
	  };
  };
}

#endif
