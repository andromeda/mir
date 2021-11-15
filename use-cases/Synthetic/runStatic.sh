#!/bin/bash

static() {
  
  cd $1
  
  mir-sa . > static.json 
  cd ..

}


if [ "$#" -eq 1 ]; then
  static $1
else
  for d in */; do
    static $d
  done
fi
