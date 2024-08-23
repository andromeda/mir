# Preventing Dynamic Library Compromise on Node.js via RWX-Based Privilege Reduction
> Presenting MIR a system addressing dynamic compromise by introducing a fine-grained read-write-execute (RWX) permission model at the boundaries of libraries.

Quick Jump: [Installation](#installation) | [Run](#run) | [Repo Structure](#repo-stracture) | [Documentation](#documentation) | [Citing](#citing)

## Installation
This repo include both static and dynamic analysis tools. 

### Static Analysis

#### Option 1: Npm
```Shell
sudo apt install default-jre 
npm i @andromeda/mir-sa --save-dev # Then install mir-sa
```
If you want to install globally, so as to analyzing any program or library in the system, replace `--save-dev` with `-g`.

### Dynamic Analysis

#### Option 1: Npm
```Shell
npm i @andromeda/mir-da --save-dev
```

If you want to install globally, so as to analyzing any program or library in the system, replace `--save-dev` with `-g`.

#### Option 2: From source
```Shell 
git clone https://github.com/andromeda/mir/
cd mir/dynamic
npm install
```
## Run

In order to quickly run static analysis:
```sh
mir-sa -p ./node_modules | jq .
```
In order to quickly run dynamic analysis:
```sh
mir-da -p ./node_modules
```

## Repo Stracture

This repo hosts all the different components of the MIR paper. This repo is stractured as follows:

* [Static Analysis](./static)
* [Dynamic Analysis](./dynamic)
* [Vulnerabilities](./vulnerabilities)

## Documentation

* [Paper](http://nikos.vasilak.is/p/mir:ccs:2021.pdf)

## Citing

If you used Mir, consider citing the following paper:

```
@inproceedings{vasilakis2021preventing,
  title={Preventing dynamic library compromise on node. js via rwx-based privilege reduction},
  author={Vasilakis, Nikos and Staicu, Cristian-Alexandru and Ntousakis, Grigoris and Kallas, Konstantinos and Karel, Ben and DeHon, Andr{\'e} and Pradel, Michael},
  booktitle={Proceedings of the 2021 ACM SIGSAC Conference on Computer and Communications Security},
  pages={1821--1838},
  year={2021}
}
```
