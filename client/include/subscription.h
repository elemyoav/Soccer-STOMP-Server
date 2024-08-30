#pragma once

#include <string>
#include <iostream>
#include <map>
#include <vector>
#include <bits/stdc++.h>


class subscription{
    private:
        std::map<std::string,std::string> team_a_events;
        std::map<std::string,std::string> team_b_events;
        std::map<std::string,std::string> general_events;
        std::map<int, std::pair<std::string, std::string>> time_to_name_and_description_before_half_time;
        std::map<int, std::pair<std::string, std::string>> time_to_name_and_description_after_half_time;
        bool beforehalftime = true;

    public:
        subscription(){};
        void add_team_a_event(std::string key, std::string value);
        void add_team_b_event(std::string key, std::string value);
        void add_general_event(std::string key, std::string value);
        void add_time_name_and_description(int key, std::pair<std::string,std::string>);
        void updateHalftime();

        std::string get_team_a_event(std::string key);
        std::string get_team_b_event(std::string key);
        std::string get_general_event(std::string key);
        std::pair<std::string, std::string> get_time_name_and_description(int key);

        std::string team_a_report();
        std::string team_b_report();
        std::string general_report();
        std::string time_name_report();    

};