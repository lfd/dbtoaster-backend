#include "iprogram.hpp"
#include "event.hpp"
#include <iostream>
#include <time.h>

namespace dbtoaster {

/**
 * Executes the program by launching the virtual method 'process_streams()'.
 * @param async If 'true' the execution is performed in a separate thread.
 */
void IProgram::run( bool async ) {
	if( async )
	{
		std::packaged_task<void()> pt([this]() {
			this->run(false);
		});
		pt();
	}
	else
	{
		start_running();
		process_streams();
		finished = true;
		stop_running();
	}
}

/**
 * Obtains a snapshot of the results of the program. If the program is
 * currently running in asynchronous mode, it will make sure that the
 * snapshot is consistent.
 * @return A snapshot of the 'TLQ_T' data-structure representing 
 *         the results of the program.
 */
IProgram::snapshot_t IProgram::get_snapshot()
{

	if( !is_finished() )
	{
		request_snapshot();
		return wait_for_snapshot();
	}
	else
		return take_snapshot();
}

/**
 * This should get overridden by a function that processes an event by
 * calling the appropriate trigger. This function can also be used for
 * performing additional tasks before or after processing an event, such
 * as handling requests for consistent snapshots of the results.
 * In order to preserve functionality, functions that override it should
 * call their base class variants.
 * @param ev The event being processed.
 */
void IProgram::process_stream_event(const event_t& ev)
{
	process_snapshot();
}  

/**
 * Signal the beginning of the execution of the program.
 */
void IProgram::start_running()
{
	running_mtx.lock();
	running = true;
	running_mtx.unlock();
}
/**
 * Signal the end of the execution of the program.
 */
void IProgram::stop_running()
{
	running_mtx.lock();
	process_snapshot();
	running = false;
	running_mtx.unlock();
}

/**
 * Function for processing requests for program results snapshot.
 * Gets executed only between the processing of events and not during,
 * in order to get consistent results.
 */
void IProgram::process_snapshot()
{
	if( snapshot_request )
	{
		assert( snapshot_ready == false );
		snapshot = take_snapshot();
		snapshot_request = false;

		{
			std::lock_guard<std::mutex> lock(snapshot_ready_mtx);
			snapshot_ready=true;
		}
		snapshot_ready_cond.notify_all();
	}
}

/*
 * Function for recording a request for a snapshot.
 */
void IProgram::request_snapshot()
{
	assert( snapshot_request == false );
	assert( snapshot_ready == true );
	if( snapshot_request || !snapshot_ready )   return;

	running_mtx.lock();
	if( is_running() )
	{
		snapshot_ready = false;
		snapshot_request = true;
	}
	else
	{
		snapshot = take_snapshot();
	}
	running_mtx.unlock();
}

/**
 * Function for waiting for a previously recorded snapshot request to
 * complete.
 * @return The snapshot taken as a response of a previously recorded 
 *         request.
 */
IProgram::snapshot_t IProgram::wait_for_snapshot()
{
	if( !snapshot_ready )
	{
		std::unique_lock<std::mutex> lock(snapshot_ready_mtx);
		while(!snapshot_ready)
		{
			snapshot_ready_cond.wait(lock);
		}
	}

	IProgram::snapshot_t result = snapshot;
	snapshot = snapshot_t();
	return result;
}

void IProgram::log_timestamp(struct timespec val) {
	log_buffer[log_idx++] = val;
}

struct timespec diff(struct timespec start, struct timespec end)  {
    struct timespec temp;
    if ((end.tv_nsec-start.tv_nsec) < 0) {
      temp.tv_sec = end.tv_sec-start.tv_sec-1;
      temp.tv_nsec = 1000000000+end.tv_nsec-start.tv_nsec;
    } else {
      temp.tv_sec = end.tv_sec-start.tv_sec;
      temp.tv_nsec = end.tv_nsec-start.tv_nsec;
    }
    return temp;
}

void IProgram::print_log_buffer() {
  struct timespec start, diff1, diff2;

	if (log_buffer.size() < 2) {
		cerr << "Log buffer is empty. Did you forget to set --log-count?" << endl;
		return;
	}

	start = log_buffer[0];
	for (size_t i=1; i < log_idx; i++)  {
		diff1 = diff(log_buffer[0], log_buffer[i]);
		diff2 = diff(start, log_buffer[i]);

		std::cout << (long)(diff1.tv_sec * 1e9 + diff1.tv_nsec) << "\t" <<
		  (long)(diff2.tv_sec * 1e9 + diff2.tv_nsec) << std::endl;

		start = log_buffer[i];
	}

	log_buffer.clear();
}


}
