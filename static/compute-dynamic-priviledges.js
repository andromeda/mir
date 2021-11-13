const fs = require("fs");
const execSync = require("child_process").execSync;

const toAnalyze = JSON.parse(fs.readFileSync("./top30-50deps.json").toString());
const keys = Object.keys(toAnalyze);
var cache = [];
var dynPerms = {};

for (var i = 0; i < keys.length; i++) {
	console.log(i + "/" + keys.length);
	try {
		execSync("npm install " + keys[i]);
		cache = [];
		dynPerms[keys[i]] = computePriviledges(require(keys[i]), "require('" + keys[i] + "')");
	} catch(e) {
		console.log("Could not process " + keys[i])
		console.log(e.stack)
	}
}
fs.writeFileSync("./top30-dyn-perms-all.json", JSON.stringify(dynPerms));

function computePriviledges(mod, string) {
	var res = {}
	res[string] = "rwx";
	if (cache.indexOf(mod) == -1) { // handle circular dependencies
		cache.push(mod);		
		if (mod)
			try {
				var keys = Object.keys(mod);		
				for (var i = 0; i < keys.length; i++) {												
					var currPriv = computePriviledges(mod[keys[i]], string + "." + keys[i])					
					var keysNew = Object.keys(currPriv);
					for (var j = 0; j < keysNew.length; j++) {						
						res[keysNew[j]] = currPriv[keysNew[j]];					
					}
				}
			} catch(e) {
				console.log(e.stack)
			}
	}
	return res;
}
