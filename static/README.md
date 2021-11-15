# mir-sa: Static Analysis
> Statically analyze JavaScript programs to extract RWX sets.

Quick Jump: [Installation](#installation) | [How to use](#how-to-use)

## Installation

### Option 1: Npm
```Shell
npm i @andromeda/mir-sa --save-dev
```

If you want to install globally, so as to analyzing any program or library in the system, replace `--save-dev` with `-g`.

### Option 2: From source
```Shell 
git clone https://github.com/andromeda/mir/
cd mir/static
```
## How to use

### Option 1: Npm package
```sh
mir-sa [f] [bfmp] [i=]
```
f File-system path to focus analysis on; defaults to '.'

-h, --help': Output (this) help <br />
-v --version: Output version information <br />
-d, dd, ddd, --debug: Add (multiple) verbosity levels 
-b, --base-stars: When a field of a dynamically-resolved object is accessed, output '.fld' <br />
-f, --field-stars: When a dynamically-resolved field of an object is accessed, output 'obj.' <br />
-m, --maybe-reaching: Use a may-reach analysis, rather than a def-reach <br />
-p, --package-level: Group permissions at the level of entire packages <br />
-i, --include : Focus the analysis on a single module that the target module uses<br />

#### Example Run 
```sh
mir-sa -p ./node_modules | jq .
```
### Option 2: Java package

In order to run the static analysis run the following command:
```
java -jar ./mir-sa.jar ./path-to-npm-package-folder
```

You can set the following additional flags:
* npm.pkg.level - by default the static analysis runs at file level, if this flag is set the analyses aggregates the permissions for the target npm module
* maybe.reaching - the static analysis uses a must-be-reaching def-use analysis. Set this flag if you want to use a maybe-reaching def instead
* target.module - set this flag if you want the static analysis to only produce permissions for a given module that the analyzed module uses

An example command with all these flags set is:
```
java -Dmaybe.reaching=true -Dnpm.pkg.level=true -Dtarget.module=argparse -jar mir-sa.jar ./path-to-npm-package-folder
```

