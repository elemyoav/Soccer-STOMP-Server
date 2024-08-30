#pragma once
#include <string>
#include <iostream>
#include <vector>
#include <sstream>
#include <fstream>
#include <cstdlib>
#include <thread>
#include <set>
#include "subscription.h"
#include "../include/ConnectionHandler.h"
#include <mutex>
using namespace std;

// TODO: implement the STOMP protocol
class StompProtocol
{
private:
    map<pair<string, string>, subscription> user_and_game_to_subscription;
    map<string,int> game_to_sub;
    map<int, string> receipt_to_response;
    int receipt;
    int sub_id;
    volatile bool logging_in;
    string userName;
    set<string> now_playing;

    bool login = false;
    std::mutex mutex;
    std::condition_variable cv;
public:
    ConnectionHandler* handler;


    StompProtocol();
    vector<string> split(string input, char sign);


    //keyboard thread commands
    void handle_input(string &in);
    void handle_login(vector<string> words);
    void handle_join(vector<string> words);
    void handle_exit(vector<string> words);
    void handle_report(vector<string> words);
    void handle_summary(vector<string> words);
    void handle_logout(vector<string> words);

    //socket thread commands
    void reader();
    void handle_receipt();
    void handle_connected();
    void handle_message();
    void handle_error();



};
