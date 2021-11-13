const fs = require("fs");
const execSync = require("child_process").execSync;

const toAnalyze = JSON.parse(fs.readFileSync("./top200-20deps.json").toString());
const keys = Object.keys(toAnalyze);
const OUT_FOLDER = "./downloaded/";

var staticPerms = {};

for (var i = 0; i < keys.length; i++) {
	console.log(i + "/" + keys.length);
	staticPerms[keys[i]] = {};
	var entry = toAnalyze[keys[i]];
	for (var j = 0; j < entry.length; j++) {
		console.log(j + "/" + entry.length + ":" + entry[j])
		var package = entry[j];
		try {
			execSync("curl `npm view " + package + " dist.tarball` > " + OUT_FOLDER + "/" + package + ".tar.gz; tar -xzf " + OUT_FOLDER + package + ".tar.gz -C " + OUT_FOLDER + "; mv " + OUT_FOLDER + "/package " + OUT_FOLDER + "/" + package + "; rm " + OUT_FOLDER + package + ".tar.gz", {timeout: 60000})
			var out = JSON.parse(execSync("java -Dnpm.pkg.level=true -Dtarget.module=" + keys[i] + " -jar mir-sa.jar " + OUT_FOLDER + package).toString().replace(/(.|[\r\n])*The policy is:/,"")); //-Dmaybe.reaching=true
			console.log(out);
			staticPerms[keys[i]][entry[j]] = out;					
		} catch(e) {
			console.log("Could not process " + keys[i])
			console.log(e.stack)
		}
	}
}
fs.writeFileSync("./top200-20deps-static-perms.json", JSON.stringify(staticPerms));
