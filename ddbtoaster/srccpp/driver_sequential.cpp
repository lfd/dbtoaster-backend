/* -*- Mode: C; indent-tabs-mode: t; c-basic-offset: 4; tab-width: 4 -*- */
#include "lib/RTEMSStreamProgram.hpp"

// Simple DBToaster driver that feeds events sequentially after mapping the complete
// data set into memory.
// TODO: print log buffer into stream, and direct this into a file (or cout, if
// nothing specified)
int main(int argc, char* argv[]) {
  dbtoaster::RTEMSStreamProgram p(argc,argv);
  dbtoaster::Program::snapshot_t snap;

  p.init();
  p.run(false);

  p.print_log_buffer();
  return 0;
}
