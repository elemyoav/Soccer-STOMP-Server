#include "subscription.h"

void subscription::add_team_a_event(std::string key, std::string value){
    team_a_events[key] = value;
}
void subscription::add_team_b_event(std::string key, std::string value){
    team_b_events[key] = value;
}
void subscription::add_general_event(std::string key, std::string value){
    general_events[key] = value;
}

void subscription::add_time_name_and_description(int key, std::pair<std::string,std::string> value){
    if (beforehalftime)
        time_to_name_and_description_before_half_time.insert({key, value});
    else
        time_to_name_and_description_after_half_time.insert({key, value});
}

void subscription::updateHalftime(){
    beforehalftime = false;
}
        
std::string subscription::get_team_a_event(std::string key){
    return team_a_events.at(key);
}
std::string subscription::get_team_b_event(std::string key){
    return team_b_events.at(key);
}
std::string subscription::get_general_event(std::string key){
    return general_events.at(key);
}
std::pair<std::string, std::string> subscription::get_time_name_and_description(int key){
    if (time_to_name_and_description_before_half_time.count(key))
        return time_to_name_and_description_before_half_time.at(key);
    else
        return time_to_name_and_description_after_half_time.at(key);
}  

std::string subscription::team_a_report(){
    std::vector<std::string> sorted_stats;
    for(std::pair<std::string, std::string> stat: team_a_events){
        sorted_stats.push_back(stat.first +":" + stat.second);
    }
    std::sort(sorted_stats.begin(),sorted_stats.end());
    std::string packet = "";
    for(std::string stat:sorted_stats){
        packet += stat.substr(4,stat.length());
    }
    return packet;
}

std::string subscription::team_b_report(){
    std::vector<std::string> sorted_stats;
    for(std::pair<std::string, std::string> stat: team_b_events){
        sorted_stats.push_back(stat.first +":" + stat.second);
    }
    std::sort(sorted_stats.begin(),sorted_stats.end());
    std::string packet = "";
    for(std::string stat:sorted_stats){
        packet += stat.substr(4,stat.length());
    }
    return packet;
}

std::string subscription::general_report(){
    std::vector<std::string> sorted_stats;
    for(std::pair<std::string, std::string> stat: general_events){
        sorted_stats.push_back(stat.first +":" + stat.second);
    }
    std::sort(sorted_stats.begin(),sorted_stats.end());
    std::string packet = "";
    for(std::string stat:sorted_stats){
        packet += stat.substr(4,stat.length());
    }
    return packet;
}

std::string subscription::time_name_report(){
    std::vector<int> sorted_times_before_half_time;
    std::vector<int> sorted_times_after_half_time;
    for(std::pair<int, std::pair<std::string, std::string>> des: time_to_name_and_description_before_half_time){
        sorted_times_before_half_time.push_back(des.first);
    }
    for(std::pair<int, std::pair<std::string, std::string>> des: time_to_name_and_description_after_half_time){
        sorted_times_after_half_time.push_back(des.first);
    }

    std::sort(sorted_times_before_half_time.begin(), sorted_times_before_half_time.end());
    std::sort(sorted_times_after_half_time.begin(), sorted_times_after_half_time.end());

    std::string packet = "";
    for(int time:sorted_times_before_half_time){
        std::pair<std::string,std::string> name_and_des = time_to_name_and_description_before_half_time.at(time);
        name_and_des.first.pop_back();
        packet+= std::to_string(time) + " -" + name_and_des.first +":\n";
        packet+= name_and_des.second+"\n";
    }
    for(int time:sorted_times_after_half_time){
        std::pair<std::string,std::string> name_and_des = time_to_name_and_description_after_half_time.at(time);
        name_and_des.first.pop_back();
        packet+= std::to_string(time) + " - " + name_and_des.first +":\n";
        packet+= name_and_des.second+"\n";
    }
    return packet;
}