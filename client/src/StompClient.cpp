#include <string>
#include <iostream>
#include <vector>
#include <sstream>
#include "ConnectionHandler.h"
#include "event.cpp"
#include <thread>
#include <mutex>
#include "subscription.h"
#include "StompProtocol.h"
using namespace std;


StompProtocol protocol; 

int main(int argc, char *argv[]) {
	// TODO: implement the STOMP client
	thread read([]()
	{
		while(1){
			while(protocol.handler && protocol.handler->open()){protocol.reader();}
		}
	});
	while(1)
	{
		string in;
		getline(cin,in);
		protocol.handle_input(in);
	}
	return 0;
}



