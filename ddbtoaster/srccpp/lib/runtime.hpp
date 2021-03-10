#ifndef DBTOASTER_RUNTIME_H
#define DBTOASTER_RUNTIME_H

#include <iostream>

#include <string>
#include <vector>
#include <set>

#include <unordered_set>
#include "filepath.hpp"
#include "optionparser.hpp"

#include "event.hpp"

#include "hpds/pstring.hpp"
#include "hpds/KDouble.hpp"

#define STRING(s) #s

#ifndef STRING_TYPE_STR
#define STRING_TYPE_STR STRING(STRING_TYPE)
#endif //STRING_TYPE_STR

#ifndef DOUBLE_TYPE_STR
#define DOUBLE_TYPE_STR STRING(DOUBLE_TYPE)
#endif //DOUBLE_TYPE_STR

#define PROCESS_RELATIONS_SEQUENTIALLY 0
#define MIX_INPUT_TUPLES 2

namespace dbtoaster {
  namespace runtime {
    struct Arg: public option::Arg
    {
      static void printError(const char* msg1, const option::Option& opt, const char* msg2)
      {
        fprintf(stderr, "%s", msg1);
        fwrite(opt.name, opt.namelen, 1, stderr);
        fprintf(stderr, "%s", msg2);
      }

      static option::ArgStatus Unknown(const option::Option& option, bool msg)
      {
        if (msg) printError("Unknown option '", option, "'\n");
        return option::ARG_ILLEGAL;
      }

      static option::ArgStatus Required(const option::Option& option, bool msg)
      {
        if (option.arg != 0)
          return option::ARG_OK;

        if (msg) printError("Option '", option, "' requires an argument\n");
        return option::ARG_ILLEGAL;
      }

      static option::ArgStatus Numeric(const option::Option& option, bool msg)
      {
        char* endptr = 0;
        if (option.arg != 0 && strtol(option.arg, &endptr, 10)){};
        if (endptr != option.arg && *endptr == 0)
          return option::ARG_OK;

        if (msg) printError("Option '", option, "' requires a numeric argument\n");
        return option::ARG_ILLEGAL;
      }
    };

    enum  optionIndex { UNKNOWN, HELP, VERBOSE, ASYNC, LOGDIR, LOGTRIG, UNIFIED, OUTFILE, BATCH_SIZE, PARALLEL_INPUT, NO_OUTPUT, TIMEOUT, SAMPLESZ, SAMPLEPRD, STATSFILE, TRACE, TRACEDIR, TRACESTEP, LOGCOUNT, BUFFER_FRAC, ITERATIONS, LOWERLAT, UPPERLAT, FTRACE, FTRACE_REP };

    const option::Descriptor usage[] = {
    { UNKNOWN,       0,"", "",           Arg::Unknown, "dbtoaster query options:" },
    { HELP,          0,"h","help",       Arg::None,    "  -h       , \t--help  \tlist available options." },
    { VERBOSE,       0,"v","verbose",    Arg::None,    "  -v       , \t--verbose  \tfor verbose output." },
    { ASYNC,         0,"a","async",      Arg::None,    "  -a       , \t--async  \tasynchronous execution mode." },
    { LOGDIR,        0,"d","log-dir",    Arg::Required,"  -d  <arg>, \t--log-dir=<arg>  \tlogging directory." },
    { LOGTRIG,       0,"l","log-trigger",Arg::Required,"  -l  <arg>, \t--log-trigger=<arg>  \tlog stream triggers (several of them can be added with using this option several times)." },
    { UNIFIED,       0,"u","unified",    Arg::Required,"  -u  <arg>, \t--unified=<arg>  \tunified logging [stream | global]." },
    { OUTFILE,       0,"o","output-file",Arg::Required,"  -o  <arg>, \t--output-file=<arg>  \toutput file." },
    { BATCH_SIZE,    0,"b","batch-size", Arg::Required,"  -b  <arg>, \t--batch-size  \texecute as batches of certain size." },
    { PARALLEL_INPUT,0,"p","par-stream", Arg::Required,"  -p  <arg>, \t--par-stream  \tparallel streams (0=off, 2=deterministic)" },
    { NO_OUTPUT     ,0,"n","no-output",  Arg::None,    "  -n       , \t--no-output  \tdo not print the output result in the standard output" },
    { TIMEOUT       ,0,"","timeout",    Arg::Numeric,   "  \t--timeout=<arg>  \tstop measurement after [arg] seconds" },
    // Statistics profiling parameters
    { SAMPLESZ, 0,"","samplesize",  Arg::Numeric, "  \t--samplesize=<arg>  \tsample window size for trigger profiles." },
    { SAMPLEPRD,0,"","sampleperiod",Arg::Numeric, "  \t--sampleperiod=<arg>  \tperiod length, as number of trigger events." },
    { STATSFILE,0,"","statsfile",   Arg::Required,"  \t--statsfile=<arg>  \toutput file for trigger profile statistics." },
    // Tracing parameters
    { TRACE,    0,"","trace",       Arg::Required,"  \t--trace=<arg>  \ttrace query execution." },
    { TRACEDIR, 0,"","trace-dir",   Arg::Required,"  \t--trace-dir=<arg>  \ttrace output dir." },
    { TRACESTEP,0,"","trace-step",  Arg::Numeric, "  \t--trace-step=<arg>  \ttrace step size." },
    { LOGCOUNT, 0,"","log-count",   Arg::Numeric, "  \t--log-count=<arg>  \tlog tuple count every [arg] updates." },
    { ITERATIONS, 1,"","iterations",   Arg::Numeric, "  \t--iterations=<arg>  \titerate [arg] times over the dataset." },
    { BUFFER_FRAC, 1,"","buffer-frac",   Arg::Numeric, "  \t--buffer-frac=<arg>  \tReserve fraction [arg] (1-100) of maximal latency buffer space." },
#ifdef USE_RDTSC
    { LOWERLAT, 0,"","lower-lat",   Arg::Numeric, "  \t--lower-lat=<arg>  \tlower bound [arg] (TSC cycles) below which latencies are recorded." },
    { UPPERLAT, 0,"","upper-lat",   Arg::Numeric, "  \t--upper-lat=<arg>  \tupper bound [arg] (TSC cycles) above which latencies are recorded." },
#else
    { LOWERLAT, 0,"","lower-lat",   Arg::Numeric, "  \t--lower-lat=<arg>  \tlower bound [arg] (ns) below which latencies are recorded." },
    { UPPERLAT, 0,"","upper-lat",   Arg::Numeric, "  \t--upper-lat=<arg>  \tupper bound [arg] (ns) above which latencies are recorded." },
#endif
#ifdef __linux__
    { FTRACE,   0,"","ftrace",      Arg::None,     "  \t--ftrace  \tActivate ftrace tracing during measurements" },
    { FTRACE_REP, 0,"","ftrace-repetitions",   Arg::Numeric, "  \t--ftrace-repetitions=<arg>  \tnumber of maximum latency threshold violations after which ftrace trace is frozen." },
#endif
    { 0, 0, 0, 0, 0, 0 } };
    
    struct runtime_options {
      std::string log_dir;
      std::vector<std::string> logged_streams_v;
      std::set<std::string> logged_streams;
      std::string _unified;
      std::string out_file;

      unsigned int sample_size;
      unsigned int sample_period;
      std::string stats_file;

      // Tracing
      bool traced;
      std::string trace_opts;
      std::string trace_dir;
      unsigned int trace_counter, trace_step;
      std::unordered_set<std::string> traced_maps;
      unsigned int log_tuple_count_every;
      unsigned int iterations;
      unsigned long lower_lat, upper_lat;
      int buffer_frac;
#ifdef __linux__
      bool ftrace;
      unsigned int ftrace_rep;
#endif
      // Verbose
      static bool _verbose;
	  static bool verbose(){ return _verbose; }

      // Execution mode
      bool async;

      unsigned int batch_size;
      unsigned int parallel;

      bool no_output;
      int timeout;

      runtime_options(int argc = 0, char* argv[] = 0);

      void process_options(int argc, char* argv[]);
      void setup_tracing();

      void init(int argc, char* argv[]);

      // Result output.
      std::string get_output_file();

      // Trigger logging.
      bool global();
      bool unified();

      path get_log_file(std::string stream_name, event_type t);
      path get_log_file(std::string stream_name);
      path get_log_file(std::string stream_name, std::string ftype, bool prefix);

      // Statistics
      // Number of samples to collect per statitics period.
      unsigned int get_stats_window_size();

      // Period size, in terms of the number of trigger invocations.
      unsigned int get_stats_period();
      std::string get_stats_file();

      // Tracing.
      void parse_tracing(const std::string& opts);
      bool is_traced_map(std::string map_name);
      bool is_traced();
      path get_trace_file();
    };
  }
}

#endif