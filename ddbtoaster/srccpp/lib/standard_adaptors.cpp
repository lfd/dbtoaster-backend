#include <tuple>
#include <algorithm>

#include "smhasher/MurmurHash2.hpp"
#include "standard_adaptors.hpp"
#include "hpds/KDouble.hpp"

#include "runtime.hpp"
using namespace ::dbtoaster::runtime;

namespace dbtoaster {
namespace adaptors {

/******************************************************************************
	csv_adaptor
******************************************************************************/

csv_adaptor::csv_adaptor(relation_id_t _id) 
		: id(_id), type(insert_tuple), schema_size(0), delimiter(",") 
{
	schema = std::string();
}

csv_adaptor::csv_adaptor(relation_id_t _id, std::string sch)
		: id(_id), type(insert_tuple), schema_size(0), delimiter(",")
{
	schema = std::string(sch);
	validate_schema();
}

csv_adaptor::csv_adaptor(relation_id_t i, int num_params,
						const std::pair<std::string,std::string> params[])
		: id(i), type(insert_tuple), schema_size(0), delimiter(",")
{
	parse_params(num_params,params);
	validate_schema();
}

void csv_adaptor::parse_params(int num_params, 
							   const std::pair<std::string, std::string> params[]) {
	std::string param_schema = "";
	std::string param_schema_prefix = "";
	for (int i = 0; i< num_params; ++i) {
	  std::string k = params[i].first;
	  std::string v = params[i].second;
	  if( runtime_options::verbose() )
		std::cerr << "csv params: " << k << ": " << v << std::endl;

	  if ( k == "delimiter" ) {
		delimiter = (char*)(v.c_str());
	  } else if ( k == "schema" ) {
		param_schema = v;
	  } else if ( k == "eventtype" ) {
		type = ( v == "insert" ? insert_tuple : delete_tuple);
	  } else if ( k == "deletions" ) {
		param_schema_prefix = ( v == "true" ? "order,event," : "" );
	  }
	  // TODO: handle parameterized events, via 'triggers' key as seen
	  // in OCaml adaptors.
	}
	param_schema.assign(parse_schema(param_schema_prefix + param_schema));
	schema = std::string(param_schema);
}

std::string csv_adaptor::parse_schema(std::string s)
{
	std::string r = "";

	typedef std::string::const_iterator iter;
    iter beg = s.begin();
    iter end = s.end();
    while(beg != end) {
        iter temp = std::find(beg, end, ',');
        if(beg != end) {
			std::string ty(beg, temp);
			if ( ty == "event" )       r += "e";
			else if ( ty == "order" )  r += "o";
			else if ( ty == "int" )    r += "l";
			else if ( ty == "long" )   r += "l";
			else if ( ty == "float" )  r += "f";
			else if ( ty == "double" ) r += "f";
			else if ( ty == "date" )   r += "d";
			else if ( ty == "hash" )   r += "h";
			else if ( ty == "char" )   r += "c";
			else if ( ty == "string" ) r += "s";
			else {
				std::cerr << "invalid csv schema type " << ty << std::endl;
				r = "";
				break;
			}
        }
        beg = temp;
        while ((beg != end) && (*beg == ','))
            beg++;
    }
	return (char*)(r.c_str());
}

void csv_adaptor::validate_schema() {
	bool valid = true;
	
	std::string::iterator it = schema.begin();
	std::string::iterator end = schema.end();
	for (; valid && it != end; ++it) {	
	  switch(*it) {
		case 'e':  // event type
		case 'o':  // order field type
				  break;
		case 'l':
		case 'f':
		case 'd':
		case 'h':
		case 'c':
		case 's':
				  schema_size++;
				  break;
		default: valid = false; break;
	  }
	}
	if ( !valid ) {
		schema = std::string();
		schema_size = 0;
	}
}

// Interpret the schema.
std::tuple<bool, bool, unsigned int, event_args_t> 
csv_adaptor::interpret_event(char* data)
{
	bool ins; unsigned int event_order=0; int y,m,d; 
	char* date_y_field;
	char* date_m_field;
	char* date_d_field;
	
	event_args_t tuple(schema_size);
	size_t tupleIdx = 0;
	bool valid = true;

	// Default to the adaptor's event type, and override with an event
	// field in the schema.
	bool insert = type == insert_tuple;

	const char * delim = delimiter.c_str();
    size_t delimSize = delimiter.size();
    char* field_start=data;
    char* field_end=data;

	std::string::iterator schema_it = schema.begin();
	std::string::iterator schema_end = schema.end();
	while(valid && (schema_it != schema_end)) {
    	field_end = strstr(field_start,delim);
        if(field_end) *field_end='\0';
        // std::cout << "  handling schema => " << *schema_it << std::endl;
        switch (*schema_it) {
			case 'e': ins=atoi(field_start); insert = ins; break;
			case 'l': tuple[tupleIdx++]=std::shared_ptr<long>(new long(atol(field_start))); break;
			case 'f': tuple[tupleIdx++]=std::shared_ptr<DOUBLE_TYPE>(new DOUBLE_TYPE(atof(field_start))); break;
			case 'h': tuple[tupleIdx++]=std::shared_ptr<int>(new int(static_cast<int>(MurmurHash2(field_start,strlen(field_start)*sizeof(char),0))));
					  break;
			case 'd': 
				date_y_field = strtok (field_start,"-");
				if(date_y_field != NULL) {
					date_m_field = strtok (NULL,"-");
					if(date_m_field != NULL) {
						date_d_field = strtok (NULL,"-");
						if(date_d_field != NULL) {
							y = atoi(date_y_field);
							m = atoi(date_m_field);
							d = atoi(date_d_field);
							if ( 0 < m && m < 13 && 0 < d && d <= 31) {
								tuple[tupleIdx++]=std::shared_ptr<date>(new date(y*10000+m*100+d));
							}
        					// std::cout << "  date is => " << date(y*10000+m*100+d) << std::endl;
						} else valid = false;
					} else valid = false;
				} else valid = false;
			    break;
			case 'o':
				event_order=atoi(field_start);
				break;
			case 'c':
  				tuple[tupleIdx++]=std::shared_ptr<char>(new char(field_start[0])); 
  				break;
			case 's': 
			    tuple[tupleIdx++]=std::shared_ptr<STRING_TYPE>(new STRING_TYPE(field_start)); 
			    break;
			default: valid = false; break;
		}

        ++schema_it;
        if(field_end) {
            field_end += delimSize;
            field_start = field_end;
        } else if(schema_it != schema_end){
        	valid = false;
        	break;
        }
	}
    // std::cout << " tuples is ==> " << tuple << std::endl;
	return std::make_tuple(valid, insert, event_order, tuple);
}

void csv_adaptor::read_adaptor_events(char* data, std::shared_ptr<std::list<event_t> > eventList, std::shared_ptr<std::list<event_t> > eventQue) {
	if ( schema.size() > 0 ) {
	  // Interpret the schema.
	  std::tuple<bool, bool, unsigned int, event_args_t> evt = interpret_event(data);
	  bool valid = std::get<0>(evt);
	  bool insert = std::get<1>(evt);
	  unsigned int event_order = std::get<2>(evt);
	  if ( valid )  {
		event_t e(insert? insert_tuple : delete_tuple, id, event_order, std::get<3>(evt));
		if(e.event_order == 0) {
			eventList->push_back(e);
		} else {
			eventQue->push_back(e);
		}
	  } else {
		std::cerr << "adaptor could not process " << data << std::endl;
		std::cerr << "schema: " << schema << std::endl;
	  }
	} else if ( runtime_options::verbose() ) {
		std::cerr << "Skipping event, no schema found." << std::endl;
	}
}

}

namespace datasets
{

namespace order_books
{

/******************************************************************************
	order_book_tuple
******************************************************************************/
order_book_tuple::order_book_tuple(const order_book_message& msg) {
	t = msg.t;
	id = msg.id;
	volume = msg.volume;
	price = msg.price;
    broker_id = 0;
}

order_book_tuple& order_book_tuple::operator=(order_book_tuple& other) {
	t = other.t;
	id = other.id;
	broker_id = other.broker_id;
	volume = other.volume;
	price = other.price;
	return *this;
}

void order_book_tuple::operator()(event_args_t& e) {
	if (e.size() > 0) e[0] = std::shared_ptr<DOUBLE_TYPE>(new DOUBLE_TYPE(t)); 
	else e.push_back(std::shared_ptr<DOUBLE_TYPE>(new DOUBLE_TYPE(t)));
	if (e.size() > 1) e[1] = std::shared_ptr<long>(new long(id)); 
	else e.push_back(std::shared_ptr<long>(new long(id)));
	if (e.size() > 2) e[2] = std::shared_ptr<long>(new long(broker_id)); 
	else e.push_back(std::shared_ptr<long>(new long(broker_id)));
	if (e.size() > 3) e[3] = std::shared_ptr<DOUBLE_TYPE>(new DOUBLE_TYPE(volume)); 
	else e.push_back(std::shared_ptr<DOUBLE_TYPE>(new DOUBLE_TYPE(volume)));
	if (e.size() > 4) e[4] = std::shared_ptr<DOUBLE_TYPE>(new DOUBLE_TYPE(price)); 
	else e.push_back(std::shared_ptr<DOUBLE_TYPE>(new DOUBLE_TYPE(price)));
}

/******************************************************************************
	order_book_adaptor
******************************************************************************/
order_book_adaptor::order_book_adaptor(
		relation_id_t bids_rel_sid, relation_id_t asks_rel_sid, int nb, order_book_type t)
          : bids_rel_id(bids_rel_sid), asks_rel_id(asks_rel_sid), num_brokers(nb), type(t)
{
	bids = std::shared_ptr<order_book>(new order_book());
	asks = std::shared_ptr<order_book>(new order_book());
	deterministic = false;
	insert_only = false;
}

order_book_adaptor::order_book_adaptor(relation_id_t bids_rel_sid, relation_id_t asks_rel_sid, int num_params,
				   std::pair<std::string, std::string> params[])
{
	bids_rel_id = bids_rel_sid;
	asks_rel_id = asks_rel_sid;
	bids = std::shared_ptr<order_book>(new order_book());
	asks = std::shared_ptr<order_book>(new order_book());
	deterministic = false;
        insert_only = false;
	num_brokers = 10;
        type = both;

	for (int i = 0; i < num_params; ++i) {
		std::string k = params[i].first;
		std::string v = params[i].second;
		if( runtime_options::verbose() )
		   std::cerr << "order book adaptor params: "
				<< params[i].first << ", " << params[i].second << std::endl;

		if ( k == "book" ) {
		  type = (v == "bids"? tbids : (v == "asks"? tasks : both));
		} else if ( k == "brokers" ) {
		  num_brokers = atoi(v.c_str());
		} else if ( k == "validate" ) { // Ignore.
		} else if ( k == "deterministic" ) {
		  deterministic = (v == "yes");
		} else if ( k == "insert-only" ) {
		  insert_only = true;
		} else if ( k == "schema" ) {
		  // simply ignore this parameter since it is hard-coded
		  // it should be always "double,long,long,double,double"
		} else {
		  std::cerr << "Invalid order book param " << k << ", " << v << std::endl;
		}
	}
}

bool order_book_adaptor::parse_error(const char* data, int field) {
	std::cerr << "Invalid field " << field << " message " << data << std::endl;
	return false;
}

// Expected message format: t, id, action, volume, price
bool order_book_adaptor::parse_message(char* data, order_book_message& r) {
	std::string msg = data;
	char* start = data;
	char* end = start;
	char action;
	char c;

	for (int i = 0; i < 5; ++i)
	{
	  while ( *end && *end != ',' ) ++end;
	  if ( start == end ) { return parse_error(data, i); }
	  if ( *end == '\0' && i != 4 ) { return parse_error(data, i); }
	  c = end[0];
	  *end = '\0';

	  switch (i) {
	  case 0: r.t = atof(start); break;
	  case 1: r.id = atol(start); break;
	  case 2:
		  action = *start;
		  if ( !(action == 'B' || action == 'S' ||
				 action == 'E' || action == 'F' ||
				 action == 'D' || action == 'X' ||
				 action == 'C' || action == 'T') )
		  {
			 return parse_error(data, i);
		  }

		  r.action = action;
		  break;

	  case 3: r.volume = atof(start); break;
	  case 4: r.price = atof(start); break;
	  default: return parse_error(data, i);
	  }

	  end[0] = c;
	  start = ++end;
	}
	return true;
}

void order_book_adaptor::process_message(const order_book_message& msg,
					 std::shared_ptr<std::list<event_t> > dest)
{
	bool valid = true;
	order_book_tuple r(msg);
	event_type t = insert_tuple;
	relation_id_t rel_id = -1;
	unsigned int event_order = msg.t * 2;

	if ( msg.action == "B" ) {
	  if (type == tbids || type == both) {
		r.broker_id = (deterministic ? msg.id : rand()) % num_brokers;
		(*bids)[msg.id] = r;
		t = insert_tuple;
	  	rel_id = bids_rel_id;
	  } else valid = false;
	}
	else if ( msg.action == "S" ) {
	  if (type == tasks || type == both) {
		r.broker_id = (deterministic ? msg.id : rand()) % num_brokers;
		(*asks)[msg.id] = r;
		t = insert_tuple;
	  	rel_id = asks_rel_id;
	  } else valid = false;
	}

	else if ( msg.action == "E" ) {
	  order_book_tuple x;
	  bool x_valid = true;
	  order_book::iterator bid_it = bids->find(msg.id);
	  if ( bid_it != bids->end() ) {
	  	rel_id = bids_rel_id;
		x = r = bid_it->second;
		r.volume -= msg.volume;
		if ( r.volume <= 0.0 ) { bids->erase(bid_it); valid = false; }
		else { (*bids)[msg.id] = r; }
	  } else {
		order_book::iterator ask_it = asks->find(msg.id);
		if ( ask_it != asks->end() ) {
	  	  rel_id = asks_rel_id;
		  x = r = ask_it->second;
		  r.volume -= msg.volume;
		  if ( r.volume <= 0.0 ) { asks->erase(ask_it); valid = false; }
		  else { (*asks)[msg.id] = r; }
		} else {
		  //std::cerr << "unknown order id " << msg.id
		  //     << " (neither bid nor ask)" << std::endl;
		  valid = false;
		  x_valid = false;
		}
	  }
	  if ( x_valid && !insert_only ) {
		event_args_t fields(5);
		x(fields);
		event_t y(delete_tuple, rel_id, event_order-1, fields);
		dest->push_back(y);
	  }
	  t = insert_tuple;
	}

	else if ( msg.action == "D" || msg.action == "F" )
	{
	  order_book::iterator bid_it = bids->find(msg.id);
	  if ( bid_it != bids->end() ) {
	  	rel_id = bids_rel_id;
		r = bid_it->second;
		bids->erase(bid_it);
	  } else {
		order_book::iterator ask_it = asks->find(msg.id);
		if ( ask_it != asks->end() ) {
	  	  rel_id = asks_rel_id;
		  r = ask_it->second;
		  asks->erase(ask_it);
		} else {
		  //std::cerr << "unknown order id " << msg.id
		  //     << " (neither bid nor ask)" << std::endl;
		  valid = false;
		}
	  }
	  t = delete_tuple;
	}

	/*
	// ignore for now...
	else if ( v->action == "X")
	else if ( v->action == "C")
	else if ( v->action == "T")
	*/
	else { valid = false; }


	if ( valid ) {
	  event_args_t fields(5);
	  r(fields);
	  if ( !(t == delete_tuple && insert_only) ) {
		event_t e(t, rel_id, event_order, fields);
		dest->push_back(e);
	  }
	}
}

void order_book_adaptor::read_adaptor_events(char* data, std::shared_ptr<std::list<event_t> > eventList, std::shared_ptr<std::list<event_t> > eventQue) {
	// Grab a message from the data.
	order_book_message r;
	bool valid = parse_message(data, r);
	  
	if ( valid ) {
	  // Process its action, updating the internal book.
	  process_message(r, eventQue);
	}
}

}
}
}
