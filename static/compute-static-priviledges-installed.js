const fs = require("fs");
const execSync = require("child_process").execSync;

const toAnalyze = JSON.parse(fs.readFileSync("./top30-50deps.json").toString());
const keys = Object.keys(toAnalyze);
const OUT_FOLDER = "./node_modules/";

var staticPerms = {};

for (var i = 0; i < keys.length; i++) {
	console.log(i + "/" + keys.length);
	staticPerms[keys[i]] = {};
	var entry = toAnalyze[keys[i]];
	var currCount = 0;
	for (var j = 0; j < entry.length && currCount < 10; j++) {
		console.log(j + "/" + entry.length + ":" + entry[j])
		var package = entry[j];
		try {
			execSync("rm -rf ./node_modules/*");			
			//execSync("curl `npm view " + package + " dist.tarball` > " + OUT_FOLDER + "/" + package + ".tar.gz; tar -xzf " + OUT_FOLDER + package + ".tar.gz -C " + OUT_FOLDER + "; mv " + OUT_FOLDER + "/package " + OUT_FOLDER + "/" + package + "; rm " + OUT_FOLDER + package + ".tar.gz", {timeout: 60000})
			execSync("npm install " + package,  {stdio:[0,1,2], timeout: 120000});
			console.log("find "+ OUT_FOLDER + "/" + package +" -name '*.js' -exec cat {} + | grep 'require(." + keys[i] + ".)' | wc -l");
			var lines = execSync("find "+ OUT_FOLDER + "/" + package +" -name '*.js' -exec cat {} + | grep 'require(." + keys[i] + ".)' | wc -l").toString();
			console.log(lines)
			var noRequire = parseInt(lines);
			console.log(noRequire)
			if (noRequire > 0) {
				console.log("java -Dnpm.pkg.level=true -Dmaybe.reaching=true -Dtarget.module=" + keys[i] + " -jar mir-sa.jar " + OUT_FOLDER + package);		
				var out = JSON.parse(execSync("java -Dnpm.pkg.level=true -Dmaybe.reaching=true -Dtarget.module=" + keys[i] + " -jar mir-sa.jar " + OUT_FOLDER + package).toString().replace(/(.|[\r\n])*The policy is:/,"")); //-Dmaybe.reaching=true
				console.log(out);			
				staticPerms[keys[i]][entry[j]] = out;					
				currCount++;		
			}	
		} catch(e) {
			console.log("Could not process " + keys[i])
			console.log(e.stack)
		}
	}
	fs.writeFileSync("./top200-20deps-static-perms-installed.json", JSON.stringify(staticPerms));
}

