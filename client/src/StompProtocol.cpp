#include "../include/StompProtocol.h"
#include "../include/event.h"

StompProtocol::StompProtocol():user_and_game_to_subscription(),login(false),
handler(nullptr),game_to_sub(),receipt(0),sub_id(0), userName(){}


//keyboard thread commands
void StompProtocol::handle_input(string& in)
{
    vector<string> words = split(in,' ');
    if(!login && words[0] != "login"){
			cout << "please log in first" << endl;
			return;
		}
    
    if(words[0] == "login") handle_login(words);
    if(words[0] == "join") handle_join(words);
    if(words[0] == "exit") handle_exit(words);
    if(words[0] == "report") handle_report(words);
    if(words[0] == "summary") handle_summary(words);
    if(words[0] == "logout") handle_logout(words);

}

void StompProtocol::handle_login(vector<string> words){
    

	if(login){
        cout << "The client is already logged in,log out before trying again" << endl;
        return;
    }

    vector<string> ip = split(words[1], ':');
    handler = new ConnectionHandler(ip[0], stoi(ip[1]));

    if(!handler->connect())
    {
        cout << "Could not connect to sever" << endl;
        delete handler;
        handler = nullptr;
        return;
    }


    logging_in = true;
    cv.notify_one();

	handler->sendFrameAscii("CONNECT\naccept-version:1.2\nhost:stomp.bgu.ac.il\nlogin:" + words[2] +
	"\npasscode:" + words[3] + "\n\n\0", '\0');
    userName = words[2];

}

void StompProtocol::handle_join(vector<string> words){
    string game = words[1];
    game_to_sub.insert({game, sub_id});
    receipt_to_response.insert({receipt, "Joined channel " + game});
    
    string packet = "SUBSCRIBE\ndestination:/"+game+"\nid:"+to_string(sub_id)+"\nreceipt:"+
    to_string(receipt)+"\n\n\0";
    handler->sendFrameAscii(packet, '\0');

    sub_id++;
    receipt++;
}

void StompProtocol::handle_exit(vector<string> words)
{
    
    string game = words[1];
    int id = game_to_sub.at(game);
    game_to_sub.erase(game);
    receipt_to_response.insert({receipt, "Exited channel " + game});

    string packet = "UNSUBSCRIBE\nid:"+to_string(id)+"\nreceipt:"+
    to_string(receipt)+"\n\n\0";
    handler->sendFrameAscii(packet, '\0');
    receipt++;
}

void StompProtocol::handle_report(vector<string> words)
{
    names_and_events x = parseEventsFile(words[1]);
    vector<Event> events = x.events;
    for(Event e: events){
        
        string packet = "SEND\ndestination:/"+x.team_a_name+"_"+x.team_b_name+"\n\n";
        packet += "user: "+userName+"\n";
        
        for(pair<string,string> update:e.get_game_updates()){
            if(update.first == "active" && update.second == "true"){
                packet+="team_a: "+e.get_team_a_name()+"\n";
                packet+="team_b: "+e.get_team_b_name()+"\n";
            }
        }
        
        packet+="event name: "+e.get_name()+"\n";
        packet+="time: " + to_string(e.get_time())+"\n";

        packet+="general game updates:\n";
        for(pair<string,string> update:e.get_game_updates()){
            packet+="    "+update.first+": "+update.second+"\n";
        }

        packet+="team a updates:\n";
        for(pair<string,string> update:e.get_team_a_updates()){
            packet+="    "+update.first+": "+update.second+"\n";
        }

        packet+="team b updates:\n";
        for(pair<string,string> update:e.get_team_b_updates()){
            packet+="    "+update.first+": "+update.second+"\n";
        }

        packet+="description:\n";
        packet+= e.get_discription();
        packet+="\n\0";

        handler ->sendFrameAscii(packet, '\0');
    }
}

void StompProtocol::handle_summary(vector<string> words){
    
    pair<string, string> key(" "+words[2] +"\n", words[1]+"\n");
        if(user_and_game_to_subscription.count(key) == 0){
        cout << "sorry no" << endl;
        return;
    }
	subscription s = user_and_game_to_subscription.at(key);

	vector<string> names = split(words[1], '_');
	string packet = names[0] + " vs " + names[1] +"\nGame stats:\nGeneral stats:\n";
	packet += s.general_report();
	packet += names[0] + " stats:\n" +  s.team_a_report();
	packet += names[1] + " stats:\n" + s.team_b_report();
	packet += "Game event reports:\n" + s.time_name_report();


	//write packet to words[2]
    ofstream outdata;
    outdata.open(words[3]);
    if(!outdata) {
        cerr << "could not open file" << endl;
        exit(1);
    }

    outdata << packet;
    outdata.close();

}

void StompProtocol::handle_logout(vector<string> words){
    string packet = "DISCONNECT\nreceipt:" + to_string(receipt) +"\n\n\0";
    receipt_to_response.insert({receipt++, "Disconnected"});

    handler->sendFrameAscii(packet, '\0');
}



//the socket thread commands
void StompProtocol::reader(){
    while(1){
        std::unique_lock<std::mutex> lk(mutex);
        cv.wait(lk,[&]{return logging_in;});

        while(handler && handler->open()){
            string line;
            if(!handler -> getLine(line)) line = "";

            if(line == "RECEIPT\n") handle_receipt();

            if(line == "CONNECTED\n") handle_connected();

            if(line == "MESSAGE\n") handle_message();

            if(line == "ERROR\n") handle_error();
        }

        logging_in = false;
    }
}

void StompProtocol::handle_receipt(){
    string receipt_id;
    handler -> getLine(receipt_id);
    vector<string> x = split(receipt_id, ':');
    int receipt__id = stoi(x[1]);
    
    //get the response
    string response = receipt_to_response.at(receipt__id);

    //erase the response from the table since it is already taken care of
    receipt_to_response.erase(receipt__id);
    
    //take care of the response
    if(response == "Disconnected"){
        login = false;
        delete handler;
        handler = nullptr;
    }
    cout <<  response << endl;
}

void StompProtocol::handle_connected(){
    cout << "Login successful" << endl;
    login = true;
}

void StompProtocol::handle_message(){
    string line;
    string game_name;
    string user;
    string event_name;
    int time;

    //subscription
    handler -> getLine(line);

    //message-id

    line = "";
    handler -> getLine(line);

    //game name
    line = "";
    handler -> getLine(line);

    game_name = split(line, '/')[1];

    //blank-line
    line = "";
    handler -> getLine(line);
    
    //user
    line = "";
    handler -> getLine(line);

    user = split(line, ':')[1];


    
    if(!user_and_game_to_subscription.count(make_pair(user, game_name)))  
    {
        user_and_game_to_subscription.insert(make_pair(make_pair(user, game_name), subscription()));
    }
    

    
    line = "";
    handler -> getLine(line);
    
    while(split(line, ':')[0] != "event name")
    {
        if(now_playing.count(game_name) == 0) now_playing.insert(game_name);
        line = "";
        handler -> getLine(line);
    }

    //event name
    event_name = split(line, ':')[1];

    //time
    line = "";
    handler -> getLine(line);

    time = stoi(split(line, ':')[1]);

    //general game updates
    line = "";
    handler -> getLine(line);

    //the updates themselves
    line = "";
    handler -> getLine(line);
    while(line.substr(0,3) == "   "){
        vector<string> words = split(line , ':');
        user_and_game_to_subscription[{user, game_name}].add_general_event(words[0], words[1]);
        if (words[0] == "    before halftime" && words[1] == " false\n")
            user_and_game_to_subscription[{user, game_name}].updateHalftime();
        line = "";
        handler -> getLine(line);
    }

    //team a updates
    line = "";
    handler -> getLine(line);


    while(line.substr(0,3) == "   "){
        vector<string> words = split(line , ':');
        user_and_game_to_subscription[{user, game_name}].add_team_a_event(words[0], words[1]);
        line = "";
        handler -> getLine(line);

    }

    //team b updates
    line = "";
    handler -> getLine(line);

    while(line.substr(0,3) == "   "){
        vector<string> words = split(line , ':');
        user_and_game_to_subscription[{user, game_name}].add_team_b_event(words[0], words[1]);
        line = "";
        handler -> getLine(line);
    }
    
    //description
    handler -> getLine(line);
    line = line.substr(12, line.size()) + "\n";

    user_and_game_to_subscription[{user, game_name}].add_time_name_and_description(time, {event_name,line});
    line = "";

    handler -> getFrameAscii(line, '\0');

}

void StompProtocol::handle_error(){
    string line;
    handler -> getLine(line);
    if(split(line, ':')[0] == "receipt-id"){
         receipt_to_response.erase(stoi(split(line, ':')[1]));
         line = "";
         handler -> getLine(line);
    }

    cout << split(line, ':')[1];
    line = "";
    handler -> getFrameAscii(line, '\0');

    line = "";

    login = false;
    delete handler;
    handler = nullptr;
}

vector<string> StompProtocol::split(string input, char sign){    
    vector<string> tokens;
    string token;
    stringstream ss(input);
    while (getline(ss, token, sign)){
        tokens.push_back(token);
    }
    return tokens;
}

