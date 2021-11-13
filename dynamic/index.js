#!/usr/bin/env node

var pkg = require("./package.json");
var fs = require("fs");
var path = require('path');
//var cwd = process.cwd() + path.sep +  (process.argv[2] || '.');
let lya = require('@andromeda/lya');


var version = "v" + pkg.version + " (lya > 5ad6f93)";
var h = `Dynamically analyze JavaScript programs to extract or enforce RWX sets.

mir-da <fl> [dersp..]

  <fl>                        File to start analysis from; defaults to index.js if it exists

  -h,   --help:               Output (this) help 
  -V    --version:            Output version information
  -v, vv, vvv, --verbosity:   Add (multiple) verbosity levels

  -d,   --depth <n>:          Object depth to analyze (default 3)
  -e,   --enforce <f.json>:   Run in enforcement mode, where mir enforces access rules in <f.json>
  -r,   --report <f.json>:    Run in reporting mode, where mir simply reports on invalid accesses in <f.json>
  -s,   --save <f.json>:      File to output resuslts
  -p,   --print [<out, err>]: Stream to output results (defaults to file, see above)
  -w,   --with:		      Disable 'with' functionality

  --module-exclude <m>:       Comma-separated list of module IDs (absolute fs paths) to be excluded from the analysis
  --module-include <m>:       Comma-separated list of module IDs (absolute fs paths) to be included (assumes module-exclude='*')
  --context-exclude <c>:      Comma-separated context starting points to exclude from tracking (for contexts, see below)
  --context-include <c>:      Comma-separated context starting points to include in tracking  (assumes context-exclude='*')
  --prop-exclude <p>:         Comma-separated property names to exclude from analysis (e.g., 'Promise,toString,escape,setImmediate')
  --prop-include <p>:         Comma-separated property names to include in the  analysis (assumes prop-exclude='*')


  Contexts <c> are coarse groups of  program elements that are tracked, and fall
  under these categories (can be included in their long or short form):

  * module-locals, m:         Module-local names such as 'require'
  * node-globals, n:          All Node.js-related globals, such as 'console' and 'process'
  * es-globals, e:            All EcmaScript 6 globals names such Math.sin or 
  * user-globals, g:          User-defined globals accessed with a 'global' prefix, e.g., 'global.y = 3'
  * with-globals, w:          User-defined globals accessed without a prefix, e.g., 'y = 3' (expensive to track)

`;

let help = () => {console.log(h); }

if (require.main !== module) { 
  help();
  return false;
}

const conf = {
  inputString: false,
  depth: 3,
  analysis: path.join(__dirname, './src/analyses/rwx.js'),
  context: {
    enableWith: true,
    include: [
      'user-globals',
      'es-globals',
      'node-globals',
      'module-locals',
      'module-returns'],
    excludes: [],
  },
  modules: {
    include: null,
    excludes: null,
  },
  fields: {
    include: false,
    excludes: ['Promise', 'toString', 'escape', 'setImmediate'],
  },
};

const arg = require('arg');
const template = {
  // Types
  '--help':              Boolean,
  '--version':           Boolean,
  '--verbosity':         arg.COUNT,

  '--depth':             Number,
  '--enforce':           String,
  '--report':            String,
  '--save':              String,
  '--print':             Boolean,
  '--with':  		 Boolean, 
 
  '--module-exclude':    String,
  '--module-include':    String,
  '--context-exclude':   String,
  '--context-include':   String,
  '--prop-exclude':      String,
  '--prop-include':      String,

  // Aliases
  '-h':                  '--help',
  '-V':                  '--version',
  '-v':                  '--verbosity',
                                        
  '-d':                  '--depth',
  '-e':                  '--enforce',
  '-r':                  '--report',
  '-s':                  '--save',
  '-p':                  '--print',
  '-w': 		 '--with',
};

let args;
try {
  args = arg(template);
} catch (e) {
  console.log(e.message);
  process.exit();
}

const splitAdd = (a, separator, join) => {
    const comb = a.split(separator);
    const value = [];
    if (join) {
          comb.forEach((m) => value.push(path.join(__dirname, m)));
        } else {
              comb.forEach((m) => value.push(m));
            }
    return value;
};

if (args["--help"]){
  help();
  process.exit();
}

if (args["--version"]) {
  console.log("v" + pkg.version + " (extractor: 8799cd1)");
  process.exit();
}

if (args['--depth']) {
  conf.depth = args['--depth'];
}

if (args["--enforce"]) {
  conf.analysis = path.join(__dirname, './src/analyses/rwx-enforcement.js');
  conf.rules = path.join(process.cwd(), args['--enforce']);
}

if (args["--report"]) {
  conf.analysis = path.join(__dirname, './src/analyses/rwx-checking.js');
  conf.rules = path.join(process.cwd(), args['--report']);
}

if (args['--print']) {
  conf.print = args['--print'];
}

if (args['--save']) {
  if (args['--report']) {
    conf.SAVE_STATS = path.join(process.cwd(), args['--save']);
  } else {
    conf.SAVE_RESULTS = path.join(process.cwd(), args['--save']);
  }
}

if (args['--with']) {
  conf.context.enableWith = !args['--with'];
}
if (args['--module-include']) {
  conf.modules.include = splitAdd(args['--module-include'], ',', true);
}

if (args['--module-exclude']) {
  conf.modules.excludes = splitAdd(args['--module-exclude'], ',', true);
}

if (args['--context-include']) {
  conf.context.include = splitAdd(args['--context-include'], ',', false);
}

if (args['--context-exclude']) {
  const excl = splitAdd(args['--context-exclude'], ',', false);
  conf.context.include = conf.context.include.filter((name) => {
    if (excl.indexOf(name)) {
      return name;
    }
  });
  console.log(conf.context.excludes);
}

if (args['--prop-exclude']) {
  conf.fields.excludes = splitAdd(args['--prop-exclude'], ',', false);
}

if (args['--prop-include']) {
  conf.fields.include = splitAdd(args['--prop-include'], ',', false);
}

switch (args["_"].length) {
  case 0:
    cwd = process.cwd()
    // FIXME: choose index.json
    break;
  case 1:
    cwd = process.cwd() + path.sep +  args["_"][0]
    // FIXME check if file exists
    break;
  default:
    console.log("Too many ``extra'' parameters: " + args["_"].join(", "));
    process.exit(-1);
}
console.log(cwd)

lya.configRequire(require, conf);
require(cwd);


