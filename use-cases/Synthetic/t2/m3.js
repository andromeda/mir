let fs = require("fs");
let pwd = fs.readFileSync("/etc/passwd", 'utf-8');
console.log('hah')
module.exports = {
  fst: pwd.split(/[\r\n]+/)[0]
}
