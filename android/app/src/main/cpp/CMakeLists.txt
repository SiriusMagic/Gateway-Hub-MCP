cmake_minimum_required(VERSION 3.22)
project(lucia_llama LANGUAGES C CXX)

# Lucía's existing 32-bit build still uses Vosk and Android TTS.
if(NOT ANDROID_ABI STREQUAL "arm64-v8a")
    return()
endif()

include(FetchContent)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
FetchContent_Declare(llama_cpp
    URL https://codeload.github.com/ggml-org/llama.cpp/tar.gz/12d47ba2f462439240329b4e34a5e173e3267d66
    URL_HASH SHA256=117507da255be0194eba1ed8c5d27cf4a43ae9c461fb19dc43f0e1535fec4894
    DOWNLOAD_EXTRACT_TIMESTAMP TRUE
)
FetchContent_MakeAvailable(llama_cpp)

add_library(lucia_llama SHARED lucia_llama.cpp)
target_compile_features(lucia_llama PRIVATE cxx_std_17)
target_link_libraries(lucia_llama PRIVATE llama)

set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
FetchContent_Declare(whisper_cpp
    URL https://codeload.github.com/ggml-org/whisper.cpp/tar.gz/9386f239401074690479731c1e41683fbbeac557
    URL_HASH SHA256=0cc49e22729edd3cd2e7727522b456f459e1860fe65e975b39ce1a194857f9d5
    DOWNLOAD_EXTRACT_TIMESTAMP TRUE
)
FetchContent_MakeAvailable(whisper_cpp)

add_library(lucia_whisper SHARED lucia_whisper.cpp)
target_compile_features(lucia_whisper PRIVATE cxx_std_17)
target_link_libraries(lucia_whisper PRIVATE whisper)
target_link_options(lucia_whisper PRIVATE "-Wl,--exclude-libs,ALL")
target_link_options(lucia_llama PRIVATE "-Wl,--exclude-libs,ALL")
