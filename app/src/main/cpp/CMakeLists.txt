cmake_minimum_required(VERSION 3.22.1)

project("surakshak")

add_library(
    surakshak
    SHARED
    c2pa_bridge.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    surakshak
    ${log-lib}
)
