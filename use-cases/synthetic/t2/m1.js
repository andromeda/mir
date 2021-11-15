let pwd = require("fs").readFileSync("/etc/passwd", 'utf-8').split(/[\r\n]+/)[0];

let p = require("./m2.js");

module.exports = {
  fst: pwd,
  snd: p.mk
}
