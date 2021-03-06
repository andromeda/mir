# mir-da: Dynamic Analysis
>Dynamically analyze JavaScript programs to extract or enforce RWX sets.

Quick Jump: [Installation](#installation) | [How to use](#how-to-use)

## Installation

### Option 1: Npm
```Shell
npm i @andromeda/mir-da --save-dev
```

If you want to install globally, so as to analyzing any program or library in the system, replace `--save-dev` with `-g`.

### Option 2: From source
```Shell 
git clone https://github.com/andromeda/mir/
cd mir/dynamic
npm install
```

## How to use
```sh
mir-da <fl> [bfmp] [i=<tm>]
```

&ensp;<fl>                        File to start analysis from; defaults to index.js if it exists

&emsp;  -h,   --help:               Output (this) help \
&emsp;  -V    --version:            Output version information \
&emsp;  -v, vv, vvv, --verbosity:   Add (multiple) verbosity levels 

&emsp;  -d,   --depth <n>:          Object depth to analyze (default 3) \
&emsp;  -e,   --enforce <f.json>:   Run in enforcement mode, where mir enforces access rules in <f.json> \
&emsp;  -r,   --report <f.json>:    Run in reporting mode, where mir simply reports on invalid accesses in <f.json>\
&emsp;  -s,   --save <f.json>:      File to output resuslts\
&emsp;  -p,   --print [<out, err>]: Stream to output results (defaults to file, see above)

&emsp;  --module-exclude <m>:       Comma-separated list of module IDs (absolute fs paths) to be excluded from the analysis\
&emsp;  --module-include <m>:       Comma-separated list of module IDs (absolute fs paths) to be included (assumes module-exclude='*')\
&emsp;  --context-exclude <c>:      Comma-separated context starting points to exclude from tracking (for contexts, see below)\
&emsp;  --context-include <c>:      Comma-separated context starting points to include in tracking  (assumes context-exclude='*')\
&emsp;  --prop-exclude \<p\>:         Comma-separated property names to exclude from analysis (e.g., 'Promise,toString,escape,setImmediate')\
&emsp;  --prop-include \<p\>:         Comma-separated property names to include in the  analysis (assumes prop-exclude='*')


&emsp;  Contexts <c> are coarse groups of  program elements that are tracked, and fall\
&emsp;  under these categories (can be included in their long or short form):

&emsp;  * module-locals, m:         Module-local names such as 'require'\
&emsp;  * node-globals, n:          All Node.js-related globals, such as 'console' and 'process'\
&emsp;  * es-globals, e:            All EcmaScript 6 globals names such Math.sin or \
&emsp;  * user-globals, g:          User-defined globals accessed with a 'global' prefix, e.g., 'global.y = 3'\
&emsp;  * with-globals, w:          User-defined globals accessed without a prefix, e.g., 'y = 3' (expensive to track)

