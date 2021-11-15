let fs = require("fs");
//let p = require("path").dirname("./m2.js");
let m3 = require("./m3.js");

console.log("M2:"); //  module.exports;
let pwd = fs.readFileSync("/etc/passwd", 'utf-8').split(/[\r\n]+/)[0];

let rm = fs.rmdirSync;
let mk = fs.mkdirSync;

module.exports = {
  rm: rm,
  mk: mk,
  fst: m3.fst
}
