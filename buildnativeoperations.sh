#!/usr/bin/env bash

#export OMP_NUM_THREADS=1

export CMAKE_COMMAND="cmake"
export MAKE_COMMAND="make"
echo eval $CMAKE_COMMAND
if [ "$(uname)" == "Darwin" ]; then
    echo "RUNNING OSX CLANG"
    # Do something under Mac OS X platform
    #export CC=clang-omp++
    export CXX=clang-omp++
elif [ "$(expr substr $(uname -s) 1 10)" == "MINGW32_NT" ] || [ "$(expr substr $(uname -s) 1 10)" == "MINGW64_NT" ]; then
    # Do something under Windows NT platform
    if [ "$2" == "cuda" ]; then
        export CMAKE_COMMAND="cmake -G \"NMake Makefiles\""
        export MAKE_COMMAND="nmake"
    else
        export CMAKE_COMMAND="cmake -G \"MSYS Makefiles\""
        export MAKE_COMMAND="make"
    fi
   CC=/mingw64/bin/gcc
    CXX=/mingw64/bin/g++
    echo "Running windows"
   # export GENERATOR="MSYS Makefiles"

fi



if [ "$#" -lt 1 ]; then
    echo "Please specify an argument"
else
    command="$1"
    echo "Running $1"
    if [ "$1" == "clean" ]; then
       rm -rf cmake_install.cmake
       rm -rf cubinbuild
       rm -rf ptxbuild
       rm -rf CMakeFiles 
       rm -f CMakeCache.txt
       rm -rf testbuild
       rm -rf eclipse/CMakeFiles

       echo "Deleted build"
    elif [ "$1" ==  "eclipse" ]; then
            cd eclipse
            export GENERATOR="Eclipse CDT4 - Unix Makefiles"
            eval $CMAKE_COMMAND -DCMAKE_ECLIPSE_GENERATE_SOURCE_PROJECT=TRUE ..
            python ./nsight-err-parse-patch.py ./project
            mv eclipse/.cproject .
            mv eclipse/.project .
     elif [ "$1" ==  "lib" ]; then
         rm -rf library  build
         mkdir librarybuild
         cd librarybuild
          eval $CMAKE_COMMAND -DLIBRARY=TRUE ..
         eval $MAKE_COMMAND && cd ..
     elif [ "$1" ==  "test" ]; then
           if [ "$#" -gt "1" ]; then
                rm -rf testbuild
                mkdir testbuild
                cd testbuild
                 eval $CMAKE_COMMAND  -DRUN_TEST=TRUE ..
                eval $MAKE_COMMAND && cd ..
                mv testbuild/test/libnd4jtests .
               ./libnd4jtests -n "$2"
           else
               rm -rf testbuild
               mkdir testbuild
               cd testbuild
               eval $CMAKE_COMMAND -DRUN_TEST=TRUE ..
               eval $MAKE_COMMAND && cd ..
               mv testbuild/test/libnd4jtests .
               ./libnd4jtests
           fi

           echo "FINISHING BUILD"
     elif [ "$1" == "cubin" ]; then
            rm -rf cubinbuild
           mkdir cubinbuild
           cd cubinbuild
            eval $CMAKE_COMMAND -DCUBIN=TRUE ..
           eval $MAKE_COMMAND && cd ..
           echo "FINISHING BUILD"
           mv cubinbuild/cubin/cuda_compile_cubin_generated_all.cu.cubin all.cubin
      elif [ "$1" == "buffer" ]; then
            rm -rf bufferbuild
           mkdir bufferbuild
           cd bufferbuild
            eval $CMAKE_COMMAND -DBUFFER=TRUE ..
           eval $MAKE_COMMAND && cd ..
           echo "FINISHING BUILD"
     elif [ "$1" == "blas" ]; then
            rm -rf blasbuild
           mkdir blasbuild
           cd blasbuild
           if [ "$#" -gt "1" ]; then
              if [ "$2" == "cuda" ]; then
                    eval $CMAKE_COMMAND -DCUDA_BLAS=true -DBLAS=TRUE ..
                   eval $MAKE_COMMAND && cd ..
                  echo "FINISHING BUILD"
              elif [ "$2" == "cpu" ]; then
                    echo "RUNNING COMMAND $CMAKE_COMMAND"
                    eval $CMAKE_COMMAND -DCPU_BLAS=true -DBLAS=TRUE ..
                   eval $MAKE_COMMAND && cd ..
                   echo "FINISHING BUILD"
              else
                   echo "Please specify cpu or gpu"

              fi

            else

                   eval $CMAKE_COMMAND  -DCPU_BLAS=true -DBLAS=TRUE ..
                  eval $MAKE_COMMAND && cd ..
                  echo "FINISHING BUILD"
           fi


      elif [ "$1" == "ptx" ]; then
           rm -rf ptxbuild
           mkdir ptxbuild
           cd ptxbuild
            eval $CMAKE_COMMAND -DPTX=TRUE ..
           eval $MAKE_COMMAND && cd ..
           echo "FINISHING BUILD"
           mv ptxbuild/ptx/cuda_compile_ptx_generated_all.cu.ptx all.ptx
     elif [ "$1" == "fatbin" ]; then
           rm -rf fatbuild
           mkdir fatbuild
           cd fatbuild
            eval $CMAKE_COMMAND -DFATBIN=TRUE ..
           eval $MAKE_COMMAND && cd ..
           echo "FINISHING BUILD"
           mv fatbuild/fatbin/cuda_compile_fatbin_generated_all.cu.fatbin all.fatbin
fi

fi
