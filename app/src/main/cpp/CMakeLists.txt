cmake_minimum_required(VERSION 3.22.1)

project("chesslens_native")

add_library(
    stockfish_native
    SHARED
    stockfish_jni.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    stockfish_native
    ${log-lib}
)
