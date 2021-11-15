#!/bin/bash

rmName() {

  cd $1

  cat static.json | sed "s;$(pwd);ANON;" > static.json

  cd ..
}

if [ "$#" -eq 1 ]; then
  rmName $1
else
  for d in */; do
    rmName $d
  done
fi
